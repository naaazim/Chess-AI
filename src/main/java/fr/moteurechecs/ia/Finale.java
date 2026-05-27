package fr.moteurechecs.ia;

import fr.moteurechecs.plateau.Case;
import fr.moteurechecs.plateau.Coup;
import fr.moteurechecs.plateau.Couleur;
import fr.moteurechecs.plateau.EtatPlateau;
import fr.moteurechecs.plateau.GenerateurCoups;
import fr.moteurechecs.plateau.Piece;
import fr.moteurechecs.plateau.Plateau;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestion des finales élémentaires gagnantes.
 *
 * Implémente des heuristiques spécifiques pour trois types de finales :
 * Roi + Dame vs Roi (KQK), Roi + Tour vs Roi (KRK) et Roi + 2 Tours vs Roi (KRRK).
 * L'objectif est de repousser le roi adverse vers un bord et d'éviter le pat.
 */
public final class Finale {

    /** Bonus appliqué pour favoriser un mat rapide dans les finales gagnantes. */
    public static final int BONUS_MAT_RAPIDE = 5000;

    private enum EndgameType {
        NONE,
        KQK,
        KRK,
        KRRK
    }

    private Finale() {
    }

    /**
     * Retourne un coup de technique de finale gagnante, ou {@code null} si non applicable.
     *
     * @param plateau      position courante
     * @param coupsLegaux  liste des coups légaux disponibles
     * @param couleurNous  couleur du joueur qui cherche le mat
     * @return le coup recommandé, ou {@code null} si la position n'est pas une finale reconnue
     */
    public static Coup getCoupFinaleGagnante(Plateau plateau, List<Coup> coupsLegaux, Couleur couleurNous) {
        if (plateau == null || coupsLegaux == null || coupsLegaux.isEmpty() || couleurNous == null) {
            return null;
        }

        EndgameType type = detecterFinale(plateau, couleurNous);
        if (type == EndgameType.NONE) {
            return null;
        }

        return choisirCoupTechnique(plateau, coupsLegaux, couleurNous, type);
    }

    // détermine le type de finale en vérifiant que l'adversaire n'a que son roi
    private static EndgameType detecterFinale(Plateau plateau, Couleur nous) {
        Couleur adverse = nous.inverse();

        if (compterPiecesNonRoi(plateau, adverse) != 0) {
            return EndgameType.NONE;
        }

        int nosPions      = compterPiece(plateau, (nous == Couleur.BLANC) ? Piece.PION_BLANC      : Piece.PION_NOIR);
        int nosCavaliers  = compterPiece(plateau, (nous == Couleur.BLANC) ? Piece.CAVALIER_BLANC  : Piece.CAVALIER_NOIR);
        int nosFous       = compterPiece(plateau, (nous == Couleur.BLANC) ? Piece.FOU_BLANC       : Piece.FOU_NOIR);
        int nosTours      = compterPiece(plateau, (nous == Couleur.BLANC) ? Piece.TOUR_BLANC      : Piece.TOUR_NOIRE);
        int nosDames      = compterPiece(plateau, (nous == Couleur.BLANC) ? Piece.DAME_BLANCHE    : Piece.DAME_NOIRE);

        // une pièce mineure ou un pion empêche de classifier la finale
        if (nosPions + nosCavaliers + nosFous > 0) {
            return EndgameType.NONE;
        }

        if (nosDames >= 1 && nosTours == 0)  return EndgameType.KQK;
        if (nosTours == 1 && nosDames == 0)  return EndgameType.KRK;
        if (nosTours >= 2 && nosDames == 0)  return EndgameType.KRRK;

        return EndgameType.NONE;
    }

    // choisit le coup le mieux scoré parmi les coups légaux selon le type de finale
    private static Coup choisirCoupTechnique(
            Plateau plateau,
            List<Coup> coupsLegaux,
            Couleur nous,
            EndgameType type) {

        Couleur adverse = nous.inverse();
        Case roiAdverseAvant = trouverRoi(plateau, adverse);
        if (roiAdverseAvant == null) {
            return null;
        }
        int distBordAvant = distanceBord(roiAdverseAvant);

        Coup meilleur      = null;
        int meilleurScore  = Integer.MIN_VALUE;

        for (Coup coup : coupsLegaux) {
            EtatPlateau sauvegarde = plateau.jouerAvecSauvegarde(coup);

            boolean adverseEnEchec     = plateau.estEnEchec(adverse);
            List<Coup> reponsesAdverses = GenerateurCoups.genererLegaux(plateau);

            if (reponsesAdverses.isEmpty()) {
                if (adverseEnEchec) {
                    // mat immédiat : coup optimal, on arrête
                    plateau.annuler(sauvegarde);
                    return coup;
                }
                // pat : éviter ce coup
                plateau.annuler(sauvegarde);
                continue;
            }

            Case roiNousApres    = trouverRoi(plateau, nous);
            Case roiAdverseApres = trouverRoi(plateau, adverse);
            if (roiNousApres == null || roiAdverseApres == null) {
                plateau.annuler(sauvegarde);
                continue;
            }

            int score = calculerScorePositionnel(
                    plateau, coup, nous, adverse,
                    roiNousApres, roiAdverseApres,
                    distBordAvant, adverseEnEchec, reponsesAdverses, type);

            plateau.annuler(sauvegarde);

            if (score > meilleurScore) {
                meilleurScore = score;
                meilleur = coup;
            }
        }

        return meilleur;
    }

