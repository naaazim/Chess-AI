package fr.moteurechecs.ia;

/**
 * Contrôleur d'état pour la phase d'ouverture d'une partie.
 *
 * Suit le nombre de demi-coups joués et décide si l'IA doit encore consulter
 * le livre d'ouvertures. Une fois la phase désactivée (manuellement ou par
 * dépassement de {@code maxBookPly}), elle ne se réactive jamais.
 *
 * Toutes les méthodes sont synchronisées pour une utilisation multi-thread.
 */
public final class GestionnairePhaseOuverture {

    private final int maxBookPly;
    private boolean openingPhaseActive;
    private int currentPly;
    private String lastOpeningName;

    /**
     * Construit un contrôleur avec une limite de demi-coups pour le livre.
     *
     * @param maxBookPly nombre maximal de demi-coups avant désactivation automatique
     * @throws IllegalArgumentException si {@code maxBookPly} est négatif
     */
    public GestionnairePhaseOuverture(int maxBookPly) {
        if (maxBookPly < 0) {
            throw new IllegalArgumentException("maxBookPly doit être >= 0");
        }
        this.maxBookPly = maxBookPly;
        this.openingPhaseActive = true;
        this.currentPly = 0;
        this.lastOpeningName = "Unknown";
    }

    /**
     * Indique si l'IA doit consulter le livre d'ouvertures pour ce coup.
     *
     * @return {@code true} si la phase d'ouverture est active et la limite non atteinte
     */
    public synchronized boolean shouldUseBook() {
        if (!openingPhaseActive) {
            return false;
        }
        // désactivation automatique dès que la limite de demi-coups est dépassée
        if (currentPly > maxBookPly) {
            disable();
            return false;
        }
        return true;
    }

    /**
     * Enregistre qu'un coup du livre a été choisi.
     *
     * @param openingName nom de l'ouverture sélectionnée
     */
    public synchronized void onBookMoveChosen(String openingName) {
        this.lastOpeningName = normalizeOpeningName(openingName);
    }

    /**
     * Enregistre que l'IA a choisi un coup hors livre.
     */
    public synchronized void onEngineMoveChosen() {
        this.lastOpeningName = "Unknown";
    }

    /**
     * Désactive définitivement la phase d'ouverture.
     */
    public synchronized void disable() {
        this.openingPhaseActive = false;
        this.lastOpeningName = "Unknown";
    }

    /**
     * Incrémente le compteur de demi-coups joués.
     */
    public synchronized void incrementPly() {
        currentPly++;
    }

    /**
     * Réinitialise le contrôleur pour une nouvelle partie.
     */
    public synchronized void reset() {
        openingPhaseActive = true;
        currentPly = 0;
        lastOpeningName = "Unknown";
    }

    /**
     * Retourne le nombre de demi-coups joués depuis le début de la partie.
     *
     * @return compteur de demi-coups
     */
    public synchronized int getCurrentPly() {
        return currentPly;
    }

    /**
     * Retourne le nom de la dernière ouverture utilisée.
     *
     * @return nom de l'ouverture, ou {@code "Unknown"} si aucune
     */
    public synchronized String getLastOpeningName() {
        return lastOpeningName;
    }

    // normalise un nom d'ouverture en remplaçant les valeurs nulles/vides par "Unknown"
    private String normalizeOpeningName(String openingName) {
        if (openingName == null || openingName.isBlank()) {
            return "Unknown";
        }
        return openingName;
    }
}
