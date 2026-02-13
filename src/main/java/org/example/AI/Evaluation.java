package org.example.AI;

import org.example.chess.*;

import java.util.List;

/**
 * <p>
 * La classe {@code Evaluation} donne une note à une position d'échecs.
 * </p>
 *
 * <p>
 * Cette version est <b>à phases</b> :
 * </p>
 * <ul>
 *   <li><b>Début/Milieu</b> : matériel + mobilité</li>
 *   <li><b>Finale</b> : matériel + activité du roi + pions avancés/poussés</li>
 * </ul>
 *
 * <p>
 * On calcule une phase entre 0 et 1 :
 * </p>
 * <ul>
 *   <li>phase = 1 : début/milieu</li>
 *   <li>phase = 0 : finale</li>
 * </ul>
 *
 * <p>
 * Puis on mélange :
 * </p>
 * <p>
 * {@code score = phase * scoreDebut + (1 - phase) * scoreFinale}
 * </p>
 */
public final class Evaluation {

    /** Valeurs classiques en centi-pions (pion = 100). */
    public static final int VALEUR_PION = 100;
    public static final int VALEUR_CAVALIER = 320;
    public static final int VALEUR_FOU = 330;
    public static final int VALEUR_TOUR = 500;
    public static final int VALEUR_DAME = 900;

    /** Score énorme utilisé pour représenter un mat. */
    public static final int SCORE_MAT = 1_000_000;

    /** Petit bonus de mobilité. */
    public static final int BONUS_MOBILITE_PAR_COUP = 2;

    /* =========================
       Phase (début/milieu/fin)
       ========================= */

    // Poids de phase (classique) : dame=4, tour=2, fou=1, cavalier=1, pion=0.
    private static final int POIDS_PHASE_DAME = 4;
    private static final int POIDS_PHASE_TOUR = 2;
    private static final int POIDS_PHASE_FOU = 1;
    private static final int POIDS_PHASE_CAVALIER = 1;

    // Phase max au début : 2 dames + 4 tours + 4 fous + 4 cavaliers
    // = 2*4 + 4*2 + 4*1 + 4*1 = 8 + 8 + 4 + 4 = 24
    private static final int PHASE_MAX = 24;

    /* =========================
       Finale : bonus simples
       ========================= */

    // Bonus d'activité du roi en finale (faible mais efficace)
    private static final int BONUS_ROI_CENTRE_FINALE = 15; // par "proximité centre"

    // Bonus pour pions avancés (très simple)
    private static final int BONUS_PION_AVANCE = 6;

    private Evaluation() { }

    /**
     * <p>Évalue la position du point de vue des Blancs.</p>
     *
     * <ul>
     *   <li>Score > 0 : avantage Blanc</li>
     *   <li>Score < 0 : avantage Noir</li>
     * </ul>
     */
    public static int evaluer(Plateau plateau) {
        if (plateau == null) throw new IllegalArgumentException("plateau null");

        // Protection positions terminales
        List<Coup> coupsLegaux = GenerateurCoups.genererLegaux(plateau);
        if (coupsLegaux.isEmpty()) {
            boolean enEchec = plateau.estEnEchec(plateau.trait());
            if (enEchec) {
                return (plateau.trait() == Couleur.BLANC) ? -SCORE_MAT : SCORE_MAT;
            }
            return 0; // pat
        }

        double phase = phase(plateau); // 1=ouverture/milieu ; 0=finale

        int scoreDebut = 0;
        scoreDebut += scoreMateriel(plateau);
        scoreDebut += scoreMobilite(plateau);

        int scoreFinale = 0;
        scoreFinale += scoreMateriel(plateau);
        scoreFinale += scoreRoiActifFinale(plateau);
        scoreFinale += scorePionsFinale(plateau);

        // Mélange (interpolation)
        double score = phase * scoreDebut + (1.0 - phase) * scoreFinale;

        // Arrondi propre
        return (int) Math.round(score);
    }

    /**
     * <p>
     * Calcule une phase entre 0 et 1.
     * </p>
     * <ul>
     *   <li>1 = beaucoup de pièces (début/milieu)</li>
     *   <li>0 = très peu de pièces (finale)</li>
     * </ul>
     */
    public static double phase(Plateau p) {
        int phasePoints = 0;

        int dames = compter(p.bitboard(Piece.DAME_BLANCHE)) + compter(p.bitboard(Piece.DAME_NOIRE));
        int tours = compter(p.bitboard(Piece.TOUR_BLANC)) + compter(p.bitboard(Piece.TOUR_NOIR));
        int fous = compter(p.bitboard(Piece.FOU_BLANC)) + compter(p.bitboard(Piece.FOU_NOIR));
        int cavaliers = compter(p.bitboard(Piece.CAVALIER_BLANC)) + compter(p.bitboard(Piece.CAVALIER_NOIR));

        phasePoints += dames * POIDS_PHASE_DAME;
        phasePoints += tours * POIDS_PHASE_TOUR;
        phasePoints += fous * POIDS_PHASE_FOU;
        phasePoints += cavaliers * POIDS_PHASE_CAVALIER;

        if (phasePoints < 0) phasePoints = 0;
        if (phasePoints > PHASE_MAX) phasePoints = PHASE_MAX;

        return phasePoints / (double) PHASE_MAX;
    }

    /* =========================
       Scores composants
       ========================= */

