package org.example.chess;

/**
 * <p>
 * Représente une pièce "exacte" : type + couleur.
 * Par exemple : {@code PION_BLANC} ou {@code CAVALIER_NOIR}.
 * </p>
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
     * <p>Caractère utilisé dans le format FEN.</p>
     *
     * @return caractère FEN
     */
    public char caractereFEN() {
        return fen;
    }

    /**
     * <p>Retourne la couleur de cette pièce.</p>
     *
     * @return BLANC ou NOIR
     */
    public Couleur couleur() {
        return Character.isUpperCase(fen) ? Couleur.BLANC : Couleur.NOIR;
    }
}
