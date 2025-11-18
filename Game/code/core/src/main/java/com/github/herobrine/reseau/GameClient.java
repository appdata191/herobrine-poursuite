package com.github.herobrine.reseau;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Connection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 🔹 Classe GameClient
 * ------------------------------
 * Ce client se connecte au serveur via KryoNet,
 * envoie des paquets (PacketString) et affiche les réponses.
 */
public class GameClient {

    // 🔸 Attributs
    public Client client;
    public boolean connected = false;
    private final String host;
    int myId;
    private final Map<Integer, PacketPlayer> remotePlayers = new ConcurrentHashMap<>();
    private volatile PacketStartGame pendingStartGame;
    private volatile PacketGameOver pendingGameOver;

    public GameClient() throws IOException 
    {
        this("localhost");
    }

    public GameClient(String host) throws IOException 
    {
        this.host = host;

        client = new Client();

        Network.register(client);

        client.addListener(new Listener() {
            
            @Override
            public void connected(Connection c) {
                myId = c.getID();

                connected = true;
            }               

            @Override
            public void disconnected(Connection c) {
                connected = false;
                System.out.println("❌ Client déconnecté du serveur.");
            }

            @Override
            public void received(Connection c, Object o) {
                if (o instanceof PacketPlayer pkt) {

                    if (pkt.id == myId) return;

                    remotePlayers.put(pkt.id, pkt);
                }

                if (o instanceof PacketDisconnect pd) {
                    System.out.println("Player " + pd.id + " disconnected, removing.");
                    remotePlayers.remove(pd.id);
                }

                if (o instanceof PacketStartGame start) {
                    pendingStartGame = start;
                    System.out.println("Received start game packet.");
                }

                if (o instanceof PacketGameOver over) {
                    pendingGameOver = over;
                }
            }
        });

        client.start();

        client.connect(5000, host, Network.TCP_PORT, Network.UDP_PORT);

        connected = true;
        System.out.println("✅ Client connecté au serveur " + host + " !");
    }

    public void sendPlayerState(float x, float y, boolean dead) 
    {
        if (!connected) return;

        PacketPlayer pkt = new PacketPlayer();
        pkt.id = myId;
        pkt.x = x;
        pkt.y = y;
        pkt.dead = dead;

        client.sendTCP(pkt);
    }

    public void stop() {
        if (client != null) {
            client.stop();
            connected = false;
            System.out.println("🛑 Client arrêté.");
        }
    }

    public String getHost() {
        return host;
    }

    public Map<Integer, PacketPlayer> getRemotePlayersSnapshot() {
        return new HashMap<>(remotePlayers);
    }

    public void sendLobbyConfig(String levelPath, int expectedPlayers) {
        if (!connected) return;
        PacketLobbyConfig cfg = new PacketLobbyConfig();
        cfg.levelPath = levelPath;
        cfg.expectedPlayers = expectedPlayers;
        client.sendTCP(cfg);
    }

    public PacketStartGame pollStartGamePacket() {
        PacketStartGame pkt = pendingStartGame;
        pendingStartGame = null;
        return pkt;
    }

    public PacketGameOver pollGameOverPacket() {
        PacketGameOver pkt = pendingGameOver;
        pendingGameOver = null;
        return pkt;
    }
}
