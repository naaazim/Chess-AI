package org.example.AI.search;

import org.example.AI.Evaluation;
import org.example.chess.Coup;
import org.example.chess.Couleur;
import org.example.chess.EtatPlateau;
import org.example.chess.GenerateurCoups;
import org.example.chess.Plateau;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Quiescence {
    private Quiescence() {
    }

    public static int quiescenceSearch(Plateau plateau, int alpha, int beta, AtomicBoolean timeIsUp) {
        if (timeIsUp.get())
            throw new TimeOutException();

        int standPat = Evaluation.evaluer(plateau);
        boolean max = (plateau.trait() == Couleur.BLANC);

        if (max) {
            if (standPat >= beta)
                return beta;
            if (alpha < standPat)
                alpha = standPat;
        } else {
            if (standPat <= alpha)
                return alpha;
            if (beta > standPat)
                beta = standPat;
        }

        List<Coup> coups = GenerateurCoups.genererLegaux(plateau);
        List<Coup> captures = new ArrayList<>();
        for (Coup c : coups) {
            if (c.estCapture() || c.estPromotion()) {
                captures.add(c);
            }
        }

        if (captures.isEmpty()) {
            return standPat;
        }

        MoveSorter.trierCoups(captures, plateau);

        if (max) {
            int meilleur = standPat;
            for (Coup capture : captures) {
                EtatPlateau s = plateau.jouerAvecSauvegarde(capture);
                int score = quiescenceSearch(plateau, alpha, beta, timeIsUp);
                plateau.annuler(s);

                meilleur = Math.max(meilleur, score);
                alpha = Math.max(alpha, meilleur);
                if (alpha >= beta)
                    break;
            }
            return meilleur;
        } else {
            int meilleur = standPat;
            for (Coup capture : captures) {
                EtatPlateau s = plateau.jouerAvecSauvegarde(capture);
                int score = quiescenceSearch(plateau, alpha, beta, timeIsUp);
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
