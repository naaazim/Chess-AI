package fr.moteurechecs.plateau;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour la classe Case.
 */
class CaseTest {

    // ─── depuisAlgebrique ───────────────────────────────────────────────────

    @Test
    void depuisAlgebrique_e4_retourneColonne4Ligne4() {
        Case c = Case.depuisAlgebrique("e4");
        assertEquals(4, c.colonne());
        assertEquals(4, c.ligne());
    }

    @Test
    void depuisAlgebrique_a1_retourneIndice56() {
        Case c = Case.depuisAlgebrique("a1");
        assertEquals(56, c.indice());
    }

    @Test
    void depuisAlgebrique_h8_retourneIndice7() {
        Case c = Case.depuisAlgebrique("h8");
        assertEquals(7, c.indice());
    }

    @Test
    void depuisAlgebrique_a8_retourneIndice0() {
        Case c = Case.depuisAlgebrique("a8");
        assertEquals(0, c.indice());
    }

    @Test
    void depuisAlgebrique_invalide_leveException() {
        assertThrows(IllegalArgumentException.class, () -> Case.depuisAlgebrique("z9"));
        assertThrows(IllegalArgumentException.class, () -> Case.depuisAlgebrique(null));
        assertThrows(IllegalArgumentException.class, () -> Case.depuisAlgebrique("e"));
    }

    // ─── versAlgebrique ─────────────────────────────────────────────────────

    @Test
    void versAlgebrique_roundtrip() {
        String[] cases = {"a1", "a8", "h1", "h8", "e4", "d5"};
        for (String notation : cases) {
            assertEquals(notation, Case.depuisAlgebrique(notation).versAlgebrique());
        }
    }

    // ─── identité du cache ──────────────────────────────────────────────────

    @Test
    void cache_memeObjetPourMemeCoordonee() {
        assertSame(Case.depuisAlgebrique("e4"), Case.depuisAlgebrique("e4"));
        assertSame(Case.depuisIndice(0), Case.depuisAlgebrique("a8"));
    }

    // ─── bit ────────────────────────────────────────────────────────────────

    @Test
    void bit_a8_est1() {
        assertEquals(1L, Case.depuisAlgebrique("a8").bit());
    }

    @Test
    void bit_h1_estDernierBit() {
        assertEquals(1L << 63, Case.depuisAlgebrique("h1").bit());
    }
}
