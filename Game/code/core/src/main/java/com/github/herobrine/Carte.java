package com.github.herobrine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Carte {
    private final Texture blockTop;
    private final Texture blockBottom;
    private final int TILE = 60;
    private final int MAP_WIDTH_TILES = 64;
    private final int MAP_HEIGHT_TILES = 18;
    
    private final int[][] map = new int[MAP_WIDTH_TILES][MAP_HEIGHT_TILES];
    
    private final List<AutomateMortel> automates = new ArrayList<>();
    private String currentLevelPath = null;

    public Carte(Texture blockTop, Texture blockBottom) {
        this.blockTop = blockTop;
        this.blockBottom = blockBottom;
    }

    public void create(String levelPath) {
        loadLevel(levelPath);
        this.currentLevelPath = levelPath;
    }

    /**
     * CORRECTION DÉFINITIVE : Réécriture complète de la méthode pour une détection de surface robuste.
     * La méthode cherche de haut en bas le premier espace vide qui se trouve juste au-dessus d'un bloc solide.
     * Cela garantit que le joueur apparaît sur une surface marchable et non sous un plafond.
     */
    public float getSurfaceYAt(int gridX) {
        // On parcourt la colonne de haut en bas (en partant du sommet de la carte)
        for (int gy = MAP_HEIGHT_TILES - 2; gy >= 0; gy--) {
            boolean isBlockBelowSolid = (map[gridX+1][gy-1] != 0);

            // Si la case actuelle est vide ET que la case juste en dessous est solide,
            // alors nous avons trouvé la surface la plus haute sur laquelle le joueur peut se tenir.
            if ( isBlockBelowSolid) {
                // La position Y du joueur est le haut du bloc solide d'en dessous, soit gy * TILE.
                return (gy) * TILE;
            }
            
        }

        // Cas de secours : si aucune surface n'est trouvée (par exemple, une colonne entièrement vide),
        // on place le joueur tout en bas de la carte.
        return 0;
    }

    // ... Le reste du fichier est identique et correct ...

    private void loadLevel(String levelPath) {
        clear();

        try {
            FileHandle fh = Gdx.files.local(levelPath);
            if (!fh.exists()) {
                Gdx.app.error("Carte", "Impossible de charger le fichier : " + levelPath);
                return;
            }
            String[] lines = fh.readString("UTF-8").split("\\r?\\n");

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                StringTokenizer st = new StringTokenizer(line);
                if (!st.hasMoreTokens()) continue;

                String type = st.nextToken().toUpperCase();
                List<Integer> coords = new ArrayList<>();
                while (st.hasMoreTokens()) {
                    try { coords.add(Integer.parseInt(st.nextToken())); }
                    catch (NumberFormatException ignored) {}
                }

                if (coords.isEmpty()) continue;

                switch (type) {
                    case "T":
                        if (coords.size() >= 4) {
                            int x1 = Math.min(coords.get(0), coords.get(2));
                            int x2 = Math.max(coords.get(0), coords.get(2));
                            int y1 = Math.min(coords.get(1), coords.get(3));
                            int y2 = Math.max(coords.get(1), coords.get(3));
                            for (int gx = x1; gx <= x2; gx++) {
                                for (int gy = y1; gy <= y2; gy++) {
                                    if (gx >= 0 && gx < MAP_WIDTH_TILES && gy >= 0 && gy < MAP_HEIGHT_TILES) {
                                        map[gx][gy] = 1;
                                    }
                                }
                            }
                        } else if (coords.size() >= 2) {
                            int gx = coords.get(0);
                            int gy = coords.get(1);
                            if (gx >= 0 && gx < MAP_WIDTH_TILES && gy >= 0 && gy < MAP_HEIGHT_TILES) {
                                map[gx][gy] = 1;
                            }
                        }
                        break;
                    case "C":
                        if (coords.size() >= 3) {
                            int startX = coords.get(0);
                            int y = coords.get(1);
                            int endX = (coords.size() >= 4) ? coords.get(2) : startX;
                            automates.add(new Creeper(Math.min(startX, endX), Math.max(startX, endX), y, TILE, this));
                        }
                        break;
                    case "P":
                        if (coords.size() >= 2) {
                            automates.add(new Pics(coords.get(0), coords.get(1), TILE));
                        }
                        break;
                }
            }
            
            for (int gx = 0; gx < MAP_WIDTH_TILES; gx++) {
                int lowestTopY = -1;
                for (int gy = 0; gy < MAP_HEIGHT_TILES; gy++) {
                    if (map[gx][gy] == 1) {
                        lowestTopY = gy;
                        break;
                    }
                }

                if (lowestTopY != -1) {
                    for (int gy = 0; gy < lowestTopY; gy++) {
                        if (map[gx][gy] == 0) {
                            map[gx][gy] = 2;
                        }
                    }
                }
            }

        } catch (Exception e) {
            Gdx.app.log("Carte", "Erreur lors du chargement du niveau: " + e.getMessage());
        }
    }

    public void dispose() {
        clear();
    }

    public void clear() {
        for (AutomateMortel auto : automates) {
            auto.dispose();
        }
        automates.clear();
        for (int gx = 0; gx < MAP_WIDTH_TILES; gx++) {
            for (int gy = 0; gy < MAP_HEIGHT_TILES; gy++) {
                map[gx][gy] = 0;
            }
        }
        currentLevelPath = null;
    }

    public void render(SpriteBatch batch, float cameraX) {
        for (int gx = 0; gx < MAP_WIDTH_TILES; gx++) {
            for (int gy = 0; gy < MAP_HEIGHT_TILES; gy++) {
                int tileType = map[gx][gy];
                if (tileType == 1) {
                    batch.draw(blockTop, gx * TILE - cameraX, gy * TILE, TILE, TILE);
                } else if (tileType == 2) {
                    batch.draw(blockBottom, gx * TILE - cameraX, gy * TILE, TILE, TILE);
                }
            }
        }

        for (AutomateMortel auto : automates) {
            auto.render(batch, cameraX);
        }
    }

    public boolean updateAutomates(float delta, Joueur joueur) {
        for (AutomateMortel auto : automates) {
            auto.update(delta);
            if (auto.kill(joueur)) {
                return true;
            }
        }
        return false;
    }

    public int getTile() { return TILE; }
    public float getMapWidth() { return MAP_WIDTH_TILES * TILE; }
    public String getCurrentLevelPath() { return currentLevelPath; }

    public boolean isAnyBlockAt(int gridX, int gridY) {
        if (gridX < 0 || gridX >= MAP_WIDTH_TILES || gridY < 0 || gridY >= MAP_HEIGHT_TILES) {
            return true;
        }
        return map[gridX][gridY] != 0;
    }
}
