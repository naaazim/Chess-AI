package fr.moteurechecs.gui;

import fr.moteurechecs.plateau.Coup;
import fr.moteurechecs.plateau.EtatPlateau;
import fr.moteurechecs.plateau.GenerateurCoups;
import fr.moteurechecs.plateau.Piece;
import fr.moteurechecs.plateau.Plateau;

import java.util.List;

/**
 * Génère une notation algébrique standard simplifiée (SAN) pour un coup donné.
 *
 * Prend en charge les roques, captures, promotions et désambiguïsations.
 * Ajoute le symbole d'échec (+) ou de mat (#) en jouant temporairement le coup.
 */
public final class NotationEchecs {

    private NotationEchecs() {
    }

    /**
     * Convertit un coup en notation SAN.
     *
     * @param plateau           position avant le coup
     * @param coup              coup à convertir
     * @param coupsLegauxAvant  liste des coups légaux avant le coup (pour désambiguïsation)
     * @return chaîne SAN (ex. "Nf3", "exd5", "O-O", "e8=Q#")
     */
    public static String versSAN(Plateau plateau, Coup coup, List<Coup> coupsLegauxAvant) {
        if (plateau == null || coup == null) {
            throw new IllegalArgumentException("plateau/coup ne peuvent pas être null");
        }

        StringBuilder san = new StringBuilder();

        if (coup.estRoque()) {
            san.append(coup.arrivee().colonne() == 6 ? "O-O" : "O-O-O");
        } else {
            Piece piece = coup.pieceDeplacee();
            boolean pion = estPion(piece);

            if (!pion) {
                san.append(lettrePiece(piece));
                san.append(calculerDesambiguation(coup, coupsLegauxAvant));
            } else if (coup.estCapture()) {
                san.append((char) ('a' + coup.depart().colonne()));
            }

            if (coup.estCapture()) {
                san.append('x');
            }

            san.append(coup.arrivee().versAlgebrique());

            if (coup.estPromotion() && coup.piecePromotion() != null) {
                san.append('=').append(lettrePiece(coup.piecePromotion()));
            }
        }

        EtatPlateau etatAvant = plateau.jouerAvecSauvegarde(coup);
        try {
            boolean roiAdverseEnEchec = plateau.estEnEchec(plateau.trait());
            List<Coup> reponsesAdverses = GenerateurCoups.genererLegaux(plateau);
            if (reponsesAdverses.isEmpty() && roiAdverseEnEchec) {
                san.append('#');
            } else if (roiAdverseEnEchec) {
                san.append('+');
            }
        } finally {
            plateau.annuler(etatAvant);
        }

        return san.toString();
    }

    private static String calculerDesambiguation(Coup coup, List<Coup> coupsLegauxAvant) {
        if (coupsLegauxAvant == null || coupsLegauxAvant.isEmpty()) {
            return "";
        }

        boolean conflit = false;
        boolean memeColonne = false;
        boolean memeLigne = false;

        for (Coup candidat : coupsLegauxAvant) {
            if (candidat == coup) {
                continue;
            }
            if (candidat.pieceDeplacee() == coup.pieceDeplacee()
                    && candidat.arrivee().equals(coup.arrivee())
                    && !candidat.depart().equals(coup.depart())) {
                conflit = true;
                if (candidat.depart().colonne() == coup.depart().colonne()) {
                    memeColonne = true;
                }
                if (candidat.depart().ligne() == coup.depart().ligne()) {
                    memeLigne = true;
                }
            }
        }

        if (!conflit) {
            return "";
        }

        char colonneLettreAlg = (char) ('a' + coup.depart().colonne());
        char rangeeChiffreAlg = (char) ('0' + coup.depart().ligne());

        if (!memeColonne) {
            return String.valueOf(colonneLettreAlg);
        }
        if (!memeLigne) {
            return String.valueOf(rangeeChiffreAlg);
        }
        return "" + colonneLettreAlg + rangeeChiffreAlg;
    }

    private static boolean estPion(Piece piece) {
        return piece == Piece.PION_BLANC || piece == Piece.PION_NOIR;
    }

    private static String lettrePiece(Piece piece) {
        return switch (piece) {
            case CAVALIER_BLANC, CAVALIER_NOIR -> "N";
            case FOU_BLANC, FOU_NOIR -> "B";
            case TOUR_BLANC, TOUR_NOIRE -> "R";
            case DAME_BLANCHE, DAME_NOIRE -> "Q";
            case ROI_BLANC, ROI_NOIR -> "K";
            default -> "";
        };
    }
}
