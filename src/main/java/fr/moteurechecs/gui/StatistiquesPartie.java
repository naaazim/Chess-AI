package fr.moteurechecs.gui;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import fr.moteurechecs.ia.Niveau;

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
public class StatistiquesPartie {

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

    public StatistiquesPartie(boolean iaBlanc, boolean iaNoir, Niveau niveauBlanc, Niveau niveauNoir) {
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

    /**
     * Calcule le temps moyen de réflexion en secondes depuis une liste de durées en millisecondes.
     *
     * @param listTemps liste des durées (en ms)
     * @return moyenne en secondes, ou 0 si la liste est vide
     */
    private double moyenneTemps(List<Long> listTemps) {
        if (listTemps.isEmpty())
            return 0;
        return listTemps.stream().mapToLong(Long::longValue).average().orElse(0) / 1000.0;
    }

    /**
     * Calcule la profondeur moyenne atteinte depuis une liste de profondeurs.
     *
     * @param listProfondeurs liste des profondeurs
     * @return moyenne, ou 0 si la liste est vide
     */
    private double moyenneProfondeur(List<Integer> listProfondeurs) {
        if (listProfondeurs.isEmpty())
            return 0;
        return listProfondeurs.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    /**
     * Ouvre une boîte de dialogue pour sauvegarder les statistiques dans un fichier texte.
     *
     * @param stage fenêtre parente pour la boîte de dialogue
     */
    public void sauvegarderStatistiques(Stage stage) {
        FileChooser selecteurFichier = new FileChooser();
        selecteurFichier.setTitle("Sauvegarder les statistiques de la partie");
        selecteurFichier.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichier Texte", "*.txt"));

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        selecteurFichier.setInitialFileName("statistiques_echecs_" + dtf.format(LocalDateTime.now()) + ".txt");

        File fichier = selecteurFichier.showSaveDialog(stage);

        if (fichier != null) {
            try (PrintWriter writer = new PrintWriter(new FileWriter(fichier))) {
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
                        + (iaBlanc ? "IA (" + niveauBlanc.name() + ", base depth " + niveauBlanc.profondeur() + ")"
                                : "Humain"));
                writer.println("  Coups joues : " + tempsReflexionBlanc.size());
                if (iaBlanc && !tempsReflexionBlanc.isEmpty()) {
                    writer.printf("  Temps moyen de reflexion : %.3f secondes/coup%n",
                            moyenneTemps(tempsReflexionBlanc));
                    writer.printf("  Average depth reached : %.2f%n", moyenneProfondeur(profondeurAtteinteBlanc));
                }
                writer.println("-----------------------------------------");

                writer.println("Joueur NOIR : "
                        + (iaNoir ? "IA (" + niveauNoir.name() + ", base depth " + niveauNoir.profondeur() + ")"
                                : "Humain"));
                writer.println("  Coups joues : " + tempsReflexionNoir.size());
                if (iaNoir && !tempsReflexionNoir.isEmpty()) {
                    writer.printf("  Temps moyen de reflexion : %.3f secondes/coup%n",
                            moyenneTemps(tempsReflexionNoir));
                    writer.printf("  Average depth reached : %.2f%n", moyenneProfondeur(profondeurAtteinteNoir));
                }
                writer.println("=========================================");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