    // calcule le score heuristique d'un coup pour la technique de finale
    private static int calculerScorePositionnel(
            Plateau plateau, Coup coup, Couleur nous, Couleur adverse,
            Case roiNousApres, Case roiAdverseApres,
            int distBordAvant, boolean adverseEnEchec,
            List<Coup> reponsesAdverses, EndgameType type) {

        int score = 0;

        // réduire la mobilité adverse : moins il a de coups, mieux c'est
        int mobiliteAdverse = Math.min(20, reponsesAdverses.size());
        score += (20 - mobiliteAdverse) * 25;

        // repousser le roi adverse vers le bord
        int distBordApres = distanceBord(roiAdverseApres);
        score += (3 - distBordApres) * 90;
        if (distBordApres < distBordAvant) {
            score += 110;
        }

        // rapprocher notre roi du roi adverse
        int distRois = distanceChebyshev(roiNousApres, roiAdverseApres);
        score += (7 - distRois) * 16;

        // bonus pour mise en échec
        if (adverseEnEchec) {
            score += 90;
        }
        // bonus si l'adversaire est presque coincé
        if (reponsesAdverses.size() <= 2) {
            score += BONUS_MAT_RAPIDE / 12;
        }

        score += scoreTechniqueParType(plateau, coup, nous, roiAdverseApres, type);

        return score;
    }

    // délègue au score spécifique selon le type de finale (KQK, KRK, KRRK)
    private static int scoreTechniqueParType(
            Plateau plateau,
            Coup coup,
            Couleur nous,
            Case roiAdverse,
            EndgameType type) {

        return switch (type) {
            case KQK  -> scoreKQK(plateau, coup, nous, roiAdverse);
            case KRK  -> scoreKRK(plateau, coup, nous, roiAdverse);
            case KRRK -> scoreKRRK(plateau, coup, nous, roiAdverse);
            case NONE -> 0;
        };
    }

    // score heuristique pour la finale Roi + Dame vs Roi
    private static int scoreKQK(Plateau plateau, Coup coup, Couleur nous, Case roiAdverse) {
        Case dame = trouverDame(plateau, nous);
        if (dame == null) {
            return 0;
        }

        int score = 0;
        int distDameRoi = distanceChebyshev(dame, roiAdverse);

        // distance optimale : ni trop proche (risque de pat) ni trop loin
        if (distDameRoi <= 1) {
            score -= 180;
        } else if (distDameRoi == 2) {
            score += 45;
        } else if (distDameRoi <= 4) {
            score += 20;
        }

        if (estAligne(dame, roiAdverse)) {
            score += 50;
        }
        if (coup.pieceDeplacee() == pieceDame(nous)) {
            score += 20;
        }

        return score;
    }

    // score heuristique pour la finale Roi + Tour vs Roi
    private static int scoreKRK(Plateau plateau, Coup coup, Couleur nous, Case roiAdverse) {
        List<Case> tours = trouverTours(plateau, nous);
        if (tours.isEmpty()) {
            return 0;
        }

        int score = 0;
        for (Case tour : tours) {
            int dist = distanceChebyshev(tour, roiAdverse);
            if (dist <= 1) {
                score -= 220; // tour trop proche → risque de capture
            }
            if (estAligne(tour, roiAdverse)) {
                score += 55; // tour alignée sur la rangée/colonne du roi adverse
            }
        }

        if (coup.pieceDeplacee() == pieceTour(nous)) {
            score += 18;
        }

        return score;
    }

