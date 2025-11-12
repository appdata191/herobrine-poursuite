package com.github.herobrine.reseau;

import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.kryonet.*;
import com.esotericsoftware.kryo.Kryo;
import java.io.IOException;

/**
 * 🔹 Classe GameServer
 * ------------------------------
 * Ce serveur utilise KryoNet pour gérer les connexions
 * des clients, recevoir des paquets (PacketString) et
 * les redistribuer à tous les clients connectés.
 */
public class GameServer {

    // 🔸 1. Attribut principal : le serveur réseau
    private Server server;

    // 🔸 2. Constructeur : création et initialisation du serveur
    public GameServer() throws IOException {
        // Créer et démarrer le serveur
        server = new Server();
        server.start();

        // Ouvrir les ports TCP et UDP définis dans Network
        server.bind(Network.TCP_PORT, Network.UDP_PORT);

        // Récupérer l’instance Kryo et enregistrer les classes (packets)
        Kryo kryo = server.getKryo();
        kryo.register(PacketString.class);

        // Ajouter un Listener pour gérer les événements réseau
        server.addListener(new Listener() {

            /** Quand un client se connecte */
            @Override
            public void connected(Connection c) {
                System.out.println("Client connecté : " + c.getID());
            }

            /** Quand le serveur reçoit un objet du client */
            @Override
            public void received(Connection c, Object o) {
                if (o instanceof PacketString packet) {
                    System.out.println("Reçu du client " + c.getID() + " : " + packet.message);
                    // Envoyer le message à tous les autres clients
                    broadcast(packet);
                }
            }

            /** Quand un client se déconnecte */
            @Override
            public void disconnected(Connection c) {
                System.out.println("Client déconnecté : " + c.getID());
            }
        });

        System.out.println("✅ Serveur en cours d’exécution sur le port TCP " + Network.TCP_PORT);
    }

    // 🔸 3. Méthode de traitement : envoyer un packet à tous les clients
    public void broadcast(Object packet) {
        server.sendToAllTCP(packet);
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