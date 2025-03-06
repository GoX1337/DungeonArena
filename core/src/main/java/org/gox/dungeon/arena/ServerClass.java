package org.gox.dungeon.arena;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerClass {
    private Server server;
    private Map<Integer, PlayerData> players;
    private List<Rectangle> variationPositions;

    public ServerClass() {
        server = new Server();
        players = new HashMap<>();
        variationPositions = new ArrayList<>();
        server.getKryo().register(PlayerData.class);
        server.getKryo().register(PlayerUpdate.class);
        server.getKryo().register(GameData.class);

        try {
            server.start();
            server.bind(54555, 54777);
            System.out.println("Serveur démarré sur les ports 54555 (TCP) et 54777 (UDP)");
        } catch (IOException e) {
            System.err.println("Erreur lors du démarrage du serveur : " + e.getMessage());
        }

        placeGroundVariations();

        server.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                System.out.println("Nouveau joueur connecté : " + connection.getID());
                PlayerData newPlayer = new PlayerData(connection.getID(), 100, 100, "idle", "S");
                players.put(connection.getID(), newPlayer);
                GameData gameData = new GameData(newPlayer, variationPositions);
                connection.sendTCP(gameData);
                broadcastPlayerUpdate(newPlayer, connection.getID());
            }

            @Override
            public void disconnected(Connection connection) {
                System.out.println("Joueur déconnecté : " + connection.getID());
                players.remove(connection.getID());
                broadcastPlayerUpdate(null, connection.getID());
            }

            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof PlayerUpdate) {
                    PlayerUpdate update = (PlayerUpdate) object;
                    PlayerData player = players.get(connection.getID());
                    if (player != null) {
                        player.x = update.x;
                        player.y = update.y;
                        player.action = update.action;
                        player.direction = update.direction;
                        broadcastPlayerUpdate(player, connection.getID());
                    }
                }
            }
        });
    }

    private void placeGroundVariations() {
        int screenWidth = 800;  // Remplace par la largeur de ton écran
        int screenHeight = 600; // Remplace par la hauteur de ton écran
        int tileSize = 256;
        int maxVariations = 10;

        for (int i = 0; i < maxVariations; i++) {
            float x, y;
            Rectangle newRect;
            boolean overlaps;

            int attempts = 0;
            do {
                x = MathUtils.random(0, screenWidth - tileSize);
                y = MathUtils.random(0, screenHeight - tileSize);
                newRect = new Rectangle(x, y, tileSize, tileSize);
                overlaps = false;

                for (Rectangle existing : variationPositions) {
                    if (newRect.overlaps(existing)) {
                        overlaps = true;
                        break;
                    }
                }
                attempts++;
                if (attempts > 100) {
                    System.out.println("Impossible de placer toutes les variations sans superposition");
                    return;
                }
            } while (overlaps);

            variationPositions.add(newRect);
        }
        System.out.println("Variations placées : " + variationPositions.size());
    }

    private void broadcastPlayerUpdate(PlayerData player, int excludeId) {
        for (Connection conn : server.getConnections()) {
            if (conn.getID() != excludeId) {
                conn.sendTCP(player != null ? new PlayerUpdate(player) : new PlayerUpdate(excludeId));
            }
        }
    }

    public static void main(String[] args) {
        new ServerClass();
    }
}
