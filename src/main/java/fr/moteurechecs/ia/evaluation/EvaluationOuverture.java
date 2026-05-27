package fr.moteurechecs.ia.evaluation;

import fr.moteurechecs.plateau.Piece;
import fr.moteurechecs.plateau.Plateau;

/**
 * Évaluation heuristique d'une position selon les principes d'ouverture.
 *
 * Trois axes sont évalués :
 * - Contrôle du centre (cases d4, e4, d5, e5)
 * - Développement des pièces mineures
 * - Sécurité du roi (roque)
 */
public final class EvaluationOuverture {

    // bitboards des cases centrales
    private static final long CENTRE          = 0x0000001818000000L; // d4, e4, d5, e5
    private static final long CENTRE_ETENDU   = 0x0000003C3C000000L; // zone étendue autour du centre

    // cases de développement pour les cavaliers blancs
    private static final long[] CASES_CAVALIER_BLANC = {
        0x0000000000000400L, // f3
        0x0000000000000800L, // g3
        0x0000000000001000L, // h3
        0x0000000004000000L, // c3
        0x0000000008000000L, // d3
        0x0000000010000000L, // e3
    };

    // cases de développement pour les cavaliers noirs
    private static final long[] CASES_CAVALIER_NOIR = {
        0x0400000000000000L, // f6
        0x0800000000000000L, // g6
        0x1000000000000000L, // h6
        0x0004000000000000L, // c6
        0x0008000000000000L, // d6
        0x0010000000000000L, // e6
    };

    private EvaluationOuverture() {
    }

    /**
     * Évalue la position selon les trois principes d'ouverture.
     *
     * @param plateau la position à évaluer
     * @return score en centipions (positif = avantage Blancs)
     */
    public static int evaluerOuverture(Plateau plateau) {
        return evaluerControleCentre(plateau)
             + evaluerDeveloppement(plateau)
             + evaluerSecuriteRoi(plateau);
    }

    /**
     * Retourne true si la position est encore en phase d'ouverture.
     *
     * @param plateau la position à tester
     * @return true si au moins deux pièces mineures ne sont pas encore développées
     */
    public static boolean estPhaseOuverture(Plateau plateau) {
        long cavaliers = plateau.bitboard(Piece.CAVALIER_BLANC) | plateau.bitboard(Piece.CAVALIER_NOIR);
        long fous      = plateau.bitboard(Piece.FOU_BLANC)      | plateau.bitboard(Piece.FOU_NOIR);

        long casesDepart = 0x0000000000000044L | 0x4400000000000000L; // b1,g1,b8,g8
        long fouDepart   = 0x0000000000000030L | 0x3000000000000000L; // c1,f1,c8,f8

        long cavaliersNonDeveloppes = cavaliers & casesDepart;
        long fousNonDeveloppes      = fous & fouDepart;

        return (Long.bitCount(cavaliersNonDeveloppes) + Long.bitCount(fousNonDeveloppes)) >= 2;
    }

    // évaluation du contrôle du centre par les pions et les cavaliers
    private static int evaluerControleCentre(Plateau plateau) {
        int score = 0;

        long pionsBlancs = plateau.bitboard(Piece.PION_BLANC);
        long pionsNoirs  = plateau.bitboard(Piece.PION_NOIR);

        // bonus pour les pions au centre étendu
        score += Long.bitCount(pionsBlancs & CENTRE_ETENDU) * 30;
        score -= Long.bitCount(pionsNoirs  & CENTRE_ETENDU) * 30;

        // bonus supplémentaire pour les pions exactement au centre
        score += Long.bitCount(pionsBlancs & CENTRE) * 50;
        score -= Long.bitCount(pionsNoirs  & CENTRE) * 50;

        // cavaliers sur les cases de développement vers le centre
        long cavaliersBlancs = plateau.bitboard(Piece.CAVALIER_BLANC);
        long cavaliersNoirs  = plateau.bitboard(Piece.CAVALIER_NOIR);

        for (long caseDevBlanc : CASES_CAVALIER_BLANC) {
            if ((cavaliersBlancs & caseDevBlanc) != 0) score += 20;
        }
        for (long caseDevNoir : CASES_CAVALIER_NOIR) {
            if ((cavaliersNoirs & caseDevNoir) != 0) score -= 20;
        }

        return score;
    }

