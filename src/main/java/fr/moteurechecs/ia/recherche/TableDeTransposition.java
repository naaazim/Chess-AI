package fr.moteurechecs.ia.recherche;

import fr.moteurechecs.plateau.Coup;
import fr.moteurechecs.plateau.Couleur;
import fr.moteurechecs.plateau.Piece;
import fr.moteurechecs.plateau.Plateau;

import java.util.Arrays;
import java.util.Random;

/**
 * Table de transposition avec hachage Zobrist 64 bits.
 *
 * Taille fixe de {@code 1 << 22} (~4 M entrées), accès sans verrou
 * (stratégie last-write-wins). La table est partagée entre tous les threads
 * de recherche via le singleton.
 */
public final class TableDeTransposition {

    /** Entrée exacte : le score est la valeur minimax précise. */
    public static final int EXACT       = 0;
    /** Borne inférieure : le vrai score est ≥ la valeur stockée (fail-high). */
    public static final int BORNE_INFERIEURE = 1;
    /** Borne supérieure : le vrai score est ≤ la valeur stockée (fail-low). */
    public static final int BORNE_SUPERIEURE = 2;

    private static final int SIZE = 1 << 22; // 4 194 304 entrées
    private static final int MASK = SIZE - 1;

    // ===== Clés Zobrist (graine fixe pour la reproductibilité) =====
    // Ordre : PION_BLANC(0), CAVALIER_BLANC(1), FOU_BLANC(2), TOUR_BLANC(3),
    //         DAME_BLANCHE(4), ROI_BLANC(5), PION_NOIR(6)..ROI_NOIR(11)
    private static final long[][] CLE_PIECE_CASE = new long[12][64];
    private static final long     CLE_TRAIT;
    private static final long[]   CLE_ROQUES    = new long[4];
    private static final long[]   CLE_EN_PASSANT = new long[8];

    static {
        Random aleatoire = new Random(0xDEADBEEFCAFEBABEL);
        for (int p = 0; p < 12; p++)
            for (int sq = 0; sq < 64; sq++)
                CLE_PIECE_CASE[p][sq] = aleatoire.nextLong();
        CLE_TRAIT = aleatoire.nextLong();
        for (int i = 0; i < 4; i++) CLE_ROQUES[i]     = aleatoire.nextLong();
        for (int i = 0; i < 8; i++) CLE_EN_PASSANT[i] = aleatoire.nextLong();
    }

    // ===== Stockage par tableaux parallèles =====
    private final long[] cles;
    private final int[]  scores;
    private final int[]  profondeurs;
    private final int[]  types;
    private final Coup[] meilleursCoups;

    private static final TableDeTransposition INSTANCE = new TableDeTransposition();

    private TableDeTransposition() {
        cles           = new long[SIZE];
        scores         = new int[SIZE];
        profondeurs    = new int[SIZE];
        types          = new int[SIZE];
        meilleursCoups = new Coup[SIZE];
    }

    /**
     * Retourne l'instance singleton de la table.
     *
     * @return table de transposition partagée
     */
    public static TableDeTransposition getInstance() {
        return INSTANCE;
    }

    /**
     * Calcule le hash Zobrist de la position courante.
     *
     * @param plateau la position à hacher
     * @return valeur de hachage 64 bits
     */
    public static long hash(Plateau plateau) {
        long h = 0L;

        // XOR pour chaque pièce présente sur chaque case
        for (Piece piece : Piece.values()) {
            int indicePiece = piece.ordinal();
            long bb = plateau.bitboard(piece);
            while (bb != 0L) {
                int sq = Long.numberOfTrailingZeros(bb);
                h ^= CLE_PIECE_CASE[indicePiece][sq];
                bb &= bb - 1L;
            }
        }

        // contribution du trait, des droits de roque et de la prise en passant
        if (plateau.trait() == Couleur.BLANC)  h ^= CLE_TRAIT;
        if (plateau.getRoqueBlancRoi())         h ^= CLE_ROQUES[0];
        if (plateau.getRoqueBlancReine())       h ^= CLE_ROQUES[1];
        if (plateau.getRoqueNoirRoi())          h ^= CLE_ROQUES[2];
        if (plateau.getRoqueNoirReine())        h ^= CLE_ROQUES[3];

        long enPassant = plateau.getEnPassant();
        if (enPassant != 0L) {
            int colonneEP = Long.numberOfTrailingZeros(enPassant) % 8;
            h ^= CLE_EN_PASSANT[colonneEP];
        }

        return h;
    }

    /**
     * Enregistre une entrée dans la table (last-write-wins).
     *
     * @param empreintePlateau  clé Zobrist de la position
     * @param score         score minimax de la position
     * @param profondeur    profondeur de recherche atteinte
     * @param type          {@link #EXACT}, {@link #BORNE_INFERIEURE} ou {@link #BORNE_SUPERIEURE}
     * @param meilleurCoup  meilleur coup trouvé (peut être {@code null})
     */
    public void store(long empreintePlateau, int score, int profondeur, int type, Coup meilleurCoup) {
        int indice = (int)(empreintePlateau & MASK);
        cles[indice]           = empreintePlateau;
        scores[indice]         = score;
        profondeurs[indice]    = profondeur;
        types[indice]          = type;
        meilleursCoups[indice] = meilleurCoup;
    }

    /**
     * Sonde la table pour une position donnée.
     *
     * @param empreintePlateau clé Zobrist de la position
     * @return l'entrée si le hash correspond, {@code null} sinon
     */
    public EntreeTT probe(long empreintePlateau) {
        int indice = (int)(empreintePlateau & MASK);
        if (cles[indice] != empreintePlateau) return null;
        return new EntreeTT(scores[indice], profondeurs[indice], types[indice], meilleursCoups[indice]);
    }

    /**
     * Vide la table entre deux parties pour éviter les faux hits.
     */
    public void clear() {
        Arrays.fill(cles, 0L);
        Arrays.fill(meilleursCoups, null); // libération mémoire des coups de la partie précédente
    }

    /**
     * Entrée lue depuis la table de transposition.
     */
    public static final class EntreeTT {
        /** Score minimax stocké. */
        public final int  score;
        /** Profondeur à laquelle ce score a été calculé. */
        public final int  profondeur;
        /** Type d'entrée : {@link TableDeTransposition#EXACT}, {@link TableDeTransposition#BORNE_INFERIEURE}
         *  ou {@link TableDeTransposition#BORNE_SUPERIEURE}. */
        public final int  type;
        /** Meilleur coup de cette position (peut être {@code null}). */
        public final Coup meilleurCoup;

        public EntreeTT(int score, int profondeur, int type, Coup meilleurCoup) {
            this.score        = score;
            this.profondeur   = profondeur;
            this.type         = type;
            this.meilleurCoup = meilleurCoup;
        }
    }
}
