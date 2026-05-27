package fr.moteurechecs.ouverture;

import fr.moteurechecs.plateau.Case;
import fr.moteurechecs.plateau.Couleur;
import fr.moteurechecs.plateau.Coup;
import fr.moteurechecs.plateau.Piece;

/**
 * Entrée du livre d'ouvertures : position + coup recommandé + nom de l'ouverture.
 *
 * Format binaire optimisé pour une lecture rapide : case de départ, case d'arrivée
 * et code de promotion sont stockés sur un octet chacun afin de minimiser la taille
 * du fichier {@code .book}.
 */
public final class EntreeLivreOuvertures {

    private final ClePosition positionKey;

    // indices de case sur 0–63 (compacité)
    private final int fromSquare;
    private final int toSquare;
    private final int promoPiece; // 0 = pas de promotion, 1–4 = type de promotion

    private final String openingName;
    private final int    moveCount; // nombre de fois ce coup a été joué dans les parties sources

    /**
     * Construit une entrée du livre d'ouvertures.
     *
     * @param positionKey clé de la position de départ
     * @param fromSquare  indice de la case de départ (0–63)
     * @param toSquare    indice de la case d'arrivée (0–63)
     * @param promoPiece  code de promotion (0 = aucune)
     * @param openingName nom de l'ouverture
     * @param moveCount   fréquence du coup dans la base de données
     */
    public EntreeLivreOuvertures(
            ClePosition positionKey,
            int fromSquare,
            int toSquare,
            int promoPiece,
            String openingName,
            int moveCount) {
        this.positionKey  = positionKey;
        this.fromSquare   = fromSquare;
        this.toSquare     = toSquare;
        this.promoPiece   = promoPiece;
        this.openingName  = openingName;
        this.moveCount    = moveCount;
    }

    /**
     * Reconstruit un objet {@link Coup} depuis cette entrée du livre.
     *
     * Le coup est générique : la légalité sera vérifiée par le générateur de coups.
     * La pièce déplacée exacte n'est pas stockée dans le livre.
     *
     * @param couleurTrait couleur du joueur au trait (pour les promotions)
     * @return coup correspondant à cette entrée
     */
    public Coup toCoup(Couleur couleurTrait) {
        Case depart  = Case.depuisIndice(fromSquare);
        Case arrivee = Case.depuisIndice(toSquare);

        // résolution du type de pièce de promotion selon la couleur
        Piece promotion = null;
        if (promoPiece > 0) {
            promotion = switch (promoPiece) {
                case 1 -> couleurTrait == Couleur.BLANC ? Piece.DAME_BLANCHE    : Piece.DAME_NOIRE;
                case 2 -> couleurTrait == Couleur.BLANC ? Piece.TOUR_BLANC      : Piece.TOUR_NOIRE;
                case 3 -> couleurTrait == Couleur.BLANC ? Piece.FOU_BLANC       : Piece.FOU_NOIR;
                case 4 -> couleurTrait == Couleur.BLANC ? Piece.CAVALIER_BLANC  : Piece.CAVALIER_NOIR;
                default -> null;
            };
        }

        // coup générique validé ultérieurement par le générateur de coups légaux
        return Coup.depuisIndices(fromSquare, toSquare, promoPiece);
    }

    /** @return clé de position associée à cette entrée */
    public ClePosition getPositionKey() { return positionKey; }
    /** @return indice de la case de départ */
    public int getFromSquare()          { return fromSquare;  }
    /** @return indice de la case d'arrivée */
    public int getToSquare()            { return toSquare;    }
    /** @return code de promotion (0 = aucune) */
    public int getPromoPiece()          { return promoPiece;  }
    /** @return nom de l'ouverture */
    public String getOpeningName()      { return openingName; }
    /** @return fréquence du coup dans la base */
    public int getMoveCount()           { return moveCount;   }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format("%s: %s%s (%s)",
                openingName,
                Case.depuisIndice(fromSquare).versAlgebrique(),
                Case.depuisIndice(toSquare).versAlgebrique(),
                moveCount);
    }
}
