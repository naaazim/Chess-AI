package org.example.AI;

import org.example.chess.*;
import org.example.AI.evaluation.*;

import java.util.List;

/**
 * <p>
 * La classe {@code Evaluation} donne une note à une position d'échecs.
 * </p>
 *
 * <p>
 * Cette version est <b>à phases</b> :
 * </p>
 * <ul>
 * <li><b>Début/Milieu</b> : matériel + mobilité</li>
 * <li><b>Finale</b> : matériel + activité du roi + pions avancés/poussés</li>
 * </ul>
 */
public final class Evaluation {

    public static final int VALEUR_PION = 100;
    public static final int VALEUR_CAVALIER = 320;
    public static final int VALEUR_FOU = 330;
    public static final int VALEUR_TOUR = 500;
    public static final int VALEUR_DAME = 900;

    public static final int SCORE_MAT = 1_000_000;

    private Evaluation() {
    }

    public static int evaluer(Plateau plateau) {
        if (plateau == null)
            throw new IllegalArgumentException("plateau null");

        List<Coup> coupsLegaux = GenerateurCoups.genererLegaux(plateau);
        if (coupsLegaux.isEmpty()) {
            boolean enEchec = plateau.estEnEchec(plateau.trait());
            if (enEchec) {
                return (plateau.trait() == Couleur.BLANC) ? -SCORE_MAT : SCORE_MAT;
            }
            return 0; // pat
        }

        double phase = GamePhase.calculerPhase(plateau);

        int scoreDebut = 0;
        scoreDebut += Materiel.scoreMateriel(plateau);
        scoreDebut += Mobilite.scoreMobilite(plateau);

        int scoreFinale = 0;
        scoreFinale += Materiel.scoreMateriel(plateau);
        scoreFinale += SecuriteRoi.scoreRoiActifFinale(plateau);
        scoreFinale += StructurePions.scorePionsFinale(plateau);

        double score = phase * scoreDebut + (1.0 - phase) * scoreFinale;

        score += PieceSquareTable.scorePositionnelPieceSquare(plateau, phase);

        return (int) Math.round(score);
    }
}