    private static int scoreMateriel(Plateau p) {
        int blancs = 0;
        int noirs = 0;

        blancs += compter(p.bitboard(Piece.PION_BLANC)) * VALEUR_PION;
        blancs += compter(p.bitboard(Piece.CAVALIER_BLANC)) * VALEUR_CAVALIER;
        blancs += compter(p.bitboard(Piece.FOU_BLANC)) * VALEUR_FOU;
        blancs += compter(p.bitboard(Piece.TOUR_BLANC)) * VALEUR_TOUR;
        blancs += compter(p.bitboard(Piece.DAME_BLANCHE)) * VALEUR_DAME;

        noirs += compter(p.bitboard(Piece.PION_NOIR)) * VALEUR_PION;
        noirs += compter(p.bitboard(Piece.CAVALIER_NOIR)) * VALEUR_CAVALIER;
        noirs += compter(p.bitboard(Piece.FOU_NOIR)) * VALEUR_FOU;
        noirs += compter(p.bitboard(Piece.TOUR_NOIR)) * VALEUR_TOUR;
        noirs += compter(p.bitboard(Piece.DAME_NOIRE)) * VALEUR_DAME;

        return blancs - noirs;
    }

    private static int scoreMobilite(Plateau p) {
        int nbCoups = GenerateurCoups.genererLegaux(p).size();
        int bonus = nbCoups * BONUS_MOBILITE_PAR_COUP;
        return (p.trait() == Couleur.BLANC) ? bonus : -bonus;
    }

    /**
     * <p>
     * Finale : bonus pour un roi actif.
     * </p>
     *
     * <p>
     * Idée simple :
     * </p>
     * <ul>
     *   <li>En finale, le roi doit aller vers le centre.</li>
     *   <li>On récompense la proximité du centre (d4/e4/d5/e5).</li>
     * </ul>
     */
    private static int scoreRoiActifFinale(Plateau p) {
        // Trouver les cases des rois
        Case roiBlanc = trouverCaseUnique(p.bitboard(Piece.ROI_BLANC));
        Case roiNoir = trouverCaseUnique(p.bitboard(Piece.ROI_NOIR));
        if (roiBlanc == null || roiNoir == null) return 0;

        // Distance au centre (plus c’est petit, mieux c’est)
        int distBlanc = distanceCentre(roiBlanc);
        int distNoir = distanceCentre(roiNoir);

        // On donne un bonus inverse à la distance
        // Exemple : dist 0 => bonus max, dist 6 => petit bonus
        int bonusBlanc = (6 - distBlanc) * BONUS_ROI_CENTRE_FINALE;
        int bonusNoir = (6 - distNoir) * BONUS_ROI_CENTRE_FINALE;

        return bonusBlanc - bonusNoir;
    }

    /**
     * <p>
     * Finale : bonus pour pions avancés (très simple).
     * </p>
     * <p>
     * Un pion blanc est "avancé" s’il est plus proche de la 8e rangée,
     * et un pion noir s’il est plus proche de la 1re.
     * </p>
     */
    private static int scorePionsFinale(Plateau p) {
        long wp = p.bitboard(Piece.PION_BLANC);
        long bp = p.bitboard(Piece.PION_NOIR);

        int bonusBlanc = 0;
        int bonusNoir = 0;

        // Pour chaque pion blanc : plus il est "haut" (vers la 8e), plus bonus
        long tmp = wp;
        while (tmp != 0) {
            int idx = Long.numberOfTrailingZeros(tmp);
            tmp &= (tmp - 1);

            int ligne = idx / 8; // 0..7 (0=a8)
            // pion blanc avance vers le haut => ligne petite = plus avancé
            int avance = (7 - ligne); // 0..7 (0 tout en haut a8)
            bonusBlanc += avance * BONUS_PION_AVANCE;
        }

        // Pour chaque pion noir : plus il est "bas" (vers la 1re), plus bonus
        tmp = bp;
        while (tmp != 0) {
            int idx = Long.numberOfTrailingZeros(tmp);
            tmp &= (tmp - 1);

            int ligne = idx / 8;
            // pion noir avance vers le bas => ligne grande = plus avancé
            int avance = ligne; // 0..7
            bonusNoir += avance * BONUS_PION_AVANCE;
        }

        return bonusBlanc - bonusNoir;
    }

    private static int compter(long bitboard) {
        return Long.bitCount(bitboard);
    }

    /**
     * Retourne la case unique correspondant au bitboard (qui doit contenir 1 seul bit).
     */
    private static Case trouverCaseUnique(long bitboardUnique) {
        if (bitboardUnique == 0) return null;
        int idx = Long.numberOfTrailingZeros(bitboardUnique);
        return Case.depuisIndice(idx);
    }

    /**
     * Distance "simple" au centre (d4/e4/d5/e5).
     * On calcule la distance de Manhattan à la case centrale la plus proche.
     */
    private static int distanceCentre(Case c) {
        int col = c.colonne();
        int lig = c.ligne();

        // centres : d4,e4,d5,e5 -> en indices (col,lig) = (3,4),(4,4),(3,3),(4,3) selon ta convention
        // Attention : avec ta convention 0=a8, la notion de "ligne" est inversée visuellement,
        // mais ici on travaille juste avec les coordonnées internes, ça reste cohérent.
        int[][] centres = new int[][] {
                {3, 3}, {4, 3}, {3, 4}, {4, 4}
        };

        int best = Integer.MAX_VALUE;
        for (int[] centre : centres) {
            int dc = Math.abs(col - centre[0]);
            int dl = Math.abs(lig - centre[1]);
            best = Math.min(best, dc + dl);
        }
        return best;
    }
}
