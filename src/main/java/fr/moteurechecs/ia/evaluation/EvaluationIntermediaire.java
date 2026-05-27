package fr.moteurechecs.ia.evaluation;

import fr.moteurechecs.plateau.Couleur;
import fr.moteurechecs.plateau.GenerateurCoups;
import fr.moteurechecs.plateau.Plateau;
import fr.moteurechecs.ia.Evaluation;

/**
 * Évaluation intermédiaire : matériel + structure de pions + mobilité.
 *
 * Utilisée par le niveau MOYEN : plus précise que {@link EvaluationSimple}
 * car elle tient compte de la mobilité des pièces et de la structure de pions,
 * mais moins coûteuse que l'évaluation complète pondérée par phase de jeu.
 */
public final class EvaluationIntermediaire implements FonctionEvaluation {

    @Override
    public int evaluer(Plateau plateau) {
        if (GenerateurCoups.genererLegaux(plateau).isEmpty()) {
            return evaluerTerminale(plateau);
        }
        return Materiel.scoreMateriel(plateau)
             + Mobilite.scoreMobilite(plateau)
             + StructurePions.scorePionsFinale(plateau);
    }

    /** Évalue une position terminale (mat ou pat). */
    private static int evaluerTerminale(Plateau plateau) {
        if (plateau.estEnEchec(plateau.trait())) {
            return (plateau.trait() == Couleur.BLANC) ? -Evaluation.SCORE_MAT : Evaluation.SCORE_MAT;
        }
        return 0; // pat
    }
}
