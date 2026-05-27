package fr.moteurechecs.ia.evaluation;

import fr.moteurechecs.plateau.Plateau;
import fr.moteurechecs.ia.Evaluation;

/**
 * Évaluation complète pondérée par phase de jeu.
 *
 * Utilisée par le niveau DIFFICILE : délègue à {@link Evaluation#evaluer(Plateau)}
 * qui inclut matériel, PST, structure de pions, sécurité du roi et mobilité,
 * avec interpolation entre milieu de jeu et finale.
 */
public final class EvaluationComplete implements FonctionEvaluation {

    @Override
    public int evaluer(Plateau plateau) {
        return Evaluation.evaluer(plateau);
    }
}
