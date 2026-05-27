package fr.moteurechecs.tournoi;

import fr.moteurechecs.ia.Niveau;
import fr.moteurechecs.tournoi.LecteurSauvegarde.BilanPartiel;
import fr.moteurechecs.tournoi.LecteurSauvegarde.EtatTournoi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fusionne plusieurs fichiers de sauvegarde partiels (produits par {@link SauvegardeTournoi})
 * en un rapport final lisible, au même format que le rapport normal du tournoi.
 *
 * Usage :
 * <pre>
 *   mvn exec:java -Dexec.mainClass=fr.moteurechecs.tournoi.RapportTournoi \
 *                 -Dexec.args="resultats_FACILE_MOYEN.txt resultats_FACILE_DIFFICILE.txt resultats_MOYEN_DIFFICILE.txt"
 * </pre>
 *
 * Sortie : {@value #FICHIER_RAPPORT}
 */
public final class RapportTournoi {

    private static final String FICHIER_RAPPORT = "rapport_final.txt";

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage : RapportTournoi <fichier1.txt> [fichier2.txt ...]");
            System.err.println("Exemple : RapportTournoi resultats_FACILE_MOYEN.txt resultats_FACILE_DIFFICILE.txt");
            System.exit(1);
        }

        // Clé "NIVEAU_A-NIVEAU_B" → bilan accumulé (mutable pendant la fusion)
        Map<String, int[]> accumulation = new HashMap<>();
        // [0]=victoiresA [1]=victoiresB [2]=victoiresBlanc [3]=victoiresNoir [4]=nuls

        for (String fichier : args) {
            System.out.println("Lecture : " + fichier);
            EtatTournoi etat = LecteurSauvegarde.lire(fichier);

            for (Map.Entry<String, BilanPartiel> entry : etat.bilanPartiels.entrySet()) {
                String cle = entry.getKey();
                BilanPartiel bp = entry.getValue();

                int[] acc = accumulation.computeIfAbsent(cle, k -> new int[5]);
                acc[0] += bp.victoiresA;
                acc[1] += bp.victoiresB;
                acc[2] += bp.victoiresBlanc;
                acc[3] += bp.victoiresNoir;
                acc[4] += bp.nuls;
            }
        }

        if (accumulation.isEmpty()) {
            System.out.println("Aucun résultat trouvé dans les fichiers fournis.");
            return;
        }

        // Convertir en ResultatMatchup, dans l'ordre de MATCHUPS pour la cohérence
        List<Tournoi.ResultatMatchup> resultats = new ArrayList<>();
        for (Niveau[] paire : Tournoi.MATCHUPS) {
            Niveau a = paire[0];
            Niveau b = paire[1];
            String cle = a.name() + "-" + b.name();
            int[] acc = accumulation.get(cle);
            if (acc != null) {
                resultats.add(new Tournoi.ResultatMatchup(a, b,
                        acc[0], acc[1], acc[2], acc[3], acc[4]));
            }
        }

        // Écrire le rapport
        File fichierSortie = new File(FICHIER_RAPPORT);
        try (PrintWriter out = new PrintWriter(new FileWriter(fichierSortie))) {
            Tournoi.ecrireRapportDans(out, resultats);
        }

        System.out.println("Rapport consolidé écrit dans : " + fichierSortie.getAbsolutePath());
    }
}
