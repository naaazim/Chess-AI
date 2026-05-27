package fr.moteurechecs.plateau;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour le roque (petit et grand, légal et illégal).
 */
class RoqueTest {

    // ─── roque légal ─────────────────────────────────────────────────────────

    @Test
    void petitRoqueBlanc_disponible() {
        // Position après 1.e4 e5 2.Nf3 Nc6 3.Bc4 Bc5 : petit roque blanc possible
        Plateau p = Plateau.depuisFEN("r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        boolean petitRoque = coups.stream()
                .anyMatch(c -> c.estRoque() && c.arrivee().equals(Case.depuisAlgebrique("g1")));
        assertTrue(petitRoque, "Petit roque blanc doit être disponible");
    }

    @Test
    void grandRoqueBlanc_disponible() {
        // Cases dégagées côté dame
        Plateau p = Plateau.depuisFEN("r3kbnr/ppp1pppp/2nq4/3p4/3P4/2NQ4/PPP1PPPP/R3KBNR w KQkq - 4 5");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        boolean grandRoque = coups.stream()
                .anyMatch(c -> c.estRoque() && c.arrivee().equals(Case.depuisAlgebrique("c1")));
        assertTrue(grandRoque, "Grand roque blanc doit être disponible");
    }

    // ─── roque interdit si le roi est en échec ───────────────────────────────

    @Test
    void roque_interdit_siRoiEnEchec() {
        // Roi blanc en échec par la tour noire e8 : le roque est interdit
        Plateau p = Plateau.depuisFEN("4r3/8/8/8/8/8/8/R3K2R w KQ - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        boolean aucunRoque = coups.stream().noneMatch(Coup::estRoque);
        assertTrue(aucunRoque, "Roque interdit quand le roi est en échec");
    }

    // ─── roque interdit si case de passage attaquée ──────────────────────────

    @Test
    void petitRoque_interdit_siCaseF1Attaquee() {
        // Tour noire f8 : f1 est attaqué → petit roque interdit
        Plateau p = Plateau.depuisFEN("5r2/8/8/8/8/8/8/R3K2R w KQ - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        boolean petitRoque = coups.stream()
                .anyMatch(c -> c.estRoque() && c.arrivee().equals(Case.depuisAlgebrique("g1")));
        assertFalse(petitRoque, "Petit roque interdit car f1 est attaqué");
    }

    @Test
    void grandRoque_interdit_siCaseD1Attaquee() {
        // Tour noire d8 : d1 est attaqué → grand roque interdit
        Plateau p = Plateau.depuisFEN("3r4/8/8/8/8/8/8/R3K2R w KQ - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        boolean grandRoque = coups.stream()
                .anyMatch(c -> c.estRoque() && c.arrivee().equals(Case.depuisAlgebrique("c1")));
        assertFalse(grandRoque, "Grand roque interdit car d1 est attaqué");
    }

    // ─── roque interdit si droits perdus ────────────────────────────────────

    @Test
    void roque_interdit_sansDroits() {
        // FEN sans droits de roque ("-")
        Plateau p = Plateau.depuisFEN("r3k2r/8/8/8/8/8/8/R3K2R w - - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        boolean aucunRoque = coups.stream().noneMatch(Coup::estRoque);
        assertTrue(aucunRoque, "Aucun roque sans droits");
    }

    // ─── roque effectif : la tour et le roi bougent bien ────────────────────

    @Test
    void petitRoque_deplaceRoiEnG1EtTourEnF1() {
        Plateau p = Plateau.depuisFEN("8/8/8/8/8/8/8/4K2R w K - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        Coup petitRoque = coups.stream()
                .filter(c -> c.estRoque() && c.arrivee().equals(Case.depuisAlgebrique("g1")))
                .findFirst().orElse(null);
        assertNotNull(petitRoque, "Petit roque doit exister");

        p.jouer(petitRoque);
        assertEquals(Piece.ROI_BLANC,  p.pieceEn(Case.depuisAlgebrique("g1")));
        assertEquals(Piece.TOUR_BLANC, p.pieceEn(Case.depuisAlgebrique("f1")));
        assertNull(p.pieceEn(Case.depuisAlgebrique("e1")));
        assertNull(p.pieceEn(Case.depuisAlgebrique("h1")));
    }

    @Test
    void grandRoque_deplaceRoiEnC1EtTourEnD1() {
        Plateau p = Plateau.depuisFEN("8/8/8/8/8/8/8/R3K3 w Q - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        Coup grandRoque = coups.stream()
                .filter(c -> c.estRoque() && c.arrivee().equals(Case.depuisAlgebrique("c1")))
                .findFirst().orElse(null);
        assertNotNull(grandRoque, "Grand roque doit exister");

        p.jouer(grandRoque);
        assertEquals(Piece.ROI_BLANC,  p.pieceEn(Case.depuisAlgebrique("c1")));
        assertEquals(Piece.TOUR_BLANC, p.pieceEn(Case.depuisAlgebrique("d1")));
        assertNull(p.pieceEn(Case.depuisAlgebrique("e1")));
        assertNull(p.pieceEn(Case.depuisAlgebrique("a1")));
    }
}
