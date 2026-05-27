package fr.moteurechecs.jeu;

import fr.moteurechecs.plateau.*;

import java.util.List;
import java.util.Scanner;

/**
 * Joueur humain en console.
 *
 * Format attendu :
 * - {@code e2e4}
 * - {@code g1f3}
 * - promotion : {@code e7e8q} (q/r/b/n)
 */
public final class JoueurHumain {

    private final Scanner scanner;

    public JoueurHumain(Scanner scanner) {
        if (scanner == null) throw new IllegalArgumentException("scanner null");
        this.scanner = scanner;
    }

    /**
     *
     * Demande un coup légal au joueur.
     *
     * @param plateau plateau courant
     * @return coup légal
     */
    public Coup demanderCoup(Plateau plateau) {
        List<Coup> coupsLegaux = GenerateurCoups.genererLegaux(plateau);

        while (true) {
            System.out.print("Entre ton coup (ex: e2e4, e7e8q) : ");
            String saisie = scanner.nextLine().trim().toLowerCase();

            Coup coup = trouverCoupDepuisSaisie(plateau, saisie, coupsLegaux);
            if (coup != null) return coup;

            System.out.println("Coup invalide ou illégal. Réessaie.");
        }
    }

    private Coup trouverCoupDepuisSaisie(Plateau plateau, String saisie, List<Coup> coupsLegaux) {
        if (saisie.length() != 4 && saisie.length() != 5) return null;

        // Extraire les cases de départ et d'arrivée depuis la saisie algébrique
        String notationDepart  = saisie.substring(0, 2);
        String notationArrivee = saisie.substring(2, 4);
        char   codePromotion   = (saisie.length() == 5) ? saisie.charAt(4) : 0;

        Case depart;
        Case arrivee;
        try {
            depart  = Case.depuisAlgebrique(notationDepart);
            arrivee = Case.depuisAlgebrique(notationArrivee);
        } catch (Exception e) {
            return null;
        }

        // Chercher parmi les coups légaux celui qui correspond à la saisie
        for (Coup candidat : coupsLegaux) {
            if (!candidat.depart().equals(depart)) continue;
            if (!candidat.arrivee().equals(arrivee)) continue;

            if (candidat.estPromotion()) {
                if (codePromotion == 0) continue;
                if (!promotionCorrespond(candidat, codePromotion, plateau.trait())) continue;
            } else {
                if (codePromotion != 0) continue;
            }

            return candidat;
        }

        return null;
    }

    private boolean promotionCorrespond(Coup c, char promo, Couleur trait) {
        Piece attendue = switch (promo) {
            case 'q' -> (trait == Couleur.BLANC) ? Piece.DAME_BLANCHE : Piece.DAME_NOIRE;
            case 'r' -> (trait == Couleur.BLANC) ? Piece.TOUR_BLANC : Piece.TOUR_NOIRE;
            case 'b' -> (trait == Couleur.BLANC) ? Piece.FOU_BLANC : Piece.FOU_NOIR;
            case 'n' -> (trait == Couleur.BLANC) ? Piece.CAVALIER_BLANC : Piece.CAVALIER_NOIR;
            default -> null;
        };
        return attendue != null && attendue == c.piecePromotion();
    }
}
