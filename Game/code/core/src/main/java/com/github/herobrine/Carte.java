package com.github.herobrine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

public class Carte {
    private Texture blockTop;
    private Texture blockBottom;
    private Texture background;

    private final int TILE = 60;
    private int cols, rows;
    private int mapCols;
    private int mapRows;
    private int groundRow = 5;

    private Block[][] blocks;
    private String currentLevelPath;

    private final List<AutomateMortel> automates = new ArrayList<>();

    private static class Raw {
        final String type;
        final List<Integer> coords;
        Raw(String type, List<Integer> coords) { this.type = type; this.coords = coords; }
    }

    public void create() {
        create(null);
    }

    public void create(String levelPath) {
        blockTop = new Texture("Bloc du dessus.png");
        blockBottom = new Texture("Bloc du dessous.png");
        background = new Texture("Fond_simple.png");

        cols = (int) Math.ceil((float) Gdx.graphics.getWidth() / TILE);
        rows = (int) Math.ceil((float) Gdx.graphics.getHeight() / TILE);

        mapCols = cols * 2;
        mapRows = rows;

        ensureArrays(mapCols, mapRows);

        if (levelPath != null && !levelPath.isEmpty()) {
            this.currentLevelPath = levelPath;
            loadLevel(levelPath);
        } else {
            Gdx.app.log("Carte", "Aucun chemin de niveau fourni, la carte est vide.");
        }
    }

    private void ensureArrays(int w, int h) {
        if (blocks != null && blocks.length >= w && blocks[0].length >= h) return;
        Block[][] nBlocks = new Block[w][h];
        if (blocks != null) {
            int copyW = Math.min(blocks.length, nBlocks.length);
            int copyH = Math.min(blocks[0].length, nBlocks[0].length);
            for (int i = 0; i < copyW; i++) {
                System.arraycopy(blocks[i], 0, nBlocks[i], 0, copyH);
            }
        }
        blocks = nBlocks;
        mapCols = w;
        mapRows = h;
    }

