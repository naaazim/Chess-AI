package fr.moteurechecs.ia;

import fr.moteurechecs.plateau.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests pour la fonction d'évaluation statique.
 */
class EvaluationTest {

    // ─── position initiale : score nul ───────────────────────────────────────

    @Test
    void positionInitiale_scoreSymetrique() {
        Plateau p = Plateau.positionInitiale();
        int score = Evaluation.evaluer(p);
        // La position est symétrique → le score brut doit être proche de 0
        // (légères variations dues aux PST, on accepte ±100 centipawns)
        assertTrue(Math.abs(score) <= 100,
                "Score position initiale trop déséquilibré : " + score);
    }

    // ─── avantage matériel ───────────────────────────────────────────────────

    @Test
    void avancageMateriel_blancsAvecUneExtraTour() {
        // Blancs ont une tour de plus
        Plateau p = Plateau.depuisFEN("4k3/8/8/8/8/8/8/R3K3 w Q - 0 1");
        int score = Evaluation.evaluer(p);
        // Blanc au trait → score positif attendu (avantage matériel ~500 cp)
        assertTrue(score > 0, "Blancs avec une tour de plus doivent avoir un score positif");
    }

    @Test
    void avancageMateriel_noirsAvecUneExtraDame() {
        // Noirs ont une dame, blancs n'ont que le roi
        // evaluer() retourne toujours du point de vue des Blancs (positif = avantage Blancs)
        Plateau p = Plateau.depuisFEN("4k3/3q4/8/8/8/8/8/4K3 b - - 0 1");
        int score = Evaluation.evaluer(p);
        assertTrue(score < 0, "Noirs avec une dame de plus → score négatif du point de vue des Blancs");
    }

    // ─── fin de partie : KQK ─────────────────────────────────────────────────

    @Test
    void finale_KQK_scoreTresPositifPourBlancs() {
        // Roi blanc e1, dame blanche d1, roi noir e8
        Plateau p = Plateau.depuisFEN("4k3/8/8/8/8/8/8/3QK3 w - - 0 1");
        int score = Evaluation.evaluer(p);
        // Blancs écrasants → score positif très élevé
        assertTrue(score > 500, "KQK doit donner un score très favorable aux blancs");
    }
}
