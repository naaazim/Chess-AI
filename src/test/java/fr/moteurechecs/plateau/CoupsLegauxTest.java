package fr.moteurechecs.plateau;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests des coups légaux et illégaux via GenerateurCoups.genererLegaux().
 * On utilise des positions FEN connues pour chaque scénario.
 */
class CoupsLegauxTest {

    // ─── position initiale ───────────────────────────────────────────────────

    @Test
    void positionInitiale_20CoupsLegaux() {
        Plateau p = Plateau.positionInitiale();
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        // 16 coups de pions (2 par pion) + 4 coups de cavaliers
        assertEquals(20, coups.size());
    }

    // ─── pion : avance simple et double ─────────────────────────────────────

    @Test
    void pion_avanceSimpleEtDouble_depuisRangee2() {
        Plateau p = Plateau.depuisFEN("8/8/8/8/8/8/4P3/4K3 w - - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        // e2-e3 et e2-e4
        long coupsPione2 = coups.stream()
                .filter(c -> c.depart().equals(Case.depuisAlgebrique("e2")))
                .count();
        assertEquals(2, coupsPione2);
    }

    @Test
    void pion_avanceSimpleSeulement_depuisRangee3() {
        Plateau p = Plateau.depuisFEN("8/8/8/8/8/4P3/8/4K3 w - - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        long coupsPion = coups.stream()
                .filter(c -> c.depart().equals(Case.depuisAlgebrique("e3")))
                .count();
        assertEquals(1, coupsPion);
    }

    @Test
    void pion_bloque_aucunCoup() {
        // Pion blanc e4, pion noir e5 : bloqué
        Plateau p = Plateau.depuisFEN("8/8/8/4p3/4P3/8/8/4K1k1 w - - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        boolean aucunCoupE4 = coups.stream()
                .noneMatch(c -> c.depart().equals(Case.depuisAlgebrique("e4")));
        assertTrue(aucunCoupE4);
    }

    // ─── pion : capture ──────────────────────────────────────────────────────

    @Test
    void pion_capture_deuxCibles() {
        // Pion blanc e4, pions noirs d5 et f5 (pas e5 qui bloquerait la montée)
        Plateau p = Plateau.depuisFEN("8/8/8/3p1p2/4P3/8/8/4K1k1 w - - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        long capturesE4 = coups.stream()
                .filter(c -> c.depart().equals(Case.depuisAlgebrique("e4")) && c.estCapture())
                .count();
        assertEquals(2, capturesE4);
    }

    // ─── coups illégaux : le roi reste en échec ──────────────────────────────

    @Test
    void coupIllegal_nepasResoudreEchec_exclu() {
        // Roi blanc e1 en échec par la dame noire e8 — seuls les coups
        // qui sortent le roi de l'échec sont légaux
        Plateau p = Plateau.depuisFEN("4q3/8/8/8/8/8/8/4K3 w - - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        // Vérifier qu'aucun coup légal ne laisse le roi en e1 sous échec
        for (Coup coup : coups) {
            EtatPlateau avant = p.jouerAvecSauvegarde(coup);
            assertFalse(p.estEnEchec(Couleur.BLANC),
                    "Coup " + coup.depart() + coup.arrivee() + " laisse le roi en échec");
            p.annuler(avant);
        }
    }

    @Test
    void deplacementRoi_verseCaseAttaquee_exclu() {
        // Roi blanc e1, tour noire e8 : e1 est sur la colonne e — le roi
        // ne peut pas rester sur la colonne e (f1 et d1 doivent être libres)
        Plateau p = Plateau.depuisFEN("4r3/8/8/8/8/8/8/4K3 w - - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        boolean versE2 = coups.stream()
                .anyMatch(c -> c.arrivee().equals(Case.depuisAlgebrique("e2")));
        assertFalse(versE2, "e2 est attaqué par la tour e8, le roi ne peut pas y aller");
    }

    // ─── cavalier ───────────────────────────────────────────────────────────

    @Test
    void cavalier_e4_huitCibles_sansObstacles() {
        Plateau p = Plateau.depuisFEN("8/8/8/8/4N3/8/8/4K1k1 w - - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        long coupsCavalier = coups.stream()
                .filter(c -> c.depart().equals(Case.depuisAlgebrique("e4")))
                .count();
        assertEquals(8, coupsCavalier);
    }

    @Test
    void cavalier_coin_deuxCibles() {
        Plateau p = Plateau.depuisFEN("N7/8/8/8/8/8/8/4K1k1 w - - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        long coupsCavalier = coups.stream()
                .filter(c -> c.depart().equals(Case.depuisAlgebrique("a8")))
                .count();
        assertEquals(2, coupsCavalier);
    }

    // ─── fou ────────────────────────────────────────────────────────────────

    @Test
    void fou_centre_treizeCases() {
        Plateau p = Plateau.depuisFEN("8/8/8/8/4B3/8/8/4K1k1 w - - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        long coupsFou = coups.stream()
                .filter(c -> c.depart().equals(Case.depuisAlgebrique("e4")))
                .count();
        assertEquals(13, coupsFou);
    }

    // ─── tour ────────────────────────────────────────────────────────────────

    @Test
    void tour_centre_quatorzeCases() {
        Plateau p = Plateau.depuisFEN("8/8/8/8/4R3/8/8/4K1k1 w - - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        long coupsTour = coups.stream()
                .filter(c -> c.depart().equals(Case.depuisAlgebrique("e4")))
                .count();
        // 7 (rangée) + 6 (colonne e : e5..e8 vers le haut, e3..e2 vers le bas, e1 bloqué par le roi)
        assertEquals(13, coupsTour);
    }

    // ─── dame ────────────────────────────────────────────────────────────────

    @Test
    void dame_centre_vingtseptCases() {
        Plateau p = Plateau.depuisFEN("8/8/8/8/4Q3/8/8/4K1k1 w - - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        long coupsDame = coups.stream()
                .filter(c -> c.depart().equals(Case.depuisAlgebrique("e4")))
                .count();
        // 7 (rangée) + 6 (colonne e, e1 bloqué par le roi) + 3 + 4 + 3 + 3 (diagonales) = 26
        assertEquals(26, coupsDame);
    }

    // ─── roi ────────────────────────────────────────────────────────────────

    @Test
    void roi_centre_huitCases() {
        Plateau p = Plateau.depuisFEN("8/8/8/8/4K3/8/8/6k1 w - - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        long coupsRoi = coups.stream()
                .filter(c -> c.depart().equals(Case.depuisAlgebrique("e4")))
                .count();
        assertEquals(8, coupsRoi);
    }

    // ─── épinglé : pièce épinglée ne peut pas bouger ─────────────────────────

    @Test
    void piece_epinglee_nepeutPasBouger() {
        // Roi blanc e1, cavalier blanc e2 épinglé par la dame noire e8
        Plateau p = Plateau.depuisFEN("4q3/8/8/8/8/8/4N3/4K3 w - - 0 1");
        List<Coup> coups = GenerateurCoups.genererLegaux(p);
        // Le cavalier en e2 est épinglé sur la colonne e → aucun de ses coups ne doit apparaître
        boolean cavalierPeutBouger = coups.stream()
                .anyMatch(c -> c.depart().equals(Case.depuisAlgebrique("e2"))
                        && c.pieceDeplacee() == Piece.CAVALIER_BLANC);
        assertFalse(cavalierPeutBouger, "Le cavalier épinglé ne doit pas pouvoir bouger");
    }
}
