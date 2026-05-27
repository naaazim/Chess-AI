package fr.moteurechecs.ia.recherche;

import fr.moteurechecs.plateau.Coup;
import fr.moteurechecs.plateau.Couleur;
import fr.moteurechecs.plateau.EtatPlateau;
import fr.moteurechecs.plateau.GenerateurCoups;
import fr.moteurechecs.plateau.Plateau;
import fr.moteurechecs.ia.Niveau;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Recherche de quiescence pour stabiliser l'évaluation statique.
 *
 * Après avoir atteint la profondeur maximale de la recherche principale,
 * on continue à explorer les captures et promotions jusqu'à atteindre une
 * position calme, évitant ainsi l'effet horizon.
 *
 * La fonction d'évaluation statique utilisée est celle du {@link Niveau}
 * courant, ce qui garantit la cohérence avec la recherche principale.
 */
public final class RechercheQuiescence {

    private RechercheQuiescence() {
    }

    /**
     * Lance la recherche de quiescence depuis la position courante.
     *
     * @param plateau   position à évaluer
     * @param alpha     borne inférieure de la fenêtre
     * @param beta      borne supérieure de la fenêtre
     * @param tempsEcoule  signal d'arrêt par dépassement de temps
     * @param niveau    niveau de l'IA (détermine la fonction d'évaluation et le tri des coups)
     * @return score de la position stable
     * @throws ExceptionTempsDepasse si le temps est écoulé
     */
    public static int quiescenceSearch(Plateau plateau, int alpha, int beta,
                                       AtomicBoolean tempsEcoule, Niveau niveau) {
        if (tempsEcoule.get())
            throw new ExceptionTempsDepasse();

        int scoreStatique = niveau.getFonctionEvaluation().evaluer(plateau);
        boolean joueurMax = (plateau.trait() == Couleur.BLANC);

        // coupe stand-pat : si le score statique est déjà au-delà de la fenêtre
        if (joueurMax) {
            if (scoreStatique >= beta)  return beta;
            if (alpha < scoreStatique)  alpha = scoreStatique;
        } else {
            if (scoreStatique <= alpha) return alpha;
            if (beta > scoreStatique)   beta  = scoreStatique;
        }

        // extraction des captures et promotions
        List<Coup> captures = extraireCaptures(plateau);
        if (captures.isEmpty()) return scoreStatique;

        if (niveau.useMoveSorting()) {
            TrieurDeCoups.trierCoups(captures, plateau);
        }

        return joueurMax
            ? rechercherMax(plateau, captures, alpha, beta, tempsEcoule, scoreStatique, niveau)
            : rechercherMin(plateau, captures, alpha, beta, tempsEcoule, scoreStatique, niveau);
    }

    // filtre : ne garde que les captures et promotions
    private static List<Coup> extraireCaptures(Plateau plateau) {
        List<Coup> coupsLegaux = GenerateurCoups.genererLegaux(plateau);
        List<Coup> captures = new ArrayList<>(coupsLegaux.size());
        for (Coup c : coupsLegaux) {
            if (c.estCapture() || c.estPromotion()) captures.add(c);
        }
        return captures;
    }

    // recherche quiescence pour le joueur maximiseur (Blancs)
    private static int rechercherMax(Plateau plateau, List<Coup> captures,
                                     int alpha, int beta,
                                     AtomicBoolean tempsEcoule, int scoreInitial,
                                     Niveau niveau) {
        int meilleur = scoreInitial;
        for (Coup capture : captures) {
            EtatPlateau sauvegarde = plateau.jouerAvecSauvegarde(capture);
            int score = quiescenceSearch(plateau, alpha, beta, tempsEcoule, niveau);
            plateau.annuler(sauvegarde);

            meilleur = Math.max(meilleur, score);
            alpha    = Math.max(alpha, meilleur);
            if (niveau.useAlphaBeta() && alpha >= beta) break;
        }
        return meilleur;
    }

    // recherche quiescence pour le joueur minimiseur (Noirs)
    private static int rechercherMin(Plateau plateau, List<Coup> captures,
                                     int alpha, int beta,
                                     AtomicBoolean tempsEcoule, int scoreInitial,
                                     Niveau niveau) {
        int meilleur = scoreInitial;
        for (Coup capture : captures) {
            EtatPlateau sauvegarde = plateau.jouerAvecSauvegarde(capture);
            int score = quiescenceSearch(plateau, alpha, beta, tempsEcoule, niveau);
            plateau.annuler(sauvegarde);

            meilleur = Math.min(meilleur, score);
            beta     = Math.min(beta, meilleur);
            if (niveau.useAlphaBeta() && alpha >= beta) break;
        }
        return meilleur;
    }
}
