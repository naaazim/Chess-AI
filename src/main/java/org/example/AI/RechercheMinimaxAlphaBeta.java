package org.example.AI;

import org.example.AI.search.AlphaBeta;
import org.example.AI.search.MoveSorter;
import org.example.AI.search.TimeOutException;
import org.example.chess.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Recherche avec Iterative Deepening Search (IDS), et limite de temps.
 * Orchestre multithreading et delegue un thread de temps et le Minimax a
 * AlphaBeta.
 */
public final class RechercheMinimaxAlphaBeta {

    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    // Livre d'ouvertures (lazy loaded)
    private static volatile OpeningBook openingBook;

    // Nom de la derniere ouverture trouvee (pour GUI)
    private static volatile String currentOpeningName = "Unknown";

    // Compteur de demi-coups joues
    private static volatile int currentPly = 0;

    private RechercheMinimaxAlphaBeta() {
    }

    /**
     * Retourne le nom de l'ouverture courante (pour la GUI).
     */
    public static String getCurrentOpeningName() {
        return currentOpeningName;
    }

    /**
     * Met a jour le compteur de demi-coups.
     * Appele par le controleur apres chaque coup.
     */
    public static void incrementPly() {
        // Le ply commence a 0 (avant le premier coup)
        // Apres chaque coup, on incremente
        currentPly++;
    }

    /**
     * Remet le compteur a zero (nouvelle partie).
     */
    public static void resetPly() {
        // Le ply commence a 0 au debut de la partie
        currentPly = 0;
        currentOpeningName = "Unknown";
    }

    /**
     * Verifie si le coup joué correspond au livre d'ouvertures.
     */
    public static void verifierOuverture(Plateau plateauAvant, List<Coup> legauxAvant, Coup coupJoue) {
        OpeningBook book = OpeningBook.getInstance();
        if (book != null && book.isLoaded() && currentPly <= 12) {
            Coup expected = book.lookup(plateauAvant, legauxAvant, currentPly);
            if (expected != null && expected.depart() == coupJoue.depart()
                    && expected.arrivee() == coupJoue.arrivee()) {
                if (book.getLastOpeningName() != null) {
                    currentOpeningName = book.getLastOpeningName();
                }
            }
        }
    }

    /**
     * Recharge le livre d'ouvertures.
     */
    public static void reloadOpeningBook(String path) {
        synchronized (RechercheMinimaxAlphaBeta.class) {
            openingBook = OpeningBook.load(path);
        }
    }

    public static Coup meilleurCoup(Plateau plateau, Niveau niveau, org.example.gui.ProfilerPartie profiler) {
        return meilleurCoup(plateau, niveau, profiler, null);
    }

