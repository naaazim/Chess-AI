package fr.moteurechecs.ia;

import fr.moteurechecs.ia.recherche.TableDeTransposition;
import fr.moteurechecs.ouverture.LivreOuvertures;
import fr.moteurechecs.plateau.Couleur;
import fr.moteurechecs.plateau.Coup;
import fr.moteurechecs.plateau.GenerateurCoups;
import fr.moteurechecs.plateau.Plateau;
import fr.moteurechecs.gui.StatistiquesPartie;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Façade principale de l'orchestrateur IA.
 *
 * Expose les méthodes publiques {@code meilleurCoup()} et délègue à
 * {@link SelecteurCoupLivre}, {@link Finale} et {@link RecherchePrincipale}
 * selon un ordre de priorité strict :
 * - Livre d'ouvertures (si activé et disponible)
 * - Technique de finale gagnante
 * - Principe d'ouverture : roque prioritaire
 * - Recherche alpha-bêta IDS
 *
 * Conserve l'état de reporting (profondeur atteinte, phase, nom d'ouverture)
 * accessible par l'interface graphique via des accesseurs statiques.
 */
public final class OrchestrateurIA {

    private static final String OPENING_UNKNOWN = "Unknown";
    private static final int    MAX_BOOK_PLY    = 20;

    private static final GestionnairePhaseOuverture OPENING_PHASE =
            new GestionnairePhaseOuverture(MAX_BOOK_PLY);

    /** Historique des empreintes Zobrist de la partie en cours (pour la détection de répétition). */
    private static final long[] historiquePartie = new long[512];

    /** Nombre d'entrées valides dans {@code historiquePartie}. */
    private static int tailleHistorique = 0;

    private static volatile String  currentOpeningName = OPENING_UNKNOWN;
    private static volatile boolean openingBookEnabled = true;
    private static volatile int     lastDepthReached   = 0;
    private static volatile int     lastDepthTarget    = 0;
    private static volatile String  lastPhaseLabel     = "OUVERTURE";

    private OrchestrateurIA() {}

    // ===== Accesseurs de reporting (lus par le GUI) =====

    /** @return profondeur réellement atteinte lors de la dernière recherche */
    public static int    getLastDepthReached()   { return lastDepthReached; }

    /** @return profondeur cible de la dernière recherche */
    public static int    getLastDepthTarget()    { return lastDepthTarget; }

    /** @return étiquette de phase de la dernière recherche (OUVERTURE/MILIEU/FINALE) */
    public static String getLastPhaseLabel()     { return lastPhaseLabel; }

    /** @return nom de l'ouverture en cours, ou {@code "Unknown"} */
    public static String getCurrentOpeningName() { return currentOpeningName; }

    /** @return {@code true} si le livre d'ouvertures est activé */
    public static boolean isOpeningBookEnabled() { return openingBookEnabled; }

    // ===== Mutateurs d'état =====

    /**
     * Active ou désactive le livre d'ouvertures.
     *
     * @param enabled {@code true} pour activer le livre
     */
    public static void setOpeningBookEnabled(boolean enabled) {
        openingBookEnabled = enabled;
    }

    /**
     * Incrémente le compteur de demi-coups (à appeler après chaque coup joué).
     */
    public static void incrementPly() {
        OPENING_PHASE.incrementPly();
    }

    /**
     * Réinitialise l'état pour une nouvelle partie (compteur ply, historique, table de transposition).
     */
    public static void resetPly() {
        OPENING_PHASE.reset();
        currentOpeningName = OPENING_UNKNOWN;
        TableDeTransposition.getInstance().clear();
        tailleHistorique = 0;
    }

    /**
     * Enregistre l'empreinte Zobrist de la position courante dans l'historique de la partie.
     *
     * Doit être appelé après chaque coup joué sur le plateau (humain ou IA),
     * juste après {@code plateau.jouer(coup)} ou {@code plateau.jouerAvecSauvegarde(coup)}.
     * Cet historique est transmis à la recherche alpha-bêta pour détecter les répétitions
     * dans l'arbre de recherche et éviter que l'IA ne déclare nulle dans une position gagnante.
     *
     * @param plateau position après le coup joué
     */
    public static void enregistrerPosition(Plateau plateau) {
        historiquePartie[tailleHistorique++] = TableDeTransposition.hash(plateau);
    }

    /**
     * Met à jour le nom d'ouverture affiché d'après le gestionnaire de phase.
     *
     * @param plateauAvant position avant le coup (conservé pour compatibilité API)
     * @param legauxAvant  coups légaux avant le coup (non utilisés)
     * @param coupJoue     coup effectivement joué
     */
    public static void verifierOuverture(Plateau plateauAvant, List<Coup> legauxAvant, Coup coupJoue) {
        currentOpeningName = OPENING_PHASE.getLastOpeningName();
    }

