package com.github.herobrine.reseau;

public class PacketPlayer {

    public float x;     
    public float y;     
    public boolean dead;
    public int id;

    public PacketPlayer() {}

    public PacketPlayer(int id, float x, float y, boolean dead) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.dead = dead;
    }
}