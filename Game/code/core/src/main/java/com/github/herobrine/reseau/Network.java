package com.github.herobrine.reseau;

import com.esotericsoftware.kryonet.EndPoint;
import com.esotericsoftware.kryo.Kryo;


/**
 * Classe Network
 * ------------------------------
 * Gère la configuration réseau commune entre le client et le serveur :
 * - les ports TCP et UDP utilisés pour la communication
 * - l’enregistrement des classes (packets) pouvant être envoyées par KryoNet
 */
public class Network {
    // 🔹 Ports réseau (doivent être identiques côté client et côté serveur)
    public static final int TCP_PORT = 54555;
    public static final int UDP_PORT = 54777;

    /**
     * Méthode statique permettant d’enregistrer toutes les classes
     * de données (packets) qui seront échangées entre client et serveur.
     *
     * @param endPoint : instance du Client ou du Server
     */
    public static void register(EndPoint endPoint) {
        Kryo kryo = endPoint.getKryo();

        // 🔹 Enregistrer ici toutes les classes de packets autorisées
        kryo.register(PacketPlayer.class);
        kryo.register(PacketDisconnect.class);
        kryo.register(PacketLobbyConfig.class);
        kryo.register(PacketStartGame.class);
        kryo.register(PacketGameOver.class);
        kryo.register(PacketDoorState.class);
        kryo.register(PacketRestartRequest.class);
        kryo.register(PacketRestartAck.class);

    }
}
