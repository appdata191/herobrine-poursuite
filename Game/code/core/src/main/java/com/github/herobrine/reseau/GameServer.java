package com.github.herobrine.reseau;

import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Connection;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


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
            }

            @Override
            public void disconnected(Connection c) {
                System.out.println("Client déconnecté : " + c.getID());

                players.remove(c.getID());

                PacketDisconnect pd = new PacketDisconnect();
                pd.id = c.getID();

                server.sendToAllTCP(pd);

                 if (gameStarted) 
                 {
                     PacketGameOver over = new PacketGameOver();
                     over.reason = "Un joueur s'est déconnecté.";
                     server.sendToAllTCP(over);
                     resetLobby();
                 } else {
                     checkStartConditions();
                 }
            }
        });

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
        gameStarted = true;
        PacketStartGame start = new PacketStartGame();
        start.levelPath = lobbyLevelPath;
        start.playerCount = expectedPlayers;
        System.out.println("Démarrage de la partie sur " + start.levelPath + " pour " + start.playerCount + " joueurs.");
        
        server.sendToAllTCP(start);
    }

    private void resetLobby() {
        gameStarted = false;
        lobbyLevelPath = null;
        expectedPlayers = 0;
    }
    // 🔸 4. Méthode de nettoyage : arrêter le serveur proprement
    public void stop() {
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
