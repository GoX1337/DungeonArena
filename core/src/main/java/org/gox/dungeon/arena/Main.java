package org.gox.dungeon.arena;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Map<String, Animation<TextureRegion>> animations;
    private PlayerData localPlayer;
    private Map<Integer, PlayerData> remotePlayers;
    private Client client;
    private float speed = 200;
    private Texture groundStone;
    private Texture groundVariation1;
    private Texture groundVariation2;
    private List<Rectangle> variationPositions;
    private boolean isConnected = false;

    private final String[] directions = {
        "E", "NEE", "NE", "NNE", "N", "NNW", "NW", "NWW",
        "W", "SWW", "SW", "SSW", "S", "SSE", "SE", "SEE"
    };

    private final Map<String, String> directionAngles = new HashMap<String, String>() {{
        put("E", "0.0");    put("NEE", "22.5"); put("NE", "45.0");  put("NNE", "67.5");
        put("N", "90.0");   put("NNW", "112.5");put("NW", "135.0"); put("NWW", "157.5");
        put("W", "180.0");  put("SWW", "202.5");put("SW", "225.0"); put("SSW", "247.5");
        put("S", "270.0");  put("SSE", "292.5");put("SE", "315.0"); put("SEE", "337.5");
    }};

    @Override
    public void create() {
        batch = new SpriteBatch();
        animations = new HashMap<>();
        remotePlayers = new HashMap<>();
        variationPositions = new ArrayList<>();

        // Initialiser le joueur local même sans connexion
        localPlayer = new PlayerData(0, Gdx.graphics.getWidth() / 2 - 128, Gdx.graphics.getHeight() / 2 - 128, "idle", "S");
        localPlayer.stateTime = 0f;
        localPlayer.attackTimer = 0f;

        try {
            groundStone = new Texture(Gdx.files.internal("ground/ground_stone1.png"));
            groundVariation1 = new Texture(Gdx.files.internal("ground/ground_variation1.png"));
            groundVariation2 = new Texture(Gdx.files.internal("ground/ground_variation2.png"));
            Gdx.app.log("MainGame", "Textures du sol chargées avec succès");
        } catch (Exception e) {
            Gdx.app.error("MainGame", "Erreur lors du chargement des textures du sol : " + e.getMessage());
            groundStone = null;
            groundVariation1 = null;
            groundVariation2 = null;
        }

        loadAnimations();
        setupNetwork();
    }

    private void setupNetwork() {
        client = new Client();
        client.start();
        client.getKryo().register(PlayerData.class);
        client.getKryo().register(PlayerUpdate.class);
        client.getKryo().register(GameData.class);

        try {
            client.connect(5000, "localhost", 54555, 54777);
            Gdx.app.log("Client", "Connecté au serveur");
            isConnected = true;
        } catch (IOException e) {
            Gdx.app.error("Client", "Erreur de connexion : " + e.getMessage());
            // On garde le joueur local même sans connexion
            isConnected = false;
        }

        client.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof GameData) {
                    GameData gameData = (GameData) object;
                    localPlayer = gameData.player;
                    variationPositions = gameData.variationPositions;
                    Gdx.app.log("Client", "Données initiales reçues : joueur " + localPlayer.id + ", " + variationPositions.size() + " variations");
                } else if (object instanceof PlayerUpdate) {
                    PlayerUpdate update = (PlayerUpdate) object;
                    if (update.isDisconnect) {
                        remotePlayers.remove(update.id);
                        Gdx.app.log("Client", "Joueur " + update.id + " déconnecté");
                    } else if (localPlayer != null && update.id != localPlayer.id) {
                        PlayerData player = remotePlayers.get(update.id);
                        if (player != null) {
                            player.x = update.x;
                            player.y = update.y;
                            player.action = update.action;
                            player.direction = update.direction;
                        } else {
                            player = new PlayerData(update.id, update.x, update.y, update.action, update.direction);
                            remotePlayers.put(update.id, player);
                            Gdx.app.log("Client", "Nouveau joueur distant ajouté : " + update.id);
                        }
                    }
                }
            }
        });
    }

    private void loadAnimations() {
        String[] actions = {"idle", "walk", "attack"};

        for (String action : actions) {
            int frameCount = (action.equals("idle")) ? 1 : 8;
            for (String direction : directions) {
                try {
                    TextureRegion[] frames = new TextureRegion[frameCount];
                    String angle = directionAngles.get(direction);

                    for (int i = 0; i < frameCount; i++) {
                        String fileName = action + "/" + direction + "/knight_armed_" + action + "_" + direction + "_" + angle + "_" + i + ".png";
                        try {
                            Texture texture = new Texture(Gdx.files.internal(fileName));
                            frames[i] = new TextureRegion(texture, 0, 0, 256, 256);
                            Gdx.app.log("MainGame", "Chargé : " + fileName);
                        } catch (Exception e) {
                            Gdx.app.error("MainGame", "Erreur lors du chargement de " + fileName + " : " + e.getMessage());
                            // Fallback à une texture d'espacement
                            Texture fallbackTexture = new Texture(1, 1, Pixmap.Format.RGBA8888);
                            frames[i] = new TextureRegion(fallbackTexture);
                        }
                    }

                    Animation<TextureRegion> animation = new Animation<>(0.05f, frames);
                    animation.setPlayMode(Animation.PlayMode.LOOP);
                    animations.put(action + "_" + direction, animation);
                } catch (Exception e) {
                    Gdx.app.error("MainGame", "Erreur lors du chargement des animations pour " + action + "_" + direction + " : " + e.getMessage());
                }
            }
        }

        // Créer une animation par défaut pour éviter les erreurs
        createDefaultAnimation();

        Gdx.app.log("MainGame", "Animations chargées: " + animations.size());
    }

    private void createDefaultAnimation() {
        // Créer une animation par défaut pour l'état idle_S en cas d'échec de chargement
        if (!animations.containsKey("idle_S")) {
            Texture defaultTexture = new Texture(256, 256, Pixmap.Format.RGBA8888);
            TextureRegion[] frames = new TextureRegion[1];
            frames[0] = new TextureRegion(defaultTexture, 0, 0, 256, 256);
            Animation<TextureRegion> animation = new Animation<>(0.05f, frames);
            animations.put("idle_S", animation);
            Gdx.app.log("MainGame", "Animation par défaut créée pour idle_S");
        }
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float delta = Gdx.graphics.getDeltaTime();
        updatePlayerMovement();

        if (localPlayer != null) {
            localPlayer.stateTime += delta;
        }
        for (PlayerData player : remotePlayers.values()) {
            player.stateTime += delta;
        }

        batch.begin();

        // Dessiner le sol
        if (groundStone != null) {
            int screenWidth = Gdx.graphics.getWidth();
            int screenHeight = Gdx.graphics.getHeight();
            int tileSize = 256;

            for (int x = 0; x < screenWidth; x += tileSize) {
                for (int y = 0; y < screenHeight; y += tileSize) {
                    batch.draw(groundStone, x, y);
                }
            }
        } else {
            Gdx.app.error("MainGame", "Fond non dessiné : groundStone est null");
        }

        // Dessiner les variations du sol
        for (int i = 0; i < variationPositions.size(); i++) {
            Rectangle pos = variationPositions.get(i);
            Texture variation = (i % 2 == 0) ? groundVariation1 : groundVariation2;
            if (variation != null) {
                batch.draw(variation, pos.x, pos.y);
            }
        }

        // Dessiner les joueurs distants
        for (PlayerData player : remotePlayers.values()) {
            drawPlayer(player);
        }

        // Dessiner le joueur local
        if (localPlayer != null) {
            drawPlayer(localPlayer);
            Gdx.app.debug("Player", "Joueur local position: " + localPlayer.x + ", " + localPlayer.y +
                " action: " + localPlayer.action + " direction: " + localPlayer.direction);
        }

        batch.end();
    }

    private void drawPlayer(PlayerData player) {
        String animKey = player.action + "_" + player.direction;
        // Vérifier si l'animation existe
        if (!animations.containsKey(animKey)) {
            Gdx.app.error("MainGame", "Animation introuvable: " + animKey + ", utilisation de idle_S");
            animKey = "idle_S"; // Fallback à l'animation par défaut
        }

        Animation<TextureRegion> animation = animations.get(animKey);
        if (animation != null) {
            TextureRegion frame = animation.getKeyFrame(player.stateTime);
            if (frame != null && frame.getTexture() != null) {
                batch.draw(frame, player.x, player.y, 256, 256);
            } else {
                Gdx.app.error("MainGame", "Frame ou texture nulle pour " + animKey);
            }
        } else {
            Gdx.app.error("MainGame", "Animation nulle pour " + animKey);
        }
    }

    private void updatePlayerMovement() {
        if (localPlayer == null) return;

        float delta = Gdx.graphics.getDeltaTime();
        boolean isMoving = false;
        float moveX = 0;
        float moveY = 0;

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            localPlayer.action = "attack";
            localPlayer.stateTime = 0;
            localPlayer.attackTimer = 0.8f;
            sendUpdate();
        }

        if (localPlayer.action.equals("attack")) {
            localPlayer.attackTimer -= delta;
            if (localPlayer.attackTimer <= 0) {
                localPlayer.action = "idle";
                sendUpdate();
            }
        } else {
            if (Gdx.input.isKeyPressed(Input.Keys.W)) {
                moveY += speed * delta;
                isMoving = true;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.S)) {
                moveY -= speed * delta;
                isMoving = true;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.D)) {
                moveX += speed * delta;
                isMoving = true;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.A)) {
                moveX -= speed * delta;
                isMoving = true;
            }

            if (moveX != 0 && moveY != 0) {
                float diagonalSpeed = speed * delta / (float)Math.sqrt(2);
                moveX = Math.signum(moveX) * diagonalSpeed;
                moveY = Math.signum(moveY) * diagonalSpeed;
            }

            localPlayer.x += moveX;
            localPlayer.y += moveY;

            if (isMoving) {
                if (moveX > 0 && moveY > 0) {
                    localPlayer.direction = "NE";
                } else if (moveX < 0 && moveY > 0) {
                    localPlayer.direction = "NW";
                } else if (moveX > 0 && moveY < 0) {
                    localPlayer.direction = "SE";
                } else if (moveX < 0 && moveY < 0) {
                    localPlayer.direction = "SW";
                } else if (moveX > 0) {
                    localPlayer.direction = "E";
                } else if (moveX < 0) {
                    localPlayer.direction = "W";
                } else if (moveY > 0) {
                    localPlayer.direction = "N";
                } else if (moveY < 0) {
                    localPlayer.direction = "S";
                }
                localPlayer.action = "walk";
            } else {
                localPlayer.action = "idle";
            }
            sendUpdate();
        }
    }

    private void sendUpdate() {
        if (client != null && localPlayer != null && isConnected) {
            client.sendTCP(new PlayerUpdate(localPlayer));
        }
    }

    @Override
    public void dispose() {
        batch.dispose();

        if (groundStone != null) groundStone.dispose();
        if (groundVariation1 != null) groundVariation1.dispose();
        if (groundVariation2 != null) groundVariation2.dispose();

        for (Animation<TextureRegion> animation : animations.values()) {
            for (TextureRegion frame : animation.getKeyFrames()) {
                if (frame != null && frame.getTexture() != null) {
                    frame.getTexture().dispose();
                }
            }
        }

        if (client != null) {
            client.stop();
            client.close();
        }
    }
}