    // score heuristique pour la finale Roi + 2 Tours vs Roi (technique de l'escalier)
    private static int scoreKRRK(Plateau plateau, Coup coup, Couleur nous, Case roiAdverse) {
        List<Case> tours = trouverTours(plateau, nous);
        if (tours.size() < 2) {
            return scoreKRK(plateau, coup, nous, roiAdverse);
        }

        int score = scoreKRK(plateau, coup, nous, roiAdverse);

        Case t1 = tours.get(0);
        Case t2 = tours.get(1);

        // bonus pour tandem (tours adjacentes en escalier)
        boolean tandemColonne = t1.colonne() == t2.colonne() && Math.abs(t1.ligne() - t2.ligne()) == 1;
        boolean tandemLigne   = t1.ligne()   == t2.ligne()   && Math.abs(t1.colonne() - t2.colonne()) == 1;
        if (tandemColonne || tandemLigne) {
            score += 70;
        }

        // bonus pour chaque tour alignée sur le roi adverse
        int nbAlignees = 0;
        if (estAligne(t1, roiAdverse)) nbAlignees++;
        if (estAligne(t2, roiAdverse)) nbAlignees++;
        score += nbAlignees * 35;

        return score;
    }

    // compte les pièces non-roi d'une couleur
    private static int compterPiecesNonRoi(Plateau plateau, Couleur couleur) {
        if (couleur == Couleur.BLANC) {
            return Long.bitCount(plateau.bitboard(Piece.PION_BLANC))
                    + Long.bitCount(plateau.bitboard(Piece.CAVALIER_BLANC))
                    + Long.bitCount(plateau.bitboard(Piece.FOU_BLANC))
                    + Long.bitCount(plateau.bitboard(Piece.TOUR_BLANC))
                    + Long.bitCount(plateau.bitboard(Piece.DAME_BLANCHE));
        }
        return Long.bitCount(plateau.bitboard(Piece.PION_NOIR))
                + Long.bitCount(plateau.bitboard(Piece.CAVALIER_NOIR))
                + Long.bitCount(plateau.bitboard(Piece.FOU_NOIR))
                + Long.bitCount(plateau.bitboard(Piece.TOUR_NOIRE))
                + Long.bitCount(plateau.bitboard(Piece.DAME_NOIRE));
    }

    // compte les occurrences d'une pièce via son bitboard
    private static int compterPiece(Plateau plateau, Piece piece) {
        return Long.bitCount(plateau.bitboard(piece));
    }

    // localise le roi d'une couleur depuis le bitboard (null si absent)
    private static Case trouverRoi(Plateau plateau, Couleur couleur) {
        long bb = plateau.bitboard(couleur == Couleur.BLANC ? Piece.ROI_BLANC : Piece.ROI_NOIR);
        if (bb == 0L) {
            return null;
        }
        return Case.depuisIndice(Long.numberOfTrailingZeros(bb));
    }

    // localise la dame d'une couleur (null si absente)
    private static Case trouverDame(Plateau plateau, Couleur couleur) {
        long bb = plateau.bitboard(pieceDame(couleur));
        if (bb == 0L) {
            return null;
        }
        return Case.depuisIndice(Long.numberOfTrailingZeros(bb));
    }

    // collecte toutes les tours d'une couleur depuis le bitboard
    private static List<Case> trouverTours(Plateau plateau, Couleur couleur) {
        long bb = plateau.bitboard(pieceTour(couleur));
        List<Case> tours = new ArrayList<>();
        while (bb != 0L) {
            long lsb = bb & -bb;
            tours.add(Case.depuisIndice(Long.numberOfTrailingZeros(lsb)));
            bb ^= lsb;
        }
        return tours;
    }

    // retourne la pièce dame pour une couleur
    private static Piece pieceDame(Couleur couleur) {
        return couleur == Couleur.BLANC ? Piece.DAME_BLANCHE : Piece.DAME_NOIRE;
    }

    // retourne la pièce tour pour une couleur
    private static Piece pieceTour(Couleur couleur) {
        return couleur == Couleur.BLANC ? Piece.TOUR_BLANC : Piece.TOUR_NOIRE;
    }

    // vérifie si deux cases partagent la même colonne ou la même rangée
    private static boolean estAligne(Case a, Case b) {
        return a.colonne() == b.colonne() || a.ligne() == b.ligne();
    }

    // distance de Chebyshev (max des écarts en x et y)
    private static int distanceChebyshev(Case a, Case b) {
        return Math.max(Math.abs(a.colonne() - b.colonne()), Math.abs(a.ligne() - b.ligne()));
    }

    // distance minimale au bord de l'échiquier (0 = sur le bord, 3 = au centre)
    private static int distanceBord(Case c) {
        int col  = c.colonne();
        int rang = c.ligne() - 1; // 0..7
        int dCol = Math.min(col, 7 - col);
        int dRang = Math.min(rang, 7 - rang);
        return Math.min(dCol, dRang);
    }
}
