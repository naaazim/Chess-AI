package org.example.AI;

import org.example.chess.*;

import java.util.List;

/**
 * <p>
 * {@code RechercheMinimaxAlphaBeta} choisit un coup avec l'algorithme
 * <b>Minimax + Alpha-Bêta</b>.
 * </p>
 *
 * <p>
 * Idée très simple :
 * </p>
 * <ul>
 *   <li>Les Blancs veulent un score le plus grand possible (MAX).</li>
 *   <li>Les Noirs veulent un score le plus petit possible (MIN).</li>
 * </ul>
 *
 * <p>
 * Alpha-Bêta sert à couper des branches inutiles et donc aller plus vite,
 * ce qui est très important car le facteur de branchement des échecs est grand.
 * </p>
 *
 * <p>
 * Convention : {@link Evaluation#evaluer(Plateau)} donne un score
 * du point de vue des Blancs.</p>
 */
public final class RechercheMinimaxAlphaBeta {

    /**
     * <p>
     * Trouve le meilleur coup pour le joueur au trait.
     * </p>
     *
     * <p>
     * Si c'est aux Blancs de jouer : on choisit le coup qui maximise le score.
     * </p>
     * <p>
     * Si c'est aux Noirs de jouer : on choisit le coup qui minimise le score.
     * </p>
     *
     * @param plateau position courante
     * @param niveau niveau de difficulté
     * @return meilleur coup (ou {@code null} si aucun coup légal)
     */
    public static Coup meilleurCoup(Plateau plateau, Niveau niveau) {
        if (plateau == null) throw new IllegalArgumentException("plateau null");
        if (niveau == null) throw new IllegalArgumentException("niveau null");

        List<Coup> coups = GenerateurCoups.genererLegaux(plateau);
        if (coups.isEmpty()) {
            return null; // mat ou pat
        }

        boolean blancsJouent = (plateau.trait() == Couleur.BLANC);

        int alpha = -Evaluation.SCORE_MAT;
        int beta = Evaluation.SCORE_MAT;

        Coup meilleur = null;

        // Blanc = MAX, Noir = MIN
        int meilleurScore = blancsJouent ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (Coup coup : coups) {
            EtatPlateau s = plateau.jouerAvecSauvegarde(coup);

            int score = minimax(plateau, niveau.profondeur() - 1, alpha, beta);

            plateau.annuler(s);

            if (blancsJouent) {
                // MAX
                if (score > meilleurScore) {
                    meilleurScore = score;
                    meilleur = coup;
                }
                alpha = Math.max(alpha, meilleurScore);
            } else {
                // MIN
                if (score < meilleurScore) {
                    meilleurScore = score;
                    meilleur = coup;
                }
                beta = Math.min(beta, meilleurScore);
            }

            // Coupe à la racine aussi
            if (alpha >= beta) {
                break;
            }
        }

        return meilleur;
    }

    /**
     * <p>
     * Minimax + Alpha-Bêta.
     * </p>
     *
     * <p>
     * Rappel :
     * </p>
     * <ul>
     *   <li>Si c'est au Blanc de jouer : on renvoie le MAX des scores enfants.</li>
     *   <li>Si c'est au Noir de jouer : on renvoie le MIN des scores enfants.</li>
     * </ul>
     *
     * @param plateau position courante
     * @param profondeur profondeur restante
     * @param alpha borne basse (meilleur déjà trouvé pour MAX)
     * @param beta borne haute (meilleur déjà trouvé pour MIN)
     * @return score minimax de la position
     */
    private static int minimax(Plateau plateau, int profondeur, int alpha, int beta) {
        if (profondeur == 0) {
            return Evaluation.evaluer(plateau); // score côté Blanc
        }

        List<Coup> coups = GenerateurCoups.genererLegaux(plateau);

        // Terminal : plus de coups légaux => mat ou pat
        if (coups.isEmpty()) {
            boolean enEchec = plateau.estEnEchec(plateau.trait());
            if (enEchec) {
                // Le joueur au trait est mat.
                // Si Blanc est mat => très mauvais pour Blanc.
                // Si Noir est mat => très bon pour Blanc.
                // On préfère mater plus vite : petit ajustement avec profondeur.
                if (plateau.trait() == Couleur.BLANC) {
                    return -Evaluation.SCORE_MAT + (10 - profondeur);
                } else {
                    return Evaluation.SCORE_MAT - (10 - profondeur);
                }
            }
            // Pat
            return 0;
        }

        boolean max = (plateau.trait() == Couleur.BLANC);

        if (max) {
            int meilleur = Integer.MIN_VALUE;

            for (Coup coup : coups) {
                EtatPlateau s = plateau.jouerAvecSauvegarde(coup);

                int score = minimax(plateau, profondeur - 1, alpha, beta);

                plateau.annuler(s);

                meilleur = Math.max(meilleur, score);
                alpha = Math.max(alpha, meilleur);

                if (alpha >= beta) {
                    break; // coupe
                }
            }

            return meilleur;
        } else {
            int meilleur = Integer.MAX_VALUE;

            for (Coup coup : coups) {
                EtatPlateau s = plateau.jouerAvecSauvegarde(coup);

                int score = minimax(plateau, profondeur - 1, alpha, beta);

                plateau.annuler(s);

                meilleur = Math.min(meilleur, score);
                beta = Math.min(beta, meilleur);

                if (alpha >= beta) {
                    break; // coupe
                }
            }

            return meilleur;
        }
    }
}
