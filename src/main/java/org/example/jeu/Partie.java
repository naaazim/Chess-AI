package org.example.jeu;

import org.example.AI.Niveau;
import org.example.chess.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * <p>
 * {@code Partie} gère une partie complète en console.
 * </p>
 *
 * <p>
 * Au lancement, on demande :
 * </p>
 * <ul>
 * <li>le mode (Humain vs Humain / Humain vs IA / IA vs IA)</li>
 * <li>le niveau de l'IA blanche si une IA joue les Blancs</li>
 * <li>le niveau de l'IA noire si une IA joue les Noirs</li>
 * </ul>
 *
 * <p>
 * Ensuite, boucle :
 * </p>
 * <ul>
 * <li>afficher le plateau</li>
 * <li>choisir le coup (humain ou IA)</li>
 * <li>jouer le coup</li>
 * <li>tester mat/pat</li>
 * </ul>
 */
public final class Partie {

    private Plateau plateau;

    private ModeJeu mode;
    private TypeJoueur blanc;
    private TypeJoueur noir;

    private Niveau niveauIABlanche;
    private Niveau niveauIANoire;

    private Scanner scanner;
    private JoueurHumain joueurHumain;

    private JoueurIA iaBlanche;
    private JoueurIA iaNoire;

    private List<EtatPlateau> historiqueEtats;

    public Partie() {
        // On initialise au lancement
    }

    /**
     * <p>
     * Lance la partie : menu + boucle de jeu.
     * </p>
     */
    public void lancer() {
        this.scanner = new Scanner(System.in);
        this.plateau = Plateau.positionInitiale();
        this.joueurHumain = new JoueurHumain(scanner);

        demanderMode();
        demanderNiveauxIA();

        // Création des IA uniquement si nécessaire
        if (blanc == TypeJoueur.IA) {
            this.iaBlanche = new JoueurIA(niveauIABlanche);
        }
        if (noir == TypeJoueur.IA) {
            this.iaNoire = new JoueurIA(niveauIANoire);
        }

        this.historiqueEtats = new ArrayList<>();
        this.historiqueEtats.add(plateau.sauvegarderEtat());

        boucleJeu();
    }

    private void demanderMode() {
        while (true) {
            System.out.println("Choisis un mode :");
            System.out.println("  1) Humain vs Humain");
            System.out.println("  2) Humain vs IA");
            System.out.println("  3) IA vs IA");
            System.out.print("Ton choix : ");

            String s = scanner.nextLine().trim();

            switch (s) {
                case "1" -> {
                    mode = ModeJeu.HUMAIN_VS_HUMAIN;
                    blanc = TypeJoueur.HUMAIN;
                    noir = TypeJoueur.HUMAIN;
                    return;
                }
                case "2" -> {
                    mode = ModeJeu.HUMAIN_VS_IA;
                    // Ici : Humain = Blanc, IA = Noir (tu peux inverser plus tard si tu veux)
                    blanc = TypeJoueur.HUMAIN;
                    noir = TypeJoueur.IA;
                    return;
                }
                case "3" -> {
                    mode = ModeJeu.IA_VS_IA;
                    blanc = TypeJoueur.IA;
                    noir = TypeJoueur.IA;
                    return;
                }
                default -> System.out.println("Choix invalide.\n");
            }
        }
    }

    /**
     * <p>
     * Demande le niveau pour l'IA blanche et/ou noire, seulement si ces IA
     * existent.
     * </p>
     */
    private void demanderNiveauxIA() {
        if (blanc == TypeJoueur.IA) {
            niveauIABlanche = demanderUnNiveau("blanche");
        }
        if (noir == TypeJoueur.IA) {
            niveauIANoire = demanderUnNiveau("noire");
        }
    }

