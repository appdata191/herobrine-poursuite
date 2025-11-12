package com.github.herobrine.reseau;

import com.esotericsoftware.kryonet.*;
import java.io.IOException;

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

    public GameClient() throws IOException {
        this("localhost");
    }

    public GameClient(String host) throws IOException {
        this.host = host;

        // Tạo đối tượng client
        client = new Client();

        Network.register(client);

        client.start();

        client.connect(5000, host, Network.TCP_PORT, Network.UDP_PORT);

        connected = true;
        System.out.println("✅ Client connecté au serveur " + host + " !");

        client.addListener(new Listener() {

            @Override
            public void disconnected(Connection c) {
                connected = false;
                System.out.println("❌ Client déconnecté du serveur.");
            }

            @Override
            public void received(Connection c, Object o) {
                if (o instanceof PacketString packet) {
                    System.out.println("Serveur : " + packet.message);
                }
            }
        });
    }

    public void sendMessage(String message) {
        if (!connected) {
            System.out.println("⚠️ Erreur : client non connecté, impossible d'envoyer.");
            return;
        }

        PacketString packet = new PacketString(message);
        client.sendTCP(packet); // Gửi packet qua TCP
        System.out.println("📤 Envoyé : " + message);
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
}
