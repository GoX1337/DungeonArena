package org.gox.dungeon.arena;

// Classe pour les mises à jour réseau
class PlayerUpdate {
    public int id;
    public float x, y;
    public String action, direction;
    public boolean isDisconnect = false;

    public PlayerUpdate() {
    } // Constructeur par défaut requis pour KryoNet

    public PlayerUpdate(PlayerData player) {
        this.id = player.id;
        this.x = player.x;
        this.y = player.y;
        this.action = player.action;
        this.direction = player.direction;
    }

    public PlayerUpdate(int id) { // Pour les déconnexions
        this.id = id;
        this.isDisconnect = true;
    }
}
