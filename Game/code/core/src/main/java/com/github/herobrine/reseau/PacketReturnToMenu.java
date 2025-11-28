package com.github.herobrine.reseau;

/**
 * Signal envoyé lorsqu'un joueur retourne au menu principal.
 * Permet de synchroniser la sortie de partie entre tous les clients.
 */
public class PacketReturnToMenu {
    public String reason;
}
