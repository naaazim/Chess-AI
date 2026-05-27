package fr.moteurechecs.ouverture;

import fr.moteurechecs.plateau.Case;
import fr.moteurechecs.plateau.Couleur;
import fr.moteurechecs.plateau.Coup;
import fr.moteurechecs.plateau.Piece;
import fr.moteurechecs.plateau.Plateau;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Livre d'ouvertures haute qualité chargé depuis un fichier binaire {@code .book}.
 *
 * Chargement paresseux via singleton (double-checked locking). Lookup O(1)
 * par {@code HashMap}. Retourne les meilleurs candidats filtrés selon des heuristiques
 * d'ouverture ; en l'absence de coup du livre, l'IA bascule immédiatement sur Minimax.
 */
public final class LivreOuvertures {

    // HashMap: hashPositionKey → liste de BookMoveEntry triée par fréquence
    private final HashMap<Long, List<BookMoveEntry>> entries;

    private volatile String lastOpeningName = "Unknown";

    private static final String DEFAULT_BOOK_PATH     = "data/openings.book";
    private static final String DEFAULT_BOOK_RESOURCE = "/data/openings.book";

    private static volatile LivreOuvertures instance;

    private static final int MAX_BOOK_PLY = 16;

    // ===== Constantes heuristiques pour le score interne du livre =====
    private static final int BONUS_CENTRE       =  30;
    private static final int BONUS_DEVELOPPEMENT =  25;
    private static final int BONUS_ROQUE        =  40;
    private static final int PENALITE_COUP_SENS  = -50;

    private LivreOuvertures(HashMap<Long, List<BookMoveEntry>> entries) {
        this.entries = entries;
    }

    /**
     * Charge le livre depuis un fichier binaire sur le disque.
     *
     * @param path chemin absolu ou relatif vers le fichier {@code .book}
     * @return instance chargée, ou {@code null} en cas d'erreur
     */
    public static LivreOuvertures load(String path) {
        if (path == null || path.isBlank()) {
            System.out.println("[LivreOuvertures] Chemin de livre invalide");
            return null;
        }

        try (InputStream flux = new FileInputStream(path)) {
            return chargerDepuisFlux(flux, path);
        } catch (FileNotFoundException e) {
            System.out.println("[LivreOuvertures] Fichier non trouvé: " + path);
            return null;
        } catch (IOException e) {
            System.err.println("[LivreOuvertures] Erreur lecture (" + path + "): " + e.getMessage());
            return null;
        }
    }

