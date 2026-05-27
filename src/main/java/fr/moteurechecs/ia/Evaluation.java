package fr.moteurechecs.ia;

import fr.moteurechecs.plateau.Case;
import fr.moteurechecs.plateau.Coup;
import fr.moteurechecs.plateau.Couleur;
import fr.moteurechecs.plateau.GenerateurCoups;
import fr.moteurechecs.plateau.Masques;
import fr.moteurechecs.plateau.Piece;
import fr.moteurechecs.plateau.Plateau;
import fr.moteurechecs.ia.evaluation.PhaseDeJeu;
import fr.moteurechecs.ia.evaluation.Materiel;
import fr.moteurechecs.ia.evaluation.Mobilite;
import fr.moteurechecs.ia.evaluation.TablePositionnelle;
import fr.moteurechecs.ia.evaluation.SecuriteRoi;
import fr.moteurechecs.ia.evaluation.StructurePions;
import fr.moteurechecs.ia.evaluation.UtilitairesEvaluation;

import java.util.List;

/**
 * Évalue une position d'échecs en centipions.
 *
 * L'évaluation est pondérée par la phase de jeu :
 * - OUVERTURE / MILIEU DE JEU : matériel + mobilité + PST
 * - FINALE : matériel + activité du roi + pions avancés + PST
 *
 * Un score positif favorise les Blancs, négatif les Noirs.
 */
public final class Evaluation {

    /** Valeur d'un pion en centipions. */
    public static final int VALEUR_PION     = 100;
    /** Valeur d'un cavalier en centipions. */
    public static final int VALEUR_CAVALIER = 320;
    /** Valeur d'un fou en centipions. */
    public static final int VALEUR_FOU      = 330;
    /** Valeur d'une tour en centipions. */
    public static final int VALEUR_TOUR     = 500;
    /** Valeur d'une dame en centipions. */
    public static final int VALEUR_DAME     = 900;

    /** Score utilisé pour représenter un mat (valeur sentinelle). */
    public static final int SCORE_MAT = 1_000_000;

    /**
     * Bonus par unité de distance au bord pour le roi adverse dans le calcul de coincement.
     * Plus le roi adverse est proche d'un bord, plus ce bonus est élevé.
     */
    private static final int BONUS_ROI_ADVERSE_AU_BORD = 40;

    /**
     * Bonus par unité de proximité entre les deux rois dans le calcul de coincement.
     * Plus notre roi est proche du roi adverse (pour aider à le coincer), plus ce bonus est élevé.
     */
    private static final int BONUS_PROXIMITE_ROIS = 10;

    /**
     * Malus de base pour une dame éloignée de son roi quand celui-ci est attaqué sur
     * 3 cases ou plus. Évite les captures de pions de bord aventurées en milieu de jeu
     * tendu, où la dame doit rester à portée de son roi pour contre-attaquer.
     */
    private static final int MALUS_DAME_AVENTUREE = 6;

    private Evaluation() {
    }

    /**
     * Évalue la position courante du plateau.
     *
     * @param plateau la position à évaluer
     * @return score en centipions (positif = avantage Blancs)
     * @throws IllegalArgumentException si le plateau est null
     */
    public static int evaluer(Plateau plateau) {
        if (plateau == null)
            throw new IllegalArgumentException("plateau null");

        // vérification mat / pat (aucun coup légal)
        List<Coup> coupsLegaux = GenerateurCoups.genererLegaux(plateau);
        if (coupsLegaux.isEmpty()) {
            return evaluerPositionTerminale(plateau);
        }

        double phase = PhaseDeJeu.calculerPhase(plateau);
        int scorePhase    = calculerScoreInterpolePourPhase(plateau, phase);
        int scorePST      = TablePositionnelle.scorePositionnelPieceSquare(plateau, phase);
        int scoreConclusion   = calculerBonusConclusion(plateau);
        int scoreDameAventuree = calculerMalusDameAventuree(plateau);

        return scorePhase + scorePST + scoreConclusion + scoreDameAventuree;
    }

    // retourne le score d'une position terminale (mat ou pat)
    private static int evaluerPositionTerminale(Plateau plateau) {
        boolean enEchec = plateau.estEnEchec(plateau.trait());
        if (enEchec) {
            return (plateau.trait() == Couleur.BLANC) ? -SCORE_MAT : SCORE_MAT;
        }
        return 0; // pat
    }

    // interpolation linéaire entre le score milieu de jeu et le score finale
    private static int calculerScoreInterpolePourPhase(Plateau plateau, double phase) {
        int scoreDebut  = calculerScoreDebutMilieu(plateau);
        int scoreFinale = calculerScoreFinale(plateau);
        return (int) Math.round(phase * scoreDebut + (1.0 - phase) * scoreFinale);
    }

    // score pour ouverture et milieu de jeu
    private static int calculerScoreDebutMilieu(Plateau plateau) {
        return Materiel.scoreMateriel(plateau)
             + Mobilite.scoreMobilite(plateau)
             + SecuriteRoi.scoreSecuriteRoiMilieuJeu(plateau);
    }

    // score pour la finale
    private static int calculerScoreFinale(Plateau plateau) {
        return Materiel.scoreMateriel(plateau)
             + SecuriteRoi.scoreRoiActifFinale(plateau)
             + StructurePions.scorePionsFinale(plateau);
    }

