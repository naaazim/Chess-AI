package org.example.AI.search;

import org.example.AI.Evaluation;
import org.example.chess.Coup;
import org.example.chess.Couleur;
import org.example.chess.Piece;
import org.example.chess.Plateau;

import java.util.List;

public final class MoveSorter {

    private MoveSorter() {
    }

    public static void trierCoups(List<Coup> coups, Plateau p) {
        coups.sort((c1, c2) -> {
            int note1 = evaluerCoupPourTri(c1, p);
            int note2 = evaluerCoupPourTri(c2, p);
            return Integer.compare(note2, note1);
        });
    }

    private static int evaluerCoupPourTri(Coup coup, Plateau p) {
        int note = 0;

        if (coup.estCapture()) {
            Piece victime = coup.pieceCapturee();
            Piece attaquant = coup.pieceDeplacee();

            if (victime == null && coup.estEnPassant()) {
                victime = (p.trait() == Couleur.BLANC) ? Piece.PION_NOIR : Piece.PION_BLANC;
            }

            int valVictime = valeurPiece(victime);
            int valAttaquant = valeurPiece(attaquant);

            note += 10 * valVictime - valAttaquant;
        }

        if (coup.estPromotion()) {
            note += valeurPiece(coup.piecePromotion());
        }

        return note;
    }

    private static int valeurPiece(Piece piece) {
        if (piece == null)
            return 0;
        return switch (piece) {
            case PION_BLANC, PION_NOIR -> Evaluation.VALEUR_PION;
            case CAVALIER_BLANC, CAVALIER_NOIR -> Evaluation.VALEUR_CAVALIER;
            case FOU_BLANC, FOU_NOIR -> Evaluation.VALEUR_FOU;
            case TOUR_BLANC, TOUR_NOIR -> Evaluation.VALEUR_TOUR;
            case DAME_BLANCHE, DAME_NOIRE -> Evaluation.VALEUR_DAME;
            case ROI_BLANC, ROI_NOIR -> 0;
        };
    }

    public static void placerEnPremier(List<Coup> coups, Coup meilleurCoupPrecedent) {
        if (meilleurCoupPrecedent != null) {
            int idx = -1;
            for (int i = 0; i < coups.size(); i++) {
                Coup c = coups.get(i);
                if (c.depart().equals(meilleurCoupPrecedent.depart()) &&
                        c.arrivee().equals(meilleurCoupPrecedent.arrivee()) &&
                        c.piecePromotion() == meilleurCoupPrecedent.piecePromotion()) {
                    idx = i;
                    break;
                }
            }
            if (idx > 0) {
                Coup c = coups.remove(idx);
                coups.add(0, c);
            }
        }
    }
}
