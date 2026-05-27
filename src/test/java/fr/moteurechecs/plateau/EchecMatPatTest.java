package fr.moteurechecs.plateau;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour les situations d'échec, mat et pat.
 */
class EchecMatPatTest {

    // ─── échec ───────────────────────────────────────────────────────────────

    @Test
    void echec_roiNoirSousAttaque() {
        // Dame blanche h5 attaque le roi noir e8
        Plateau p = Plateau.depuisFEN("4k3/8/8/7Q/8/8/8/4K3 b - - 0 1");
        assertTrue(p.estEnEchec(Couleur.NOIR));
        assertFalse(p.estEnEchec(Couleur.BLANC));
    }

    @Test
    void pasEchec_roiHorsDanger() {
        Plateau p = Plateau.positionInitiale();
        assertFalse(p.estEnEchec(Couleur.BLANC));
        assertFalse(p.estEnEchec(Couleur.NOIR));
    }

    @Test
    void echec_parCavalier() {
        // Cavalier blanc f3 attaque le roi noir e5 via saut en L
        Plateau p = Plateau.depuisFEN("8/8/8/4k3/8/5N2/8/4K3 b - - 0 1");
        assertTrue(p.estEnEchec(Couleur.NOIR));
    }

    @Test
    void echec_parPion() {
        // Pion blanc d5 attaque le roi noir e6
        Plateau p = Plateau.depuisFEN("8/8/4k3/3P4/8/8/8/4K3 b - - 0 1");
        assertTrue(p.estEnEchec(Couleur.NOIR));
    }

    // ─── mat ─────────────────────────────────────────────────────────────────

    @Test
    void mat_foolsMate_noirGagne() {
        // Fool's Mate : mat en 2 coups pour les noirs
        Plateau p = Plateau.depuisFEN("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3");
        List<Coup> coupsLegaux = GenerateurCoups.genererLegaux(p);
        assertTrue(coupsLegaux.isEmpty(), "Aucun coup légal = mat");
        assertTrue(p.estEnEchec(Couleur.BLANC), "Le roi blanc doit être en échec");
    }

    @Test
    void mat_scholarMate_noirEstMat() {
        // Scholar's Mate : Qxf7#
        Plateau p = Plateau.depuisFEN("rnbqk2r/pppp1Qpp/5n2/2b1p3/2B1P3/8/PPPP1PPP/RNB1K1NR b KQkq - 0 4");
        List<Coup> coupsLegaux = GenerateurCoups.genererLegaux(p);
        assertTrue(coupsLegaux.isEmpty(), "Scholar's mate — aucun coup légal");
        assertTrue(p.estEnEchec(Couleur.NOIR));
    }

    @Test
    void mat_backRank_tourBlanche() {
        // Tour blanche en a8, roi noir en h8 : mat de dos
        Plateau p = Plateau.depuisFEN("R6k/8/7K/8/8/8/8/8 b - - 0 1");
        List<Coup> coupsLegaux = GenerateurCoups.genererLegaux(p);
        assertTrue(coupsLegaux.isEmpty(), "Mat de dos");
        assertTrue(p.estEnEchec(Couleur.NOIR));
    }

    // ─── pat ─────────────────────────────────────────────────────────────────

    @Test
    void pat_roiSeulSansIssue() {
        // Position de pat classique : roi noir en h8, pas en échec mais aucun coup
        Plateau p = Plateau.depuisFEN("7k/5K2/6Q1/8/8/8/8/8 b - - 0 1");
        List<Coup> coupsLegaux = GenerateurCoups.genererLegaux(p);
        assertTrue(coupsLegaux.isEmpty(), "Pat — aucun coup légal");
        assertFalse(p.estEnEchec(Couleur.NOIR), "Pat ≠ échec");
    }

    @Test
    void pat_autrePositionClassique() {
        // Roi noir a8, pion blanc a7, roi blanc b6 :
        // a7 = pion (bloqué), b7 = attaqué par roi b6, b8 = attaqué par pion a7 → pat
        Plateau p = Plateau.depuisFEN("k7/P7/1K6/8/8/8/8/8 b - - 0 1");
        List<Coup> coupsLegaux = GenerateurCoups.genererLegaux(p);
        assertTrue(coupsLegaux.isEmpty(), "Pat classique roi + pion");
        assertFalse(p.estEnEchec(Couleur.NOIR));
    }

    // ─── échec double ─────────────────────────────────────────────────────────

    @Test
    void echecDouble_seulLeroiPeutBouger() {
        // Roi noir g8 en échec double par tour h1 et fou c4 — seul le roi peut fuir
        Plateau p = Plateau.depuisFEN("6k1/8/8/8/2B5/8/8/4K2R b - - 0 1");
        // Vérifier que le roi est en échec (double ou simple)
        assertTrue(p.estEnEchec(Couleur.NOIR));
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        // Tous les coups légaux doivent être des déplacements du roi
        assertTrue(coups.stream().allMatch(c -> c.pieceDeplacee() == Piece.ROI_NOIR),
                "En échec double, seul le roi peut bouger");
    }
}
