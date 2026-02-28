package org.example.gui;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.example.AI.Niveau;
import org.example.AI.RechercheMinimaxAlphaBeta;
import org.example.chess.*;

import java.util.List;
import java.util.Stack;

/**
 * Contrôleur principal de la partie GUI.
 * Fait le lien entre la Vue (JavaFX) et le Modèle (Plateau / Moteur).
 */
public class ControleurPartieGUI {

    private final Plateau plateau;
    private final VueEchiquier vue;
    private final SelecteurCoup selecteur;

    private final ProfilerPartie profiler;

    // Configuration partie
    private boolean iaJoueBlanc;
    private boolean iaJoueNoir;
    private Niveau niveauIABlanc;
    private Niveau niveauIANoir;

    private boolean partieTerminee;
    private Coup dernierCoupJoue;

    // Historique complet (undo + notation SAN)
    private final Stack<EntreeHistorique> historique = new Stack<>();
    private final ObservableList<String> historiqueAffichage = FXCollections.observableArrayList();

    // Affichage des calculs de l'IA en temps réel
    private final ObservableList<String> calculAIAffichage = FXCollections.observableArrayList();

    // Avantage matériel (positif = blancs, négatif = noirs)
    private final SimpleIntegerProperty avantageMateriel = new SimpleIntegerProperty(0);
    private final SimpleStringProperty avantageMaterielTexte = new SimpleStringProperty("Égalité matérielle");

    // Nom de l'ouverture courante
    private final SimpleStringProperty nomOuverture = new SimpleStringProperty("Unknown");

    public ControleurPartieGUI(boolean iaBlanc, boolean iaNoir, Niveau niveauBlanc, Niveau niveauNoir) {
        this.plateau = Plateau.positionInitiale();
        this.selecteur = new SelecteurCoup();
        this.vue = new VueEchiquier(this); // La vue a besoin du contrôleur pour les clics

        this.iaJoueBlanc = iaBlanc;
        this.iaJoueNoir = iaNoir;
        this.niveauIABlanc = niveauBlanc;
        this.niveauIANoir = niveauNoir;
        this.partieTerminee = false;

        this.profiler = new ProfilerPartie(iaBlanc, iaNoir, niveauBlanc, niveauNoir);

        rafraichirVue();
        // Ne pas appeler verifierTourIA() ici pour éviter le lancement en double, c'est
        // appelé par demarrer()
    }

    // Constructeur de compatibilité si besoin (ou à supprimer si on met à jour tous
    // les appels)
    public ControleurPartieGUI(boolean iaBlanc, boolean iaNoir, Niveau niveauUnique) {
        this(iaBlanc, iaNoir, niveauUnique, niveauUnique);
    }

    public VueEchiquier getVue() {
        return vue;
    }

    public ObservableList<String> getHistoriqueAffichage() {
        return historiqueAffichage;
    }

    public ObservableList<String> getCalculAIAffichage() {
        return calculAIAffichage;
    }

    public ReadOnlyIntegerProperty avantageMaterielProperty() {
        return avantageMateriel;
    }

    public ReadOnlyStringProperty avantageMaterielTexteProperty() {
        return avantageMaterielTexte;
    }

    public ReadOnlyStringProperty nomOuvertureProperty() {
        return nomOuverture;
    }

    /**
     * Tente d'annuler le dernier coup (ou les 2 derniers si on joue contre l'IA).
     */
    public void annulerCoup() {
        if (partieTerminee) {
            // Si la partie est finie, on peut vouloir l'inspecter, mais annuler peut être
            // complexe
            // si on est mat. Pour simplifier, on autorise l'annulation si historique non
            // vide.
            partieTerminee = false;
        }

        if (historique.isEmpty())
            return;

        // Mode Humain vs IA : on annule 2 coups pour revenir au tour de l'humain
        // Sauf si c'est le tout début ou cas particulier.
        boolean modeHvIA = (iaJoueBlanc && !iaJoueNoir) || (!iaJoueBlanc && iaJoueNoir);

        if (modeHvIA) {
            // On doit annuler le coup de l'IA ET le coup du joueur
            // Vérifions qu'on a bien au moins 2 états (coup joueur + coup IA)
            // Ou alors, si c'est à l'IA de jouer (ex: on annule pendant son tour ?
            // Difficile si le thread tourne. On suppose qu'on annule quand c'est à nous).

            // Si c'est à l'humain de jouer, le dernier coup était l'IA, l'avant dernier
            // était l'humain.
            // Donc on pop 2 fois.
            if (!estTourIA() && historique.size() >= 2) {
                annulerDernierCoupInterne();
                annulerDernierCoupInterne();
                dernierCoupJoue = historique.isEmpty() ? null : historique.peek().coup();

                rafraichirVue();
                preparerTourSuivant(); // Pour remettre à jour les coups légaux
            }
        } else {
            // Mode H vs H ou IA vs IA : on annule 1 seul coup
            annulerDernierCoupInterne();
            dernierCoupJoue = historique.isEmpty() ? null : historique.peek().coup();
            rafraichirVue();
            preparerTourSuivant();
        }
    }

