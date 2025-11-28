package com.github.herobrine.reseau;

import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Connection;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * 🔹 Classe GameServer
 * ------------------------------
 * Ce serveur utilise KryoNet pour gérer les connexions
 * des clients, recevoir des paquets (PacketOrder) et
 * les redistribuer à tous les clients connectés.
 */
public class GameServer {

    // 🔸 1. Attribut principal : le serveur réseau
    private Server server;
    private Map<Integer, PacketPlayer> players = new HashMap<>();
    private int expectedPlayers = 0;
    private String lobbyLevelPath = null;
    private boolean gameStarted = false;
    private final ConcurrentHashMap<Integer, RestartAckState> pendingRestartAcks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService restartScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "RestartAckMonitor");
        t.setDaemon(true);
        return t;
    });
    private volatile int currentRestartId = 0;
    private volatile String pendingRestartLevel = null;
    private static final long RESTART_RETRY_DELAY_MS = 1000L;
    private static final int MAX_RESTART_ATTEMPTS = 5;

    private static class RestartAckState {
        final int restartId;
        int attempts = 0;
        long lastSendTime = 0L;
        boolean acknowledged = false;

        RestartAckState(int restartId) {
            this.restartId = restartId;
        }
    }

    // 🔸 2. Constructeur : création et initialisation du serveur
    public GameServer() throws IOException {
        // Créer et démarrer le serveur
        server = new Server();
        Network.register(server);
        server.start();

        server.bind(Network.TCP_PORT, Network.UDP_PORT);


        // Ajouter un Listener pour gérer les événements réseau
        server.addListener(new Listener() {

            /** Quand un client se connecte */
            @Override
            public void connected(Connection c) {
                System.out.println("Client connecté : " + c.getID());

                // créer un joueur par défaut
                PacketPlayer p = new PacketPlayer(c.getID(), 0, 0, false);
                players.put(c.getID(), p);

                // envoyer l'état actuel aux autres
                broadcastAllPlayers();
                checkStartConditions();
            }

            @Override
            public void received(Connection c, Object o) {

                // le client envoie sa position et son état "dead"
                if (o instanceof PacketPlayer pkt) {

                    // mise à jour dans la liste serveur
                    PacketPlayer p = players.get(c.getID());
                    if (p != null) {
                        p.x = pkt.x;
                        p.y = pkt.y;
                        p.dead = pkt.dead;
                    }

                    // renvoyer l'état de TOUS les joueurs à TOUS les clients
                    broadcastAllPlayers();
                    return;
                }

                if (o instanceof PacketLobbyConfig config) {
                    gameStarted = false;
                    lobbyLevelPath = config.levelPath;
                    expectedPlayers = Math.max(0, config.expectedPlayers);
                    System.out.println("Configuration lobby reçue : " + lobbyLevelPath + " (" + expectedPlayers + " joueurs)");
                    checkStartConditions();
                    return;
                }

                if (o instanceof PacketDoorState doorState) {
                    server.sendToAllTCP(doorState);
                    return;
                }

                if (o instanceof PacketGameOver over) {
                    server.sendToAllTCP(over);
                    return;
                }
                if (o instanceof PacketRestartAck ack) {
                    handleRestartAck(c.getID(), ack.restartId);
                    return;
                }

                if (o instanceof PacketReturnToMenu returnToMenu) {
                    handleReturnToMenu(returnToMenu);
                    return;
                }
            }

            @Override
            public void disconnected(Connection c) {
                System.out.println("Client déconnecté : " + c.getID());

                players.remove(c.getID());
                pendingRestartAcks.remove(c.getID());

                PacketDisconnect pd = new PacketDisconnect();
                pd.id = c.getID();

                server.sendToAllTCP(pd);

                if (gameStarted) 
                {
                    PacketReturnToMenu pkt = new PacketReturnToMenu();
                    pkt.reason = "Un joueur s'est déconnecté.";
                    handleReturnToMenu(pkt);
                } else {
                    checkStartConditions();
                }
            }
        });

        restartScheduler.scheduleAtFixedRate(this::checkPendingRestartAcks,
                RESTART_RETRY_DELAY_MS,
                RESTART_RETRY_DELAY_MS,
                TimeUnit.MILLISECONDS);

        System.out.println("Serveur lancé sur le port " + Network.TCP_PORT);
    }

    /**
     * Envoie la liste complète des joueurs à tous les clients
     */
    private void broadcastAllPlayers() {

        for (PacketPlayer p : players.values()) {
            server.sendToAllTCP(p);
        }
    }

    private void checkStartConditions() {
        if (gameStarted) return;
        if (lobbyLevelPath == null || lobbyLevelPath.isBlank()) return;
        if (expectedPlayers <= 0) return;
        if (players.size() >= expectedPlayers) {
            startGame();
        }
    }

    private void startGame() {
        if (gameStarted) return;
        broadcastStartGame(lobbyLevelPath, expectedPlayers);
    }

    private void resetLobby() {
        gameStarted = false;
        lobbyLevelPath = null;
        expectedPlayers = 0;
        pendingRestartAcks.clear();
        pendingRestartLevel = null;
    }

    private void broadcastStartGame(String levelPath, int playerCount) {
        if (levelPath == null || levelPath.isBlank()) {
            System.out.println("Impossible d'envoyer un démarrage : niveau inconnu.");
            return;
        }
        PacketStartGame start = new PacketStartGame();
        start.levelPath = levelPath;
        start.playerCount = playerCount > 0 ? playerCount : players.size();
        System.out.println("Démarrage de la partie sur " + start.levelPath + " pour " + start.playerCount + " joueurs.");
        server.sendToAllTCP(start);
        gameStarted = true;
    }

    private void broadcastRestartRequest(String levelPath) {
        if (levelPath == null || levelPath.isBlank()) {
            System.out.println("Impossible d'envoyer un redémarrage : niveau inconnu.");
            return;
        }
        resetPlayersStateForRestart();
        currentRestartId++;
        pendingRestartLevel = levelPath;
        pendingRestartAcks.clear();
        Connection[] connections = server.getConnections();
        long now = System.currentTimeMillis();
        for (Connection conn : connections) {
            RestartAckState state = new RestartAckState(currentRestartId);
            state.attempts = 1;
            state.lastSendTime = now;
            pendingRestartAcks.put(conn.getID(), state);
            sendRestartRequest(conn.getID(), levelPath, currentRestartId);
        }
        if (connections.length == 0) {
            pendingRestartLevel = null;
        } else {
            System.out.println("Redémarrage demandé (" + connections.length + " clients) via requête #" + currentRestartId);
        }
        broadcastAllPlayers(); // diffuse l'état "vivant" remis à zéro
        gameStarted = true;
    }

    private void sendRestartRequest(int connectionId, String levelPath, int restartId) {
        if (levelPath == null || levelPath.isBlank()) {
            return;
        }
        PacketRestartRequest request = new PacketRestartRequest();
        request.levelPath = levelPath;
        request.playerCount = expectedPlayers > 0 ? expectedPlayers : players.size();
        request.restartId = restartId;
        server.sendToTCP(connectionId, request);
    }

    private void handleRestartAck(int connectionId, int restartId) {
        RestartAckState state = pendingRestartAcks.get(connectionId);
        if (state == null) {
            return;
        }
        if (state.restartId != restartId) {
            return;
        }
        pendingRestartAcks.remove(connectionId);
        System.out.println("Confirmation de redémarrage #" + restartId + " reçue du client " + connectionId + ".");
        if (pendingRestartAcks.isEmpty()) {
            pendingRestartLevel = null;
        }
    }

    private void checkPendingRestartAcks() {
        if (pendingRestartAcks.isEmpty()) {
            return;
        }
        if (pendingRestartLevel == null) {
            pendingRestartAcks.clear();
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, RestartAckState>> it = pendingRestartAcks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, RestartAckState> entry = it.next();
            RestartAckState state = entry.getValue();
            if (now - state.lastSendTime < RESTART_RETRY_DELAY_MS) {
                continue;
            }
        if (state.attempts >= MAX_RESTART_ATTEMPTS) {
            System.out.println("Client " + entry.getKey() + " n'a pas confirmé le redémarrage #" + state.restartId + " après " + state.attempts + " tentatives.");
            it.remove();
            continue;
        }
        sendRestartRequest(entry.getKey(), pendingRestartLevel, state.restartId);
        state.lastSendTime = now;
        state.attempts++;
        }
        if (pendingRestartAcks.isEmpty()) {
            pendingRestartLevel = null;
        }
    }

    private void resetPlayersStateForRestart() {
        for (PacketPlayer p : players.values()) {
            p.dead = false;
        }
    }

    public void restartGame(String levelPath) {
        String targetLevel = (levelPath != null && !levelPath.isBlank()) ? levelPath : lobbyLevelPath;
        if (targetLevel == null || targetLevel.isBlank()) {
            System.out.println("Impossible de redémarrer : aucun niveau actif.");
            return;
        }
        lobbyLevelPath = targetLevel;
        broadcastRestartRequest(targetLevel);
    }

    private void handleReturnToMenu(PacketReturnToMenu pkt) {
        server.sendToAllTCP(pkt);
        resetLobby();
        pendingRestartAcks.clear();
        pendingRestartLevel = null;
        currentRestartId = 0;
    }

    public void broadcastReturnToMenu(String reason) {
        PacketReturnToMenu pkt = new PacketReturnToMenu();
        pkt.reason = reason;
        handleReturnToMenu(pkt);
    }
    // 🔸 4. Méthode de nettoyage : arrêter le serveur proprement
    public void stop() {
        restartScheduler.shutdownNow();
        server.stop();
        server.close();
        System.out.println("🛑 Serveur arrêté.");
    }

    // 🔸 5. Point d’entrée principal
    public static void main(String[] args) {
        try {
            new GameServer(); // créer et lancer le serveur
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
