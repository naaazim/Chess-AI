package fr.moteurechecs.tournoi;

import fr.moteurechecs.plateau.*;
import fr.moteurechecs.ia.Niveau;
import fr.moteurechecs.ia.OrchestrateurIA;
import fr.moteurechecs.tournoi.LecteurSauvegarde.BilanPartiel;
import fr.moteurechecs.tournoi.LecteurSauvegarde.EtatTournoi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tournoi automatique entre toutes les paires de niveaux IA (sans doublons miroirs).
 *
 * Les 6 confrontations couvrent les paires uniques de niveaux : FACILE-FACILE,
 * FACILE-MOYEN, FACILE-DIFFICILE, MOYEN-MOYEN, MOYEN-DIFFICILE, DIFFICILE-DIFFICILE.
 *
 * Pour chaque confrontation, les couleurs alternent : les {@value #MOITIE_PAR_MATCHUP}
 * premières parties voient A jouer les Blancs, les {@value #MOITIE_PAR_MATCHUP} suivantes
 * voient B jouer les Blancs. Cela élimine le biais de couleur.
 *
 * Sauvegarde incrémentale : chaque partie est écrite immédiatement sur disque.
 * En cas d'interruption, relancer le programme reprend automatiquement là où il s'est arrêté.
 *
 * Mode couple unique : passer deux noms de niveau en arguments CLI pour ne jouer
 * qu'un seul matchup (permet plusieurs terminaux en parallèle).
 *
 * Lancer avec : {@code mvn exec:java -Dexec.mainClass=fr.moteurechecs.tournoi.Tournoi}
 */
public final class Tournoi {

    // ─── Paramètres ──────────────────────────────────────────────────────────

    /** Nombre total de parties par confrontation (doit être pair pour l'alternance). */
    static final int PARTIES_PAR_MATCHUP = 50;

    /** Moitié des parties par confrontation (alternance de couleurs). */
    static final int MOITIE_PAR_MATCHUP = PARTIES_PAR_MATCHUP / 2;

    /** Nombre maximum de demi-coups par partie avant déclaration nulle. */
    static final int MAX_PLIES = 400;

    /** Règle des 50 coups : 100 demi-coups sans capture ni mouvement de pion = nul. */
    private static final int REGLE_50_COUPS_PLIES = 100;

    /** Fichier de sortie des résultats (mode tournoi complet). */
    static final String FICHIER_RESULTATS = "resultats_tournoi.txt";

    /** Toutes les paires de niveaux sans doublon miroir (triangle supérieur + diagonale). */
    static final List<Niveau[]> MATCHUPS = genererMatchups();

    // ─── Entrée principale ────────────────────────────────────────────────────

    public static void main(String[] args) throws IOException {

        // ── Déterminer le mode (couple unique ou tournoi complet) ─────────────
        List<Niveau[]> matchupsAJouer;
        String fichierResultats;
        boolean modeUnique = false;

        if (args.length == 2) {
            Niveau a = null;
            Niveau b = null;
            try {
                a = Niveau.valueOf(args[0].toUpperCase());
                b = Niveau.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // arguments invalides → mode tournoi complet
            }
            if (a != null && b != null) {
                // Normaliser l'ordre (triangle supérieur)
                if (a.ordinal() > b.ordinal()) {
                    Niveau tmp = a; a = b; b = tmp;
                }
                fichierResultats = "resultats_" + a.name() + "_" + b.name() + ".txt";
                matchupsAJouer   = Collections.singletonList(new Niveau[]{a, b});
                modeUnique       = true;
                System.out.println("Mode couple unique : " + a + " vs " + b
                        + " → " + fichierResultats);
            } else {
                fichierResultats = FICHIER_RESULTATS;
                matchupsAJouer   = MATCHUPS;
                System.out.println("Mode tournoi complet → " + FICHIER_RESULTATS);
            }
        } else {
            fichierResultats = FICHIER_RESULTATS;
            matchupsAJouer   = MATCHUPS;
            System.out.println("Mode tournoi complet → " + FICHIER_RESULTATS);
        }

        // ── Lire l'état de sauvegarde existant ────────────────────────────────
        EtatTournoi etat = LecteurSauvegarde.lire(fichierResultats);
        afficherResumReprise(etat);

        int totalMatchups = matchupsAJouer.size();
        int totalParties  = totalMatchups * PARTIES_PAR_MATCHUP;

        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║      TOURNOI DEEP BLACK — Démarrage              ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
        System.out.printf("%nParties par confrontation : %d (×%d Blancs + ×%d Noirs)%n",
                PARTIES_PAR_MATCHUP, MOITIE_PAR_MATCHUP, MOITIE_PAR_MATCHUP);
        System.out.printf("Confrontations totales    : %d%n", totalMatchups);
        System.out.printf("Parties totales estimées  : %d%n%n", totalParties);

        List<ResultatMatchup> resultats = new ArrayList<>();

        try (SauvegardeTournoi sauvegarde = new SauvegardeTournoi(fichierResultats)) {

            for (int i = 0; i < matchupsAJouer.size(); i++) {
                Niveau niveauA = matchupsAJouer.get(i)[0];
                Niveau niveauB = matchupsAJouer.get(i)[1];
                String cle     = niveauA.name() + "-" + niveauB.name();

                // a) Matchup déjà entièrement terminé ?
                if (etat.matchupsTermines.contains(cle)) {
                    System.out.printf("  [DÉJÀ JOUÉ] %s vs %s — ignoré.%n", niveauA, niveauB);
                    // Reconstituer le ResultatMatchup depuis le bilan partiel pour le rapport
                    BilanPartiel bp = etat.bilanPartiels.get(cle);
                    if (bp != null) {
                        resultats.add(new ResultatMatchup(niveauA, niveauB,
                                bp.victoiresA, bp.victoiresB,
                                bp.victoiresBlanc, bp.victoiresNoir, bp.nuls));
                    }
                    continue;
                }

                System.out.printf("──── Matchup %d/%d : %s vs %s ────%n",
                        i + 1, totalMatchups, niveauA, niveauB);

                // b) Reprise partielle ou c) démarrage normal
                BilanPartiel bilanInitial = etat.bilanPartiels.get(cle);
                int partieDepart = (bilanInitial != null) ? bilanInitial.dernierePartie + 1 : 1;

                if (bilanInitial != null) {
                    System.out.printf("  → Reprise à partir de la partie %d (déjà jouées : %d)%n",
                            partieDepart, bilanInitial.dernierePartie);
                }

                ResultatMatchup resultat = jouerMatchup(
                        niveauA, niveauB, i + 1, totalMatchups,
                        partieDepart, bilanInitial, sauvegarde);
                resultats.add(resultat);

                System.out.printf("  → %s : %d | Nuls : %d | %s : %d%n%n",
                        niveauA, resultat.victoiresA, resultat.nuls, niveauB, resultat.victoiresB);
            }
        } // sauvegarde.close() automatique

        // En mode tournoi complet, écrire le rapport final consolidé
        if (!modeUnique) {
            ecrireFichierResultats(resultats);
            System.out.println("╔══════════════════════════════════════════════════╗");
            System.out.println("║  Tournoi terminé ! Résultats → " + FICHIER_RESULTATS + "  ║");
            System.out.println("╚══════════════════════════════════════════════════╝");
        } else {
            System.out.println("╔══════════════════════════════════════════════════╗");
            System.out.println("║  Matchup terminé ! Sauvegarde → " + fichierResultats + "  ║");
            System.out.println("╚══════════════════════════════════════════════════╝");
        }
    }

    // ─── Affichage du résumé de reprise ──────────────────────────────────────

    private static void afficherResumReprise(EtatTournoi etat) {
        if (etat.estVide()) {
            System.out.println("Démarrage à zéro.");
            return;
        }
        int nbTermines = etat.matchupsTermines.size();
        BilanPartiel enCours = etat.bilanPartiels.entrySet().stream()
                .filter(e -> !etat.matchupsTermines.contains(e.getKey()))
                .map(e -> e.getValue())
                .findFirst()
                .orElse(null);
        if (enCours != null) {
            System.out.printf("Reprise détectée : %d matchup(s) terminé(s), "
                    + "1 matchup en cours à la partie %d.%n",
                    nbTermines, enCours.dernierePartie);
        } else {
            System.out.printf("Reprise détectée : %d matchup(s) terminé(s).%n", nbTermines);
        }
    }

    // ─── Génération des paires de niveaux ─────────────────────────────────────

    /** Génère toutes les paires uniques (sans doublons miroirs) de niveaux disponibles. */
    private static List<Niveau[]> genererMatchups() {
        Niveau[] niveaux = Niveau.values();
        List<Niveau[]> liste = new ArrayList<>();
        for (int i = 0; i < niveaux.length; i++) {
            for (int j = i; j < niveaux.length; j++) {
                liste.add(new Niveau[]{niveaux[i], niveaux[j]});
            }
        }
        return Collections.unmodifiableList(liste);
    }

    // ─── Logique de matchup ───────────────────────────────────────────────────

    /**
     * Joue {@value #PARTIES_PAR_MATCHUP} parties entre deux niveaux avec alternance de couleurs.
     *
     * Parties 1..{@value #MOITIE_PAR_MATCHUP} : A = Blancs, B = Noirs.<br>
     * Parties suivantes : B = Blancs, A = Noirs.
     *
     * @param partieDepart   numéro de la première partie à jouer (1 pour un démarrage normal,
     *                       {@code bilanInitial.dernierePartie + 1} pour une reprise)
     * @param bilanInitial   compteurs déjà accumulés lors d'une exécution précédente, ou {@code null}
     * @param sauvegarde     gestionnaire d'écriture incrémentale sur disque
     * @return bilan complet de la confrontation (incluant les parties antérieures si reprise)
     */
    private static ResultatMatchup jouerMatchup(Niveau niveauA, Niveau niveauB,
                                                int matchupIdx, int totalMatchups,
                                                int partieDepart,
                                                BilanPartiel bilanInitial,
                                                SauvegardeTournoi sauvegarde)
            throws IOException {

        // Initialiser les compteurs (depuis le bilan partiel ou à zéro)
        int victoiresA     = (bilanInitial != null) ? bilanInitial.victoiresA     : 0;
        int victoiresB     = (bilanInitial != null) ? bilanInitial.victoiresB     : 0;
        int victoiresBlanc = (bilanInitial != null) ? bilanInitial.victoiresBlanc : 0;
        int victoiresNoir  = (bilanInitial != null) ? bilanInitial.victoiresNoir  : 0;
        int nuls           = (bilanInitial != null) ? bilanInitial.nuls           : 0;

        for (int i = partieDepart; i <= PARTIES_PAR_MATCHUP; i++) {
            // première moitié : A = Blancs; deuxième moitié : B = Blancs
            boolean premiereMoitie = (i <= MOITIE_PAR_MATCHUP);
            Niveau blanc = premiereMoitie ? niveauA : niveauB;
            Niveau noir  = premiereMoitie ? niveauB : niveauA;

            System.out.printf("  ── Partie %d/%d ── %s (Blancs) vs %s (Noirs)%n",
                    i, PARTIES_PAR_MATCHUP, blanc, noir);

            Resultat r = jouerUnePartie(blanc, noir);

            switch (r) {
                case BLANC_GAGNE -> {
                    victoiresBlanc++;
                    if (premiereMoitie) victoiresA++; else victoiresB++;
                }
                case NOIR_GAGNE -> {
                    victoiresNoir++;
                    if (premiereMoitie) victoiresB++; else victoiresA++;
                }
                case NUL -> nuls++;
            }

            // Sauvegarde incrémentale immédiate
            sauvegarde.enregistrerPartie(niveauA, niveauB, i, blanc, noir, r);

            String annonce = switch (r) {
                case BLANC_GAGNE -> "  → Vainqueur : " + blanc + " (Blancs)";
                case NOIR_GAGNE  -> "  → Vainqueur : " + noir  + " (Noirs)";
                case NUL         -> "  → Résultat  : Nul";
            };
            System.out.println(annonce);
            System.out.printf("     Bilan running — %s:%d  %s:%d  =%d%n%n",
                    niveauA, victoiresA, niveauB, victoiresB, nuls);
        }

        // Marquer le matchup comme entièrement terminé
        sauvegarde.marquerMatchupTermine(niveauA, niveauB);

        return new ResultatMatchup(niveauA, niveauB,
                victoiresA, victoiresB,
                victoiresBlanc, victoiresNoir, nuls);
    }

    // ─── Simulation d'une partie ──────────────────────────────────────────────

    /**
     * Joue une partie complète entre deux niveaux IA et retourne le résultat.
     *
     * Conditions de fin gérées : mat, pat, triple répétition,
     * règle des 50 coups, dépassement du nombre max de plies.
     */
    static Resultat jouerUnePartie(Niveau niveauBlanc, Niveau niveauNoir) {
        Plateau plateau = Plateau.positionInitiale();
        OrchestrateurIA.resetPly();

        List<EtatPlateau> historiqueEtats = new ArrayList<>();
        historiqueEtats.add(plateau.sauvegarderEtat());

        int pliesSansProgression = 0;

        for (int ply = 0; ply < MAX_PLIES; ply++) {

            List<Coup> coupsLegaux = GenerateurCoups.genererLegaux(plateau);
            if (coupsLegaux.isEmpty()) {
                if (plateau.estEnEchec(plateau.trait())) {
                    return (plateau.trait() == Couleur.BLANC) ? Resultat.NOIR_GAGNE : Resultat.BLANC_GAGNE;
                }
                return Resultat.NUL; // pat
            }

            Niveau niveauCourant = (plateau.trait() == Couleur.BLANC) ? niveauBlanc : niveauNoir;
            Coup meilleur = OrchestrateurIA.meilleurCoup(plateau, niveauCourant);
            if (meilleur == null) {
                return Resultat.NUL;
            }

            boolean estCapture = meilleur.estCapture();
            boolean estPion    = meilleur.pieceDeplacee() == Piece.PION_BLANC
                               || meilleur.pieceDeplacee() == Piece.PION_NOIR;
            if (estCapture || estPion) {
                pliesSansProgression = 0;
            } else {
                pliesSansProgression++;
            }
            if (pliesSansProgression >= REGLE_50_COUPS_PLIES) {
                return Resultat.NUL;
            }

            plateau.jouer(meilleur);
            OrchestrateurIA.enregistrerPosition(plateau);
            OrchestrateurIA.incrementPly();

            EtatPlateau etatCourant = plateau.sauvegarderEtat();
            if (Collections.frequency(historiqueEtats, etatCourant) >= 2) {
                return Resultat.NUL; // triple répétition
            }
            historiqueEtats.add(etatCourant);
        }

        return Resultat.NUL;
    }

    // ─── Écriture des résultats ───────────────────────────────────────────────

    static void ecrireFichierResultats(List<ResultatMatchup> resultats) throws IOException {
        File fichier = new File(FICHIER_RESULTATS);
        try (PrintWriter out = new PrintWriter(new FileWriter(fichier))) {
            ecrireRapportDans(out, resultats);
        }
        System.out.println("Résultats sauvegardés dans : " + fichier.getAbsolutePath());
    }

    /**
     * Écrit le rapport complet dans le PrintWriter fourni.
     * Utilisé à la fois par le tournoi normal et par {@link RapportTournoi}.
     */
    static void ecrireRapportDans(PrintWriter out, List<ResultatMatchup> resultats) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        // ── En-tête ──────────────────────────────────────────────────
        out.println("╔══════════════════════════════════════════════════════════════╗");
        out.println("║         TOURNOI DEEP BLACK — Résultats complets              ║");
        out.println("╚══════════════════════════════════════════════════════════════╝");
        out.println("Date               : " + date);
        out.printf ("Parties/matchup    : %d (%d en tant que Blancs + %d en tant que Noirs)%n",
                PARTIES_PAR_MATCHUP, MOITIE_PAR_MATCHUP, MOITIE_PAR_MATCHUP);
        out.printf ("Max plies/partie   : %d%n", MAX_PLIES);
        out.printf ("Confrontations     : %d (paires uniques, doublons miroirs éliminés)%n",
                MATCHUPS.size());
        out.println();

        // ── Table des confrontations ──────────────────────────────────
        ecrireTableConfront(out, resultats);

        // ── Résumé par niveau (confrontations mixtes uniquement) ──────
        ecrireResumePorNiveau(out, resultats);

        // ── Avantage couleur (toutes confrontations) ──────────────────
        ecrireAvantageCouleur(out, resultats);

        // ── Détail confrontation par confrontation ────────────────────
        ecrireDetailConfront(out, resultats);
    }

    // écrit le tableau récapitulatif des confrontations
    private static void ecrireTableConfront(PrintWriter out, List<ResultatMatchup> resultats) {
        Niveau[] niveaux = Niveau.values();
        int colWidth   = 22;
        int labelWidth = 12;

        out.println("═══════════════════════════════════════════════════════════════");
        out.println(" TABLEAU DES CONFRONTATIONS");
        out.println(" Format : A=victoires_A  B=victoires_B  ==nuls");
        out.println(" Lignes = niveau A (joue Blancs en 1ère moitié)");
        out.println(" Colonnes = niveau B (joue Blancs en 2ème moitié)");
        out.println("═══════════════════════════════════════════════════════════════");
        out.println();

        String sep = "+" + "-".repeat(labelWidth) + "+"
                + ("-".repeat(colWidth) + "+").repeat(niveaux.length);

        out.println(sep);

        // en-tête colonnes
        StringBuilder entete = new StringBuilder();
        entete.append(String.format("|%-" + labelWidth + "s|", "A \\ B"));
        for (Niveau n : niveaux) {
            entete.append(String.format("%-" + colWidth + "s|", centrer(n.name(), colWidth)));
        }
        out.println(entete);
        out.println(sep);

        // lignes (une par niveau A)
        for (Niveau a : niveaux) {
            StringBuilder ligne = new StringBuilder();
            ligne.append(String.format("|%-" + labelWidth + "s|", a.name()));
            for (Niveau b : niveaux) {
                ResultatMatchup r = trouverMatchup(resultats, a, b);
                String cellule;
                if (r == null) {
                    // doublon miroir éliminé
                    cellule = "—";
                } else {
                    cellule = String.format("A=%d  B=%d  =%d", r.victoiresA, r.victoiresB, r.nuls);
                }
                ligne.append(String.format("%-" + colWidth + "s|", centrer(cellule, colWidth)));
            }
            out.println(ligne);
            out.println(sep);
        }
        out.println();
    }

    // écrit le résumé par niveau (confrontations mixtes uniquement)
    private static void ecrireResumePorNiveau(PrintWriter out, List<ResultatMatchup> resultats) {
        Niveau[] niveaux = Niveau.values();

        out.println("═══════════════════════════════════════════════════════════════");
        out.println(" RÉSUMÉ PAR NIVEAU (confrontations mixtes uniquement)");
        out.println("═══════════════════════════════════════════════════════════════");
        out.println();
        out.printf("%-12s %8s %8s %8s %10s%n", "Niveau", "Victoires", "Défaites", "Nuls", "Score %");
        out.println("-".repeat(52));

        for (Niveau n : niveaux) {
            int victoires    = 0;
            int defaites     = 0;
            int nulesNiv     = 0;

            for (ResultatMatchup r : resultats) {
                boolean estA = (r.niveauA == n);
                boolean estB = (r.niveauB == n);
                boolean confMixte = (r.niveauA != r.niveauB);

                if (!confMixte) continue; // ignorer les auto-confrontations

                if (estA) {
                    victoires += r.victoiresA;
                    defaites  += r.victoiresB;
                    nulesNiv  += r.nuls;
                } else if (estB) {
                    victoires += r.victoiresB;
                    defaites  += r.victoiresA;
                    nulesNiv  += r.nuls;
                }
            }

            int total = victoires + defaites + nulesNiv;
            double points = victoires + nulesNiv * 0.5;
            double pct    = (total > 0) ? (points / total * 100.0) : 0.0;

            out.printf("%-12s %8d %8d %8d %9.1f%%%n",
                    n.name(), victoires, defaites, nulesNiv, pct);
        }
        out.println();
    }

    // écrit la section avantage couleur globale
    private static void ecrireAvantageCouleur(PrintWriter out, List<ResultatMatchup> resultats) {
        int totalBlanc = 0;
        int totalNoir  = 0;
        int totalNuls  = 0;

        for (ResultatMatchup r : resultats) {
            totalBlanc += r.victoiresBlanc;
            totalNoir  += r.victoiresNoir;
            totalNuls  += r.nuls;
        }

        int totalParties = totalBlanc + totalNoir + totalNuls;

        out.println("═══════════════════════════════════════════════════════════════");
        out.println(" AVANTAGE COULEUR (toutes confrontations, toutes moitiés)");
        out.println("═══════════════════════════════════════════════════════════════");
        out.println();
        out.printf("  Victoires Blancs : %4d  (%5.1f%%)%n",
                totalBlanc, 100.0 * totalBlanc / Math.max(1, totalParties));
        out.printf("  Victoires Noirs  : %4d  (%5.1f%%)%n",
                totalNoir,  100.0 * totalNoir  / Math.max(1, totalParties));
        out.printf("  Nuls             : %4d  (%5.1f%%)%n",
                totalNuls,  100.0 * totalNuls  / Math.max(1, totalParties));
        out.printf("  Total parties    : %4d%n", totalParties);
        out.println();
    }

    // écrit le détail de chaque confrontation
    private static void ecrireDetailConfront(PrintWriter out, List<ResultatMatchup> resultats) {
        out.println("═══════════════════════════════════════════════════════════════");
        out.println(" DÉTAIL CONFRONTATION PAR CONFRONTATION");
        out.println(" (1ère moitié = A joue Blancs ; 2ème moitié = B joue Blancs)");
        out.println("═══════════════════════════════════════════════════════════════");
        out.println();

        for (ResultatMatchup r : resultats) {
            String libelle = r.niveauA == r.niveauB
                    ? r.niveauA + " (auto-confrontation)"
                    : r.niveauA + " (A) vs " + r.niveauB + " (B)";

            out.printf("%-44s — %d parties%n", libelle, r.total());
            out.println("  ─ Par niveau ─");
            out.printf("  Victoires %-10s (A) : %3d  (%5.1f%%)%n",
                    r.niveauA, r.victoiresA, 100.0 * r.victoiresA / r.total());
            out.printf("  Victoires %-10s (B) : %3d  (%5.1f%%)%n",
                    r.niveauB, r.victoiresB, 100.0 * r.victoiresB / r.total());
            out.printf("  Nuls                       : %3d  (%5.1f%%)%n",
                    r.nuls, 100.0 * r.nuls / r.total());
            out.println("  ─ Par couleur ─");
            out.printf("  Victoires Blancs           : %3d  (%5.1f%%)%n",
                    r.victoiresBlanc, 100.0 * r.victoiresBlanc / r.total());
            out.printf("  Victoires Noirs            : %3d  (%5.1f%%)%n",
                    r.victoiresNoir,  100.0 * r.victoiresNoir  / r.total());
            out.println();
        }
    }

    // ─── Utilitaires ──────────────────────────────────────────────────────────

    /**
     * Cherche un matchup dans la liste. Comme les paires sont stockées
     * dans l'ordre (A ≤ B en ordinal), on cherche dans les deux sens.
     */
    static ResultatMatchup trouverMatchup(List<ResultatMatchup> liste, Niveau a, Niveau b) {
        for (ResultatMatchup r : liste) {
            if (r.niveauA == a && r.niveauB == b) return r;
        }
        return null; // paire miroir non jouée (triangle inférieur)
    }

    /** Centre un texte dans un champ de largeur donnée. */
    static String centrer(String texte, int largeur) {
        if (texte.length() >= largeur) return texte.substring(0, largeur);
        int padding = largeur - texte.length();
        int gauche  = padding / 2;
        int droite  = padding - gauche;
        return " ".repeat(gauche) + texte + " ".repeat(droite);
    }

    // ─── Enums et records ─────────────────────────────────────────────────────

    /** Résultat d'une partie. */
    enum Resultat {
        BLANC_GAGNE,
        NOIR_GAGNE,
        NUL
    }

    /**
     * Bilan d'une confrontation entre niveauA et niveauB sur {@value #PARTIES_PAR_MATCHUP} parties.
     *
     * - {@code victoiresA} / {@code victoiresB} : victoires par niveau (indépendamment de la couleur)
     * - {@code victoiresBlanc} / {@code victoiresNoir} : victoires par couleur (indépendamment du niveau)
     */
    static final class ResultatMatchup {
        final Niveau niveauA;
        final Niveau niveauB;
        /** Victoires du niveau A (quelle que soit sa couleur). */
        final int victoiresA;
        /** Victoires du niveau B (quelle que soit sa couleur). */
        final int victoiresB;
        /** Victoires des Blancs (quel que soit le niveau). */
        final int victoiresBlanc;
        /** Victoires des Noirs (quel que soit le niveau). */
        final int victoiresNoir;
        final int nuls;

        ResultatMatchup(Niveau niveauA, Niveau niveauB,
                        int victoiresA, int victoiresB,
                        int victoiresBlanc, int victoiresNoir, int nuls) {
            this.niveauA       = niveauA;
            this.niveauB       = niveauB;
            this.victoiresA    = victoiresA;
            this.victoiresB    = victoiresB;
            this.victoiresBlanc = victoiresBlanc;
            this.victoiresNoir  = victoiresNoir;
            this.nuls          = nuls;
        }

        int total() {
            return victoiresA + victoiresB + nuls;
        }
    }
}
