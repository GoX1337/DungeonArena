package org.gox.dungeon.arena;

import com.badlogic.gdx.math.Rectangle;
import java.util.List;

public class GameData {
    public PlayerData player;
    public List<Rectangle> variationPositions;

    // Constructeur par défaut nécessaire pour Kryo
    public GameData() {
    }

    public GameData(PlayerData player, List<Rectangle> variationPositions) {
        this.player = player;
        this.variationPositions = variationPositions;
    }
}
