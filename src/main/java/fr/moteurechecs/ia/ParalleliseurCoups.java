package fr.moteurechecs.ia;

import fr.moteurechecs.ia.recherche.ExceptionTempsDepasse;
import fr.moteurechecs.ia.recherche.RecursionAlphaBeta;
import fr.moteurechecs.ia.recherche.TableDeTransposition;
import fr.moteurechecs.ia.recherche.TrieurDeCoups;
import fr.moteurechecs.plateau.Coup;
import fr.moteurechecs.plateau.Plateau;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Recherche parallèle à une profondeur fixée.
 *
 * Distribue l'évaluation de chaque coup racine sur un thread dédié du pool partagé.
 * Le pool est dimensionné selon le nombre de processeurs disponibles.
 */
public final class ParalleliseurCoups {

    private static final int NB_THREADS = Runtime.getRuntime().availableProcessors();

    private static final ExecutorService pool = Executors.newFixedThreadPool(NB_THREADS, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    private ParalleliseurCoups() {}

    /**
     * Évalue tous les coups racines en parallèle et retourne le meilleur résultat.
     *
     * @param plateau          position de départ
     * @param coups            liste des coups racines
     * @param meilleurActuel   coup de référence pour l'ordering (placé en tête)
     * @param profondeur       profondeur de recherche
     * @param alpha            borne alpha initiale
     * @param beta             borne bêta initiale
     * @param blancsJouent     {@code true} si c'est au tour des Blancs
     * @param finTemps         signal d'arrêt partagé entre les threads
     * @param observateur      callback GUI pour les évaluations intermédiaires (peut être {@code null})
     * @param niveau           niveau de difficulté (détermine le tri et la table de transposition)
     * @param historiquePartie tableau d'empreintes Zobrist des positions jouées depuis le début
     *                         de la partie (transmis à la recherche récursive pour détecter les répétitions)
     * @param tailleHistorique nombre d'entrées valides dans {@code historiquePartie}
     * @return résultat contenant le meilleur coup, son score, et un flag d'abandon
     */
    public static ResultatRecherche rechercherAUneProfondeur(
            Plateau plateau, List<Coup> coups, Coup meilleurActuel,
            int profondeur, int alpha, int beta,
            boolean blancsJouent, AtomicBoolean finTemps,
            Consumer<String> observateur, Niveau niveau,
            long[] historiquePartie, int tailleHistorique) {

        if (niveau.useMoveSorting()) {
            TrieurDeCoups.placerEnPremier(coups, meilleurActuel);
        }

        List<Future<EvaluationCoup>> taches = soumettreTaches(
                plateau, coups, profondeur, alpha, beta, finTemps, niveau,
                historiquePartie, tailleHistorique);

        return collecterResultats(taches, blancsJouent, observateur, profondeur, finTemps);
    }

    /**
     * Soumet une tâche par coup racine dans le pool.
     *
     * Chaque thread reçoit une copie indépendante du tableau d'historique.
     * L'empreinte Zobrist de la position obtenue après avoir joué le coup racine
     * est placée à l'indice {@code tailleHistorique} avant d'appeler la récursion,
     * afin que la détection de répétition fonctionne dès le premier niveau.
     */
    private static List<Future<EvaluationCoup>> soumettreTaches(
            Plateau plateau, List<Coup> coups,
            int profondeur, int alpha, int beta,
            AtomicBoolean finTemps, Niveau niveau,
            long[] historiquePartie, int tailleHistorique) {

        List<Future<EvaluationCoup>> taches = new ArrayList<>(coups.size());
        for (Coup coup : coups) {
            Callable<EvaluationCoup> tache = () -> {
                Plateau copie = plateau.copie();
                copie.jouerAvecSauvegarde(coup);

                // Copie isolée du tableau d'historique pour ce thread (taille suffisante
                // pour l'historique de partie + la profondeur de recherche maximale)
                long[] pileThread = Arrays.copyOf(historiquePartie, tailleHistorique + 128);
                pileThread[tailleHistorique] = TableDeTransposition.hash(copie);

                int eval = RecursionAlphaBeta.minimax(copie, profondeur - 1, alpha, beta,
                        finTemps, niveau, pileThread, tailleHistorique + 1);
                return new EvaluationCoup(coup, eval);
            };
            taches.add(pool.submit(tache));
        }
        return taches;
    }

    // collecte les résultats des futures et sélectionne le meilleur
    private static ResultatRecherche collecterResultats(
            List<Future<EvaluationCoup>> taches, boolean blancsJouent,
            Consumer<String> observateur, int profondeur, AtomicBoolean finTemps) {

        int     meilleurScore = blancsJouent ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        Coup    meilleurCoup  = null;
        boolean abandonne     = false;

        for (Future<EvaluationCoup> tache : taches) {
            try {
                EvaluationCoup res = tache.get();
                notifierObservateur(observateur, res, profondeur);
                if (blancsJouent) {
                    if (res.score > meilleurScore) { meilleurScore = res.score; meilleurCoup = res.coup; }
                } else {
                    if (res.score < meilleurScore) { meilleurScore = res.score; meilleurCoup = res.coup; }
                }
            } catch (ExecutionException e) {
                if (e.getCause() instanceof ExceptionTempsDepasse) abandonne = true;
            } catch (InterruptedException e) {
                abandonne = true;
            }
        }

        annulerTachesRestantes(taches);
        if (finTemps.get()) abandonne = true;
        return new ResultatRecherche(meilleurCoup, meilleurScore, abandonne);
    }

    private static void notifierObservateur(Consumer<String> observateur,
            EvaluationCoup res, int profondeur) {
        if (observateur == null) return;
        String info = String.format("Depth %d: %s-%s (Score: %d)",
                profondeur,
                res.coup.depart().versAlgebrique(),
                res.coup.arrivee().versAlgebrique(),
                res.score);
        observateur.accept(info);
    }

    private static void annulerTachesRestantes(List<Future<EvaluationCoup>> taches) {
        for (Future<EvaluationCoup> tache : taches) {
            tache.cancel(true);
        }
    }

    // ===== Classes internes =====

    /** Résultat d'une itération de recherche à une profondeur donnée. */
    public static final class ResultatRecherche {
        public final Coup    meilleurCoup;
        public final int     meilleurScore;
        public final boolean abandonne;

        public ResultatRecherche(Coup coup, int score, boolean abandonne) {
            this.meilleurCoup  = coup;
            this.meilleurScore = score;
            this.abandonne     = abandonne;
        }
    }

    /** Association entre un coup racine et son score minimax évalué. */
    static final class EvaluationCoup {
        final Coup coup;
        final int  score;

        EvaluationCoup(Coup coup, int score) {
            this.coup  = coup;
            this.score = score;
        }
    }
}
