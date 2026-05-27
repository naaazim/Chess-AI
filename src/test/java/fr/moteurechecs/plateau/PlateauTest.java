package fr.moteurechecs.plateau;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour Plateau : position initiale, FEN, pieceEn, trait, echec.
 */
class PlateauTest {

    // ─── position initiale ───────────────────────────────────────────────────

    @Test
    void positionInitiale_traitEstBlanc() {
        Plateau p = Plateau.positionInitiale();
        assertEquals(Couleur.BLANC, p.trait());
    }

    @Test
    void positionInitiale_piecesEnBonnesPositions() {
        Plateau p = Plateau.positionInitiale();
        assertEquals(Piece.TOUR_BLANC,    p.pieceEn(Case.depuisAlgebrique("a1")));
        assertEquals(Piece.ROI_BLANC,     p.pieceEn(Case.depuisAlgebrique("e1")));
        assertEquals(Piece.DAME_BLANCHE,  p.pieceEn(Case.depuisAlgebrique("d1")));
        assertEquals(Piece.ROI_NOIR,      p.pieceEn(Case.depuisAlgebrique("e8")));
        assertEquals(Piece.DAME_NOIRE,    p.pieceEn(Case.depuisAlgebrique("d8")));
        assertEquals(Piece.PION_BLANC,    p.pieceEn(Case.depuisAlgebrique("e2")));
        assertEquals(Piece.PION_NOIR,     p.pieceEn(Case.depuisAlgebrique("e7")));
    }

    @Test
    void positionInitiale_casesVidesAuCentre() {
        Plateau p = Plateau.positionInitiale();
        assertNull(p.pieceEn(Case.depuisAlgebrique("e4")));
        assertNull(p.pieceEn(Case.depuisAlgebrique("d5")));
        assertNull(p.pieceEn(Case.depuisAlgebrique("e5")));
    }

    @Test
    void positionInitiale_pasEnEchec() {
        Plateau p = Plateau.positionInitiale();
        assertFalse(p.estEnEchec(Couleur.BLANC));
        assertFalse(p.estEnEchec(Couleur.NOIR));
    }

    // ─── depuisFEN ───────────────────────────────────────────────────────────

    @Test
    void depuisFEN_positionInitialeFEN() {
        Plateau p = Plateau.depuisFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        assertEquals(Couleur.BLANC, p.trait());
        assertEquals(Piece.ROI_BLANC, p.pieceEn(Case.depuisAlgebrique("e1")));
        assertEquals(Piece.ROI_NOIR,  p.pieceEn(Case.depuisAlgebrique("e8")));
    }

    @Test
    void depuisFEN_traitNoir() {
        Plateau p = Plateau.depuisFEN("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");
        assertEquals(Couleur.NOIR, p.trait());
        assertEquals(Piece.PION_BLANC, p.pieceEn(Case.depuisAlgebrique("e4")));
        assertNull(p.pieceEn(Case.depuisAlgebrique("e2")));
    }

    // ─── jouer / annuler ─────────────────────────────────────────────────────

    @Test
    void jouerEtAnnuler_restaureLEtat() {
        Plateau p = Plateau.positionInitiale();
        EtatPlateau etatAvant = p.sauvegarderEtat();

        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        assertFalse(coups.isEmpty());

        EtatPlateau sauvegarde = p.jouerAvecSauvegarde(coups.get(0));
        p.annuler(sauvegarde);

        assertEquals(etatAvant, p.sauvegarderEtat());
    }

    @Test
    void jouer_changeLeTrait() {
        Plateau p = Plateau.positionInitiale();
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        p.jouer(coups.get(0));
        assertEquals(Couleur.NOIR, p.trait());
    }

    // ─── estOccupe ───────────────────────────────────────────────────────────

    @Test
    void estOccupe_casesInitiales() {
        Plateau p = Plateau.positionInitiale();
        assertTrue(p.estOccupe(Case.depuisAlgebrique("e1")));
        assertTrue(p.estOccupe(Case.depuisAlgebrique("e2")));
        assertFalse(p.estOccupe(Case.depuisAlgebrique("e4")));
    }
}
