package com.github.herobrine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;
import java.util.List;

/**
 * Outil de création de carte simple.
 *
 * - Barre de choix en bas droite : TOP / CREEPER / PICS
 * - Flèches pour changer la sélection ; ENTER pour entrer en mode placement.
 * - LEFT CLICK pour ancrer / indiquer second point ; RIGHT CLICK annule placement.
 * - Pour TOP : 1er clic -> ancre ; 2e clic -> prévisualisation d'une ligne de TOP (et remplissage BOTTOM automatiquement).
 *   Appuyer sur ENTER après la prévisualisation pour valider et ajouter la ligne au niveau.
 * - Pour CREEPER / PICS : ENTER puis LEFT CLICK pour positionner. ENTER valide.
 * - ESC ouvre le SaveMenu (en bas à droite comme les autres menus).
 *
 * Remarque : l'enregistrement écrit dans local/assets/levels/<name>.txt
 *
 * Retour de updateInput() :
 *   0 = aucun événement
 *   1 = demande retour au LaunchMenu (après confirmation dans SaveMenu)
 *   2 = demande quitter (après confirmation)
 */
public class CreateMap {
    private final int TILE = 60;
    private final float EXTRA_HEIGHT;
    private Texture background;
    private Texture blockTop;
    private Texture blockBottom;
    private Texture creeperTex;
    private Texture picsTex;

    private boolean active = false;

    private enum Tool { TOP, CREEPER, PICS }
    private final Tool[] tools = new Tool[]{ Tool.TOP, Tool.CREEPER, Tool.PICS };
    private int selectionIndex = 0;

    // placement state
    private boolean placing = false;
    private Integer anchorGX = null, anchorGY = null; // for TOP first click
    private Integer previewGX = null, previewGY = null; // second click preview
    // tile sous la souris pour l'aperçu (snap)
    private Integer hoverGX = null, hoverGY = null;

    // data lists to save later
    private final List<int[]> topSegments = new ArrayList<>(); // each int[]{x1,y1,x2,y2}
    private final List<int[]> creepers = new ArrayList<>();    // each int[]{sx,ex,gy} (sx==ex for single)
    private final List<int[]> pics = new ArrayList<>();        // each int[]{gx,gy}

    // UI
    private final float uiW = 300f;
    private final float uiH = 120f;

    private final SaveMenu saveMenu;

    public CreateMap(float extraHeight) {
        this.EXTRA_HEIGHT = extraHeight;
        background = new Texture("Fond_simple.png");
        blockTop = new Texture("Bloc du dessus.png");
        blockBottom = new Texture("Bloc du dessous.png");
        // textures for preview (may be same names as game assets)
        creeperTex = new Texture("creeper.png");
        picsTex = new Texture("Pics.png");
        saveMenu = new SaveMenu(EXTRA_HEIGHT);
    }

    public void activate() {
        active = true;
        placing = false;
        anchorGX = null;
        previewGX = null;
    }

    public void deactivate() {
        active = false;
        placing = false;
        anchorGX = null;
        previewGX = null;
    }

    public boolean isActive() { return active; }

