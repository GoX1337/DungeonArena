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

public class DungeonArenaServer {

    private Server server;
    private Map<Integer, PlayerData> players;
    private List<Rectangle> variationPositions;

    public DungeonArenaServer() {
        server = new Server();
        players = new HashMap<>();
        variationPositions = new ArrayList<>();

        // Enregistrer toutes les classes nécessaires
        server.getKryo().register(PlayerData.class);
        server.getKryo().register(PlayerUpdate.class);
        server.getKryo().register(GameData.class);
        server.getKryo().register(ArrayList.class);
        server.getKryo().register(Rectangle.class);

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

                // Créer un nouveau joueur au centre de l'écran
                // On utilise des valeurs fixes pour le centre de l'écran, à adapter à votre taille d'écran
                float startX = 400;  // Centre en X (à adapter selon la taille de votre écran)
                float startY = 300;  // Centre en Y (à adapter selon la taille de votre écran)

                PlayerData newPlayer = new PlayerData(connection.getID(), startX, startY, "idle", "S");
                players.put(connection.getID(), newPlayer);

                // Envoyer les données initiales du jeu au nouveau joueur
                GameData gameData = new GameData(newPlayer, variationPositions);
                connection.sendTCP(gameData);

                // Envoyer tous les joueurs existants au nouveau joueur
                for (PlayerData existingPlayer : players.values()) {
                    if (existingPlayer.id != connection.getID()) {
                        connection.sendTCP(new PlayerUpdate(existingPlayer));
                    }
                }

                // Annoncer le nouveau joueur à tous les autres joueurs
                broadcastNewPlayer(newPlayer, connection.getID());
                System.out.println("Joueur " + connection.getID() + " initialisé à la position (" +
                    startX + ", " + startY + ")");
            }

            @Override
            public void disconnected(Connection connection) {
                System.out.println("Joueur déconnecté : " + connection.getID());
                players.remove(connection.getID());

                // Créer une mise à jour de déconnexion
                PlayerUpdate disconnectUpdate = new PlayerUpdate();
                disconnectUpdate.id = connection.getID();
                disconnectUpdate.isDisconnect = true;

                // Annoncer la déconnexion à tous les autres joueurs
                server.sendToAllTCP(disconnectUpdate);
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

                        // Créer une nouvelle mise à jour avec l'ID serveur
                        PlayerUpdate serverUpdate = new PlayerUpdate(player);

                        // Envoyer la mise à jour à tous les autres joueurs
                        broadcastPlayerUpdate(serverUpdate, connection.getID());
                    }
                }
            }
        });
    }

    private void placeGroundVariations() {
        int screenWidth = 800;  // Taille de l'écran
        int screenHeight = 600; // Taille de l'écran
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

    private void broadcastNewPlayer(PlayerData player, int sourceId) {
        PlayerUpdate update = new PlayerUpdate(player);
        for (Connection conn : server.getConnections()) {
            if (conn.getID() != sourceId) {
                conn.sendTCP(update);
                System.out.println("Nouveau joueur " + player.id + " annoncé au joueur " + conn.getID());
            }
        }
    }

    private void broadcastPlayerUpdate(PlayerUpdate update, int sourceId) {
        for (Connection conn : server.getConnections()) {
            if (conn.getID() != sourceId) {
                conn.sendTCP(update);
            }
        }
    }

    public static void main(String[] args) {
        new DungeonArenaServer();
    }
}
