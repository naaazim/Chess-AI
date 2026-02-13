package org.example.gui;

import org.example.chess.Case;
import org.example.chess.Coup;
import org.example.chess.Piece;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gère la logique de sélection (départ -> arrivée) pour un joueur humain.
 * <p>
 * État :
 * <ul>
 * <li>Aucune sélection</li>
 * <li>Case départ sélectionnée (en attente d'arrivée)</li>
 * </ul>
 * </p>
 */
public class SelecteurCoup {

    private Case caseDepart;
    private List<Coup> coupsLegaux; // Liste complète des coups légaux possibles dans la position

    public SelecteurCoup() {
        this.caseDepart = null;
        this.coupsLegaux = Collections.emptyList();
    }

    /**
     * Met à jour la liste des coups légaux (à appeler à chaque nouveau tour).
     */
    public void setCoupsLegaux(List<Coup> coups) {
        this.coupsLegaux = (coups != null) ? coups : Collections.emptyList();
        reinitialiser();
    }

    /**
     * Réinitialise la sélection en cours.
     */
    public void reinitialiser() {
        this.caseDepart = null;
    }

    /**
     * Tente de sélectionner une case.
     *
     * @param caseCliquee la case cliquée par l'utilisateur
     * @return true si une action a été effectuée (sélection ou tentative de coup),
     *         false sinon
     */
    public boolean selectionner(Case caseCliquee) {
        if (caseDepart == null) {
            // Premier clic : on sélectionne la case de départ
            // On vérifie s'il existe au moins un coup légal partant de cette case
            boolean aDesCoups = coupsLegaux.stream()
                    .anyMatch(c -> c.depart().equals(caseCliquee));

            if (aDesCoups) {
                this.caseDepart = caseCliquee;
                return true;
            }
            return false;
        } else {
            // Deuxième clic : on tente de jouer vers cette case
            // Ou on change de pièce si on clique sur une autre pièce de notre couleur (non
            // géré ici pour simplifier,
            // on considère ça comme une nouvelle sélection si le coup n'est pas valide ?)
            // ->
            // Pour l'instant : si clic sur case arrivée valide -> coup trouvé.
            // Sinon -> on désélectionne ou on change de source.

            // Simplification : si on clique sur la même case, on désélectionne
            if (caseCliquee.equals(caseDepart)) {
                reinitialiser();
                return true;
            }

            // On regarde si c'est un coup légal
            // Attention : promotion !
            // Pour l'instant, on suppose promotion Reine par défaut pour simplifier l'UI.
            // Idéalement faudrait une popup.
            return true;
            // La validation finale se fera via trouverCoup
        }
    }

    public Case getCaseDepart() {
        return caseDepart;
    }

    /**
     * Cherche un coup légal correspondant à (Depart, Arrivee).
     * Gère la promotion par défaut (Reine).
     *
     * @param arrivee la case d'arrivée
     * @return le Coup complet s'il existe, null sinon
     */
    public Coup trouverCoup(Case arrivee) {
        if (caseDepart == null)
            return null;

        // On cherche dans les coups légaux
        List<Coup> candidats = coupsLegaux.stream()
                .filter(c -> c.depart().equals(caseDepart) && c.arrivee().equals(arrivee))
                .collect(Collectors.toList());

        if (candidats.isEmpty())
            return null;

        // Si un seul candidat, c'est lui (cas normal)
        if (candidats.size() == 1) {
            return candidats.get(0);
        }

        // Si plusieurs candidats, c'est une promotion (car même départ/arrivée mais
        // pièce promo différente)
        // On choisit la Reine par défaut (souvent le mieux) ou la première trouvée.
        return candidats.stream()
                .filter(c -> c.estPromotion()
                        && (c.piecePromotion() == Piece.DAME_BLANCHE || c.piecePromotion() == Piece.DAME_NOIRE))
                .findFirst()
                .orElse(candidats.get(0));
    }
}
