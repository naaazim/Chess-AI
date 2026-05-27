package fr.moteurechecs.gui;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.scene.Node;
import fr.moteurechecs.ia.Niveau;
import fr.moteurechecs.ia.OrchestrateurIA;
import fr.moteurechecs.plateau.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;

public class ControleurPartieGUI {

    private final Plateau plateau;
    private final VueEchiquier vue;
    private final SelecteurCoup selecteur;
    private final RessourcesPieces ressourcesPieces;

    private final StatistiquesPartie profiler;

    private boolean iaJoueBlanc;
    private boolean iaJoueNoir;
    private Niveau niveauIABlanc;
    private Niveau niveauIANoir;

    private boolean partieTerminee;
    private Coup dernierCoupJoue;

    private final Stack<EntreeHistorique> historique = new Stack<>();
    private final ObservableList<String> historiqueAffichage = FXCollections.observableArrayList();
    private final ObservableList<String> calculAIAffichage  = FXCollections.observableArrayList();

    private final SimpleIntegerProperty  avantageMateriel      = new SimpleIntegerProperty(0);
    private final SimpleStringProperty   avantageMaterielTexte = new SimpleStringProperty("Égalité matérielle");
    private final SimpleStringProperty   nomOuverture          = new SimpleStringProperty("Unknown");
    private final SimpleObjectProperty<Couleur> traitProp      = new SimpleObjectProperty<>(Couleur.BLANC);

    // Pièces capturées (par le camp adverse)
    private final ObservableList<Piece> piecesCaptureesBlanches = FXCollections.observableArrayList();
    private final ObservableList<Piece> piecesCaptureeNoires    = FXCollections.observableArrayList();

    // États POST-coup pour la détection correcte de triple répétition
    private final List<EtatPlateau> historiqueEtatsApres = new ArrayList<>();

    public ControleurPartieGUI(boolean iaBlanc, boolean iaNoir, Niveau niveauBlanc, Niveau niveauNoir) {
        this.plateau          = Plateau.positionInitiale();
        this.selecteur        = new SelecteurCoup();
        this.ressourcesPieces = new RessourcesPieces();
        this.vue              = new VueEchiquier(this);

        this.iaJoueBlanc  = iaBlanc;
        this.iaJoueNoir   = iaNoir;
        this.niveauIABlanc = niveauBlanc;
        this.niveauIANoir  = niveauNoir;
        this.partieTerminee = false;

        this.profiler = new StatistiquesPartie(iaBlanc, iaNoir, niveauBlanc, niveauNoir);
        OrchestrateurIA.resetPly();

        rafraichirVue();
    }

