package org.example.AI;

import org.example.AI.search.AlphaBeta;
import org.example.AI.search.MoveSorter;
import org.example.AI.search.TimeOutException;
import org.example.chess.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Recherche avec Iterative Deepening Search (IDS), et limite de temps.
 * Orchestre multithreading et délégue un thread de temps et le Minimax à
 * AlphaBeta.
 */
public final class RechercheMinimaxAlphaBeta {

    private static final int NUM_THREADS = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    private RechercheMinimaxAlphaBeta() {
    }

    public static Coup meilleurCoup(Plateau plateau, Niveau niveau, org.example.gui.ProfilerPartie profiler) {
        if (plateau == null)
            throw new IllegalArgumentException("plateau null");
        if (niveau == null)
            throw new IllegalArgumentException("niveau null");

        List<Coup> coups = GenerateurCoups.genererLegaux(plateau);
        if (coups.isEmpty())
            return null;

        long tempsMaxMs = switch (niveau) {
            case FACILE -> 1000L;
            case MOYEN -> 2500L;
            case DIFFICILE -> 5000L;
        };

        long startTime = System.currentTimeMillis();
        AtomicBoolean timeIsUp = new AtomicBoolean(false);

        boolean blancsJouent = (plateau.trait() == Couleur.BLANC);

        MoveSorter.trierCoups(coups, plateau);

        Coup meilleurGlobal = coups.get(0);
        int profondeurAtteinte = 1;

        ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();
        timer.schedule(() -> timeIsUp.set(true), tempsMaxMs, TimeUnit.MILLISECONDS);

        for (int depth = 1; depth <= 30; depth++) {
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
                "IA (" + niveau + ") a joué " + meilleurGlobal.depart().versAlgebrique() + "-"
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
