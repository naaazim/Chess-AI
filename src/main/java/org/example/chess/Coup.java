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

    /**
     * Crée un coup depuis deux indices de cases (0-63).
     * Utile pour le livre d'ouvertures.
     * @param fromIdx indice de départ (0=a8, 63=h1)
     * @param toIdx indice d'arrivée
     * @param promoCode 0=pas promotion, 1=dame, 2=tour, 3=fou, 4=cavalier
     */
    public static Coup depuisIndices(int fromIdx, int toIdx, int promoCode) {
        Case depart = Case.depuisIndice(fromIdx);
        Case arrivee = Case.depuisIndice(toIdx);

        Piece promotion = null;
        boolean promo = promoCode > 0;
        if (promo) {
            promotion = switch (promoCode) {
                case 1 -> Piece.DAME_BLANCHE;
                case 2 -> Piece.TOUR_BLANC;
                case 3 -> Piece.FOU_BLANC;
                case 4 -> Piece.CAVALIER_BLANC;
                default -> null;
            };
        }

        // Placeholder - la piece sera determined lors de la validation
        // Utiliser un pion comme placeholder (sera ignore si coup illegal)
        Piece placeholderPiece = Piece.PION_BLANC;
        return new Coup(depart, arrivee, placeholderPiece, false, null, promo, promotion, false, false);
    }

    public boolean caseArriveeContientPiece(Plateau plateau) {
        return plateau.estOccupe(arrivee);
    }
}
