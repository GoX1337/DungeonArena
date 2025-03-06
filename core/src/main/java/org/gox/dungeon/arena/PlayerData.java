package org.gox.dungeon.arena;

public class PlayerData {
    public int id;
    public float x, y;
    public String action, direction;
    public float stateTime = 0; // Pour l’animation
    public float attackTimer = 0.8f; // Timer pour l’attaque

    public PlayerData() {}
    public PlayerData(int id, float x, float y, String action, String direction) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.action = action;
        this.direction = direction;
    }
}
