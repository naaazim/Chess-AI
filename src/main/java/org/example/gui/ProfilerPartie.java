package org.example.gui;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.AI.Niveau;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Gère le profilage d'une partie (temps par coup, profondeur moyenne, etc.)
 * et l'exportation des statistiques vers un fichier texte.
 */
public class ProfilerPartie {

    private final boolean iaBlanc;
    private final boolean iaNoir;
    private final Niveau niveauBlanc;
    private final Niveau niveauNoir;

    private final long debutPartieMillis;
    private long finPartieMillis;

    private final List<Long> tempsReflexionBlanc = new ArrayList<>();
    private final List<Long> tempsReflexionNoir = new ArrayList<>();

    private final List<Integer> profondeurAtteinteBlanc = new ArrayList<>();
    private final List<Integer> profondeurAtteinteNoir = new ArrayList<>();

    private String vainqueur = "Non terminé";

    public ProfilerPartie(boolean iaBlanc, boolean iaNoir, Niveau niveauBlanc, Niveau niveauNoir) {
        this.iaBlanc = iaBlanc;
        this.iaNoir = iaNoir;
        this.niveauBlanc = niveauBlanc;
        this.niveauNoir = niveauNoir;
        this.debutPartieMillis = System.currentTimeMillis();
    }

    public void enregistrerCoup(boolean estBlanc, long tempsMillis, int profondeur) {
        if (estBlanc) {
            tempsReflexionBlanc.add(tempsMillis);
            profondeurAtteinteBlanc.add(profondeur);
        } else {
            tempsReflexionNoir.add(tempsMillis);
            profondeurAtteinteNoir.add(profondeur);
        }
    }

    public void marquerFinDePartie(String vainqueur) {
        this.finPartieMillis = System.currentTimeMillis();
        this.vainqueur = vainqueur;
    }

    private double moyenneTemps(List<Long> tempsList) {
        if (tempsList.isEmpty())
            return 0;
        return tempsList.stream().mapToLong(Long::longValue).average().orElse(0) / 1000.0;
    }

    private double moyenneProfondeur(List<Integer> profondeurList) {
        if (profondeurList.isEmpty())
            return 0;
        return profondeurList.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    public void sauvegarderStatistiques(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sauvegarder les statistiques de la partie");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier Texte", "*.txt"));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        fileChooser.setInitialFileName("statistiques_echecs_" + dtf.format(LocalDateTime.now()) + ".txt");

        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                writer.println("=========================================");
                writer.println("   Statistiques de la Partie d'Echecs    ");
                writer.println("=========================================");
                writer.println(
                        "Date : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));

                long dureeTotalSecondes = (finPartieMillis > 0 ? (finPartieMillis - debutPartieMillis)
                        : (System.currentTimeMillis() - debutPartieMillis)) / 1000;
                writer.println("Duree totale de la partie : " + dureeTotalSecondes + " secondes");
                writer.println("Vainqueur : " + vainqueur);
                writer.println("-----------------------------------------");

                writer.println("Joueur BLANC : "
                        + (iaBlanc ? "IA (" + niveauBlanc.name() + ", prof. base " + niveauBlanc.profondeur() + ")"
                                : "Humain"));
                writer.println("  Coups joues : " + tempsReflexionBlanc.size());
                if (iaBlanc && !tempsReflexionBlanc.isEmpty()) {
                    writer.printf("  Temps moyen de reflexion : %.3f secondes/coup%n",
                            moyenneTemps(tempsReflexionBlanc));
                    writer.printf("  Profondeur moyenne atteinte : %.2f%n", moyenneProfondeur(profondeurAtteinteBlanc));
                }
                writer.println("-----------------------------------------");

                writer.println("Joueur NOIR : "
                        + (iaNoir ? "IA (" + niveauNoir.name() + ", prof. base " + niveauNoir.profondeur() + ")"
                                : "Humain"));
                writer.println("  Coups joues : " + tempsReflexionNoir.size());
                if (iaNoir && !tempsReflexionNoir.isEmpty()) {
                    writer.printf("  Temps moyen de reflexion : %.3f secondes/coup%n",
                            moyenneTemps(tempsReflexionNoir));
                    writer.printf("  Profondeur moyenne atteinte : %.2f%n", moyenneProfondeur(profondeurAtteinteNoir));
                }
                writer.println("=========================================");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
