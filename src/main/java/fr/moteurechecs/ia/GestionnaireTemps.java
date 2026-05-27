package fr.moteurechecs.ia;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minuteur d'arrêt pour la recherche IA.
 *
 * Déclenche un signal atomique après l'écoulement d'une durée configurable.
 * Le signal est partageable avec les threads de recherche via {@link #getSignal()}.
 */
public final class GestionnaireTemps {

    private final AtomicBoolean signal = new AtomicBoolean(false);
    private ScheduledExecutorService minuteur;

    /**
     * Démarre le minuteur : le signal sera mis à {@code true} après {@code dureeMs} millisecondes.
     *
     * @param dureeMs durée en millisecondes avant déclenchement
     */
    public void demarrer(long dureeMs) {
        signal.set(false);
        minuteur = Executors.newSingleThreadScheduledExecutor();
        minuteur.schedule(() -> signal.set(true), dureeMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Arrête le minuteur et libère les ressources.
     */
    public void arreter() {
        if (minuteur != null) {
            minuteur.shutdownNow();
        }
    }

    /**
     * Indique si le temps est écoulé.
     *
     * @return {@code true} si le délai configuré est dépassé
     */
    public boolean estEcoule() {
        return signal.get();
    }

    /**
     * Retourne le signal atomique partageable avec les threads de recherche.
     *
     * @return référence à l'AtomicBoolean de fin de temps
     */
    public AtomicBoolean getSignal() {
        return signal;
    }
}
