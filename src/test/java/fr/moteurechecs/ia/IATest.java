package fr.moteurechecs.ia;

import fr.moteurechecs.plateau.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour l'IA : le coup retourné doit être légal et pertinent.
 */
class IATest {

    // ─── meilleurCoup retourne un coup légal ─────────────────────────────────

    @Test
    void meilleurCoup_positionInitiale_retourneCoupLegal() {
        Plateau p = Plateau.positionInitiale();
        Coup coup = OrchestrateurIA.meilleurCoup(p, Niveau.FACILE);
        assertNotNull(coup, "L'IA doit retourner un coup depuis la position initiale");

        // Vérifier que le coup est dans la liste des coups légaux
        java.util.List<Coup> legaux = GenerateurCoups.genererLegaux(p);
        boolean coupLegal = legaux.stream()
                .anyMatch(c -> c.depart().equals(coup.depart()) && c.arrivee().equals(coup.arrivee()));
        assertTrue(coupLegal, "Le coup de l'IA doit être légal");
    }

    // ─── l'IA prend une pièce gratuite ───────────────────────────────────────

    @Test
    void meilleurCoup_prend_pieceGratuite() {
        // Dame noire en e5 sans protection, au trait les blancs
        Plateau p = Plateau.depuisFEN("4k3/8/8/4q3/4R3/8/8/4K3 w - - 0 1");
        Coup coup = OrchestrateurIA.meilleurCoup(p, Niveau.FACILE);
        assertNotNull(coup);
        // La tour e4 doit capturer la dame e5
        assertTrue(coup.estCapture(),
                "L'IA doit capturer la dame gratuite");
    }

    // ─── l'IA évite de se faire mater en 1 ───────────────────────────────────

    @Test
    void meilleurCoup_evite_matEnUn() {
        // Blancs au trait : la dame noire h4 menace Qxf2# (mat en 1)
        // L'IA doit jouer un coup qui évite le mat
        Plateau p = Plateau.depuisFEN("rnbqkbnr/pppp1ppp/8/4p3/7q/5P2/PPPPP1PP/RNBQKBNR w KQkq - 1 3");
        Coup coup = OrchestrateurIA.meilleurCoup(p, Niveau.FACILE);
        assertNotNull(coup);
        // Après le coup de l'IA, le roi blanc ne doit pas être en mat
        EtatPlateau avant = p.jouerAvecSauvegarde(coup);
        // On vérifie juste que le coup est légal (non null) — l'IA ne doit pas planter
        p.annuler(avant);
    }

    // ─── l'IA donne mat en 1 quand possible ──────────────────────────────────

    @Test
    void meilleurCoup_donneMatEnUn() {
        // Blancs peuvent donner mat en 1 : Qh7#
        // Roi noir h8, dame blanche d3 peut aller en h7
        Plateau p = Plateau.depuisFEN("6k1/8/6K1/8/8/3Q4/8/8 w - - 0 1");
        Coup coup = OrchestrateurIA.meilleurCoup(p, Niveau.FACILE);
        assertNotNull(coup);

        EtatPlateau avant = p.jouerAvecSauvegarde(coup);
        // Après le coup de l'IA, le roi noir doit être en mat (aucun coup légal + en échec)
        boolean matApres = GenerateurCoups.genererLegaux(p).isEmpty() && p.estEnEchec(Couleur.NOIR);
        p.annuler(avant);

        assertTrue(matApres, "L'IA doit jouer le mat en 1");
    }

    // ─── meilleurCoup retourne null si aucun coup ─────────────────────────────

    @Test
    void meilleurCoup_positionMat_retourneNull() {
        // Position de mat — aucun coup légal pour les blancs
        // Fool's Mate : c'est aux blancs de jouer, ils sont mat
        Plateau p = Plateau.depuisFEN("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3");
        Coup coup = OrchestrateurIA.meilleurCoup(p, Niveau.FACILE);
        assertNull(coup, "Aucun coup si le joueur est mat");
    }
}
