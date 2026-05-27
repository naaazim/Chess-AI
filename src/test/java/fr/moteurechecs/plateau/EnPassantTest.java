package fr.moteurechecs.plateau;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour la prise en passant.
 */
class EnPassantTest {

    // ─── en passant disponible ───────────────────────────────────────────────

    @Test
    void enPassant_blanc_disponibleApresPionNoirAvanceDeuxCases() {
        // Pion noir en d5 vient d'avancer de d7→d5 : case e.p. est d6
        Plateau p = Plateau.depuisFEN("8/8/8/3pP3/8/8/8/4K1k1 w - d6 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        boolean enPassant = coups.stream()
                .anyMatch(c -> c.estEnPassant()
                        && c.arrivee().equals(Case.depuisAlgebrique("d6")));
        assertTrue(enPassant, "Prise en passant vers d6 doit être disponible");
    }

    @Test
    void enPassant_noir_disponible() {
        // Pion blanc e5 vient d'avancer de e2→e5... ici c'est e4→e5 non valide.
        // Position : pion noir d4, pion blanc e4 vient d'avancer (e.p. = e3)
        Plateau p = Plateau.depuisFEN("4k3/8/8/8/3pP3/8/8/4K3 b - e3 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        boolean enPassant = coups.stream()
                .anyMatch(c -> c.estEnPassant()
                        && c.arrivee().equals(Case.depuisAlgebrique("e3")));
        assertTrue(enPassant, "Prise en passant noire vers e3 doit être disponible");
    }

    // ─── en passant absent si case e.p. non précisée ────────────────────────

    @Test
    void enPassant_absent_sansEnPassantDansLeFEN() {
        // Même position mais sans case e.p. dans le FEN
        Plateau p = Plateau.depuisFEN("8/8/8/3pP3/8/8/8/4K1k1 w - - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        boolean enPassant = coups.stream().anyMatch(Coup::estEnPassant);
        assertFalse(enPassant, "Pas de prise en passant sans case e.p.");
    }

    // ─── en passant exécuté : le pion capturé disparaît ─────────────────────

    @Test
    void enPassant_executee_pionCaptureSupprime() {
        Plateau p = Plateau.depuisFEN("8/8/8/3pP3/8/8/8/4K1k1 w - d6 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        Coup ep = coups.stream()
                .filter(Coup::estEnPassant)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Coup e.p. introuvable"));

        p.jouer(ep);
        // Le pion noir d5 doit avoir disparu
        assertNull(p.pieceEn(Case.depuisAlgebrique("d5")),
                "Le pion capturé en passant doit disparaître de d5");
        // Le pion blanc doit être en d6
        assertEquals(Piece.PION_BLANC, p.pieceEn(Case.depuisAlgebrique("d6")));
    }

    // ─── en passant annulé après le coup ────────────────────────────────────

    @Test
    void enPassant_annuleeApresJouer_restaurePionCapture() {
        Plateau p = Plateau.depuisFEN("8/8/8/3pP3/8/8/8/4K1k1 w - d6 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        Coup ep = coups.stream().filter(Coup::estEnPassant).findFirst()
                .orElseThrow(() -> new AssertionError("Coup e.p. introuvable"));

        EtatPlateau avant = p.jouerAvecSauvegarde(ep);
        p.annuler(avant);

        // Le pion noir d5 doit être restauré
        assertEquals(Piece.PION_NOIR,  p.pieceEn(Case.depuisAlgebrique("d5")));
        assertEquals(Piece.PION_BLANC, p.pieceEn(Case.depuisAlgebrique("e5")));
        assertNull(p.pieceEn(Case.depuisAlgebrique("d6")));
    }

    // ─── en passant illégal (laisserait roi en échec) ────────────────────────

    @Test
    void enPassant_illegal_siLaisseRoiEnEchec() {
        // Roi blanc e5, pion blanc d5, pion noir c5 e.p. en c6,
        // mais une tour noire a5 épigle le pion blanc d5 sur le roi e5.
        // Si on prend en passant d5xc6, la rangée 5 est ouverte → roi e5 attaqué par a5.
        Plateau p = Plateau.depuisFEN("8/8/8/r1pPK3/8/8/8/6k1 w - c6 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        boolean epD5C6 = coups.stream()
                .anyMatch(c -> c.estEnPassant()
                        && c.depart().equals(Case.depuisAlgebrique("d5"))
                        && c.arrivee().equals(Case.depuisAlgebrique("c6")));
        assertFalse(epD5C6, "En passant illégal car laisse le roi en échec");
    }
}
