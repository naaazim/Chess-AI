package org.example.AI.evaluation;

import org.example.chess.Couleur;
import org.example.chess.GenerateurCoups;
import org.example.chess.Plateau;

public final class Mobilite {
    private static final int BONUS_MOBILITE_PAR_COUP = 2;

    private Mobilite() {
    }

    public static int scoreMobilite(Plateau p) {
        int nbCoups = GenerateurCoups.genererLegaux(p).size();
        int bonus = nbCoups * BONUS_MOBILITE_PAR_COUP;
        return (p.trait() == Couleur.BLANC) ? bonus : -bonus;
    }
}
