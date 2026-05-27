package fr.moteurechecs.ouverture;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parseur de fichiers PGN (Portable Game Notation).
 *
 * Extrait les parties d'un fichier PGN et les convertit en listes de coups SAN.
 * Le parseur est permissif : les parties invalides ou incomplètes sont ignorées
 * sans interrompre le traitement des parties suivantes.
 */
public class AnalyseurPGN {

    /**
     * Parse un fichier PGN et retourne toutes les parties valides.
     *
     * @param chemin chemin vers le fichier PGN
     * @return liste des parties parsées (peut être vide)
     * @throws IOException en cas d'erreur de lecture du fichier
     */
    public static List<PartiePGN> parserFichier(String chemin) throws IOException {
        List<PartiePGN> parties = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(chemin))) {
            StringBuilder contenu = new StringBuilder();
            String ligne;

            while ((ligne = reader.readLine()) != null) {
                contenu.append(ligne).append("\n");
            }

            // découpage du contenu en parties individuelles sur le tag [Event
            String[] partiesStr = contenu.toString().split("\\[Event ");
            for (String partieStr : partiesStr) {
                if (partieStr.trim().isEmpty()) continue;

                PartiePGN partie = parserPartie("[Event " + partieStr);
                if (partie != null && !partie.coups.isEmpty()) {
                    parties.add(partie);
                }
            }
        }

        return parties;
    }

    // parse une seule partie PGN et extrait métadonnées + coups
    private static PartiePGN parserPartie(String pgn) {
        PartiePGN partie = new PartiePGN();

        // extraction des tags de métadonnées
        String[] lignes = pgn.split("\n");
        for (String ligne : lignes) {
            ligne = ligne.trim();
            if (ligne.startsWith("[White "))   { partie.white   = extraireValeur(ligne); }
            else if (ligne.startsWith("[Black "))   { partie.black   = extraireValeur(ligne); }
            else if (ligne.startsWith("[Result "))  { partie.result  = extraireValeur(ligne); }
            else if (ligne.startsWith("[ECO "))     { partie.eco     = extraireValeur(ligne); }
            else if (ligne.startsWith("[Opening ")) { partie.opening = extraireValeur(ligne); }
        }

        // localisation du début des coups (marqueur "1.")
        int debutCoups = pgn.lastIndexOf("1.");
        if (debutCoups == -1) return null;

        String coupsStr = pgn.substring(debutCoups);
        // suppression du résultat final (1-0, 0-1, 1/2-1/2, *)
        coupsStr = coupsStr.replaceAll("\\s*(1-0|0-1|1/2-1/2|\\*)\\s*$", "");

        // extraction des tokens SAN
        String[] tokens   = coupsStr.split("\\s+");
        boolean traitBlanc = true;

        for (String token : tokens) {
            token = token.replaceAll("[^a-hKQRBNO0-9+#=]", "");

            if (token.isEmpty() || token.matches("^\\d+\\.$")) {
                continue;
            }

            try {
                partie.coups.add(token);

                if (token.contains(".")) {
                    // nouveau numéro de coup
                    String[] parties = token.split("\\.");
                    if (parties.length > 1 && !parties[1].trim().isEmpty()) {
                        traitBlanc = true;
                        continue;
                    }
                }

                traitBlanc = !traitBlanc;
            } catch (Exception e) {
                // token invalide : ignoré
            }
        }

        return partie;
    }

    // extrait la valeur entre guillemets d'un tag PGN (ex: [White "Magnus"])
    private static String extraireValeur(String ligne) {
        int debut = ligne.indexOf("\"");
        int fin   = ligne.lastIndexOf("\"");
        if (debut == -1 || fin == -1 || debut == fin) return "";
        return ligne.substring(debut + 1, fin);
    }

    /**
     * Représente une partie PGN parsée avec ses métadonnées et ses coups SAN.
     */
    public static class PartiePGN {
        /** Nom du joueur blanc. */
        public String       white   = "";
        /** Nom du joueur noir. */
        public String       black   = "";
        /** Résultat de la partie. */
        public String       result  = "";
        /** Code ECO de l'ouverture. */
        public String       eco     = "";
        /** Nom de l'ouverture. */
        public String       opening = "";
        /** Liste des coups en notation SAN. */
        public List<String> coups   = new ArrayList<>();

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return String.format("%s vs %s [%s] %s", white, black, eco, opening);
        }
    }
}
