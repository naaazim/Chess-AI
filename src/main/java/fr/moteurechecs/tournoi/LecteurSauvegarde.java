package fr.moteurechecs.tournoi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Lit un fichier de sauvegarde incrémentale produit par {@link SauvegardeTournoi}
 * et reconstitue l'état du tournoi (matchups terminés + bilans partiels).
 */
public final class LecteurSauvegarde {

    private LecteurSauvegarde() {}

    /**
     * Lit le fichier de sauvegarde et retourne l'état courant du tournoi.
     * Si le fichier n'existe pas, retourne un {@link EtatTournoi} vide (sans erreur).
     */
    public static EtatTournoi lire(String cheminFichier) throws IOException {
        File fichier = new File(cheminFichier);
        if (!fichier.exists()) {
            return new EtatTournoi(Collections.emptySet(), Collections.emptyMap());
        }

        Set<String> matchupsTermines = new HashSet<>();
        Map<String, MutableBilan> bilansEnCours = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(fichier))) {
            String ligne;
            while ((ligne = reader.readLine()) != null) {
                ligne = ligne.trim();
                if (ligne.isEmpty()) continue;

                String[] parts = ligne.split("\\|");
                if (parts.length == 0) continue;

                switch (parts[0]) {
                    case "MATCHUP_TERMINE" -> {
                        if (parts.length >= 3) {
                            String cle = parts[1] + "-" + parts[2];
                            matchupsTermines.add(cle);
                        }
                    }
                    case "PARTIE" -> {
                        if (parts.length >= 7) {
                            String niveauA  = parts[1];
                            String niveauB  = parts[2];
                            int    numPart  = Integer.parseInt(parts[3]);
                            String blanc    = parts[4];
                            String noir     = parts[5];
                            String resultat = parts[6];

                            String cle = niveauA + "-" + niveauB;
                            MutableBilan b = bilansEnCours.computeIfAbsent(cle, k -> new MutableBilan());

                            b.dernierePartie = Math.max(b.dernierePartie, numPart);

                            boolean premiereMoitie = blanc.equals(niveauA);

                            switch (resultat) {
                                case "BLANC_GAGNE" -> {
                                    b.victoiresBlanc++;
                                    if (premiereMoitie) b.victoiresA++; else b.victoiresB++;
                                }
                                case "NOIR_GAGNE" -> {
                                    b.victoiresNoir++;
                                    if (premiereMoitie) b.victoiresB++; else b.victoiresA++;
                                }
                                case "NUL" -> b.nuls++;
                            }
                        }
                    }
                    default -> { /* ligne inconnue — ignorée */ }
                }
            }
        }

        // Convertir les bilans mutables en BilanPartiel immuables
        Map<String, BilanPartiel> bilanPartiels = new HashMap<>();
        for (Map.Entry<String, MutableBilan> entry : bilansEnCours.entrySet()) {
            MutableBilan mb = entry.getValue();
            bilanPartiels.put(entry.getKey(),
                    new BilanPartiel(mb.dernierePartie,
                            mb.victoiresA, mb.victoiresB,
                            mb.victoiresBlanc, mb.victoiresNoir,
                            mb.nuls));
        }

        return new EtatTournoi(
                Collections.unmodifiableSet(matchupsTermines),
                Collections.unmodifiableMap(bilanPartiels));
    }

    // ─── Types publics ────────────────────────────────────────────────────────

    /** État complet du tournoi reconstruit depuis le fichier de sauvegarde. */
    public static final class EtatTournoi {
        /** Clés {@code "NIVEAU_A-NIVEAU_B"} des matchups entièrement terminés. */
        public final Set<String> matchupsTermines;
        /** Bilans partiels des matchups en cours (clé = {@code "NIVEAU_A-NIVEAU_B"}). */
        public final Map<String, BilanPartiel> bilanPartiels;

        EtatTournoi(Set<String> matchupsTermines, Map<String, BilanPartiel> bilanPartiels) {
            this.matchupsTermines = matchupsTermines;
            this.bilanPartiels    = bilanPartiels;
        }

        public boolean estVide() {
            return matchupsTermines.isEmpty() && bilanPartiels.isEmpty();
        }
    }

    /** Bilan partiel d'un matchup dont la sauvegarde a été interrompue. */
    public static final class BilanPartiel {
        /** Numéro de la dernière partie déjà sauvegardée (1-based). */
        public final int dernierePartie;
        public final int victoiresA;
        public final int victoiresB;
        public final int victoiresBlanc;
        public final int victoiresNoir;
        public final int nuls;

        public BilanPartiel(int dernierePartie,
                            int victoiresA, int victoiresB,
                            int victoiresBlanc, int victoiresNoir,
                            int nuls) {
            this.dernierePartie = dernierePartie;
            this.victoiresA     = victoiresA;
            this.victoiresB     = victoiresB;
            this.victoiresBlanc = victoiresBlanc;
            this.victoiresNoir  = victoiresNoir;
            this.nuls           = nuls;
        }
    }

    // ─── Interne ──────────────────────────────────────────────────────────────

    /** Accumulateur mutable utilisé pendant la lecture. */
    private static final class MutableBilan {
        int dernierePartie = 0;
        int victoiresA     = 0;
        int victoiresB     = 0;
        int victoiresBlanc = 0;
        int victoiresNoir  = 0;
        int nuls           = 0;
    }
}
