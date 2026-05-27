package fr.moteurechecs.plateau;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour la promotion de pion.
 */
class PromotionTest {

    // ─── 4 promotions générées ───────────────────────────────────────────────

    @Test
    void promotion_quatreCoupsGeneres_pion_e7() {
        // Pion blanc e7, rien devant lui
        Plateau p = Plateau.depuisFEN("8/4P3/8/8/8/8/8/4K1k1 w - - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        long promotions = coups.stream()
                .filter(c -> c.depart().equals(Case.depuisAlgebrique("e7")) && c.estPromotion())
                .count();
        assertEquals(4, promotions, "4 promotions possibles (Q, R, B, N)");
    }

    @Test
    void promotion_inclutDame_Tour_Fou_Cavalier() {
        Plateau p = Plateau.depuisFEN("8/4P3/8/8/8/8/8/4K1k1 w - - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        List<Piece> piecesPromo = coups.stream()
                .filter(c -> c.estPromotion() && c.depart().equals(Case.depuisAlgebrique("e7")))
                .map(Coup::piecePromotion)
                .toList();
        assertTrue(piecesPromo.contains(Piece.DAME_BLANCHE));
        assertTrue(piecesPromo.contains(Piece.TOUR_BLANC));
        assertTrue(piecesPromo.contains(Piece.FOU_BLANC));
        assertTrue(piecesPromo.contains(Piece.CAVALIER_BLANC));
    }

    // ─── promotion avec capture ──────────────────────────────────────────────

    @Test
    void promotion_avecCapture_huitCoupsGeneres() {
        // Pion blanc e7, pièces noires en d8 et f8 (en plus de e8 libre)
        Plateau p = Plateau.depuisFEN("3r1r2/4P3/8/8/8/8/8/4K1k1 w - - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        long promotions = coups.stream()
                .filter(c -> c.depart().equals(Case.depuisAlgebrique("e7")) && c.estPromotion())
                .count();
        // 4 (e8) + 4 (xd8) + 4 (xf8) = 12
        assertEquals(12, promotions, "12 promotions avec 2 captures possibles");
    }

    // ─── promotion exécutée ──────────────────────────────────────────────────

    @Test
    void promotion_executee_remplacePionParDame() {
        Plateau p = Plateau.depuisFEN("8/4P3/8/8/8/8/8/4K1k1 w - - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        Coup promoReine = coups.stream()
                .filter(c -> c.estPromotion() && c.piecePromotion() == Piece.DAME_BLANCHE)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Promotion en dame introuvable"));

        p.jouer(promoReine);
        assertEquals(Piece.DAME_BLANCHE, p.pieceEn(Case.depuisAlgebrique("e8")));
        assertNull(p.pieceEn(Case.depuisAlgebrique("e7")));
    }

    @Test
    void promotion_executee_remplacePionParCavalier() {
        Plateau p = Plateau.depuisFEN("8/4P3/8/8/8/8/8/4K1k1 w - - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        Coup promoCavalier = coups.stream()
                .filter(c -> c.estPromotion() && c.piecePromotion() == Piece.CAVALIER_BLANC)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Promotion en cavalier introuvable"));

        p.jouer(promoCavalier);
        assertEquals(Piece.CAVALIER_BLANC, p.pieceEn(Case.depuisAlgebrique("e8")));
    }

    // ─── promotion annulée ───────────────────────────────────────────────────

    @Test
    void promotion_annulee_restaurePion() {
        Plateau p = Plateau.depuisFEN("8/4P3/8/8/8/8/8/4K1k1 w - - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        Coup promo = coups.stream().filter(c -> c.estPromotion() && c.piecePromotion() == Piece.DAME_BLANCHE)
                .findFirst().orElseThrow();

        EtatPlateau avant = p.jouerAvecSauvegarde(promo);
        p.annuler(avant);

        assertEquals(Piece.PION_BLANC, p.pieceEn(Case.depuisAlgebrique("e7")));
        assertNull(p.pieceEn(Case.depuisAlgebrique("e8")));
    }

    // ─── promotion noire ─────────────────────────────────────────────────────

    @Test
    void promotion_noire_quatreCoupsGeneres() {
        Plateau p = Plateau.depuisFEN("4K1k1/8/8/8/8/8/4p3/8 b - - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        long promotions = coups.stream()
                .filter(c -> c.depart().equals(Case.depuisAlgebrique("e2")) && c.estPromotion())
                .count();
        assertEquals(4, promotions);
    }
}