    /**
     * Appelé par la vue lors d'un clic sur une case.
     */
    public void traiterClic(Case caseCliquee) {
        if (partieTerminee)
            return;

        // Si c'est au tour de l'IA, on ignore les clics humains
        if (estTourIA())
            return;

        // Mise à jour des coups légaux pour le sélecteur si besoin
        // (Optimisation : on pourrait le faire une seule fois par tour, mais ici on le
        // fait à la demande ou au début du tour)
        // Le sélecteur a besoin de la liste à jour.

        // Logique de sélection
        boolean action = selecteur.selectionner(caseCliquee);

        if (action) {
            // Tentative de coup ?
            Case depart = selecteur.getCaseDepart();
            if (depart != null && !depart.equals(caseCliquee)) {
                // On a un départ et on vient de cliquer ailleurs -> tentative coup
                Coup coup = selecteur.trouverCoup(caseCliquee);
                if (coup != null) {
                    jouerCoup(coup);
                    selecteur.reinitialiser();
                } else {
                    // Coup illégal : on réinitialise ou on change de sélection
                    // Ici simple : on garde la sélection si c'est la meme couleur, sinon reset.
                    // Pour simplifier : reset si invalide.
                    // selecteur.reinitialiser(); // ou pas, dépend de l'UX voulue.
                    // UX "magnétique" : si on clique sur une case vide invalide, on désélectionne.
                    selecteur.reinitialiser();
                    rafraichirVue(); // pour effacer la surbrillance
                }
            }
            rafraichirVue();
        }
    }

    private boolean estTourIA() {
        if (plateau.trait() == Couleur.BLANC && iaJoueBlanc)
            return true;
        if (plateau.trait() == Couleur.NOIR && iaJoueNoir)
            return true;
        return false;
    }

    private void annulerDernierCoupInterne() {
        if (historique.isEmpty()) {
            return;
        }
        EntreeHistorique entree = historique.pop();
        plateau.annuler(entree.etatAvant());
    }

    private void verifierTourIA() {
        if (partieTerminee)
            return;

        if (estTourIA()) {
            Platform.runLater(() -> calculAIAffichage.clear());

            // Lancer l'IA dans un thread pour ne pas bloquer l'UI
            Task<Coup> taskIA = new Task<>() {
                @Override
                protected Coup call() throws Exception {
                    // Simuler une mini "réflexion" pour pas que ça soit instantané (optionnel)
                    Thread.sleep(100);
                    Niveau niveauCourant = (plateau.trait() == Couleur.BLANC) ? niveauIABlanc : niveauIANoir;
                    return RechercheMinimaxAlphaBeta.meilleurCoup(plateau, niveauCourant, profiler,
                            info -> Platform.runLater(() -> calculAIAffichage.add(info)));
                }
            };

            taskIA.setOnSucceeded(e -> {
                Coup coupIA = taskIA.getValue();
                if (coupIA != null) {
                    jouerCoup(coupIA);
                } else {
                    // Pas de coup => Mat ou Pat détecté au prochain check
                    interpreterFinDePartie();
                }
            });

            taskIA.setOnFailed(e -> {
                taskIA.getException().printStackTrace();
            });

            new Thread(taskIA).start();
        }
    }

    private void jouerCoup(Coup coup) {
        List<Coup> legauxAvantCoup = GenerateurCoups.genererLegaux(plateau);
        String notationSAN = NotationEchecs.versSAN(plateau, coup, legauxAvantCoup);

        RechercheMinimaxAlphaBeta.verifierOuverture(plateau, legauxAvantCoup, coup);

        // Sauvegarde pour undo
        // Attention : Plateau.jouerAvecSauvegarde joue le coup et retourne l'état
        // AVANT.
        // Mais ici on veut utiliser notre propre gestion ou celle du plateau ?
        // Plateau.jouerAvecSauvegarde fait : create state -> jouer -> return state.
        // Donc on récupère l'état PRECEDENT le coup. C'est ce qu'on veut empiler.
        EtatPlateau state = plateau.jouerAvecSauvegarde(coup);
        historique.push(new EntreeHistorique(state, coup, notationSAN));

        // Note: jouerAvecSauvegarde a DEJA joué le coup.
        dernierCoupJoue = coup;

        // Incrémenter le compteur de ply pour le livre d'ouvertures
        RechercheMinimaxAlphaBeta.incrementPly();

        // Après avoir joué, on met à jour la vue
        rafraichirVue();

        // Vérifier fin de partie
        interpreterFinDePartie();

        if (!partieTerminee) {
            // Préparer le tour suivant
            preparerTourSuivant();
        }
    }

    private void preparerTourSuivant() {
        // Mettre à jour les coups légaux pour le sélecteur (pour l'humain)
        // On le fait ici pour que le sélecteur soit prêt quand l'humain clique.
        if (!estTourIA()) {
            List<Coup> legaux = GenerateurCoups.genererLegaux(plateau);
            selecteur.setCoupsLegaux(legaux);
            if (legaux.isEmpty()) {
                interpreterFinDePartie();
            }
        } else {
            // Si c'est à l'IA, on lance la routine
            verifierTourIA();
        }
    }

