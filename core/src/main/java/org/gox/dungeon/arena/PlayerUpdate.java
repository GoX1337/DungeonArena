package org.gox.dungeon.arena;

public class PlayerUpdate {

    public int id;
    public float x;
    public float y;
    public String action;
    public String direction;
    public boolean isDisconnect;

    // Constructeur par défaut nécessaire pour Kryo
    public PlayerUpdate() {
        this.isDisconnect = false;
    }

    // Constructeur pour déconnexion
    public PlayerUpdate(int id) {
        this.id = id;
        this.isDisconnect = true;
    }

    // Constructeur pour mise à jour normale
    public PlayerUpdate(PlayerData player) {
        this.id = player.id;
        this.x = player.x;
        this.y = player.y;
        this.action = player.action;
        this.direction = player.direction;
        this.isDisconnect = false;
    }
}
