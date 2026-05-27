package fr.moteurechecs.ia;

import fr.moteurechecs.ia.evaluation.PhaseDeJeu;
import fr.moteurechecs.ia.recherche.TrieurDeCoups;
import fr.moteurechecs.plateau.Coup;
import fr.moteurechecs.plateau.Couleur;
import fr.moteurechecs.plateau.Plateau;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Moteur de recherche principal avec Iterative Deepening Search (IDS).
 *
 * Orchestre la boucle IDS : profondeur croissante de 1 jusqu'à {@code profMax},
 * fenêtres d'aspiration autour du score de l'itération précédente, et arrêt sur minuterie
 * via {@link GestionnaireTemps}. La recherche parallèle à chaque profondeur est déléguée
 * à {@link ParalleliseurCoups}.
 */
public final class RecherchePrincipale {

    private RecherchePrincipale() {}

    /**
     * Lance la recherche IDS et retourne le meilleur coup avec ses métadonnées.
     *
     * @param plateau          position à analyser
     * @param coups            liste des coups légaux (déjà générés)
     * @param niveau           niveau de difficulté
     * @param observateur      callback GUI pour les mises à jour intermédiaires (peut être {@code null})
     * @param historiquePartie tableau d'empreintes Zobrist des positions de la partie en cours,
     *                         transmis à la recherche pour détecter les répétitions
     * @param tailleHistorique nombre d'entrées valides dans {@code historiquePartie}
     * @return résultat contenant le meilleur coup et les informations de reporting
     */
    public static ResultatIDS rechercherAvecIDS(
            Plateau plateau, List<Coup> coups, Niveau niveau, Consumer<String> observateur,
            long[] historiquePartie, int tailleHistorique) {

        double phase          = PhaseDeJeu.calculerPhase(plateau);
        int    profMax        = calculerProfondeurMax(niveau, phase);
        String etiquettePhase = etiquetterPhase(phase);
        boolean blancsJouent  = (plateau.trait() == Couleur.BLANC);

        if (niveau.useMoveSorting()) {
            TrieurDeCoups.trierCoups(coups, plateau);
        }

        GestionnaireTemps minuteur = new GestionnaireTemps();
        minuteur.demarrer(determinerTempsMs(niveau));
        AtomicBoolean finTemps = minuteur.getSignal();
        long startTime = System.currentTimeMillis();

        Coup meilleurGlobal         = coups.get(0);
        int  profondeurAtteinte     = 1;
        int  meilleurScorePrecedent = Integer.MIN_VALUE;

        for (int profondeur = 1; profondeur <= profMax; profondeur++) {
            if (minuteur.estEcoule()) break;

            ParalleliseurCoups.ResultatRecherche res = chercherAvecAspiration(
                    plateau, coups, meilleurGlobal, profondeur,
                    meilleurScorePrecedent, blancsJouent, finTemps, observateur, niveau,
                    historiquePartie, tailleHistorique);

            if (!res.abandonne && res.meilleurCoup != null) {
                meilleurGlobal         = res.meilleurCoup;
                profondeurAtteinte     = profondeur;
                meilleurScorePrecedent = res.meilleurScore;
                if (Math.abs(res.meilleurScore) >= Evaluation.SCORE_MAT - 100) break;
            } else {
                break;
            }
        }

        minuteur.arreter();
        long tempsTotal = System.currentTimeMillis() - startTime;
        return new ResultatIDS(meilleurGlobal, profondeurAtteinte, profMax, etiquettePhase, tempsTotal);
    }

    // tente la recherche avec fenêtre d'aspiration, réessaie avec fenêtre complète si fail
    private static ParalleliseurCoups.ResultatRecherche chercherAvecAspiration(
            Plateau plateau, List<Coup> coups, Coup meilleurActuel,
            int profondeur, int scorePrecedent, boolean blancsJouent,
            AtomicBoolean finTemps, Consumer<String> observateur, Niveau niveau,
            long[] historiquePartie, int tailleHistorique) {

        final int delta = 50;
        int aspirAlpha, aspirBeta;

        if (niveau.useAlphaBeta() && profondeur >= 4 && scorePrecedent != Integer.MIN_VALUE) {
            aspirAlpha = scorePrecedent - delta;
            aspirBeta  = scorePrecedent + delta;
        } else {
            aspirAlpha = -Evaluation.SCORE_MAT;
            aspirBeta  =  Evaluation.SCORE_MAT;
        }

        ParalleliseurCoups.ResultatRecherche res = ParalleliseurCoups.rechercherAUneProfondeur(
                plateau, coups, meilleurActuel, profondeur,
                aspirAlpha, aspirBeta, blancsJouent, finTemps, observateur, niveau,
                historiquePartie, tailleHistorique);

        if (doitReessayerFenetre(res, aspirAlpha, aspirBeta, profondeur, scorePrecedent, finTemps, niveau)) {
            res = ParalleliseurCoups.rechercherAUneProfondeur(
                    plateau, coups, meilleurActuel, profondeur,
                    -Evaluation.SCORE_MAT, Evaluation.SCORE_MAT,
                    blancsJouent, finTemps, observateur, niveau,
                    historiquePartie, tailleHistorique);
        }
        return res;
    }

    // détermine si le résultat sort de la fenêtre d'aspiration et mérite une re-tentative
    private static boolean doitReessayerFenetre(
            ParalleliseurCoups.ResultatRecherche res,
            int aspirAlpha, int aspirBeta,
            int profondeur, int scorePrecedent,
            AtomicBoolean finTemps, Niveau niveau) {
        return !res.abandonne
                && res.meilleurCoup != null
                && niveau.useAlphaBeta()
                && profondeur >= 4
                && scorePrecedent != Integer.MIN_VALUE
                && (res.meilleurScore <= aspirAlpha || res.meilleurScore >= aspirBeta)
                && !finTemps.get();
    }

    private static long determinerTempsMs(Niveau niveau) {
        return switch (niveau) {
            case FACILE    -> 1000L;
            case MOYEN     -> 2500L;
            case DIFFICILE -> 5000L;
        };
    }

    private static int calculerProfondeurMax(Niveau niveau, double phase) {
        return switch (niveau) {
            case FACILE, MOYEN -> niveau.profondeur();
            case DIFFICILE -> {
                if (phase > 0.7)      yield 10;
                else if (phase > 0.3) yield 8;
                else                  yield 14;
            }
        };
    }

    private static String etiquetterPhase(double phase) {
        if (phase > 0.7) return "OUVERTURE";
        if (phase > 0.3) return "MILIEU";
        return "FINALE";
    }

    // ===== Classe résultat =====

    /** Résultat complet de la recherche IDS, retourné à l'orchestrateur. */
    public static final class ResultatIDS {
        public final Coup   meilleurCoup;
        public final int    profondeurAtteinte;
        public final int    profMax;
        public final String etiquettePhase;
        public final long   tempsTotal;

        ResultatIDS(Coup coup, int profAtteinte, int pMax, String phase, long temps) {
            this.meilleurCoup       = coup;
            this.profondeurAtteinte = profAtteinte;
            this.profMax            = pMax;
            this.etiquettePhase     = phase;
            this.tempsTotal         = temps;
        }
    }
}
