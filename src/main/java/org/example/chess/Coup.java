package org.example.chess;

/**
 * <p>
 * Représente un coup d'échecs.
 * </p>
 *
 * <p>
 * Pour être efficace (important avec un grand facteur de branchement),
 * un {@code Coup} contient directement les infos utiles :
 * </p>
 * <ul>
 * <li>case de départ</li>
 * <li>case d’arrivée</li>
 * <li>pièce déplacée</li>
 * <li>options : capture, promotion, roque, en passant</li>
 * </ul>
 */
public final class Coup {

    private final Case depart;
    private final Case arrivee;
    private final Piece pieceDeplacee;

    private final boolean capture;
    private final Piece pieceCapturee; // optionnel (peut être null)
    private final boolean promotion;
    private final Piece piecePromotion; // optionnel (peut être null)
    private final boolean roque;
    private final boolean enPassant;

    /**
     * <p>
     * Construit un coup.
     * </p>
     *
     * @param depart         case de départ
     * @param arrivee        case d’arrivée
     * @param pieceDeplacee  pièce déplacée (obligatoire)
     * @param capture        vrai si capture
     * @param pieceCapturee  pièce capturée (optionnel, peut être null)
     * @param promotion      vrai si promotion
     * @param piecePromotion pièce de promotion (optionnel, peut être null)
     * @param roque          vrai si roque
     * @param enPassant      vrai si en passant
     */
    public Coup(Case depart,
            Case arrivee,
            Piece pieceDeplacee,
            boolean capture,
            Piece pieceCapturee,
            boolean promotion,
            Piece piecePromotion,
            boolean roque,
            boolean enPassant) {
        if (depart == null || arrivee == null) {
            throw new IllegalArgumentException("Départ/Arrivée ne peuvent pas être null");
        }
        if (pieceDeplacee == null) {
            throw new IllegalArgumentException("pieceDeplacee est obligatoire");
        }
        this.depart = depart;
        this.arrivee = arrivee;
        this.pieceDeplacee = pieceDeplacee;

        this.capture = capture;
        this.pieceCapturee = pieceCapturee;
        this.promotion = promotion;
        this.piecePromotion = piecePromotion;
        this.roque = roque;
        this.enPassant = enPassant;
    }

    public Case depart() {
        return depart;
    }

    public Case arrivee() {
        return arrivee;
    }

    public Piece pieceDeplacee() {
        return pieceDeplacee;
    }

    public boolean estCapture() {
        return capture;
    }

    public Piece pieceCapturee() {
        return pieceCapturee;
    }

    public boolean estPromotion() {
        return promotion;
    }

    public Piece piecePromotion() {
        return piecePromotion;
    }

    public boolean estRoque() {
        return roque;
    }

    public boolean estEnPassant() {
        return enPassant;
    }

    public boolean caseArriveeContientPiece(Plateau plateau) {
        return plateau.estOccupe(arrivee);
    }
}
