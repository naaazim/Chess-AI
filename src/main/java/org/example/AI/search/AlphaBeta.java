package org.example.AI.search;

import org.example.AI.Evaluation;
import org.example.chess.Coup;
import org.example.chess.Couleur;
import org.example.chess.EtatPlateau;
import org.example.chess.GenerateurCoups;
import org.example.chess.Plateau;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AlphaBeta {
    private AlphaBeta() {
    }

    public static int minimax(Plateau plateau, int profondeur, int alpha, int beta, AtomicBoolean timeIsUp) {
        if (timeIsUp.get())
            throw new TimeOutException();

        if (profondeur == 0) {
            return Quiescence.quiescenceSearch(plateau, alpha, beta, timeIsUp);
        }

        List<Coup> coups = GenerateurCoups.genererLegaux(plateau);

        if (coups.isEmpty()) {
            if (plateau.estEnEchec(plateau.trait())) {
                return (plateau.trait() == Couleur.BLANC) ? -Evaluation.SCORE_MAT + (20 - profondeur)
                        : Evaluation.SCORE_MAT - (20 - profondeur);
            }
            return 0;
        }

        MoveSorter.trierCoups(coups, plateau);
        boolean max = (plateau.trait() == Couleur.BLANC);

        if (max) {
            int meilleur = Integer.MIN_VALUE;
            for (Coup coup : coups) {
                EtatPlateau s = plateau.jouerAvecSauvegarde(coup);
                int score = minimax(plateau, profondeur - 1, alpha, beta, timeIsUp);
                plateau.annuler(s);

                meilleur = Math.max(meilleur, score);
                alpha = Math.max(alpha, meilleur);
                if (alpha >= beta)
                    break;
            }
            return meilleur;
        } else {
            int meilleur = Integer.MAX_VALUE;
            for (Coup coup : coups) {
                EtatPlateau s = plateau.jouerAvecSauvegarde(coup);
                int score = minimax(plateau, profondeur - 1, alpha, beta, timeIsUp);
                plateau.annuler(s);

                meilleur = Math.min(meilleur, score);
                beta = Math.min(beta, meilleur);
                if (alpha >= beta)
                    break;
            }
            return meilleur;
        }
    }
}
