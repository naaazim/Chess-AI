package fr.moteurechecs.ouverture;

import fr.moteurechecs.plateau.Couleur;
import fr.moteurechecs.plateau.Plateau;

/**
 * Clé de position pour le livre d'ouvertures.
 *
 * Encode la position en 5 composantes : occupation des cases (64 bits),
 * identité des pièces (blancs/noirs), droits de roque (4 bits), case en passant
 * (8 bits) et couleur au trait. Combinées en un {@code long} via {@link #toHashKey()},
 * elles permettent un accès O(1) dans la {@code HashMap} du livre.
 */
public final class ClePosition {

    private final long    piecesKey;   // occupation complète du plateau
    private final long    couleurKey;  // bitboard des pièces blanches
    private final long    castlingKey; // 4 bits : droits de roque
    private final long    epKey;       // case en passant (0 si aucune)
    private final boolean traitBlanc;  // true si les Blancs jouent

    /**
     * Construit une clé de position à partir de ses cinq composantes.
     *
     * @param piecesKey    bitboard d'occupation du plateau
     * @param couleurKey   bitboard des pièces blanches
     * @param castlingKey  droits de roque encodés sur 4 bits
     * @param epKey        case de prise en passant (0 si absente)
     * @param traitBlanc   {@code true} si c'est aux Blancs de jouer
     */
    public ClePosition(long piecesKey, long couleurKey, long castlingKey, long epKey, boolean traitBlanc) {
        this.piecesKey   = piecesKey;
        this.couleurKey  = couleurKey;
        this.castlingKey = castlingKey;
        this.epKey       = epKey;
        this.traitBlanc  = traitBlanc;
    }

    /**
     * Génère une clé de position depuis le plateau courant.
     *
     * @param plateau position à encoder
     * @return clé de position correspondante
     */
    public static ClePosition fromPlateau(Plateau plateau) {
        // occupation complète + bitboard des blancs
        long pieces   = plateau.occupes();
        long couleurs = plateau.blancs();

        // droits de roque packés sur 4 bits
        long castling = 0;
        if (plateau.getRoqueBlancRoi())   castling |= 1;
        if (plateau.getRoqueBlancReine()) castling |= 2;
        if (plateau.getRoqueNoirRoi())    castling |= 4;
        if (plateau.getRoqueNoirReine())  castling |= 8;

        long ep   = plateau.getEnPassant();
        boolean trait = (plateau.trait() == Couleur.BLANC);

        return new ClePosition(pieces, couleurs, castling, ep, trait);
    }

    /**
     * Génère un hash 64 bits utilisé comme clé dans la {@code HashMap} du livre.
     *
     * @return hash combinant les cinq composantes
     */
    public long toHashKey() {
        // le bit de trait est placé dans le bit 63 pour une bonne distribution
        long traitBit = traitBlanc ? 1L : 0L;
        return piecesKey ^ (couleurKey >>> 1) ^ (castlingKey << 48) ^ (epKey << 52) ^ (traitBit << 63);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClePosition that = (ClePosition) o;
        return piecesKey   == that.piecesKey
                && couleurKey  == that.couleurKey
                && castlingKey == that.castlingKey
                && epKey       == that.epKey
                && traitBlanc  == that.traitBlanc;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Long.hashCode(piecesKey) ^ Long.hashCode(couleurKey);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("Key[%016x:%016x:%x:%x:%s]",
                piecesKey, couleurKey, castlingKey, epKey, traitBlanc ? "W" : "B");
    }

    /** @return bitboard d'occupation du plateau */
    public long getPiecesKey()   { return piecesKey;   }
    /** @return bitboard des pièces blanches */
    public long getCouleurKey()  { return couleurKey;  }
    /** @return droits de roque (4 bits) */
    public long getCastlingKey() { return castlingKey; }
    /** @return case de prise en passant */
    public long getEpKey()       { return epKey;       }
    /** @return {@code true} si les Blancs jouent */
    public boolean isTraitBlanc() { return traitBlanc; }
}
