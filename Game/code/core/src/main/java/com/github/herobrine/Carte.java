package com.github.herobrine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle; // NOUVEL IMPORT

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
    private final List<AutomateNonMortel> automatesNonMortels = new ArrayList<>(); // NOUVEAU : Liste pour les automates non mortels
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
     * CORRECTION DE LA MÉTHODE getSurfaceYAt :
     * La version précédente contenait des erreurs d'indexation et de logique.
     * Cette version robuste cherche le premier bloc solide dans la colonne gridX
     * et retourne la position Y juste au-dessus. Si la colonne est vide, elle
     * cherche des blocs solides dans les colonnes adjacentes.
     */
    public float getGroundYAtGridX(int gridX) {
        // Étape 1 : On essaie de trouver le sol directement sous la position de départ.
        // On parcourt la colonne de haut en bas pour trouver le premier bloc solide.
        for (int gy = MAP_HEIGHT_TILES - 1; gy >= 0; gy--) {
            // Vérifie si la tuile à (gridX, gy) est un bloc solide (non vide)
            if (map[gridX][gy-1] != 0) {
                // Si un bloc est trouvé, la surface est juste au-dessus de ce bloc.
                return MAP_HEIGHT_TILES * TILE ;
            }
        }

        // Cas de secours ultime : si toute la carte est vide ou aucun sol n'est trouvé, on retourne 0.
        return 0;
    }

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
                    // NOUVEAU : Chargement des portes
                    case "D":
                        if (coords.size() >= 2) {
                            // Les coordonnées sont en tuiles, on les convertit en pixels
                            Porte porte = new Porte(coords.get(0) * TILE, coords.get(1) * TILE, TILE);
                            automatesNonMortels.add(porte);
                        }
                        break;
                    // NOUVEAU : Chargement des plaques de pression
                    case "PP":
                        if (coords.size() >= 2) {
                            // Les coordonnées sont en tuiles, on les convertit en pixels
                            PlaqueDePression plaque = new PlaqueDePression(coords.get(0) * TILE, coords.get(1) * TILE, TILE);
                            automatesNonMortels.add(plaque);
                        }
                        break;
                }
            }
            
            // NOUVEAU : Association des plaques de pression aux portes
            // On parcourt toutes les plaques de pression pour leur assigner la porte la plus proche à leur droite.
            for (AutomateNonMortel automate : automatesNonMortels) {
                if (automate instanceof PlaqueDePression) {
                    PlaqueDePression plaque = (PlaqueDePression) automate;
                    Porte porteLaPlusProche = null;
                    float distanceMin = Float.MAX_VALUE;

                    for (AutomateNonMortel autreAutomate : automatesNonMortels) {
                        if (autreAutomate instanceof Porte) {
                            Porte porte = (Porte) autreAutomate;
                            // On cherche une porte à droite de la plaque de pression
                            float distance = porte.getX() - plaque.getX();

                            // Si la porte est à droite (distance > 0) et plus proche que la précédente trouvée
                            if (distance > 0 && distance < distanceMin) {
                                distanceMin = distance;
                                porteLaPlusProche = porte;
                            }
                        }
                    }

                    if (porteLaPlusProche != null) {
                        plaque.setPorteAssociee(porteLaPlusProche);
                    }
                }
            }

            // Logique existante pour marquer les blocs du dessous (type 2)
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
        
        // NOUVEAU : Libération des ressources des automates non mortels
        for (AutomateNonMortel auto : automatesNonMortels) {
            auto.dispose();
        }
        automatesNonMortels.clear();

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

        // NOUVEAU : Rendu des automates non mortels
        for (AutomateNonMortel auto : automatesNonMortels) {
            auto.render(batch, cameraX);
        }
    }

    public boolean updateAutomates(float delta, Joueur joueur) {

        for (AutomateMortel auto : automates) {
            auto.update(delta);
            if (auto.kill(joueur)) {

                joueur.setDead(true);
                return true;

            }
        }

        // NOUVEAU : Mise à jour des automates non mortels et gestion des interactions
        // On utilise la hitbox du joueur pour les collisions
        Rectangle joueurHitbox = new Rectangle(joueur.getX(), joueur.getY(), joueur.getWidth(), joueur.getHeight());

        for (AutomateNonMortel auto : automatesNonMortels) {
            auto.update(delta); // Met à jour le timer de la porte

            if (auto instanceof PlaqueDePression) {
                PlaqueDePression plaque = (PlaqueDePression) auto;
                // Si le joueur est en collision avec la plaque de pression
                if (joueurHitbox.overlaps(plaque.getHitbox())) {
                    plaque.activerPorte();
                }
            } else if (auto instanceof Porte) {
                Porte porte = (Porte) auto;
                // Si la porte est fermée ET que le joueur tente de la traverser
                if (!porte.estOuverte() && joueurHitbox.overlaps(porte.getHitbox())) {
                    // Empêche le joueur de traverser la porte fermée
                    // On doit ajuster la position du joueur pour le "repousser" hors de la porte
                    // Cette logique est simplifiée et peut être améliorée pour une meilleure gestion des collisions
                    
                    // Calcul de la profondeur de pénétration
                    float overlapX = Math.min(joueurHitbox.x + joueurHitbox.width, porte.getHitbox().x + porte.getHitbox().width) - Math.max(joueurHitbox.x, porte.getHitbox().x);
                    float overlapY = Math.min(joueurHitbox.y + joueurHitbox.height, porte.getHitbox().y + porte.getHitbox().height) - Math.max(joueurHitbox.y, porte.getHitbox().y);

                    if (overlapX < overlapY) { // Collision horizontale
                        if (joueurHitbox.x < porte.getHitbox().x) { // Joueur vient de la gauche
                            joueur.setX(porte.getHitbox().x - joueur.getWidth());
                        } else { // Joueur vient de la droite
                            joueur.setX(porte.getHitbox().x + porte.getHitbox().width);
                        }
                    } else { // Collision verticale
                        if (joueurHitbox.y < porte.getHitbox().y) { // Joueur vient du bas
                            joueur.setY(porte.getHitbox().y - joueur.getHeight());
                        } else { // Joueur vient du haut
                            joueur.setY(porte.getHitbox().y + porte.getHitbox().height);
                        }
                    }
                }
            }
        }
        return false;
    }

    public int getTile() { return TILE; }
    public float getMapWidth() { return MAP_WIDTH_TILES * TILE; }
    public String getCurrentLevelPath() { return currentLevelPath; }

    public boolean isAnyBlockAt(int gridX, int gridY) {
        if (gridX < 0 || gridX >= MAP_WIDTH_TILES || gridY < 0 || gridY >= MAP_HEIGHT_TILES) {
            return true; // Considère l'extérieur de la carte comme un bloc solide pour éviter de tomber
        }
        return map[gridX][gridY] != 0;
    }
}
