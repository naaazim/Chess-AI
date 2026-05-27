package fr.moteurechecs.plateau;

/**
 * Représente une pièce "exacte" : type + couleur.
 * Par exemple : {@code PION_BLANC} ou {@code CAVALIER_NOIR}.
 */
public enum Piece {
    PION_BLANC('P'),
    CAVALIER_BLANC('N'),
    FOU_BLANC('B'),
    TOUR_BLANC('R'),
    DAME_BLANCHE('Q'),
    ROI_BLANC('K'),

    PION_NOIR('p'),
    CAVALIER_NOIR('n'),
    FOU_NOIR('b'),
    TOUR_NOIRE('r'),
    DAME_NOIRE('q'),
    ROI_NOIR('k');

    private final char fen;

    Piece(char fen) {
        this.fen = fen;
    }

    /**
     *
     * Caractère utilisé dans le format FEN.
     *
     * @return caractère FEN
     */
    public char caractereFEN() {
        return fen;
    }

    /**
     *
     * Retourne la couleur de cette pièce.
     *
     * @return BLANC ou NOIR
     */
    public Couleur couleur() {
        return Character.isUpperCase(fen) ? Couleur.BLANC : Couleur.NOIR;
    }
}