    // bonus pour le développement des pièces mineures
    private static int evaluerDeveloppement(Plateau plateau) {
        int score = 0;

        // cases de départ des cavaliers
        long cavaliersDepartBlanc = 0x0000000000000004L | 0x0000000000000040L; // b1, g1
        long cavaliersDepartNoir  = 0x0400000000000000L | 0x4000000000000000L; // b8, g8

        long cavaliersDeveloppesBlanc = plateau.bitboard(Piece.CAVALIER_BLANC) & ~cavaliersDepartBlanc;
        long cavaliersDeveloppesNoir  = plateau.bitboard(Piece.CAVALIER_NOIR)  & ~cavaliersDepartNoir;

        score += Long.bitCount(cavaliersDeveloppesBlanc) * 40;
        score -= Long.bitCount(cavaliersDeveloppesNoir)  * 40;

        // cases de départ des fous
        long fousDepartBlanc = 0x0000000000000020L | 0x0000000000000010L; // c1, f1
        long fousDepartNoir  = 0x2000000000000000L | 0x1000000000000000L; // c8, f8

        long fousDeveloppesBlanc = plateau.bitboard(Piece.FOU_BLANC) & ~fousDepartBlanc;
        long fousDeveloppesNoir  = plateau.bitboard(Piece.FOU_NOIR)  & ~fousDepartNoir;

        score += Long.bitCount(fousDeveloppesBlanc) * 35;
        score -= Long.bitCount(fousDeveloppesNoir)  * 35;

        // pénalité si la dame sort trop tôt
        score += evaluerPositionDame(plateau.bitboard(Piece.DAME_BLANCHE), true);
        score += evaluerPositionDame(plateau.bitboard(Piece.DAME_NOIRE),   false);

        // bonus si les droits de roque sont encore disponibles
        if (plateau.getRoqueBlancRoi() || plateau.getRoqueBlancReine()) score += 25;
        if (plateau.getRoqueNoirRoi()  || plateau.getRoqueNoirReine())  score -= 25;

        return score;
    }

    // pénalité si la dame est avancée prématurément
    private static int evaluerPositionDame(long bitboardDame, boolean estBlanc) {
        if (bitboardDame == 0) return 0;
        int indice = Long.numberOfTrailingZeros(bitboardDame);
        int rangee = 8 - (indice / 8);
        if (estBlanc && rangee >= 4) return -15;
        if (!estBlanc && rangee <= 4) return  15;
        return 0;
    }

    // bonus pour la sécurité du roi (position et roque)
    private static int evaluerSecuriteRoi(Plateau plateau) {
        int score = 0;

        boolean roiBlancRoque = plateau.getRoqueBlancRoi() || plateau.getRoqueBlancReine();
        boolean roiNoirRoque  = plateau.getRoqueNoirRoi()  || plateau.getRoqueNoirReine();

        if (roiBlancRoque) score += 50;
        if (roiNoirRoque)  score -= 50;

        score += evaluerPositionRoi(plateau.bitboard(Piece.ROI_BLANC), true);
        score += evaluerPositionRoi(plateau.bitboard(Piece.ROI_NOIR),  false);

        score += evaluerPionsProtectionRoi(plateau, roiBlancRoque, roiNoirRoque);

        return score;
    }

    // bonus/pénalité selon la position du roi (centre vs flanc)
    private static int evaluerPositionRoi(long bitboardRoi, boolean estBlanc) {
        if (bitboardRoi == 0) return 0;
        int indice  = Long.numberOfTrailingZeros(bitboardRoi);
        int rangee  = 8 - (indice / 8);
        int colonne = indice % 8;

        boolean bienPlace = estBlanc
            ? ((rangee <= 2) || (colonne >= 5))
            : ((rangee >= 6) || (colonne <= 2));

        int delta = bienPlace ? 20 : -20;
        return estBlanc ? delta : -delta;
    }

    // bonus pour les pions devant le roi roqué
    private static int evaluerPionsProtectionRoi(Plateau plateau, boolean roiBlancRoque, boolean roiNoirRoque) {
        int score = 0;
        if (roiBlancRoque) {
            long protectionCoteRoi  = 0x0000000000000038L; // f2, g2, h2
            long protectionCoteDame = 0x000000000000000EL; // a2, b2, c2
            score += Long.bitCount(plateau.bitboard(Piece.PION_BLANC) & protectionCoteRoi)  * 15;
            score += Long.bitCount(plateau.bitboard(Piece.PION_BLANC) & protectionCoteDame) * 15;
        }
        if (roiNoirRoque) {
            long protectionCoteRoi  = 0x3800000000000000L; // f7, g7, h7
            long protectionCoteDame = 0x0E00000000000000L; // a7, b7, c7
            score -= Long.bitCount(plateau.bitboard(Piece.PION_NOIR) & protectionCoteRoi)  * 15;
            score -= Long.bitCount(plateau.bitboard(Piece.PION_NOIR) & protectionCoteDame) * 15;
        }
        return score;
    }
}