    private void interpreterFinDePartie() {
        // Règle des 3 coups (triple répétition)
        EtatPlateau etatCourant = plateau.sauvegarderEtat();
        int repetitions = 1; // L'état actuel compte pour 1
        for (EntreeHistorique entree : historique) {
            if (entree.etatAvant().equals(etatCourant)) {
                repetitions++;
            }
        }

        if (repetitions >= 3) {
            partieTerminee = true;
            profiler.marquerFinDePartie("Nul");
            afficherFin("Match Nul par triple répétition !");
            return;
        }

        // On regarde si le joueur au trait a des coups légaux
        List<Coup> legaux = GenerateurCoups.genererLegaux(plateau);
        if (legaux.isEmpty()) {
            partieTerminee = true;
            boolean echec = plateau.estEnEchec(plateau.trait());
            String vainqueur = echec ? plateau.trait().inverse().toString() : "Nul";
            profiler.marquerFinDePartie(vainqueur);

            String message = echec ? "ECHEC ET MAT ! " + vainqueur + " gagne." : "PAT ! Match nul.";
            afficherFin(message);
        } else {
            // Règle des 50 coups non gérée pour le moment,
            // mais la répétition est désormais implémentée ci-dessus.
        }
    }

    private void afficherFin(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
            alert.setTitle("Fin de partie");
            alert.setHeaderText(null);
            alert.showAndWait();

            Alert statsAlert = new Alert(Alert.AlertType.CONFIRMATION, "Sauvegarder les statistiques de cette partie ?",
                    ButtonType.YES, ButtonType.NO);
            statsAlert.setTitle("Statistiques");
            statsAlert.setHeaderText("Fin de partie");
            statsAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    javafx.stage.Window window = vue.getScene().getWindow();
                    if (window instanceof javafx.stage.Stage) {
                        profiler.sauvegarderStatistiques((javafx.stage.Stage) window);
                    }
                }
            });
        });
    }

    public void rafraichirVue() {
        // On passe les infos nécessaires à la vue
        Platform.runLater(() -> {
            // On récupère la sélection courante pour la surbrillance
            Case selection = selecteur.getCaseDepart();
            List<Coup> coupsPossibles = selecteur.getCoupsPossiblesSelection();
            vue.rafraichir(plateau, selection, dernierCoupJoue, coupsPossibles);
            mettreAJourHistoriqueAffichage();
            mettreAJourAvantageMateriel();
            nomOuverture.set(RechercheMinimaxAlphaBeta.getCurrentOpeningName());
        });
    }

    /**
     * Initialisation manuelle du premier tour humain si besoin.
     */
    public void demarrer() {
        preparerTourSuivant();
    }

    private void mettreAJourHistoriqueAffichage() {
        historiqueAffichage.clear();
        for (int i = 0; i < historique.size(); i += 2) {
            int numeroCoup = (i / 2) + 1;
            String coupBlanc = historique.get(i).notationSAN();
            String coupNoir = (i + 1 < historique.size()) ? historique.get(i + 1).notationSAN() : "";
            if (coupNoir.isBlank()) {
                historiqueAffichage.add(numeroCoup + ". " + coupBlanc);
            } else {
                historiqueAffichage.add(numeroCoup + ". " + coupBlanc + "   " + coupNoir);
            }
        }
    }

    private void mettreAJourAvantageMateriel() {
        int scoreBlanc = scoreMateriel(
                Piece.PION_BLANC, 1,
                Piece.CAVALIER_BLANC, 3,
                Piece.FOU_BLANC, 3,
                Piece.TOUR_BLANC, 5,
                Piece.DAME_BLANCHE, 9);
        int scoreNoir = scoreMateriel(
                Piece.PION_NOIR, 1,
                Piece.CAVALIER_NOIR, 3,
                Piece.FOU_NOIR, 3,
                Piece.TOUR_NOIRE, 5,
                Piece.DAME_NOIRE, 9);

        int ecart = scoreBlanc - scoreNoir;
        avantageMateriel.set(ecart);

        if (ecart > 0) {
            avantageMaterielTexte.set("Avantage Blancs : +" + ecart);
        } else if (ecart < 0) {
            avantageMaterielTexte.set("Avantage Noirs : +" + (-ecart));
        } else {
            avantageMaterielTexte.set("Égalité matérielle");
        }
    }

    private int scoreMateriel(
            Piece p1, int v1,
            Piece p2, int v2,
            Piece p3, int v3,
            Piece p4, int v4,
            Piece p5, int v5) {
        return Long.bitCount(plateau.bitboard(p1)) * v1
                + Long.bitCount(plateau.bitboard(p2)) * v2
                + Long.bitCount(plateau.bitboard(p3)) * v3
                + Long.bitCount(plateau.bitboard(p4)) * v4
                + Long.bitCount(plateau.bitboard(p5)) * v5;
    }

    private record EntreeHistorique(EtatPlateau etatAvant, Coup coup, String notationSAN) {
    }
}
