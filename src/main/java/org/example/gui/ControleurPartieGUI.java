package org.example.gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.example.AI.Niveau;
import org.example.AI.RechercheMinimaxAlphaBeta;
import org.example.chess.*;

import java.util.List;

/**
 * Contrôleur principal de la partie GUI.
 * Fait le lien entre la Vue (JavaFX) et le Modèle (Plateau / Moteur).
 */
public class ControleurPartieGUI {

    private final Plateau plateau;
    private final VueEchiquier vue;
    private final SelecteurCoup selecteur;

    // Configuration partie
    private boolean iaJoueBlanc;
    private boolean iaJoueNoir;
    private Niveau niveauIABlanc;
    private Niveau niveauIANoir;

    private boolean partieTerminee;
    private Coup dernierCoupJoue;

    // Historique pour l'annulation
    private final java.util.Stack<EtatPlateau> historique = new java.util.Stack<>();

    public ControleurPartieGUI(boolean iaBlanc, boolean iaNoir, Niveau niveauBlanc, Niveau niveauNoir) {
        this.plateau = Plateau.positionInitiale();
        this.selecteur = new SelecteurCoup();
        this.vue = new VueEchiquier(this); // La vue a besoin du contrôleur pour les clics

        this.iaJoueBlanc = iaBlanc;
        this.iaJoueNoir = iaNoir;
        this.niveauIABlanc = niveauBlanc;
        this.niveauIANoir = niveauNoir;
        this.partieTerminee = false;

        rafraichirVue();
        verifierTourIA();
    }

    // Constructeur de compatibilité si besoin (ou à supprimer si on met à jour tous
    // les appels)
    public ControleurPartieGUI(boolean iaBlanc, boolean iaNoir, Niveau niveauUnique) {
        this(iaBlanc, iaNoir, niveauUnique, niveauUnique);
    }

    public VueEchiquier getVue() {
        return vue;
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
                // Annulation coup IA
                EtatPlateau stateIA = historique.pop();
                plateau.annuler(stateIA);

                // Annulation coup Humain
                EtatPlateau stateHumain = historique.pop();
                plateau.annuler(stateHumain);

                dernierCoupJoue = null; // On perd l'info du dernier coup joué visuellement (ou il faudrait le stocker
                                        // dans l'historique aussi)
                // Pour faire propre, il faudrait que l'historique stocke aussi le "dernierCoup"
                // pour l'affichage.
                // Ici on simplifie : plus de surbrillance dernier coup après undo.

                rafraichirVue();
                preparerTourSuivant(); // Pour remettre à jour les coups légaux
            }
        } else {
            // Mode H vs H ou IA vs IA : on annule 1 seul coup
            EtatPlateau state = historique.pop();
            plateau.annuler(state);
            dernierCoupJoue = null;
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

    private void verifierTourIA() {
        if (partieTerminee)
            return;

        if (estTourIA()) {
            // Lancer l'IA dans un thread pour ne pas bloquer l'UI
            Task<Coup> taskIA = new Task<>() {
                @Override
                protected Coup call() throws Exception {
                    // Simuler une mini "réflexion" pour pas que ça soit instantané (optionnel)
                    Thread.sleep(100);
                    Niveau niveauCourant = (plateau.trait() == Couleur.BLANC) ? niveauIABlanc : niveauIANoir;
                    return RechercheMinimaxAlphaBeta.meilleurCoup(plateau, niveauCourant);
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
        // Sauvegarde pour undo
        // Attention : Plateau.jouerAvecSauvegarde joue le coup et retourne l'état
        // AVANT.
        // Mais ici on veut utiliser notre propre gestion ou celle du plateau ?
        // Plateau.jouerAvecSauvegarde fait : create state -> jouer -> return state.
        // Donc on récupère l'état PRECEDENT le coup. C'est ce qu'on veut empiler.
        EtatPlateau state = plateau.jouerAvecSauvegarde(coup);
        historique.push(state);

        // Note: jouerAvecSauvegarde a DEJA joué le coup.
        dernierCoupJoue = coup;

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
        // On regarde si le joueur au trait a des coups légaux
        List<Coup> legaux = GenerateurCoups.genererLegaux(plateau);
        if (legaux.isEmpty()) {
            partieTerminee = true;
            boolean echec = plateau.estEnEchec(plateau.trait());
            String message = echec ? "ECHEC ET MAT ! " + plateau.trait().inverse() + " gagne." : "PAT ! Match nul.";
            afficherFin(message);
        } else {
            // Cas particulier : matériel insuffisant, répétition... (non géré par le moteur
            // actuel ?)
            // Le moteur ne semble pas gérer la règle des 50 coups ou répétition dans
            // Plateau (sauf si caché dans EtatPlateau/Historique manquant).
            // On s'en tient à Mat/Pat.
        }
    }

    private void afficherFin(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
            alert.setTitle("Fin de partie");
            alert.setHeaderText(null);
            alert.showAndWait();
        });
    }

    public void rafraichirVue() {
        // On passe les infos nécessaires à la vue
        Platform.runLater(() -> {
            // On récupère la sélection courante pour la surbrillance
            Case selection = selecteur.getCaseDepart();
            vue.rafraichir(plateau, selection, dernierCoupJoue);
        });
    }

    /**
     * Initialisation manuelle du premier tour humain si besoin.
     */
    public void demarrer() {
        preparerTourSuivant();
    }
}