    /**
     * Calcule un bonus de coincement lorsqu'un camp est massivement dominant (avantage ≥ une dame).
     *
     * Ce bonus incite l'IA à repousser le roi adverse vers les bords et les coins,
     * et à rapprocher son propre roi pour aider au mat, dans les positions où l'avantage
     * matériel est si écrasant que la conversion doit être accélérée.
     *
     * @param plateau la position à évaluer
     * @return bonus en centipions (positif = avantage Blancs, négatif = avantage Noirs)
     */
    private static int calculerBonusConclusion(Plateau plateau) {
        int avantageBrut = Materiel.scoreMateriel(plateau);
        if (avantageBrut >= VALEUR_DAME) {
            return bonusCoincement(plateau, Couleur.BLANC);
        }
        if (avantageBrut <= -VALEUR_DAME) {
            return -bonusCoincement(plateau, Couleur.NOIR);
        }
        return 0;
    }

    /**
     * Calcule le bonus de coincement du côté du camp gagnant.
     *
     * Deux composantes :
     * - Distance du roi adverse au bord le plus proche : plus il est au bord, mieux c'est.
     * - Distance de Manhattan entre les deux rois : plus nos rois sont proches, mieux c'est.
     *
     * @param plateau  la position à évaluer
     * @param gagnant  couleur du camp gagnant (celui qui cherche à coincer)
     * @return bonus en centipions (toujours positif ; le signe est géré par l'appelant)
     */
    private static int bonusCoincement(Plateau plateau, Couleur gagnant) {
        Couleur perdant = gagnant.inverse();

        Case roiGagnant = UtilitairesEvaluation.trouverCaseUnique(
                plateau.bitboard(gagnant == Couleur.BLANC ? Piece.ROI_BLANC : Piece.ROI_NOIR));
        Case roiPerdant = UtilitairesEvaluation.trouverCaseUnique(
                plateau.bitboard(perdant == Couleur.BLANC ? Piece.ROI_BLANC : Piece.ROI_NOIR));

        if (roiGagnant == null || roiPerdant == null) return 0;

        // Distance du roi adverse au bord le plus proche (0 = coin, 3-4 = centre)
        // colonne() ∈ [0..7], ligne() ∈ [1..8]
        int col = roiPerdant.colonne();   // 0..7
        int lig = roiPerdant.ligne();     // 1..8
        int distanceBordRoiAdverse = Math.min(
                Math.min(col, 7 - col),
                Math.min(lig - 1, 8 - lig));
        int bonusBord = (4 - distanceBordRoiAdverse) * BONUS_ROI_ADVERSE_AU_BORD;

        // Distance de Manhattan entre les deux rois (max = 14)
        int distanceEntreRois = Math.abs(roiGagnant.colonne() - roiPerdant.colonne())
                              + Math.abs(roiGagnant.ligne()   - roiPerdant.ligne());
        int bonusProximite = (14 - distanceEntreRois) * BONUS_PROXIMITE_ROIS;

        return bonusBord + bonusProximite;
    }

    /**
     * Malus si la dame s'éloigne de son roi alors que celui-ci est attaqué sur plusieurs
     * cases adjacentes. Évite les captures de pions de bord aventurées (Qxa6, Qxa7…)
     * en milieu de jeu tendu où le roi allié est sous pression.
     *
     * Le malus est appliqué uniquement lorsque les deux conditions suivantes sont
     * réunies simultanément :
     * - Au moins 3 cases autour du roi allié sont attaquées par l'adversaire.
     * - La distance de Manhattan dame–roi est ≥ 4.
     *
     * @param plateau la position à évaluer
     * @return malus en centipions (positif = avantage Blancs, i.e. les Noirs sont pénalisés)
     */
    private static int calculerMalusDameAventuree(Plateau plateau) {
        int malusBlanc = 0;
        Case dameBlanc = UtilitairesEvaluation.trouverCaseUnique(plateau.bitboard(Piece.DAME_BLANCHE));
        if (dameBlanc != null) {
            Case roiBlanc = UtilitairesEvaluation.trouverCaseUnique(plateau.bitboard(Piece.ROI_BLANC));
            if (roiBlanc != null) {
                int dist = Math.abs(dameBlanc.colonne() - roiBlanc.colonne())
                         + Math.abs(dameBlanc.ligne()   - roiBlanc.ligne());
                long casesAutour    = Masques.attaquesRoi(roiBlanc.indice());
                long attaquesNoires = plateau.casesAttaqueesPar(Couleur.NOIR);
                int  casesAttaquees = Long.bitCount(casesAutour & attaquesNoires);
                if (casesAttaquees >= 3 && dist >= 4) {
                    malusBlanc = (dist - 3) * casesAttaquees * MALUS_DAME_AVENTUREE;
                }
            }
        }

        int malusNoir = 0;
        Case dameNoir = UtilitairesEvaluation.trouverCaseUnique(plateau.bitboard(Piece.DAME_NOIRE));
        if (dameNoir != null) {
            Case roiNoir = UtilitairesEvaluation.trouverCaseUnique(plateau.bitboard(Piece.ROI_NOIR));
            if (roiNoir != null) {
                int dist = Math.abs(dameNoir.colonne() - roiNoir.colonne())
                         + Math.abs(dameNoir.ligne()   - roiNoir.ligne());
                long casesAutour      = Masques.attaquesRoi(roiNoir.indice());
                long attaquesBlanches = plateau.casesAttaqueesPar(Couleur.BLANC);
                int  casesAttaquees   = Long.bitCount(casesAutour & attaquesBlanches);
                if (casesAttaquees >= 3 && dist >= 4) {
                    malusNoir = (dist - 3) * casesAttaquees * MALUS_DAME_AVENTUREE;
                }
            }
        }

        // malusBlanc est un désavantage blanc → soustrait ; malusNoir est un avantage blanc → ajouté
        return -malusBlanc + malusNoir;
    }
}