    private void loadLevel(String path) {
        try {
            FileHandle fh = Gdx.files.local(path);
            if (!fh.exists()) {
                Gdx.app.error("Carte", "Fichier de niveau non trouve : " + path);
                return;
            }
            String[] lines = fh.readString("UTF-8").split("\\r?\\n");

            List<Raw> raws = new ArrayList<>();
            int maxX = mapCols - 1;
            int maxY = mapRows - 1;

            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                StringTokenizer st = new StringTokenizer(line);
                if (!st.hasMoreTokens()) continue;
                String type = st.nextToken().toUpperCase();
                List<Integer> coords = new ArrayList<>();
                while (st.hasMoreTokens()) {
                    String tk = st.nextToken();
                    try { coords.add(Integer.parseInt(tk)); }
                    catch (NumberFormatException ignored) { }
                }
                if (coords.size() == 0) continue;
                if (coords.size() >= 2) {
                    if (coords.size() == 2) {
                        maxX = Math.max(maxX, coords.get(0));
                        maxY = Math.max(maxY, coords.get(1));
                    } else {
                        maxX = Math.max(maxX, Math.max(coords.get(0), coords.get(2)));
                        maxY = Math.max(maxY, Math.max(coords.get(1), coords.get(3)));
                    }
                }
                raws.add(new Raw(type, coords));
            }

            int explicitMaxX = mapCols - 1;
            int explicitMaxY = mapRows - 1;
            for (Raw r : raws) {
                List<Integer> c = r.coords;
                if (c.size() >= 2) {
                    if (c.size() == 2) {
                        explicitMaxX = Math.max(explicitMaxX, c.get(0));
                        explicitMaxY = Math.max(explicitMaxY, c.get(1));
                    } else {
                        explicitMaxX = Math.max(explicitMaxX, Math.max(c.get(0), c.get(2)));
                        explicitMaxY = Math.max(explicitMaxY, Math.max(c.get(1), c.get(3)));
                    }
                }
            }

            ensureArrays(Math.max(mapCols, explicitMaxX + 1), Math.max(mapRows, explicitMaxY + 1));

            for (Raw r : raws) {
                if (r.type.equals("B") || r.type.equals("BOTTOM")) {
                    placeFromRaw(r, Block.Type.BOTTOM);
                }
            }

            for (Raw r : raws) {
                if (r.type.equals("T") || r.type.equals("TOP")) {
                    placeFromRaw(r, Block.Type.TOP);
                }
            }

            for (int x = 0; x < mapCols; x++) {
                List<Integer> tops = new ArrayList<>();
                for (int y = 0; y < mapRows; y++) if (blocks[x][y] != null && blocks[x][y].isTop()) tops.add(y);
                if (tops.isEmpty()) continue;
                Collections.sort(tops);
                int lowestTop = tops.get(0);
                for (int by = 0; by < lowestTop; by++) {
                    if (inBounds(x, by) && blocks[x][by] == null) {
                        blocks[x][by] = new Block(x, by, Block.Type.BOTTOM, blockBottom);
                    }
                }
            }

            for (Raw r : raws) {
                if (r.type.equals("C") || r.type.equals("CREEPER")) {
                    List<Integer> c = r.coords;
                    if (c.size() >= 4) {
                        int x1 = c.get(0), y1 = c.get(1), x2 = c.get(2), y2 = c.get(3);
                        int sx = Math.min(x1, x2);
                        int ex = Math.max(x1, x2);
                        int gy = y1;
                        automates.add(new Creeper(sx, ex, gy, TILE, this));
                    } else if (c.size() == 2) {
                        int gx = c.get(0), gy = c.get(1);
                        automates.add(new Creeper(gx, gx, gy, TILE, this));
                    }
                } else if (r.type.equals("P") || r.type.equals("PIC") || r.type.equals("PICS") || r.type.equals("SPIKE") || r.type.equals("SPIKES")) {
                    List<Integer> c = r.coords;
                    if (c.size() >= 2) {
                        int gx = c.get(0), gy = c.get(1);
                        automates.add(new Pics(gx, gy, TILE));
                    }
                }
            }

        } catch (Exception e) {
            Gdx.app.log("Carte", "Erreur lecture niveau: " + e.getMessage());
        }
    }
    
    private void placeFromRaw(Raw r, Block.Type placeType) {
        List<Integer> c = r.coords;
        if (c.size() == 2) {
            int x = c.get(0), y = c.get(1);
            if (inBounds(x, y)) setBlock(x, y, placeType);
        } else if (c.size() >= 4) {
            int x1 = c.get(0), y1 = c.get(1), x2 = c.get(2), y2 = c.get(3);
            int sx = Math.min(x1, x2), ex = Math.max(x1, x2);
            int sy = Math.min(y1, y2), ey = Math.max(y1, y2);
            for (int gx = sx; gx <= ex; gx++) {
                for (int gy = sy; gy <= ey; gy++) {
                    if (inBounds(gx, gy)) setBlock(gx, gy, placeType);
                }
            }
        }
    }

    private void setBlock(int gx, int gy, Block.Type type) {
        if (!inBounds(gx, gy)) return;
        if (type == Block.Type.TOP) {
            blocks[gx][gy] = new Block(gx, gy, Block.Type.TOP, blockTop);
        } else {
            if (blocks[gx][gy] == null) blocks[gx][gy] = new Block(gx, gy, Block.Type.BOTTOM, blockBottom);
        }
    }

    private boolean inBounds(int gx, int gy) {
        return gx >= 0 && gy >= 0 && gx < mapCols && gy < mapRows;
    }

    public boolean updateAutomates(float delta, Joueur joueur) {
        for (AutomateMortel a : automates) {
            a.update(delta);
            if (a.kill(joueur)) return true;
        }
        return false;
    }

    public void render(SpriteBatch batch, float cameraX) {
        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();
        int mapWidthPx = getMapWidth();
        final float EXTRA_HEIGHT = 60f;
        float destHeight = screenH + EXTRA_HEIGHT;
        float bgW = background.getWidth();
        float bgH = background.getHeight();
        if (bgH <= 0) bgH = 1;
        float destWidth = (bgW / bgH) * destHeight;
        if (destWidth < 1f) destWidth = screenW;

        for (float bx = 0; bx < mapWidthPx; bx += destWidth) {
            batch.draw(background, bx - cameraX, 0f, destWidth, destHeight);
        }

        for (int c = 0; c < mapCols; c++) {
            for (int r = 0; r < mapRows; r++) {
                Block b = blocks[c][r];
                if (b != null) b.render(batch, TILE, cameraX);
            }
        }

        for (AutomateMortel a : automates) a.render(batch, cameraX);
    }

    /**
     * NOUVEAU : Trouve la position Y de la surface pour une colonne X donnée.
     * @param gx La coordonnée X en tuiles.
     * @return La position Y en pixels où le joueur doit apparaître.
     */
    public float getSurfaceYAt(int gx) {
        if (!inBounds(gx, 0)) {
            return getGroundY(); // Si gx est hors limites, retourne le sol par défaut
        }
        // On scanne la colonne de haut en bas
        for (int gy = mapRows - 1; gy >= 0; gy--) {
            if (isTopBlockAt(gx, gy)) {
                // Le joueur doit être au-dessus de cette tuile
                return (gy + 1) * TILE;
            }
        }
        // S'il n'y a aucun bloc TOP dans cette colonne, on retourne le sol par défaut
        return getGroundY();
    }

    public float getGroundY() {
        return (groundRow + 1) * TILE;
    }

    public int getTile() { return TILE; }
    public int getMapWidth() { return mapCols * TILE; }
    public int getMapCols() { return mapCols; }
    public int getMapRows() { return mapRows; }

    public boolean isTopBlockAt(int gx, int gy) {
        if (!inBounds(gx, gy)) return false;
        Block b = blocks[gx][gy];
        return b != null && b.isTop();
    }

    public boolean isAnyBlockAt(int gx, int gy) {
        if (!inBounds(gx, gy)) return false;
        return blocks[gx][gy] != null;
    }
    
    public String getCurrentLevelPath() { return currentLevelPath; }

    public void dispose() {
        if (blockTop != null) blockTop.dispose();
        if (blockBottom != null) blockBottom.dispose();
        if (background != null) background.dispose();
        for (AutomateMortel a : automates) {
            if (a != null) a.dispose();
        }
        automates.clear();
    }
}
