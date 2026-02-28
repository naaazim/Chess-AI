package org.example.jeu;

import org.example.chess.*;

import java.util.List;
import java.util.Scanner;

/**
 * <p>
 * Joueur humain en console.
 * </p>
 *
 * <p>
 * Format attendu :
 * </p>
 * <ul>
 *   <li>{@code e2e4}</li>
 *   <li>{@code g1f3}</li>
 *   <li>promotion : {@code e7e8q} (q/r/b/n)</li>
 * </ul>
 */
public final class JoueurHumain {

    private final Scanner scanner;

    public JoueurHumain(Scanner scanner) {
        if (scanner == null) throw new IllegalArgumentException("scanner null");
        this.scanner = scanner;
    }

    /**
     * <p>Demande un coup légal au joueur.</p>
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

        String fromStr = saisie.substring(0, 2);
        String toStr = saisie.substring(2, 4);
        char promo = (saisie.length() == 5) ? saisie.charAt(4) : 0;

        Case depart;
        Case arrivee;
        try {
            depart = Case.depuisAlgebrique(fromStr);
            arrivee = Case.depuisAlgebrique(toStr);
        } catch (Exception e) {
            return null;
        }

        for (Coup c : coupsLegaux) {
            if (!c.depart().equals(depart)) continue;
            if (!c.arrivee().equals(arrivee)) continue;

            if (c.estPromotion()) {
                if (promo == 0) continue;
                if (!promotionCorrespond(c, promo, plateau.trait())) continue;
            } else {
                if (promo != 0) continue;
            }

            return c;
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
