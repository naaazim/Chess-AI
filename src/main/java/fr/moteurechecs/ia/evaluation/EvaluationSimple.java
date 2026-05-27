package fr.moteurechecs.ia.evaluation;

import fr.moteurechecs.ia.Evaluation;
import fr.moteurechecs.plateau.Couleur;
import fr.moteurechecs.plateau.GenerateurCoups;
import fr.moteurechecs.plateau.Piece;
import fr.moteurechecs.plateau.Plateau;

/**
 * Évaluation légère pour le niveau FACILE.
 *
 * Combine trois termes afin d'obtenir un jeu reconnaissable comme "des échecs"
 * sans atteindre la qualité de {@code EvaluationIntermediaire} :
 * - MATÉRIEL — somme pondérée des pièces (P=100, C=320, F=330, T=500, D=900).
 * - DÉVELOPPEMENT — +15 cp par pièce mineure (cavalier ou fou) sortie de
 *       sa case de départ naturelle.
 * - CENTRE — +10 cp par pion/pièce mineure sur le centre direct (d4/e4/d5/e5) ;
 *       +5 cp sur le centre étendu (c3-f3, c6-f6, d2/e2, d7/e7).
 *
 * Les bonus restent inférieurs à la valeur d'un pion pour que les captures
 * restent toujours prioritaires sur n'importe quel gain positionnel.
 */
public final class EvaluationSimple implements FonctionEvaluation {

    // ===== Cases de départ des pièces mineures (indices 0=a8..63=h1) =====
    // b1=57, g1=62, c1=58, f1=61
    private static final long DEPART_MINEURS_BLANC =
            (1L << 57) | (1L << 62) | (1L << 58) | (1L << 61);
    // b8=1, g8=6, c8=2, f8=5
    private static final long DEPART_MINEURS_NOIR  =
            (1L << 1)  | (1L << 6)  | (1L << 2)  | (1L << 5);

    // ===== Masques de centre =====
    // Centre direct : d4=35, e4=36, d5=27, e5=28
    private static final long CENTRE_DIRECT =
            (1L << 35) | (1L << 36) | (1L << 27) | (1L << 28);
    // Centre étendu : d2=51, e2=52, d7=11, e7=12,
    //                 c3=42, d3=43, e3=44, f3=45, c6=18, d6=19, e6=20, f6=21
    private static final long CENTRE_ETENDU =
            (1L << 51) | (1L << 52) | (1L << 11) | (1L << 12)
          | (1L << 42) | (1L << 43) | (1L << 44) | (1L << 45)
          | (1L << 18) | (1L << 19) | (1L << 20) | (1L << 21);

    private static final int BONUS_DEVELOPPEMENT = 15;
    private static final int BONUS_CENTRE_DIRECT = 10;
    private static final int BONUS_CENTRE_ETENDU =  5;

    @Override
    public int evaluer(Plateau plateau) {
        if (GenerateurCoups.genererLegaux(plateau).isEmpty()) {
            return evaluerTerminale(plateau);
        }
        return Materiel.scoreMateriel(plateau)
             + scoreDeveloppement(plateau)
             + scoreCentre(plateau);
    }

    // +15 cp par pièce mineure sortie de sa case de départ
    private static int scoreDeveloppement(Plateau plateau) {
        long mineursBlancs = plateau.bitboard(Piece.CAVALIER_BLANC)
                           | plateau.bitboard(Piece.FOU_BLANC);
        long mineursNoirs  = plateau.bitboard(Piece.CAVALIER_NOIR)
                           | plateau.bitboard(Piece.FOU_NOIR);
        int developpesBlancs = Long.bitCount(mineursBlancs & ~DEPART_MINEURS_BLANC);
        int developpesNoirs  = Long.bitCount(mineursNoirs  & ~DEPART_MINEURS_NOIR);
        return (developpesBlancs - developpesNoirs) * BONUS_DEVELOPPEMENT;
    }

    // +10 cp centre direct / +5 cp centre étendu, pour pions et pièces mineures uniquement
    private static int scoreCentre(Plateau plateau) {
        long actifsBlancs = plateau.bitboard(Piece.PION_BLANC)
                          | plateau.bitboard(Piece.CAVALIER_BLANC)
                          | plateau.bitboard(Piece.FOU_BLANC);
        long actifsNoirs  = plateau.bitboard(Piece.PION_NOIR)
                          | plateau.bitboard(Piece.CAVALIER_NOIR)
                          | plateau.bitboard(Piece.FOU_NOIR);
        int direct = (Long.bitCount(actifsBlancs & CENTRE_DIRECT)
                    - Long.bitCount(actifsNoirs  & CENTRE_DIRECT)) * BONUS_CENTRE_DIRECT;
        int etendu = (Long.bitCount(actifsBlancs & CENTRE_ETENDU)
                    - Long.bitCount(actifsNoirs  & CENTRE_ETENDU)) * BONUS_CENTRE_ETENDU;
        return direct + etendu;
    }

    /** Évalue une position terminale (mat ou pat). */
    private static int evaluerTerminale(Plateau plateau) {
        if (plateau.estEnEchec(plateau.trait())) {
            return (plateau.trait() == Couleur.BLANC) ? -Evaluation.SCORE_MAT : Evaluation.SCORE_MAT;
        }
        return 0; // pat
    }
}