    // lit le contenu binaire depuis un flux déjà ouvert
    private static LivreOuvertures chargerDepuisFlux(InputStream flux, String source) {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(flux))) {
            HashMap<Long, List<BookMoveEntry>> map = new HashMap<>();

            int version        = dis.readInt();
            int nombrePositions = dis.readInt();

            System.out.println("[LivreOuvertures] Source: " + source);
            System.out.println("[LivreOuvertures] Version: " + version + ", Positions: " + nombrePositions);

            // lecture séquentielle de toutes les entrées
            while (dis.available() > 0) {
                try {
                    long   hashKey = dis.readLong();
                    byte   fromSq  = dis.readByte();
                    byte   toSq    = dis.readByte();
                    byte   promo   = dis.readByte();
                    String name    = dis.readUTF();
                    int    freq    = dis.readInt();

                    BookMoveEntry entree = new BookMoveEntry();
                    entree.fromSquare  = fromSq;
                    entree.toSquare    = toSq;
                    entree.promoPiece  = promo;
                    entree.openingName = name;
                    entree.frequency   = freq;

                    map.computeIfAbsent(hashKey, k -> new ArrayList<>()).add(entree);
                } catch (IOException e) {
                    break;
                }
            }

            // tri de chaque position par fréquence décroissante
            for (List<BookMoveEntry> liste : map.values()) {
                liste.sort((a, b) -> Integer.compare(b.frequency, a.frequency));
            }

            System.out.println("[LivreOuvertures] Chargé " + map.size() + " positions, "
                    + map.values().stream().mapToInt(List::size).sum() + " coups");

            return new LivreOuvertures(map);
        } catch (IOException e) {
            System.err.println("[LivreOuvertures] Erreur lecture (" + source + "): " + e.getMessage());
            return null;
        }
    }

    /**
     * Charge le livre depuis le classpath, avec repli sur le disque.
     *
     * @return instance chargée, ou {@code null} si introuvable
     */
    public static LivreOuvertures loadDefault() {
        InputStream ressource = LivreOuvertures.class.getResourceAsStream(DEFAULT_BOOK_RESOURCE);
        if (ressource != null) {
            LivreOuvertures livre = chargerDepuisFlux(ressource, "classpath:" + DEFAULT_BOOK_RESOURCE);
            if (livre != null) {
                return livre;
            }
        } else {
            System.out.println("[LivreOuvertures] Ressource absente du classpath: " + DEFAULT_BOOK_RESOURCE);
        }
        return load(DEFAULT_BOOK_PATH);
    }

    /**
     * Retourne l'instance singleton (chargement paresseux).
     *
     * @return instance unique du livre, ou {@code null} si non chargé
     */
    public static LivreOuvertures getInstance() {
        if (instance == null) {
            synchronized (LivreOuvertures.class) {
                if (instance == null) {
                    instance = loadDefault();
                }
            }
        }
        return instance;
    }

    /**
     * Recharge le livre depuis un nouveau fichier.
     *
     * @param path chemin vers le nouveau fichier {@code .book}
     */
    public static void reload(String path) {
        synchronized (LivreOuvertures.class) {
            instance = load(path);
        }
    }

    /**
     * Retourne les meilleurs candidats du livre pour la position courante.
     *
     * @param plateau     position courante
     * @param coupsLegaux coups légaux disponibles (filtre les coups illégaux)
     * @param topK        nombre maximal de candidats retournés
     * @return liste de candidats valides, triés par fréquence
     */
    public List<BookCandidate> getCandidates(Plateau plateau, List<Coup> coupsLegaux, int topK) {
        if (plateau == null || coupsLegaux == null || coupsLegaux.isEmpty()) {
            return Collections.emptyList();
        }
        ClePosition cle = ClePosition.fromPlateau(plateau);
        return getCandidates(cle, coupsLegaux, plateau.trait(), topK);
    }

    /**
     * Surcharge utilitaire : déduit la couleur au trait depuis le premier coup légal.
     *
     * @param cle         clé de position
     * @param coupsLegaux coups légaux disponibles
     * @param topK        nombre maximal de candidats
     * @return liste de candidats valides
     */
    public List<BookCandidate> getCandidates(ClePosition cle, List<Coup> coupsLegaux, int topK) {
        if (coupsLegaux == null || coupsLegaux.isEmpty()) {
            return Collections.emptyList();
        }
        Couleur trait = coupsLegaux.get(0).pieceDeplacee().couleur();
        return getCandidates(cle, coupsLegaux, trait, topK);
    }

    /**
     * Retourne les meilleurs candidats du livre pour une {@link ClePosition} donnée.
     *
     * @param cle         clé de position
     * @param coupsLegaux coups légaux disponibles
     * @param trait       couleur du joueur au trait
     * @param topK        nombre maximal de candidats
     * @return liste de candidats valides et légaux
     */
    public List<BookCandidate> getCandidates(ClePosition cle, List<Coup> coupsLegaux,
                                              Couleur trait, int topK) {
        if (cle == null || coupsLegaux == null || coupsLegaux.isEmpty()) {
            return Collections.emptyList();
        }
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }

        List<BookMoveEntry> coups = entries.get(cle.toHashKey());
        if (coups == null || coups.isEmpty()) {
            return Collections.emptyList();
        }

        int limite = topK <= 0 ? coups.size() : Math.min(topK, coups.size());
        List<BookCandidate> candidats = new ArrayList<>(limite);

        for (BookMoveEntry bme : coups) {
            Coup coupLegal = trouverCoupLegal(bme, coupsLegaux, trait);
            if (coupLegal == null) continue;

            candidats.add(new BookCandidate(
                    coupLegal,
                    bme.frequency,
                    bme.openingName != null ? bme.openingName : "Unknown"));

            if (candidats.size() >= limite) break;
        }

        return candidats.isEmpty() ? Collections.emptyList() : candidats;
    }

    /**
     * Recherche le meilleur coup dans le livre (avec score heuristique interne).
     *
     * @param plateau     position actuelle
     * @param coupsLegaux coups légaux disponibles
     * @param plyNumber   numéro du demi-coup courant
     * @return meilleur coup du livre, ou {@code null} si la position est inconnue
     */
    public Coup lookup(Plateau plateau, List<Coup> coupsLegaux, int plyNumber) {
        if (plyNumber > MAX_BOOK_PLY) {
            return null;
        }
        if (entries == null || entries.isEmpty()) {
            return null;
        }

        ClePosition cle    = ClePosition.fromPlateau(plateau);
        long        hashKey = cle.toHashKey();
        List<BookMoveEntry> coupsLivre = entries.get(hashKey);
        if (coupsLivre == null || coupsLivre.isEmpty()) {
            return null;
        }

        Couleur      trait          = plateau.trait();
        BookMoveEntry meilleurEntree = null;
        int           meilleurScore  = Integer.MIN_VALUE;

        for (BookMoveEntry bme : coupsLivre) {
            Coup coupLegal = trouverCoupLegal(bme, coupsLegaux, trait);
            if (coupLegal == null) continue;

            int score = calculerScore(plateau, coupLegal, bme, trait);
            if (score > meilleurScore) {
                meilleurScore  = score;
                meilleurEntree = bme;
            }
        }

        if (meilleurEntree != null) {
            lastOpeningName = meilleurEntree.openingName;
            return trouverCoupLegal(meilleurEntree, coupsLegaux, trait);
        }
        return null;
    }

    // cherche dans la liste de coups légaux celui qui correspond à l'entrée du livre
    private Coup trouverCoupLegal(BookMoveEntry bme, List<Coup> coupsLegaux, Couleur trait) {
        for (Coup c : coupsLegaux) {
            if (c.depart().indice()  != bme.fromSquare) continue;
            if (c.arrivee().indice() != bme.toSquare)   continue;

            // correspondance de la promotion
            if (bme.promoPiece == 0) {
                if (!c.estPromotion()) return c;
            } else {
                if (c.estPromotion() && getPromoCode(c.piecePromotion()) == bme.promoPiece) {
                    return c;
                }
            }
        }
        return null;
    }

    // calcule le score heuristique d'un coup du livre (fréquence + bonifications)
    private int calculerScore(Plateau plateau, Coup coup, BookMoveEntry entree, Couleur trait) {
        int score = entree.frequency * 10;

        Case   arrivee    = coup.arrivee();
        String arriveeStr = arrivee.versAlgebrique();

        // bonus centre (e4/d4/e5/d5)
        if (arriveeStr.equals("e4") || arriveeStr.equals("d4")
                || arriveeStr.equals("e5") || arriveeStr.equals("d5")) {
            score += BONUS_CENTRE;
        }

        // bonus développement cavalier vers le centre
        Piece piece = coup.pieceDeplacee();
        if (piece == Piece.CAVALIER_BLANC || piece == Piece.CAVALIER_NOIR) {
            String dest = arrivee.versAlgebrique();
            if (trait == Couleur.BLANC) {
                if (dest.equals("f3") || dest.equals("g3") || dest.equals("e2")
                        || dest.equals("c3") || dest.equals("d3")) {
                    score += BONUS_DEVELOPPEMENT;
                }
            } else {
                if (dest.equals("f6") || dest.equals("g6") || dest.equals("e7")
                        || dest.equals("c6") || dest.equals("d6")) {
                    score += BONUS_DEVELOPPEMENT;
                }
            }
        }

        // bonus développement fou
        if (piece == Piece.FOU_BLANC || piece == Piece.FOU_NOIR) {
            score += BONUS_DEVELOPPEMENT;
        }

        // bonus roque
        if (coup.estRoque()) {
            score += BONUS_ROQUE;
        }

        // pénalité pour pions de bord peu fréquents en début de partie
        if (!coup.estPromotion()) {
            String dest = arrivee.versAlgebrique();
            if ((dest.equals("a3") || dest.equals("h3")
                    || dest.equals("a4") || dest.equals("h4")) && entree.frequency < 5) {
                score += PENALITE_COUP_SENS;
            }
            if ((dest.equals("b4") || dest.equals("g4")) && entree.frequency < 3) {
                score += PENALITE_COUP_SENS;
            }
        }

        // pénalité si le coup expose notre roi
        Plateau copie = plateau.copie();
        copie.jouer(coup);
        if (copie.estEnEchec(trait)) {
            score -= 100;
        }

        return score;
    }

    // retourne le code de promotion (1=Dame, 2=Tour, 3=Fou, 4=Cavalier)
    private int getPromoCode(Piece p) {
        if (p == null) return 0;
        return switch (p) {
            case DAME_BLANCHE,   DAME_NOIRE    -> 1;
            case TOUR_BLANC,     TOUR_NOIRE    -> 2;
            case FOU_BLANC,      FOU_NOIR      -> 3;
            case CAVALIER_BLANC, CAVALIER_NOIR -> 4;
            default -> 0;
        };
    }

    /**
     * Retourne le nom de la dernière ouverture identifiée.
     *
     * @return nom de l'ouverture
     */
    public String getLastOpeningName() {
        return lastOpeningName;
    }

    /**
     * Indique si le livre est chargé et non vide.
     *
     * @return {@code true} si le livre contient des entrées
     */
    public boolean isLoaded() {
        return entries != null && !entries.isEmpty();
    }

    /**
     * Retourne le nombre de positions stockées dans le livre.
     *
     * @return nombre de positions
     */
    public int size() {
        return entries != null ? entries.size() : 0;
    }

    // entrée interne du livre (non exposée à l'extérieur)
    private static class BookMoveEntry {
        int    fromSquare;
        int    toSquare;
        int    promoPiece;
        String openingName = "Unknown";
        int    frequency   = 1;
    }

    /**
     * Candidat de coup issu du livre d'ouvertures, exposé à la couche IA.
     */
    public static final class BookCandidate {
        private final Coup   move;
        private final int    moveCount;
        private final String openingName;

        private BookCandidate(Coup move, int moveCount, String openingName) {
            this.move        = move;
            this.moveCount   = moveCount;
            this.openingName = openingName;
        }

        /**
         * @return coup candidat du livre
         */
        public Coup getMove() { return move; }

        /**
         * @return fréquence du coup dans la base de données
         */
        public int getMoveCount() { return moveCount; }

        /**
         * @return nom de l'ouverture associée
         */
        public String getOpeningName() { return openingName; }
    }
}
