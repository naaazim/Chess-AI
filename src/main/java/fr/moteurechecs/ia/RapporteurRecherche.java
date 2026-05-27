package fr.moteurechecs.ia;

import fr.moteurechecs.plateau.Coup;

import java.util.function.Consumer;

/**
 * Utilitaire statique de journalisation et de notification pour la recherche IA.
 *
 * Fournit des méthodes pour afficher les informations de recherche dans la console
 * et notifier l'interface graphique via un observateur.
 */
public final class RapporteurRecherche {

    private RapporteurRecherche() {}

    /**
     * Affiche dans la console un résumé du coup choisi par l'IA.
     *
     * @param niveau             niveau de difficulté utilisé
     * @param coup               coup sélectionné
     * @param profondeurAtteinte profondeur réellement atteinte
     * @param profMax            profondeur maximale cible
     * @param etiquettePhase     étiquette de phase (OUVERTURE/MILIEU/FINALE)
     * @param tempsTotal         temps de réflexion en millisecondes
     */
    public static void afficherLog(Niveau niveau, Coup coup,
            int profondeurAtteinte, int profMax, String etiquettePhase, long tempsTotal) {
        System.out.printf(
                "IA (%s) a joué %s-%s (Depth: %d / Max: %d, Phase: %s, Temps: %dms)%n",
                niveau,
                coup.depart().versAlgebrique(),
                coup.arrivee().versAlgebrique(),
                profondeurAtteinte, profMax, etiquettePhase, tempsTotal);
    }

    /**
     * Notifie l'observateur GUI avec une ligne formatée décrivant le coup joué.
     *
     * @param observateur        callback vers l'IHM (peut être {@code null})
     * @param coup               coup sélectionné
     * @param profondeurAtteinte profondeur réellement atteinte
     * @param profMax            profondeur maximale cible
     * @param etiquettePhase     étiquette de phase (OUVERTURE/MILIEU/FINALE)
     * @param tempsTotal         temps de réflexion en millisecondes
     */
    public static void notifierObservateur(Consumer<String> observateur, Coup coup,
            int profondeurAtteinte, int profMax, String etiquettePhase, long tempsTotal) {
        if (observateur == null) return;
        String phaseCourt = switch (etiquettePhase) {
            case "OUVERTURE" -> "Ouverture";
            case "MILIEU"    -> "Milieu";
            default          -> "Finale";
        };
        String coupStr = coup.depart().versAlgebrique() + "\u2192" + coup.arrivee().versAlgebrique();
        String ligne   = String.format("\u2b1b %s | Depth: %d/%d | %s | %.1fs",
                coupStr, profondeurAtteinte, profMax, phaseCourt, tempsTotal / 1000.0);
        observateur.accept(ligne);
    }
}