    public static Coup meilleurCoup(Plateau plateau, Niveau niveau, org.example.gui.ProfilerPartie profiler,
            Consumer<String> observateur) {
        if (plateau == null)
            throw new IllegalArgumentException("plateau null");
        if (niveau == null)
            throw new IllegalArgumentException("niveau null");

        List<Coup> coups = GenerateurCoups.genererLegaux(plateau);
        if (coups.isEmpty())
            return null;

        // ========== OPENING BOOK LOOKUP ==========
        OpeningBook book = OpeningBook.getInstance();

        if (book != null && book.isLoaded() && currentPly <= 12) {
            Coup bookCoup = book.lookup(plateau, coups, currentPly);
            if (bookCoup != null) {
                // Mise a jour du nom de l'ouverture pour la GUI
                if (book.getLastOpeningName() != null) {
                    currentOpeningName = book.getLastOpeningName();
                }
                System.out.println("[IA] Opening book move: " + bookCoup);
                return bookCoup;
            }
        }
        // ========== END OPENING BOOK ==========

        long tempsMaxMs = switch (niveau) {
            case FACILE -> 1000L;
            case MOYEN -> 2500L;
            case DIFFICILE -> 5000L;
        };

        int maxDepth = switch (niveau) {
            case FACILE -> 4;
            case MOYEN -> 6;
            case DIFFICILE -> 30;
        };

        long startTime = System.currentTimeMillis();
        AtomicBoolean timeIsUp = new AtomicBoolean(false);

        boolean blancsJouent = (plateau.trait() == Couleur.BLANC);

        MoveSorter.trierCoups(coups, plateau);

        Coup meilleurGlobal = coups.get(0);
        int profondeurAtteinte = 1;

        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
        timer.schedule(() -> timeIsUp.set(true), tempsMaxMs, TimeUnit.MILLISECONDS);

        for (int depth = 1; depth <= maxDepth; depth++) {
            if (timeIsUp.get())
                break;

            try {
                MoveSorter.placerEnPremier(coups, meilleurGlobal);

                List<Future<MoveScore>> futures = new ArrayList<>();
                final int currentDepth = depth;
                for (Coup coup : coups) {
                    Callable<MoveScore> task = () -> {
                        Plateau copie = plateau.copie();
                        copie.jouerAvecSauvegarde(coup);
                        int alpha = -Evaluation.SCORE_MAT;
                        int beta = Evaluation.SCORE_MAT;
                        int eval = AlphaBeta.minimax(copie, currentDepth - 1, alpha, beta, timeIsUp);
                        return new MoveScore(coup, eval);
                    };
                    futures.add(pool.submit(task));
                }

                int bestScoreIter = blancsJouent ? Integer.MIN_VALUE : Integer.MAX_VALUE;
                Coup meilleurIter = null;
                boolean searchAborted = false;

                for (Future<MoveScore> future : futures) {
                    try {
                        MoveScore res = future.get();

                        if (observateur != null) {
                            String info = String.format("Prof %d: %s-%s (Score: %d)",
                                    currentDepth,
                                    res.coup.depart().versAlgebrique(),
                                    res.coup.arrivee().versAlgebrique(),
                                    res.score);
                            observateur.accept(info);
                        }

                        if (blancsJouent) {
                            if (res.score > bestScoreIter) {
                                bestScoreIter = res.score;
                                meilleurIter = res.coup;
                            }
                        } else {
                            if (res.score < bestScoreIter) {
                                bestScoreIter = res.score;
                                meilleurIter = res.coup;
                            }
                        }
                    } catch (ExecutionException e) {
                        if (e.getCause() instanceof TimeOutException) {
                            searchAborted = true;
                        }
                    } catch (InterruptedException e) {
                        searchAborted = true;
                    }
                }

                for (Future<MoveScore> future : futures) {
                    future.cancel(true);
                }

                if (!searchAborted && !timeIsUp.get() && meilleurIter != null) {
                    meilleurGlobal = meilleurIter;
                    profondeurAtteinte = depth;

                    if (Math.abs(bestScoreIter) >= Evaluation.SCORE_MAT - 100) {
                        break;
                    }
                } else {
                    break;
                }

            } catch (Exception e) {
                break;
            }
        }

        timer.shutdownNow();

        long tempsTotal = System.currentTimeMillis() - startTime;
        if (profiler != null) {
            profiler.enregistrerCoup(blancsJouent, tempsTotal, profondeurAtteinte);
        }

        System.out.println(
                "IA (" + niveau + ") a joue " + meilleurGlobal.depart().versAlgebrique() + "-"
                        + meilleurGlobal.arrivee().versAlgebrique()
                        + " (Prof: " + profondeurAtteinte + ", Temps: " + tempsTotal + "ms)");

        return meilleurGlobal;
    }

    public static Coup meilleurCoup(Plateau plateau, Niveau niveau) {
        return meilleurCoup(plateau, niveau, null);
    }

    private static class MoveScore {
        Coup coup;
        int score;

        MoveScore(Coup c, int s) {
            coup = c;
            score = s;
        }
    }
}