    public ControleurPartieGUI(boolean iaBlanc, boolean iaNoir, Niveau niveauUnique) {
        this(iaBlanc, iaNoir, niveauUnique, niveauUnique);
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public VueEchiquier getVue() { return vue; }

    public ObservableList<String> getHistoriqueAffichage() { return historiqueAffichage; }
    public ObservableList<String> getCalculAIAffichage()   { return calculAIAffichage; }

    public ReadOnlyIntegerProperty       avantageMaterielProperty()      { return avantageMateriel; }
    public ReadOnlyStringProperty        avantageMaterielTexteProperty() { return avantageMaterielTexte; }
    public ReadOnlyStringProperty        nomOuvertureProperty()          { return nomOuverture; }
    public ReadOnlyObjectProperty<Couleur> traitProperty()               { return traitProp; }

    public ObservableList<Piece> getPiecesCaptureesBlanches() { return piecesCaptureesBlanches; }
    public ObservableList<Piece> getPiecesCaptureeNoires()    { return piecesCaptureeNoires; }

    public boolean estIABlanc()     { return iaJoueBlanc; }
    public boolean estIANoir()      { return iaJoueNoir; }
    public Niveau  getNiveauIABlanc() { return niveauIABlanc; }
    public Niveau  getNiveauIANoir()  { return niveauIANoir; }

    // ── Annulation ───────────────────────────────────────────────────────────

    public void annulerCoup() {
        if (partieTerminee) partieTerminee = false;
        if (historique.isEmpty()) return;

        boolean modeHvIA = (iaJoueBlanc && !iaJoueNoir) || (!iaJoueBlanc && iaJoueNoir);
        if (modeHvIA) {
            if (!estTourIA() && historique.size() >= 2) {
                annulerDernierCoupInterne();
                annulerDernierCoupInterne();
                dernierCoupJoue = historique.isEmpty() ? null : historique.peek().coup();
                rafraichirVue();
                preparerTourSuivant();
            }
        } else {
            annulerDernierCoupInterne();
            dernierCoupJoue = historique.isEmpty() ? null : historique.peek().coup();
            rafraichirVue();
            preparerTourSuivant();
        }
    }

    // ── Gestion des clics ────────────────────────────────────────────────────

    public void traiterClic(Case caseCliquee) {
        if (partieTerminee) return;
        if (estTourIA()) return;

        boolean action = selecteur.selectionner(caseCliquee);
        if (action) {
            Case depart = selecteur.getCaseDepart();
            if (depart != null && !depart.equals(caseCliquee)) {
                List<Coup> candidats = selecteur.trouverCoupsCandidats(caseCliquee);
                if (!candidats.isEmpty()) {
                    Coup coup = choisirCoupHumain(candidats);
                    if (coup == null) { rafraichirVue(); return; }
                    jouerCoup(coup);
                    selecteur.reinitialiser();
                } else {
                    selecteur.reinitialiser();
                    rafraichirVue();
                }
            }
            rafraichirVue();
        }
    }

    private Coup choisirCoupHumain(List<Coup> candidats) {
        if (candidats == null || candidats.isEmpty()) return null;
        if (candidats.size() == 1) return candidats.get(0);
        boolean promotions = candidats.stream().allMatch(Coup::estPromotion);
        if (!promotions) return candidats.get(0);
        return demanderPiecePromotion(candidats);
    }

    private Coup demanderPiecePromotion(List<Coup> promotions) {
        Dialog<Coup> dialog = new Dialog<>();
        dialog.setTitle("Promotion");
        dialog.setHeaderText("Choisissez la pièce de promotion");

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().add(ButtonType.CANCEL);

        HBox ligne = new HBox(12);
        ligne.setAlignment(Pos.CENTER);

        List<Coup> ordonnes = new ArrayList<>(promotions);
        ordonnes.sort(Comparator.comparingInt(this::prioritePromotion));

        for (Coup coup : ordonnes) {
            Piece piecePromo = coup.piecePromotion();
            Button bouton = new Button(nomPiecePromotion(piecePromo));
            bouton.setMinWidth(110);
            Node icon = ressourcesPieces.getRepresentation(piecePromo, 34);
            if (icon != null) bouton.setGraphic(icon);
            bouton.setOnAction(e -> { dialog.setResult(coup); dialog.close(); });
            ligne.getChildren().add(bouton);
        }

        pane.setContent(ligne);
        return dialog.showAndWait().orElse(null);
    }

    private int prioritePromotion(Coup coup) {
        Piece p = coup.piecePromotion();
        if (p == Piece.DAME_BLANCHE || p == Piece.DAME_NOIRE)     return 0;
        if (p == Piece.TOUR_BLANC   || p == Piece.TOUR_NOIRE)     return 1;
        if (p == Piece.FOU_BLANC    || p == Piece.FOU_NOIR)       return 2;
        if (p == Piece.CAVALIER_BLANC || p == Piece.CAVALIER_NOIR) return 3;
        return 4;
    }

    private String nomPiecePromotion(Piece p) {
        if (p == null) return "Pièce";
        return switch (p) {
            case DAME_BLANCHE, DAME_NOIRE     -> "Dame";
            case TOUR_BLANC,   TOUR_NOIRE     -> "Tour";
            case FOU_BLANC,    FOU_NOIR       -> "Fou";
            case CAVALIER_BLANC, CAVALIER_NOIR -> "Cavalier";
            default -> "Pièce";
        };
    }

    // ── Tour IA ──────────────────────────────────────────────────────────────

    private boolean estTourIA() {
        if (plateau.trait() == Couleur.BLANC && iaJoueBlanc) return true;
        if (plateau.trait() == Couleur.NOIR  && iaJoueNoir)  return true;
        return false;
    }

    private void annulerDernierCoupInterne() {
        if (historique.isEmpty()) return;
        EntreeHistorique entree = historique.pop();
        plateau.annuler(entree.etatAvant());
        if (!historiqueEtatsApres.isEmpty()) {
            historiqueEtatsApres.remove(historiqueEtatsApres.size() - 1);
        }
    }

    private void verifierTourIA() {
        if (partieTerminee) return;
        if (!estTourIA()) return;

        Platform.runLater(() -> calculAIAffichage.clear());

        Task<Coup> taskIA = new Task<>() {
            @Override
            protected Coup call() throws Exception {
                Thread.sleep(100);
                Niveau niveauCourant = (plateau.trait() == Couleur.BLANC) ? niveauIABlanc : niveauIANoir;
                return OrchestrateurIA.meilleurCoup(plateau, niveauCourant, profiler,
                        info -> Platform.runLater(() -> calculAIAffichage.add(info)));
            }
        };

        taskIA.setOnSucceeded(e -> {
            Coup coupIA = taskIA.getValue();
            if (coupIA != null) jouerCoup(coupIA);
            else interpreterFinDePartie();
        });
        taskIA.setOnFailed(e -> taskIA.getException().printStackTrace());

        new Thread(taskIA).start();
    }

    private void jouerCoup(Coup coup) {
        List<Coup> legauxAvantCoup = GenerateurCoups.genererLegaux(plateau);
        String notationSAN = NotationEchecs.versSAN(plateau, coup, legauxAvantCoup);

        EtatPlateau etatAvant = plateau.jouerAvecSauvegarde(coup);
        historique.push(new EntreeHistorique(etatAvant, coup, notationSAN));
        historiqueEtatsApres.add(plateau.sauvegarderEtat()); // état POST-coup

        dernierCoupJoue = coup;
        OrchestrateurIA.enregistrerPosition(plateau);
        OrchestrateurIA.incrementPly();

        rafraichirVue();
        interpreterFinDePartie();
        if (!partieTerminee) preparerTourSuivant();
    }

    private void preparerTourSuivant() {
        if (!estTourIA()) {
            List<Coup> legaux = GenerateurCoups.genererLegaux(plateau);
            selecteur.setCoupsLegaux(legaux);
            if (legaux.isEmpty()) interpreterFinDePartie();
        } else {
            verifierTourIA();
        }
    }

    private void interpreterFinDePartie() {
        EtatPlateau etatCourant = plateau.sauvegarderEtat();
        int occurrences = 0;
        for (EtatPlateau e : historiqueEtatsApres) {
            if (e.equals(etatCourant)) occurrences++;
        }
        if (occurrences >= 2) {
            partieTerminee = true;
            profiler.marquerFinDePartie("Nul");
            afficherFin("Match Nul par triple répétition !");
            return;
        }

        List<Coup> legaux = GenerateurCoups.genererLegaux(plateau);
        if (legaux.isEmpty()) {
            partieTerminee = true;
            boolean echec = plateau.estEnEchec(plateau.trait());
            String vainqueur = echec ? plateau.trait().inverse().toString() : "Nul";
            profiler.marquerFinDePartie(vainqueur);
            String message = echec
                    ? "ECHEC ET MAT ! " + vainqueur + " gagne."
                    : "PAT ! Match nul.";
            afficherFin(message);
        }
    }

    private void afficherFin(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
            alert.setTitle("Fin de partie");
            alert.setHeaderText(null);
            alert.showAndWait();

            Alert statsAlert = new Alert(Alert.AlertType.CONFIRMATION,
                    "Sauvegarder les statistiques de cette partie ?",
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

    // ── Rafraîchissement ─────────────────────────────────────────────────────

    public void rafraichirVue() {
        Platform.runLater(() -> {
            Case selection = selecteur.getCaseDepart();
            List<Coup> coupsPossibles = selecteur.getCoupsPossiblesSelection();
            vue.rafraichir(plateau, selection, dernierCoupJoue, coupsPossibles);
            mettreAJourHistoriqueAffichage();
            mettreAJourAvantageMateriel();
            nomOuverture.set(OrchestrateurIA.getCurrentOpeningName());
            traitProp.set(plateau.trait());
        });
    }

    public void demarrer() {
        preparerTourSuivant();
    }

    // ── Historique SAN ───────────────────────────────────────────────────────

    private void mettreAJourHistoriqueAffichage() {
        historiqueAffichage.clear();
        for (int i = 0; i < historique.size(); i += 2) {
            int numeroCoup = (i / 2) + 1;
            String coupBlanc = historique.get(i).notationSAN();
            String coupNoir  = (i + 1 < historique.size()) ? historique.get(i + 1).notationSAN() : "";
            if (coupNoir.isBlank()) {
                historiqueAffichage.add(numeroCoup + ". " + coupBlanc);
            } else {
                historiqueAffichage.add(numeroCoup + ". " + coupBlanc + "   " + coupNoir);
            }
        }
    }

    // ── Avantage matériel + pièces capturées ─────────────────────────────────

    private void mettreAJourAvantageMateriel() {
        int scoreBlanc = scoreMateriel(
                Piece.PION_BLANC,    1,
                Piece.CAVALIER_BLANC, 3,
                Piece.FOU_BLANC,     3,
                Piece.TOUR_BLANC,    5,
                Piece.DAME_BLANCHE,  9);
        int scoreNoir = scoreMateriel(
                Piece.PION_NOIR,    1,
                Piece.CAVALIER_NOIR, 3,
                Piece.FOU_NOIR,     3,
                Piece.TOUR_NOIRE,   5,
                Piece.DAME_NOIRE,   9);

        int ecart = scoreBlanc - scoreNoir;
        avantageMateriel.set(ecart);

        if (ecart > 0) {
            avantageMaterielTexte.set("Avantage Blancs : +" + ecart);
        } else if (ecart < 0) {
            avantageMaterielTexte.set("Avantage Noirs : +" + (-ecart));
        } else {
            avantageMaterielTexte.set("Égalité matérielle");
        }

        calculerPiecesCapturees();
    }

    private int scoreMateriel(Piece p1, int v1, Piece p2, int v2, Piece p3, int v3,
                               Piece p4, int v4, Piece p5, int v5) {
        return Long.bitCount(plateau.bitboard(p1)) * v1
             + Long.bitCount(plateau.bitboard(p2)) * v2
             + Long.bitCount(plateau.bitboard(p3)) * v3
             + Long.bitCount(plateau.bitboard(p4)) * v4
             + Long.bitCount(plateau.bitboard(p5)) * v5;
    }

    private void calculerPiecesCapturees() {
        List<Piece> blanches = new ArrayList<>();
        ajouterManquants(blanches, Piece.PION_BLANC,    8);
        ajouterManquants(blanches, Piece.CAVALIER_BLANC, 2);
        ajouterManquants(blanches, Piece.FOU_BLANC,     2);
        ajouterManquants(blanches, Piece.TOUR_BLANC,    2);
        ajouterManquants(blanches, Piece.DAME_BLANCHE,  1);

        List<Piece> noires = new ArrayList<>();
        ajouterManquants(noires, Piece.PION_NOIR,    8);
        ajouterManquants(noires, Piece.CAVALIER_NOIR, 2);
        ajouterManquants(noires, Piece.FOU_NOIR,     2);
        ajouterManquants(noires, Piece.TOUR_NOIRE,   2);
        ajouterManquants(noires, Piece.DAME_NOIRE,   1);

        piecesCaptureesBlanches.setAll(blanches);
        piecesCaptureeNoires.setAll(noires);
    }

    private void ajouterManquants(List<Piece> liste, Piece piece, int stockInitial) {
        int manquants = stockInitial - Long.bitCount(plateau.bitboard(piece));
        for (int i = 0; i < manquants; i++) liste.add(piece);
    }

    // ── Record interne ───────────────────────────────────────────────────────

    private record EntreeHistorique(EtatPlateau etatAvant, Coup coup, String notationSAN) {}
}