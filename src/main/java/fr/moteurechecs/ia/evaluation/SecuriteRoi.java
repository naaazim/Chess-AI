package fr.moteurechecs.ia.evaluation;

import fr.moteurechecs.plateau.Arbitre;
import fr.moteurechecs.plateau.Case;
import fr.moteurechecs.plateau.Couleur;
import fr.moteurechecs.plateau.Masques;
import fr.moteurechecs.plateau.Piece;
import fr.moteurechecs.plateau.Plateau;

/**
 * Évalue la sécurité et l'activité du roi selon la phase de jeu.
 *
 * En milieu de jeu : malus progressif si des pièces adverses attaquent
 * les cases autour du roi allié.<br>
 * En finale : bonus de centralisation pour inciter le roi à jouer activement.
 */
public final class SecuriteRoi {

    /**
     * Bonus par unité de proximité au centre pour le roi en phase de finale.
     * Valeur élevée pour forcer l'IA à centraliser activement son roi
     * et à pénétrer dans la position adverse.
     */
    private static final int BONUS_ROI_CENTRE_FINALE = 35;

    /**
     * Malus de base par case adjacente au roi attaquée par l'adversaire, en centipions.
     * Le malus total est le carré du nombre de cases attaquées × cette constante,
     * ce qui crée une progressivité : un roi légèrement exposé subit un faible malus,
     * un roi très exposé subit un malus sévère.
     */
    private static final int MALUS_CASE_ROI_ATTAQUEE = 8;

    private SecuriteRoi() {
    }

    /**
     * Calcule le score d'activité du roi pour la finale.
     *
     * @param plateau la position à évaluer
     * @return score en centipions (positif = avantage Blancs)
     */
    public static int scoreRoiActifFinale(Plateau plateau) {
        Case roiBlanc = UtilitairesEvaluation.trouverCaseUnique(plateau.bitboard(Piece.ROI_BLANC));
        Case roiNoir  = UtilitairesEvaluation.trouverCaseUnique(plateau.bitboard(Piece.ROI_NOIR));
        if (roiBlanc == null || roiNoir == null)
            return 0;

        int bonusBlanc = calculerBonusCentralisation(roiBlanc);
        int bonusNoir  = calculerBonusCentralisation(roiNoir);
        return bonusBlanc - bonusNoir;
    }

    /**
     * Évalue la sécurité du roi en milieu de jeu.
     *
     * Calcule un malus basé sur le nombre de pièces adverses qui attaquent
     * les cases autour du roi. Le malus est progressif (carré du nombre de cases
     * attaquées) pour refléter le danger croissant quand le roi est très exposé.
     *
     * @param plateau la position à évaluer
     * @return score en centipions (positif = avantage Blancs)
     */
    public static int scoreSecuriteRoiMilieuJeu(Plateau plateau) {
        int malusRoiBlanc = 0;
        Case roiBlanc = UtilitairesEvaluation.trouverCaseUnique(plateau.bitboard(Piece.ROI_BLANC));
        if (roiBlanc != null) {
            long casesAutourRoi   = Masques.attaquesRoi(roiBlanc.indice());
            long attaquesAdverses = Arbitre.casesAttaqueesPar(plateau, Couleur.NOIR);
            int  casesAttaquees   = Long.bitCount(casesAutourRoi & attaquesAdverses);
            malusRoiBlanc = casesAttaquees * casesAttaquees * MALUS_CASE_ROI_ATTAQUEE;
        }

        int malusRoiNoir = 0;
        Case roiNoir = UtilitairesEvaluation.trouverCaseUnique(plateau.bitboard(Piece.ROI_NOIR));
        if (roiNoir != null) {
            long casesAutourRoi   = Masques.attaquesRoi(roiNoir.indice());
            long attaquesAdverses = Arbitre.casesAttaqueesPar(plateau, Couleur.BLANC);
            int  casesAttaquees   = Long.bitCount(casesAutourRoi & attaquesAdverses);
            malusRoiNoir = casesAttaquees * casesAttaquees * MALUS_CASE_ROI_ATTAQUEE;
        }

        // malusRoiBlanc est un désavantage blanc → soustrait ; malusRoiNoir est un avantage blanc → ajouté
        return -malusRoiBlanc + malusRoiNoir;
    }

    // bonus de centralisation pour un roi (plus proche du centre = plus grand bonus)
    private static int calculerBonusCentralisation(Case roi) {
        int distanceCentre = distanceCentre(roi);
        return (6 - distanceCentre) * BONUS_ROI_CENTRE_FINALE;
    }

    // distance de Manhattan minimale vers les 4 cases centrales
    private static int distanceCentre(Case caseRoi) {
        int col = caseRoi.colonne();
        int lig = caseRoi.ligne();

        int[][] centres = {{3, 3}, {4, 3}, {3, 4}, {4, 4}};
        int meilleure = Integer.MAX_VALUE;
        for (int[] centre : centres) {
            int dc = Math.abs(col - centre[0]);
            int dl = Math.abs(lig - centre[1]);
            meilleure = Math.min(meilleure, dc + dl);
        }
        return meilleure;
    }
}
