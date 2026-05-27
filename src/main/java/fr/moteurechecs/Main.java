package fr.moteurechecs;

import fr.moteurechecs.gui.MainFX;
import fr.moteurechecs.jeu.Partie;

import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Point d'entrée principal de DEEP BLACK.
 *
 * Affiche la bannière ASCII, propose le choix entre CLI et GUI, puis
 * lance le mode sélectionné par l'utilisateur.
 */
public class Main {

    // ─────────────────────────────────────────────
    //  ASCII art — DEEP BLACK (sur une seule ligne)
    // ─────────────────────────────────────────────
    static final String[] BANNER = {
            "██████╗ ███████╗███████╗██████╗     ██████╗ ██╗      █████╗  ██████╗██╗  ██╗",
            "██╔══██╗██╔════╝██╔════╝██╔══██╗    ██╔══██╗██║     ██╔══██╗██╔════╝██║ ██╔╝",
            "██║  ██║█████╗  █████╗  ██████╔╝    ██████╔╝██║     ███████║██║     █████╔╝ ",
            "██║  ██║██╔══╝  ██╔══╝  ██╔═══╝     ██╔══██╗██║     ██╔══██║██║     ██╔═██╗ ",
            "██████╔╝███████╗███████╗██║         ██████╔╝███████╗██║  ██║╚██████╗██║  ██╗",
            "╚═════╝ ╚══════╝╚══════╝╚═╝         ╚═════╝ ╚══════╝╚═╝  ╚═╝ ╚═════╝╚═╝  ╚═╝"
    };

    static final int BANNER_WIDTH = 80;

    // ─────────────────────────────────────────────
    //  Point d'entrée
    // ─────────────────────────────────────────────
    public static void main(String[] args) throws InterruptedException {
        printBanner();
        printMenu();

        int choix = readChoice();

        System.out.println();
        switch (choix) {
            case 1 -> {
                printInfo("Démarrage du mode CLI...");
                Thread.sleep(400);
                Partie partie = new Partie();
                partie.lancer();
            }
            case 2 -> {
                printInfo("Démarrage du mode GUI...");
                Thread.sleep(400);
                MainFX.main(args);
            }
            default -> printError("Choix invalide. Veuillez relancer et entrer 1 ou 2.");
        }
    }

    // ─────────────────────────────────────────────
    //  Affichage de la bannière ASCII
    // ─────────────────────────────────────────────
    static void printBanner() throws InterruptedException {
        System.out.println();
        System.out.println("  ╔" + "═".repeat(BANNER_WIDTH) + "╗");
        System.out.println("  ║" + " ".repeat(BANNER_WIDTH) + "║");
        for (String ligneBanniere : BANNER) {
            System.out.println("  ║  " + ligneBanniere + "  ║");
            Thread.sleep(55);
        }
        System.out.println("  ║" + " ".repeat(BANNER_WIDTH) + "║");
        System.out.println("  ╚" + "═".repeat(BANNER_WIDTH) + "╝");
        System.out.println();
        Thread.sleep(100);
        System.out.println();
    }

    // ─────────────────────────────────────────────
    //  Menu de sélection
    // ─────────────────────────────────────────────
    static void printMenu() throws InterruptedException {
        Thread.sleep(150);
        System.out.println("  ┌─ Choisissez votre interface ───────────────────────────────────────┐");
        System.out.println("  │                                                                    │");
        System.out.println("  │    1  ›  CLI  —  Command Line Interface                            │");
        System.out.println("  │    2  ›  GUI  —  Graphic User Interface                            │");
        System.out.println("  │                                                                    │");
        System.out.println("  └────────────────────────────────────────────────────────────────────┘");
        System.out.println();
    }

    // ─────────────────────────────────────────────
    //  Saisie interactive (prompt style CLI)
    // ─────────────────────────────────────────────

    /**
     * Affiche un prompt style agent CLI et retourne le choix entré par l'utilisateur.
     * Boucle jusqu'à ce qu'un entier valide soit saisi.
     *
     * @return entier correspondant au choix de l'utilisateur
     */
    static int readChoice() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("  deep-black  ❯  ");
            String saisie = scanner.nextLine().trim();
            try {
                return Integer.parseInt(saisie);
            } catch (NumberFormatException e) {
                printError("Entrée invalide. Veuillez taper 1 ou 2.");
                System.out.println();
            }
        }
    }

    // ─────────────────────────────────────────────
    //  Messages utilitaires
    // ─────────────────────────────────────────────

    static void printInfo(String msg) {
        System.out.println("  [  ›  ] " + msg);
    }

    static void printError(String msg) {
        System.out.println("  [  ✕  ] " + msg);
    }


}