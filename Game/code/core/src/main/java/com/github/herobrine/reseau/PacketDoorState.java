package com.github.herobrine.reseau;

public class PacketDoorState {
    public int doorId;
    public boolean open;

    public PacketDoorState() {}

    public PacketDoorState(int doorId, boolean open) {
        this.doorId = doorId;
        this.open = open;
    }
}