    /**
     * Recharge le livre d'ouvertures depuis un fichier.
     *
     * @param path chemin vers le fichier PGN ou livre binaire
     */
    public static void reloadOpeningBook(String path) {
        LivreOuvertures.reload(path);
    }

    // ===== API principale =====

    /**
     * Calcule le meilleur coup pour la position donnée.
     *
     * @param plateau position courante
     * @param niveau  niveau de difficulté
     * @return meilleur coup trouvé, ou {@code null} si aucun coup légal
     */
    public static Coup meilleurCoup(Plateau plateau, Niveau niveau) {
        return meilleurCoup(plateau, niveau, null, null);
    }

    /**
     * Calcule le meilleur coup avec enregistrement des statistiques.
     *
     * @param plateau  position courante
     * @param niveau   niveau de difficulté
     * @param profiler collecteur de statistiques de partie (peut être {@code null})
     * @return meilleur coup trouvé
     */
    public static Coup meilleurCoup(Plateau plateau, Niveau niveau, StatistiquesPartie profiler) {
        return meilleurCoup(plateau, niveau, profiler, null);
    }

    /**
     * Calcule le meilleur coup avec profiler et observateur en temps réel.
     *
     * @param plateau     position courante
     * @param niveau      niveau de difficulté
     * @param profiler    collecteur de statistiques (peut être {@code null})
     * @param observateur callback pour les mises à jour de l'IHM (peut être {@code null})
     * @return meilleur coup trouvé, ou {@code null} si aucun coup légal
     * @throws IllegalArgumentException si {@code plateau} ou {@code niveau} est {@code null}
     */
    public static Coup meilleurCoup(Plateau plateau, Niveau niveau,
            StatistiquesPartie profiler, Consumer<String> observateur) {

        if (plateau == null) throw new IllegalArgumentException("plateau null");
        if (niveau  == null) throw new IllegalArgumentException("niveau null");

        List<Coup> coups = GenerateurCoups.genererLegaux(plateau);
        if (coups.isEmpty()) return null;

        // PRIORITÉ 1 : LIVRE D'OUVERTURES
        Optional<Coup> coupLivre = SelecteurCoupLivre.chercherCoupDansLivre(
                plateau, coups, niveau, OPENING_PHASE, openingBookEnabled);
        if (coupLivre.isPresent()) {
            currentOpeningName = OPENING_PHASE.getLastOpeningName();
            return coupLivre.get();
        }

        OPENING_PHASE.onEngineMoveChosen();
        currentOpeningName = OPENING_PHASE.getLastOpeningName();

        // PRIORITÉ 2 : FINALE GAGNANTE
        Coup coupFinale = Finale.getCoupFinaleGagnante(plateau, coups, plateau.trait());
        if (coupFinale != null) {
            System.out.println("[IA] Finale gagnante: " + coupFinale);
            return coupFinale;
        }

        // PRIORITÉ 2.5 : PRINCIPE OUVERTURE (ROQUE)
        Coup roquePrioritaire = SelecteurCoupLivre.trouverRoquePrioritaire(
                plateau, coups, OPENING_PHASE.getCurrentPly(), MAX_BOOK_PLY + 4);
        if (roquePrioritaire != null) {
            System.out.println("[IA] Principe ouverture (roque): " + roquePrioritaire);
            return roquePrioritaire;
        }

        // PRIORITÉ 3 : RECHERCHE ALPHA-BÊTA IDS
        RecherchePrincipale.ResultatIDS resultat =
                RecherchePrincipale.rechercherAvecIDS(plateau, coups, niveau, observateur,
                        historiquePartie, tailleHistorique);

        lastDepthReached = resultat.profondeurAtteinte;
        lastDepthTarget  = resultat.profMax;
        lastPhaseLabel   = resultat.etiquettePhase;

        if (profiler != null) {
            boolean blancsJouent = (plateau.trait() == Couleur.BLANC);
            profiler.enregistrerCoup(blancsJouent, resultat.tempsTotal, resultat.profondeurAtteinte);
        }

        RapporteurRecherche.afficherLog(niveau, resultat.meilleurCoup,
                resultat.profondeurAtteinte, resultat.profMax,
                resultat.etiquettePhase, resultat.tempsTotal);
        RapporteurRecherche.notifierObservateur(observateur, resultat.meilleurCoup,
                resultat.profondeurAtteinte, resultat.profMax,
                resultat.etiquettePhase, resultat.tempsTotal);

        return resultat.meilleurCoup;
    }
}
