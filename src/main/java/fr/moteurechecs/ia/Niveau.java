package fr.moteurechecs.ia;

import fr.moteurechecs.ia.evaluation.EvaluationComplete;
import fr.moteurechecs.ia.evaluation.EvaluationIntermediaire;
import fr.moteurechecs.ia.evaluation.EvaluationSimple;
import fr.moteurechecs.ia.evaluation.FonctionEvaluation;

/**
 * Niveaux de difficulté disponibles pour l'IA.
 *
 * Chaque niveau définit un ensemble de capacités qui différencient structurellement
 * le comportement de l'IA, au-delà de la simple profondeur de recherche :
 *
 * - FACILE — recherche naïve (pas d'élagage, pas de tri), évaluation matérielle.
 * - MOYEN — alpha-bêta + tri des coups, évaluation intermédiaire.
 * - DIFFICILE — toutes les optimisations actives, évaluation complète.
 */
public enum Niveau {

    //                        prof  α-β            tri                 book                     TT                          éval
    FACILE   (3, false, false, false, false, new EvaluationSimple()),
    MOYEN    (5, true,  true,  false, false, new EvaluationIntermediaire()),
    DIFFICILE(8, true,  true,  true,  true,  new EvaluationComplete());

    private final int               profondeur;
    private final boolean           useAlphaBeta;
    private final boolean           useMoveSorting;
    private final boolean           useOpeningBook;
    private final boolean           useTranspositionTable;
    private final FonctionEvaluation fonctionEvaluation;

    Niveau(int profondeur,
           boolean useAlphaBeta,
           boolean useMoveSorting,
           boolean useOpeningBook,
           boolean useTranspositionTable,
           FonctionEvaluation fonctionEvaluation) {
        this.profondeur            = profondeur;
        this.useAlphaBeta          = useAlphaBeta;
        this.useMoveSorting        = useMoveSorting;
        this.useOpeningBook        = useOpeningBook;
        this.useTranspositionTable = useTranspositionTable;
        this.fonctionEvaluation    = fonctionEvaluation;
    }

    /**
     * Profondeur de recherche de base (en demi-coups).
     * Peut être ajustée dynamiquement selon la phase de jeu pour DIFFICILE.
     */
    public int profondeur() {
        return profondeur;
    }

    /**
     * {@code true} si l'élagage alpha-bêta doit être appliqué pendant la recherche.
     * FACILE explore l'arbre complet sans élagage.
     */
    public boolean useAlphaBeta() {
        return useAlphaBeta;
    }

    /**
     * {@code true} si le tri des coups (MVV-LVA) doit être appliqué.
     * Le tri améliore l'efficacité de l'élagage alpha-bêta.
     */
    public boolean useMoveSorting() {
        return useMoveSorting;
    }

    /**
     * {@code true} si le livre d'ouvertures peut être consulté.
     * Désactivé pour FACILE et MOYEN.
     */
    public boolean useOpeningBook() {
        return useOpeningBook;
    }

    /**
     * {@code true} si la table de transposition doit être utilisée.
     * Désactivée pour FACILE et MOYEN (évite de mémoriser des positions complexes).
     */
    public boolean useTranspositionTable() {
        return useTranspositionTable;
    }

    /**
     * Retourne la fonction d'évaluation statique associée à ce niveau.
     */
    public FonctionEvaluation getFonctionEvaluation() {
        return fonctionEvaluation;
    }
}
