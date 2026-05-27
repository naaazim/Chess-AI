package fr.moteurechecs.ia.evaluation;

import fr.moteurechecs.plateau.Plateau;

/**
 * Interface de la fonction d'évaluation statique.
 *
 * Chaque niveau de l'IA utilise une implémentation différente pour adapter
 * la précision de l'évaluation à sa capacité de calcul :
 * - {@link EvaluationSimple} — matériel uniquement (FACILE)
 * - {@link EvaluationIntermediaire} — matériel + structure + mobilité (MOYEN)
 * - {@link EvaluationComplete} — évaluation pondérée par phase de jeu (DIFFICILE)
 */
public interface FonctionEvaluation {

    /**
     * Évalue statiquement la position courante.
     *
     * @param plateau la position à évaluer (non null)
     * @return score en centipions — positif = avantage Blancs, négatif = avantage Noirs
     */
    int evaluer(Plateau plateau);
}