    /**
     * Update input & internal state.
     * Retour :
     *  0 = rien
     *  1 = retour menu demandé (SaveMenu confirmed)
     *  2 = quitter (SaveMenu confirmed)
     */
    public int updateInput() {
        if (!active) return 0;

        // If save menu active, delegate
        if (saveMenu.isActive()) {
            int r = saveMenu.updateInput();
            if (r == SaveMenu.RESULT_SAVED) {
                // save immediately from the editor using the filename provided
                String name = saveMenu.getFilename();
                if (name != null && !name.trim().isEmpty()) {
                    saveToFile(name.trim());
                }
                saveMenu.deactivate();
                return 0; // remain in editor
            } else if (r == SaveMenu.RESULT_RETURN_MENU) {
                // confirmed return to launch menu
                saveMenu.deactivate();
                deactivate();
                return 1;
            } else if (r == SaveMenu.RESULT_QUIT) {
                saveMenu.deactivate();
                deactivate();
                return 2;
            } else {
                return 0;
            }
        }

        // navigation selection only when not placing
        if (!placing) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
                selectionIndex = Math.min(tools.length - 1, selectionIndex + 1);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
                selectionIndex = Math.max(0, selectionIndex - 1);
            }
        }

        // Enter toggles placing mode or validates preview
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (!placing) {
                placing = true;
                anchorGX = null;
                previewGX = null;
            } else {
                // if placing and we have a preview (or single selection), commit it
                Tool t = tools[selectionIndex];
                if (t == Tool.TOP) {
                    if (anchorGX != null && previewGX != null) {
                        topSegments.add(new int[]{ anchorGX, anchorGY, previewGX, previewGY });
                    }
                    // reset placement (remain on same selection)
                    placing = false;
                    anchorGX = null;
                    previewGX = null;
                } else if (t == Tool.CREEPER) {
                    // if preview exists (positioned), save creeper
                    if (previewGX != null) {
                        // store as sx==ex for single placement
                        creepers.add(new int[]{ previewGX, previewGX, previewGY });
                    }
                    placing = false;
                    previewGX = null;
                } else if (t == Tool.PICS) {
                    if (previewGX != null) {
                        pics.add(new int[]{ previewGX, previewGY });
                    }
                    placing = false;
                    previewGX = null;
                }
            }
        }

        // Right click cancels placement and returns to selection
        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            if (placing) {
                placing = false;
                anchorGX = null;
                previewGX = null;
            }
        }

        // Left click behavior depends on tool & placing state
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            if (!placing) return 0;
            // compute tile coords from mouse
            int mx = Gdx.input.getX();
            int my = Gdx.input.getY();
            int gx = mx / TILE;
            int gy = (Gdx.graphics.getHeight() - my) / TILE;

            Tool t = tools[selectionIndex];
            if (t == Tool.TOP) {
                if (anchorGX == null) {
                    // first click anchor
                    anchorGX = gx; anchorGY = gy;
                    previewGX = null; previewGY = null;
                } else {
                    // second click -> preview line between anchor and this
                    previewGX = gx; previewGY = gy;
                }
            } else {
                // creeper or pics: set preview position (single tile)
                previewGX = gx; previewGY = gy;
            }
        }

        // ESC opens save menu
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            saveMenu.activate();
        }

        // met à jour la tuile sous la souris pour le rendu preview (snap)
        // -> only when in placing mode (after ENTER). Otherwise no hover-preview.
        if (placing) {
            int mx = Gdx.input.getX();
            int my = Gdx.input.getY();
            hoverGX = mx / TILE;
            hoverGY = (Gdx.graphics.getHeight() - my) / TILE;
        } else {
            hoverGX = null;
            hoverGY = null;
        }

        return 0;
    }

    public void render(SpriteBatch batch, BitmapFont font) {
        if (!active) return;

        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();

        // draw background tiled (similar to Carte)
        float bgW = background.getWidth();
        float bgH = background.getHeight();
        if (bgH <= 0) bgH = 1;
        float destHeight = sh + EXTRA_HEIGHT;
        float destWidth = (bgW / bgH) * destHeight;
        if (destWidth < 1f) destWidth = sw;

        for (float bx = 0; bx < sw; bx += destWidth) {
            batch.draw(background, bx, 0f, destWidth, destHeight);
        }

        // draw existing placed tops and fill bottoms
        int cols = (int) Math.ceil((float) sw / TILE);
        int rows = (int) Math.ceil((float) sh / TILE);

        // draw tops
        for (int[] seg : topSegments) {
            int x1 = Math.min(seg[0], seg[2]);
            int x2 = Math.max(seg[0], seg[2]);
            int y1 = Math.min(seg[1], seg[3]);
            int y2 = Math.max(seg[1], seg[3]);
            for (int gx = x1; gx <= x2; gx++) {
                for (int gy = y1; gy <= y2; gy++) {
                    float px = gx * TILE;
                    float py = gy * TILE;
                    batch.draw(blockTop, px, py, TILE, TILE);
                }
                // fill bottoms under the lowest top in this column until y=0
                for (int by = 0; by < y1; by++) {
                    float px = gx * TILE;
                    float py = by * TILE;
                    batch.draw(blockBottom, px, py, TILE, TILE);
                }
            }
        }

        // draw creepers placed
        for (int[] c : creepers) {
            int sx = c[0], ex = c[1], gy = c[2];
            float x = sx * TILE;
            float y = gy * TILE + TILE; // feet on top of tile
            batch.draw(creeperTex, x, y, 60f, 120f);
        }

        // draw pics placed (they sit on top of tile gy)
        for (int[] p : pics) {
            int gx = p[0], gy = p[1];
            float x = gx * TILE;
            float y = gy * TILE + TILE;
            batch.draw(picsTex, x, y, 60f, 20f);
        }

        // draw preview (if any)
        if (placing) {
            Tool t = tools[selectionIndex];
            if (t == Tool.TOP) {
                if (anchorGX != null && previewGX == null) {
                    // draw anchor
                    float px = anchorGX * TILE;
                    float py = anchorGY * TILE;
                    batch.draw(blockTop, px, py, TILE, TILE);
                } else if (anchorGX != null && previewGX != null) {
                    int x1 = Math.min(anchorGX, previewGX);
                    int x2 = Math.max(anchorGX, previewGX);
                    int y1 = Math.min(anchorGY, previewGY);
                    int y2 = Math.max(anchorGY, previewGY);
                    for (int gx = x1; gx <= x2; gx++) {
                        for (int gy = y1; gy <= y2; gy++) {
                            float px = gx * TILE;
                            float py = gy * TILE;
                            batch.draw(blockTop, px, py, TILE, TILE);
                        }
                        // preview bottoms
                        for (int by = 0; by < y1; by++) {
                            float px = gx * TILE;
                            float py = by * TILE;
                            batch.draw(blockBottom, px, py, TILE, TILE);
                        }
                    }
                }
            } else if (previewGX != null) {
                float px = previewGX * TILE;
                float py = previewGY * TILE;
                if (t == Tool.CREEPER) {
                    batch.draw(creeperTex, px, py + TILE, 60f, 120f);
                } else if (t == Tool.PICS) {
                    batch.draw(picsTex, px, py + TILE, 60f, 20f);
                }
            }
        }

        // --- Hover preview (l'icône sélectionnée suit la souris et se cale sur la grille) ---
        if (hoverGX != null && hoverGY != null) {
            Tool tHover = tools[selectionIndex];
            float hpx = hoverGX * TILE;
            float hpy = hoverGY * TILE;
            // semi-transparent pour indiquer que c'est un aperçu
            batch.setColor(1f, 1f, 1f, 0.6f);
            if (tHover == Tool.TOP) {
                batch.draw(blockTop, hpx, hpy, TILE, TILE);
            } else if (tHover == Tool.CREEPER) {
                batch.draw(creeperTex, hpx, hpy + TILE, 60f, 120f);
            } else if (tHover == Tool.PICS) {
                batch.draw(picsTex, hpx, hpy + TILE, 60f, 20f);
            }
            batch.setColor(1f, 1f, 1f, 1f);
        }

        // draw UI (selection) bottom-right as a horizontal bar (icons only, arrow above selected)
        float slotW = 64f;
        float slotH = 64f;
        float slotPadding = 8f;
        int toolCount = tools.length;
        float barWidth = toolCount * slotW + (toolCount + 1) * slotPadding;
        float barX = sw - barWidth - 20f;
        float barY = 12f;

        for (int i = 0; i < toolCount; i++) {
            float xSlot = barX + slotPadding + i * (slotW + slotPadding);
            // draw icon for each tool
            switch (tools[i]) {
                case TOP:
                    batch.draw(blockTop, xSlot, barY, slotW, slotH);
                    break;
                case CREEPER:
                    batch.draw(creeperTex, xSlot, barY, slotW, slotH);
                    break;
                case PICS:
                    batch.draw(picsTex, xSlot, barY, slotW, slotH);
                    break;
            }

            // draw selection arrow above the selected slot
            if (selectionIndex == i) {
                String arrow = "▲";
                font.getData().setScale(1.2f);
                GlyphLayout layout = new GlyphLayout(font, arrow);
                float ax = xSlot + (slotW - layout.width) / 2f;
                float ay = barY + slotH + 18f;
                font.draw(batch, arrow, ax, ay);
                layout.reset();
                font.getData().setScale(1f);
            }
        }

        // instructions (kept outside the icon bar)
        font.getData().setScale(0.8f);
        font.draw(batch, "←/→ select   ENTER to place/confirm   LMB place   RMB cancel   ESC save/exit",
                barX, barY + slotH + 44f);
        font.getData().setScale(1f);

        // render save menu if active
        if (saveMenu.isActive()) saveMenu.render(batch, font);
    }

    public void dispose() {
        if (background != null) background.dispose();
        if (blockTop != null) blockTop.dispose();
        if (blockBottom != null) blockBottom.dispose();
        if (creeperTex != null) creeperTex.dispose();
        if (picsTex != null) picsTex.dispose();
        saveMenu.dispose();
    }

    // Expose a simple save function used by SaveMenu
    void saveToFile(String name) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("# Generated by CreateMap\n");
            // write top segments as T lines
            for (int[] seg : topSegments) {
                sb.append("T ").append(seg[0]).append(" ").append(seg[1]).append(" ").append(seg[2]).append(" ").append(seg[3]).append("\n");
            }
            // creepers as C lines
            for (int[] c : creepers) {
                sb.append("C ").append(c[0]).append(" ").append(c[2]).append(" ").append(c[1]).append(" ").append(c[2]).append("\n");
            }
            // pics P lines
            for (int[] p : pics) {
                sb.append("P ").append(p[0]).append(" ").append(p[1]).append("\n");
            }

            FileHandle fh = Gdx.files.local("assets/levels/" + name + ".txt");
            fh.parent().mkdirs();
            fh.writeString(sb.toString(), false, "UTF-8");
        } catch (Exception e) {
            Gdx.app.log("CreateMap", "Erreur save: " + e.getMessage());
        }
    }

    // SaveMenu needs accessors
    SaveMenu getSaveMenu() { return saveMenu; }
}