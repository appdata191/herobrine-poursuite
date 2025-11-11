package com.github.herobrine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

public class CreateMap {
    private final int TILE = 60;
    private final float EXTRA_HEIGHT;
    
    // CORRIGÉ : Les textures sont maintenant des dépendances
    private final Texture background;
    private final Texture blockTop;
    private final Texture blockBottom;
    private final Texture creeperTex;
    private final Texture picsTex;
    
    private Texture scrollbarBgTex;
    private Texture scrollbarHandleTex;
    private Texture uiSlotTex;

    private final ShapeRenderer shapeRenderer;
    private boolean active = false;
    private float cameraX = 0f;
    private final int EDITOR_MAP_WIDTH_TILES = 64;
    private final int EDITOR_MAP_WIDTH_PX = EDITOR_MAP_WIDTH_TILES * TILE;
    private final float SCROLL_SPEED = 400f;
    private final int SCROLL_MARGIN = 40;

    private String loadedLevelName = null;

    private enum EditorMode { SELECTION, PLACING }
    private EditorMode currentMode = EditorMode.SELECTION;

    private enum Tool { TOP, CREEPER, PICS }
    private final Tool[] tools = new Tool[]{ Tool.TOP, Tool.CREEPER, Tool.PICS };
    private int selectionIndex = 0;

    private Integer hoverGX = null, hoverGY = null;
    private Integer anchorGX = null, anchorGY = null;
    private Integer previewGX = null, previewGY = null;

    private static class EditorCreeper {
        // ... (classe interne identique)
        int startX, endX, y;
        float currentX;
        int direction = 1;
        static final float SPEED = 80f;

        EditorCreeper(int startX, int endX, int y) {
            this.startX = startX;
            this.endX = endX;
            this.y = y;
            this.currentX = startX * 60;
        }

        void update(float delta) {
            currentX += direction * SPEED * delta;
            if (currentX > endX * 60) {
                currentX = endX * 60;
                direction = -1;
            }
            if (currentX < startX * 60) {
                currentX = startX * 60;
                direction = 1;
            }
        }
    }

    private final List<int[]> topSegments = new ArrayList<>();
    private final List<EditorCreeper> creepers = new ArrayList<>();
    private final List<int[]> pics = new ArrayList<>();

    private final SaveMenu saveMenu;
    private final Rectangle scrollbarHandle = new Rectangle();
    private boolean isDraggingScrollbar = false;
    private final List<Rectangle> toolSlots = new ArrayList<>();

    // CORRIGÉ : Le constructeur reçoit les textures en dépendances
    public CreateMap(float extraHeight, Texture background, Texture blockTop, Texture blockBottom, Texture creeperTex, Texture picsTex) {
        this.EXTRA_HEIGHT = extraHeight;
        this.background = background;
        this.blockTop = blockTop;
        this.blockBottom = blockBottom;
        this.creeperTex = creeperTex;
        this.picsTex = picsTex;
        
        saveMenu = new SaveMenu(EXTRA_HEIGHT);
        shapeRenderer = new ShapeRenderer();

        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(0.1f, 0.1f, 0.1f, 0.7f);
        pm.fill();
        scrollbarBgTex = new Texture(pm);
        pm.setColor(0.8f, 0.8f, 0.8f, 0.7f);
        pm.fill();
        scrollbarHandleTex = new Texture(pm);
        pm.setColor(Color.DARK_GRAY);
        pm.fill();
        uiSlotTex = new Texture(pm);
        pm.dispose();
    }

    // ... (le reste du fichier est identique à la version précédente)
    public void activate(String levelPathToLoad) {
        active = true;
        cameraX = 0;
        currentMode = EditorMode.SELECTION;
        resetPlacementState();
        topSegments.clear();
        creepers.clear();
        pics.clear();

        if (levelPathToLoad != null) {
            this.loadedLevelName = new FileHandle(levelPathToLoad).nameWithoutExtension();
            loadFromFile(levelPathToLoad);
        } else {
            this.loadedLevelName = null;
        }
    }

    public void deactivate() {
        active = false;
        loadedLevelName = null;
    }

    public boolean isActive() { return active; }

    private void resetPlacementState() {
        anchorGX = null;
        anchorGY = null;
        previewGX = null;
        previewGY = null;
    }