    private Niveau demanderUnNiveau(String nomCouleur) {
        while (true) {
            System.out.println("\nChoisis le niveau de l'IA " + nomCouleur + " :");
            System.out.println("1) Facile");
            System.out.println("2) Moyen");
            System.out.println("3) Difficile");
            System.out.print("Ton choix : ");

            String s = scanner.nextLine().trim();

            switch (s) {
                case "1" -> {
                    return Niveau.FACILE;
                }
                case "2" -> {
                    return Niveau.MOYEN;
                }
                case "3" -> {
                    return Niveau.DIFFICILE;
                }
                default -> System.out.println("Choix invalide.\n");
            }
        }
    }

    private void boucleJeu() {
        while (true) {
            System.out.println("\n" + plateau);

            FinDePartie fin = verifierFin();
            if (fin != null) {
                afficherFin(fin);
                return;
            }

            Coup coup = choisirCoupPourTrait();
            if (coup == null) {
                System.out.println("Aucun coup possible.");
                return;
            }

            System.out.println("Coup joué : " + coupEnAlgebriqueSimple(coup));
            plateau.jouer(coup);
            historiqueEtats.add(plateau.sauvegarderEtat());
        }
    }

    private Coup choisirCoupPourTrait() {
        Couleur trait = plateau.trait();

        if (trait == Couleur.BLANC) {
            if (blanc == TypeJoueur.HUMAIN) {
                return joueurHumain.demanderCoup(plateau);
            } else {
                return iaBlanche.choisirCoup(plateau);
            }
        } else {
            if (noir == TypeJoueur.HUMAIN) {
                return joueurHumain.demanderCoup(plateau);
            } else {
                return iaNoire.choisirCoup(plateau);
            }
        }
    }

    private FinDePartie verifierFin() {
        if (historiqueEtats != null && !historiqueEtats.isEmpty()) {
            EtatPlateau etatCourant = historiqueEtats.get(historiqueEtats.size() - 1);
            if (Collections.frequency(historiqueEtats, etatCourant) >= 3) {
                return FinDePartie.nulRepetition();
            }
        }

        List<Coup> coupsLegaux = GenerateurCoups.genererLegaux(plateau);
        if (!coupsLegaux.isEmpty())
            return null;

        boolean enEchec = plateau.estEnEchec(plateau.trait());
        if (enEchec) {
            Couleur gagnant = plateau.trait().inverse();
            return FinDePartie.mat(gagnant);
        } else {
            return FinDePartie.pat();
        }
    }

    private void afficherFin(FinDePartie fin) {
        System.out.println("\n" + plateau);
        if (fin.type == FinDePartie.Type.MAT) {
            System.out.println("Échec et mat ! Gagnant : " + fin.gagnant);
        } else if (fin.type == FinDePartie.Type.NUL_REPETITION) {
            System.out.println("🤝 Nul par triple répétition !");
        } else {
            System.out.println("🤝 Pat ! Match nul.");
        }
    }

    private String coupEnAlgebriqueSimple(Coup c) {
        String s = c.depart().versAlgebrique() + c.arrivee().versAlgebrique();
        if (c.estPromotion()) {
            Piece promo = c.piecePromotion();
            char lettre = switch (promo) {
                case DAME_BLANCHE, DAME_NOIRE -> 'q';
                case TOUR_BLANC, TOUR_NOIR -> 'r';
                case FOU_BLANC, FOU_NOIR -> 'b';
                case CAVALIER_BLANC, CAVALIER_NOIR -> 'n';
                default -> '?';
            };
            s += lettre;
        }
        return s;
    }

    private static final class FinDePartie {
        enum Type {
            MAT, PAT, NUL_REPETITION
        }

        final Type type;
        final Couleur gagnant;

        private FinDePartie(Type type, Couleur gagnant) {
            this.type = type;
            this.gagnant = gagnant;
        }

        static FinDePartie mat(Couleur gagnant) {
            return new FinDePartie(Type.MAT, gagnant);
        }

        static FinDePartie pat() {
            return new FinDePartie(Type.PAT, null);
        }

        static FinDePartie nulRepetition() {
            return new FinDePartie(Type.NUL_REPETITION, null);
        }
    }
}
