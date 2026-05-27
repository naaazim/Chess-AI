package fr.moteurechecs.plateau;

/**
 * Représente un coup d'échecs.
 *
 * Pour être efficace (important avec un grand facteur de branchement),
 * un {@code Coup} contient directement les infos utiles :
 * - case de départ
 * - case d’arrivée
 * - pièce déplacée
 * - options : capture, promotion, roque, en passant
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
     * Construit un coup.
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

    /** @return case de départ du coup */
    public Case depart() {
        return depart;
    }

    /** @return case d'arrivée du coup */
    public Case arrivee() {
        return arrivee;
    }

    /** @return pièce déplacée (jamais null) */
    public Piece pieceDeplacee() {
        return pieceDeplacee;
    }

    /** @return {@code true} si le coup est une capture */
    public boolean estCapture() {
        return capture;
    }

    /** @return pièce capturée, ou {@code null} si pas de capture */
    public Piece pieceCapturee() {
        return pieceCapturee;
    }

    /** @return {@code true} si le coup est une promotion */
    public boolean estPromotion() {
        return promotion;
    }

    /** @return pièce de promotion, ou {@code null} si pas de promotion */
    public Piece piecePromotion() {
        return piecePromotion;
    }

    /** @return {@code true} si le coup est un roque */
    public boolean estRoque() {
        return roque;
    }

    /** @return {@code true} si le coup est une prise en passant */
    public boolean estEnPassant() {
        return enPassant;
    }

    /**
     * Crée un coup depuis deux indices de cases (0–63).
     * Utile pour le livre d'ouvertures.
     *
     * @param indiceDepart  indice de départ (0=a8, 63=h1)
     * @param indiceArrivee indice d'arrivée (0=a8, 63=h1)
     * @param codePromotion 0=pas promotion, 1=dame, 2=tour, 3=fou, 4=cavalier
     * @return coup correspondant
     */
    public static Coup depuisIndices(int indiceDepart, int indiceArrivee, int codePromotion) {
        Case depart  = Case.depuisIndice(indiceDepart);
        Case arrivee = Case.depuisIndice(indiceArrivee);

        // Déterminer la pièce de promotion selon le code
        Piece piecePromo = null;
        boolean estPromo = codePromotion > 0;
        if (estPromo) {
            piecePromo = switch (codePromotion) {
                case 1 -> Piece.DAME_BLANCHE;
                case 2 -> Piece.TOUR_BLANC;
                case 3 -> Piece.FOU_BLANC;
                case 4 -> Piece.CAVALIER_BLANC;
                default -> null;
            };
        }

        // Pièce fictive : sera ignorée si le coup est illégal lors de la validation
        Piece pieceFictive = Piece.PION_BLANC;
        return new Coup(depart, arrivee, pieceFictive, false, null, estPromo, piecePromo, false, false);
    }

    public boolean caseArriveeContientPiece(Plateau plateau) {
        return plateau.estOccupe(arrivee);
    }
}