    public void loadFromFile(String path) {
        try {
            FileHandle fh = Gdx.files.local(path);
            if (!fh.exists()) {
                Gdx.app.error("CreateMap", "Impossible de charger le fichier : " + path);
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
                            topSegments.add(new int[]{coords.get(0), coords.get(1), coords.get(2), coords.get(3)});
                        } else if (coords.size() >= 2) {
                            int x = coords.get(0);
                            int y = coords.get(1);
                            topSegments.add(new int[]{x, y, x, y});
                        }
                        break;
                    case "C":
                        if (coords.size() >= 3) {
                            int startX = coords.get(0);
                            int y = coords.get(1);
                            int endX = (coords.size() >= 4) ? coords.get(2) : startX;
                            creepers.add(new EditorCreeper(Math.min(startX, endX), Math.max(startX, endX), y));
                        }
                        break;
                    case "P":
                        if (coords.size() >= 2) {
                            pics.add(new int[]{coords.get(0), coords.get(1)});
                        }
                        break;
                }
            }
        } catch (Exception e) {
            Gdx.app.log("CreateMap", "Erreur lors du chargement du niveau pour modification: " + e.getMessage());
        }
    }

    public int updateInput() {
        if (!active) return 0;
        float delta = Gdx.graphics.getDeltaTime();
        int mx = Gdx.input.getX();
        int my = Gdx.input.getY();

        if (saveMenu.isActive()) {
            int r = saveMenu.updateInput();
            switch (r) {
                case SaveMenu.RESULT_SAVE_AS_VALIDATE:
                    String newName = saveMenu.getFilenameInput();
                    if (newName != null && !newName.trim().isEmpty()) {
                        saveToFile(newName);
                        this.loadedLevelName = newName;
                    }
                    saveMenu.deactivate();
                    return 0;
                case SaveMenu.RESULT_SAVE_DIRECT:
                    if (loadedLevelName != null) {
                        saveToFile(loadedLevelName);
                    }
                    saveMenu.deactivate();
                    return 0;
                case SaveMenu.RESULT_RETURN_MENU:
                    saveMenu.deactivate();
                    deactivate();
                    return 1;
                case SaveMenu.RESULT_QUIT:
                    Gdx.app.exit();
                    return 0;
                case SaveMenu.RESULT_RESUME_EDITING:
                    saveMenu.deactivate();
                    return 0;
                case SaveMenu.RESULT_SAVE_AS_CANCEL:
                    return 0;
                default:
                    return 0;
            }
        }

        updateCamera(delta, mx, my);
        for (EditorCreeper creeper : creepers) {
            creeper.update(delta);
        }

        int worldMouseX = mx + (int)cameraX;
        int worldMouseY = Gdx.graphics.getHeight() - my;
        hoverGX = worldMouseX / TILE;
        hoverGY = worldMouseY / TILE;

        handleToolShortcuts();

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            if (scrollbarHandle.contains(mx, my)) {
                isDraggingScrollbar = true;
            }
            else if (checkToolSelectionClick(mx, my)) {
                currentMode = EditorMode.PLACING;
                resetPlacementState();
            }
            else if (currentMode == EditorMode.PLACING) {
                handlePlacingClick();
            }
            else if (currentMode == EditorMode.SELECTION) {
                if (pickToolFromMap(hoverGX, hoverGY)) {
                    currentMode = EditorMode.PLACING;
                    resetPlacementState();
                }
            }
        }

        if (!Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
            isDraggingScrollbar = false;
        }

        if (Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)) {
            if (currentMode == EditorMode.PLACING) {
                resetPlacementState();
                currentMode = EditorMode.SELECTION;
            } else {
                deleteItem(hoverGX, hoverGY);
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            saveMenu.activate(loadedLevelName);
        }

        return 0;
    }

    private void handleToolShortcuts() {
        int[] keys = {Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3};
        for (int i = 0; i < keys.length && i < tools.length; i++) {
            if (Gdx.input.isKeyJustPressed(keys[i])) {
                selectionIndex = i;
                currentMode = EditorMode.PLACING;
                resetPlacementState();
                break;
            }
        }
    }

    private boolean pickToolFromMap(int gx, int gy) {
        for (EditorCreeper c : creepers) {
            int creeperTileX = (int)(c.currentX / TILE);
            int creeperBaseY = c.y;
            if (gx == creeperTileX && (gy == creeperBaseY + 1 || gy == creeperBaseY + 2)) {
                selectionIndex = 1;
                return true;
            }
        }

        for (int[] p : pics) {
            if (p[0] == gx && p[1] == gy) {
                selectionIndex = 2;
                return true;
            }
        }

        for (int[] seg : topSegments) {
            int x1 = Math.min(seg[0], seg[2]);
            int x2 = Math.max(seg[0], seg[2]);
            int y1 = Math.min(seg[1], seg[3]);
            int y2 = Math.max(seg[1], seg[3]);
            if (gx >= x1 && gx <= x2 && gy >= y1 && gy <= y2) {
                selectionIndex = 0;
                return true;
            }
        }
        return false;
    }

    private void handlePlacingClick() {
        Tool currentTool = tools[selectionIndex];
        if (currentTool == Tool.TOP || currentTool == Tool.CREEPER) {
            if (anchorGX == null) {
                anchorGX = hoverGX;
                anchorGY = hoverGY;
            } else {
                previewGX = hoverGX;
                previewGY = hoverGY;
                if (currentTool == Tool.TOP) {
                    topSegments.add(new int[]{anchorGX, anchorGY, previewGX, previewGY});
                } else {
                    int startX = Math.min(anchorGX, previewGX);
                    int endX = Math.max(anchorGX, previewGX);
                    creepers.add(new EditorCreeper(startX, endX, anchorGY));
                }
                resetPlacementState();
            }
        } else {
            placeItem(hoverGX, hoverGY);
        }
    }

    private void placeItem(int gx, int gy) {
        Tool currentTool = tools[selectionIndex];
        if (currentTool == Tool.PICS) {
            pics.add(new int[]{gx, gy});
        }
    }

    private void deleteItem(int gx, int gy) {
        for (int i = creepers.size() - 1; i >= 0; i--) {
            EditorCreeper c = creepers.get(i);
            int creeperTileX = (int)(c.currentX / TILE);
            int creeperBaseY = c.y;
            if (gx == creeperTileX && (gy == creeperBaseY + 1 || gy == creeperBaseY + 2)) {
                creepers.remove(i);
                return;
            }
        }

        for (int i = pics.size() - 1; i >= 0; i--) {
            int[] p = pics.get(i);
            if (p[0] == gx && p[1] == gy) {
                pics.remove(i);
                return;
            }
        }

        for (int i = topSegments.size() - 1; i >= 0; i--) {
            int[] seg = topSegments.get(i);
            int x1 = Math.min(seg[0], seg[2]);
            int x2 = Math.max(seg[0], seg[2]);
            int y1 = Math.min(seg[1], seg[3]);
            int y2 = Math.max(seg[1], seg[3]);

            if (gx >= x1 && gx <= x2 && gy >= y1 && gy <= y2) {
                topSegments.remove(i);
                return;
            }
        }
    }

    private boolean checkToolSelectionClick(int mx, int my) {
        for (int i = 0; i < toolSlots.size(); i++) {
            Rectangle slot = toolSlots.get(i);
            if (slot.contains(mx, Gdx.graphics.getHeight() - my)) {
                selectionIndex = i;
                return true;
            }
        }
        return false;
    }

    private void updateCamera(float delta, int mouseX, int mouseY) {
        float screenW = Gdx.graphics.getWidth();
        float maxCamX = EDITOR_MAP_WIDTH_PX - screenW;
        if (maxCamX < 0) maxCamX = 0;

        if (isDraggingScrollbar) {
            float scrollbarWidth = screenW;
            cameraX = (mouseX / scrollbarWidth) * maxCamX;
        }
        else if (!saveMenu.isActive()) {
            if (mouseX < SCROLL_MARGIN) {
                cameraX -= SCROLL_SPEED * delta;
            }
            if (mouseX > screenW - SCROLL_MARGIN) {
                cameraX += SCROLL_SPEED * delta;
            }
        }

        if (cameraX < 0) cameraX = 0;
        if (cameraX > maxCamX) cameraX = maxCamX;
    }

    public void render(SpriteBatch batch, BitmapFont font) {
        if (!active) return;

        if (batch.isDrawing()) {
            batch.end();
        }
        
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0f, 0f, 0f, 1f);

        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();
        int startGX = (int) (cameraX / TILE);
        int endGX = (int) ((cameraX + sw) / TILE) + 1;
        if (endGX > EDITOR_MAP_WIDTH_TILES) endGX = EDITOR_MAP_WIDTH_TILES;

        for (int gx = startGX; gx <= endGX; gx++) {
            shapeRenderer.line(gx * TILE - cameraX, 0, gx * TILE - cameraX, sh);
        }
        for (int gy = 0; gy * TILE < sh; gy++) {
            shapeRenderer.line(-cameraX, gy * TILE, EDITOR_MAP_WIDTH_PX - cameraX, gy * TILE);
        }
        shapeRenderer.end();

        batch.begin();

        float bgW = background.getWidth();
        float bgH = background.getHeight();
        if (bgH <= 0) bgH = 1;
        float destHeight = sh + EXTRA_HEIGHT;
        float destWidth = (bgW / bgH) * destHeight;
        if (destWidth < 1f) destWidth = sw;

        for (float bx = 0; bx < EDITOR_MAP_WIDTH_PX; bx += destWidth) {
            batch.draw(background, bx - cameraX, 0f, destWidth, destHeight);
        }

        int[] lowestTopInColumn = new int[EDITOR_MAP_WIDTH_TILES];
        Arrays.fill(lowestTopInColumn, Integer.MAX_VALUE);

        for (int[] seg : topSegments) {
            int x1 = Math.min(seg[0], seg[2]);
            int x2 = Math.max(seg[0], seg[2]);
            int y1 = Math.min(seg[1], seg[3]);
            int y2 = Math.max(seg[1], seg[3]);
            for (int gx = x1; gx <= x2; gx++) {
                for (int gy = y1; gy <= y2; gy++) {
                    if (gx >= 0 && gx < EDITOR_MAP_WIDTH_TILES) {
                        if (gy < lowestTopInColumn[gx]) {
                            lowestTopInColumn[gx] = gy;
                        }
                    }
                }
            }
        }

        for (int gx = 0; gx < EDITOR_MAP_WIDTH_TILES; gx++) {
            int lowestY = lowestTopInColumn[gx];
            if (lowestY != Integer.MAX_VALUE) {
                for (int by = 0; by < lowestY; by++) {
                    batch.draw(blockBottom, gx * TILE - cameraX, by * TILE, TILE, TILE);
                }
            }
        }

        for (int[] seg : topSegments) {
            int x1 = Math.min(seg[0], seg[2]);
            int x2 = Math.max(seg[0], seg[2]);
            int y1 = Math.min(seg[1], seg[3]);
            int y2 = Math.max(seg[1], seg[3]);
            for (int gx = x1; gx <= x2; gx++) {
                for (int gy = y1; gy <= y2; gy++) {
                    batch.draw(blockTop, gx * TILE - cameraX, gy * TILE, TILE, TILE);
                }
            }
        }

        for (EditorCreeper c : creepers) {
            float y = c.y * TILE + TILE;
            batch.draw(creeperTex, c.currentX - cameraX, y, 60f, 120f);
        }

        for (int[] p : pics) {
            float x = p[0] * TILE;
            float y = p[1] * TILE + TILE;
            batch.draw(picsTex, x - cameraX, y, 60f, 20f);
        }

        if (currentMode == EditorMode.PLACING && hoverGX != null && hoverGY != null) {
            Tool currentTool = tools[selectionIndex];
            batch.setColor(1f, 1f, 1f, 0.6f);

            if (currentTool == Tool.TOP) {
                if (anchorGX == null) {
                    batch.draw(blockTop, hoverGX * TILE - cameraX, hoverGY * TILE, TILE, TILE);
                } else {
                    int x1 = Math.min(anchorGX, hoverGX);
                    int x2 = Math.max(anchorGX, hoverGX);
                    int y1 = Math.min(anchorGY, hoverGY);
                    int y2 = Math.max(anchorGY, hoverGY);
                    for (int gx = x1; gx <= x2; gx++) {
                        for (int gy = y1; gy <= y2; gy++) {
                            batch.draw(blockTop, gx * TILE - cameraX, gy * TILE, TILE, TILE);
                        }
                    }
                }
            } else if (currentTool == Tool.CREEPER) {
                if (anchorGX == null) {
                    batch.draw(creeperTex, hoverGX * TILE - cameraX, hoverGY * TILE + TILE, 60f, 120f);
                } else {
                    batch.draw(creeperTex, anchorGX * TILE - cameraX, anchorGY * TILE + TILE, 60f, 120f);
                    batch.draw(creeperTex, hoverGX * TILE - cameraX, anchorGY * TILE + TILE, 60f, 120f);
                }
            } else if (currentTool == Tool.PICS) {
                batch.draw(picsTex, hoverGX * TILE - cameraX, hoverGY * TILE + TILE, 60f, 20f);
            }
            batch.setColor(1f, 1f, 1f, 1f);
        }

        renderUI(batch, font);

        batch.end();
        if (saveMenu.isActive()) {
            saveMenu.render(batch, shapeRenderer, font);
        }
    }

    private void renderUI(SpriteBatch batch, BitmapFont font) {
        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();
        toolSlots.clear();

        float slotSize = 64f;
        float slotPadding = 10f;
        int toolCount = tools.length;
        float barWidth = toolCount * (slotSize + slotPadding) + slotPadding;
        float barX = sw - barWidth - 20f;
        float barY = 40f;

        for (int i = 0; i < toolCount; i++) {
            float xSlot = barX + slotPadding + i * (slotSize + slotPadding);
            Rectangle slotRect = new Rectangle(xSlot, barY, slotSize, slotSize);
            toolSlots.add(slotRect);

            batch.draw(uiSlotTex, slotRect.x, slotRect.y, slotRect.width, slotRect.height);

            if (selectionIndex == i) {
                batch.setColor(Color.GOLD);
                batch.draw(scrollbarHandleTex, slotRect.x, slotRect.y, slotRect.width, slotRect.height);
                batch.setColor(Color.WHITE);
            }

            Texture icon = null;
            switch (tools[i]) {
                case TOP: icon = blockTop; break;
                case CREEPER: icon = creeperTex; break;
                case PICS: icon = picsTex; break;
            }
            if (icon != null) {
                batch.draw(icon, slotRect.x + 4, slotRect.y + 4, slotRect.width - 8, slotRect.height - 8);
            }
        }

        String instruction = "Mode: " + (currentMode == EditorMode.SELECTION ? "Selection/Destruction" : "Pose");
        font.getData().setScale(0.8f);
        font.draw(batch, instruction, 20, sh - 20);
        font.draw(batch, "LMB: Placer/Pipette   RMB: Annuler/Detruire   ESC: Menu   1-3: Outils", 20, sh - 40);
        font.getData().setScale(1f);

        float scrollbarHeight = 20f;
        float scrollbarY = 5f;
        batch.draw(scrollbarBgTex, 0, scrollbarY, sw, scrollbarHeight);

        float maxCamX = EDITOR_MAP_WIDTH_PX - sw;
        if (maxCamX > 0) {
            float handleWidth = (sw / (float)EDITOR_MAP_WIDTH_PX) * sw;
            if (handleWidth < 20) handleWidth = 20;
            float handleX = (cameraX / maxCamX) * (sw - handleWidth);

            scrollbarHandle.set(handleX, scrollbarY, handleWidth, scrollbarHandle.height);
            batch.draw(scrollbarHandleTex, scrollbarHandle.x, scrollbarHandle.y, scrollbarHandle.width, scrollbarHandle.height);
        }
    }

    // CORRIGÉ : Ne dispose plus les textures partagées
    public void dispose() {
        if (saveMenu != null) saveMenu.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (scrollbarBgTex != null) scrollbarBgTex.dispose();
        if (scrollbarHandleTex != null) scrollbarHandleTex.dispose();
        if (uiSlotTex != null) uiSlotTex.dispose();
    }

    void saveToFile(String name) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("# Generated by CreateMap\n");
            for (int[] seg : topSegments) {
                sb.append("T ").append(seg[0]).append(" ").append(seg[1]).append(" ").append(seg[2]).append(" ").append(seg[3]).append("\n");
            }
            for (EditorCreeper c : creepers) {
                sb.append("C ").append(c.startX).append(" ").append(c.y).append(" ").append(c.endX).append(" ").append(c.y).append("\n");
            }
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
}
