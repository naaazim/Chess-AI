package fr.moteurechecs.ouverture;

import fr.moteurechecs.plateau.Case;
import fr.moteurechecs.plateau.Couleur;
import fr.moteurechecs.plateau.Coup;
import fr.moteurechecs.plateau.GenerateurCoups;
import fr.moteurechecs.plateau.Piece;
import fr.moteurechecs.plateau.Plateau;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Constructeur du livre d'ouvertures binaire depuis un fichier PGN Lichess.
 *
 * Lit le PGN en streaming, convertit chaque token SAN en coup légal, compte
 * la fréquence par position et écrit un fichier {@code .book} binaire conservant
 * les {@code TOP_K} meilleurs coups par position. Les parties dont le SAN ne peut
 * être parsé sont partiellement importées jusqu'au premier coup invalide.
 */
public class ConstructeurLivreOuvertures {

    // stockage : hashPositionKey → liste de (coup + fréquence + nom)
    private static final Map<Long, List<BookEntry>> POSITIONS = new HashMap<>();

    private static int MIN_ELO  = 0;
    private static int MAX_PLY  = 16;
    private static int TOP_K    = 6; // nombre maximal de coups conservés par position

    // ===== Compteurs pour les logs =====
    private static int partiesLues          = 0;
    private static int partiesImportees     = 0;
    private static int partiesRejeteesSAN   = 0;
    private static int coupsTotal           = 0;
    private static int positionsUniques     = 0;

    /**
     * Point d'entrée principal : génère un fichier {@code .book} depuis un PGN.
     *
     * @param args args[0] = fichier PGN, args[1] = fichier de sortie,
     *             args[2] = elo minimum (optionnel), args[3] = profondeur max (optionnel)
     * @throws Exception en cas d'erreur de lecture/écriture
     */
    public static void main(String[] args) throws Exception {
        reinitialiserEtat();

        if (args.length < 2) {
            System.out.println("Usage: java ConstructeurLivreOuvertures <input.pgn> <output.book> [minElo] [maxPly]");
            System.out.println("  minElo: Elo minimum (defaut: 0)");
            System.out.println("  maxPly: Profondeur max (defaut: 16)");
            System.exit(1);
        }

        String pgnPath  = args[0];
        String bookPath = args[1];

        if (args.length >= 3) MIN_ELO = Integer.parseInt(args[2]);
        if (args.length >= 4) MAX_PLY = Integer.parseInt(args[3]);

        System.out.println("=== Opening Book Builder ===");
        System.out.println("Input: "   + pgnPath);
        System.out.println("Output: "  + bookPath);
        System.out.println("Min Elo: " + MIN_ELO);
        System.out.println("Max Ply: " + MAX_PLY);

        parserPGN(pgnPath);
        positionsUniques = POSITIONS.size();
        ecrireBook(bookPath);

        System.out.println("\n=== Résumé ===");
        System.out.println("Parties lues: "          + partiesLues);
        System.out.println("Parties importées: "     + partiesImportees);
        System.out.println("Parties rejetées (SAN): " + partiesRejeteesSAN);
        System.out.println("Coups total: "           + coupsTotal);
        System.out.println("Positions uniques: "     + positionsUniques);
    }

    // réinitialise les compteurs et la map entre deux exécutions
    private static void reinitialiserEtat() {
        POSITIONS.clear();
        partiesLues        = 0;
        partiesImportees   = 0;
        partiesRejeteesSAN = 0;
        coupsTotal         = 0;
        positionsUniques   = 0;
    }

    // lit le fichier PGN en streaming et traite chaque partie
    private static void parserPGN(String pgnPath) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(pgnPath), StandardCharsets.UTF_8))) {

            StringBuilder partieEnCours = new StringBuilder(8192);
            String ligne;
            int    nombreParties = 0;
            boolean partieADesCoups = false;

            while ((ligne = reader.readLine()) != null) {
                // un nouveau tag [Event marque le début de la partie suivante
                if (ligne.startsWith("[Event ")
                        && !partieEnCours.isEmpty()
                        && partieADesCoups) {
                    partiesLues++;
                    if (traiterPartie(partieEnCours.toString())) {
                        partiesImportees++;
                    } else {
                        partiesRejeteesSAN++;
                    }
                    nombreParties++;
                    if (nombreParties % 10000 == 0) {
                        System.out.println("... " + nombreParties + " parties traitées");
                    }
                    partieEnCours.setLength(0);
                    partieADesCoups = false;
                }

                if (ligne.isBlank() && partieEnCours.isEmpty()) {
                    continue;
                }

                partieEnCours.append(ligne).append('\n');
                if (!ligne.isBlank() && !ligne.startsWith("[")) {
                    partieADesCoups = true;
                }
            }

            // traiter la dernière partie du fichier
            if (!partieEnCours.isEmpty() && partieADesCoups) {
                partiesLues++;
                if (traiterPartie(partieEnCours.toString())) {
                    partiesImportees++;
                } else {
                    partiesRejeteesSAN++;
                }
            }
        }
        System.out.println("Parties lues: " + partiesLues);
    }

    /**
     * Traite une partie PGN individuelle.
     *
     * @param pgn texte PGN de la partie
     * @return {@code true} si au moins un coup d'ouverture a été importé
     */
    private static boolean traiterPartie(String pgn) {
        Map<String, String> tags = extraireTags(pgn);

        // filtrage par elo minimum
        int eloBlans  = parseElo(tags.get("WhiteElo"));
        int eloNoirs  = parseElo(tags.get("BlackElo"));
        if (eloBlans < MIN_ELO || eloNoirs < MIN_ELO) {
            return false;
        }

        // nom de l'ouverture depuis les tags (Opening > ECO > Unknown)
        String nomOuverture = tags.get("Opening");
        if (nomOuverture == null) nomOuverture = tags.get("ECO");
        if (nomOuverture == null) nomOuverture = "Unknown";

        Plateau  plateau            = Plateau.positionInitiale();
        String[] tokens             = extraireCoups(pgn);
        boolean  aImporteDesCoups   = false;
        int      ply                = 0;

        for (String san : tokens) {
            if (ply >= MAX_PLY) break;

            san = nettoyerSAN(san);
            if (san.isEmpty()) continue;

            List<Coup> legaux = GenerateurCoups.genererLegaux(plateau);
            if (legaux.isEmpty()) break;

            Coup coup = convertirSAN(san, plateau, legaux);
            if (coup == null) break; // SAN non parsable : on conserve ce qui a déjà été importé

            long hashKey = ClePosition.fromPlateau(plateau).toHashKey();
            ajouterCoup(hashKey, coup, nomOuverture);

            plateau.jouer(coup);
            ply++;
            coupsTotal++;
            aImporteDesCoups = true;
        }

        return aImporteDesCoups;
    }

    // enregistre ou incrémente un coup dans la map de positions
    private static void ajouterCoup(long hashKey, Coup coup, String nomOuverture) {
        List<BookEntry> entrees = POSITIONS.computeIfAbsent(hashKey, k -> new ArrayList<>());

        // recherche d'un coup identique existant
        for (BookEntry entree : entrees) {
            if (entree.fromSquare  == coup.depart().indice()
                    && entree.toSquare == coup.arrivee().indice()
                    && entree.promoPiece == (coup.estPromotion() ? getPromoCode(coup) : 0)) {
                entree.frequency++;
                return;
            }

            // mise à jour du nom si plus fréquent
            if (entree.frequency > entrees.get(0).frequency) {
                entree.openingName = nomOuverture;
            }
        }

        // nouveau coup : création de l'entrée
        BookEntry nouvelleEntree = new BookEntry();
        nouvelleEntree.fromSquare  = (byte) coup.depart().indice();
        nouvelleEntree.toSquare    = (byte) coup.arrivee().indice();
        nouvelleEntree.promoPiece  = (byte) (coup.estPromotion() ? getPromoCode(coup) : 0);
        nouvelleEntree.frequency   = 1;
        nouvelleEntree.openingName = nomOuverture;
        entrees.add(nouvelleEntree);

        // garder uniquement les TOP_K meilleurs coups par position
        if (entrees.size() > TOP_K) {
            entrees.sort((a, b) -> Integer.compare(b.frequency, a.frequency));
            entrees.remove(entrees.size() - 1);
        }
    }

    // convertit un token SAN en coup légal via correspondance case d'arrivée + désambiguïsation
    private static Coup convertirSAN(String san, Plateau plateau, List<Coup> legaux) {
        if (legaux.isEmpty()) return null;

        String normalise = normaliserSanPourMatching(san);
        if (normalise.isEmpty()) return null;

        // ===== cas spéciaux : roques =====
        if (normalise.equals("O-O"))   return trouverRoque(legaux, plateau.trait(), true);
        if (normalise.equals("O-O-O")) return trouverRoque(legaux, plateau.trait(), false);

        // extraction de la promotion (ex: e8=Q)
        char promotionChar = 0;
        int  indicEq       = normalise.indexOf('=');
        if (indicEq >= 0) {
            if (indicEq + 1 < normalise.length()) {
                promotionChar = Character.toUpperCase(normalise.charAt(indicEq + 1));
            }
            normalise = normalise.substring(0, indicEq);
        }

        if (normalise.length() < 2) return null;

        String destination = normalise.substring(normalise.length() - 2);
        if (!estCaseAlgebrique(destination)) return null;

        String prefixe   = normalise.substring(0, normalise.length() - 2);
        char   pieceChar = 'P';

        if (!prefixe.isEmpty()) {
            char premier = prefixe.charAt(0);
            if ("KQRBN".indexOf(premier) >= 0) {
                pieceChar = premier;
                prefixe   = prefixe.substring(1);
            }
        }

        boolean captureAnnoncee  = prefixe.indexOf('x') >= 0;
        String  desambiguation   = prefixe.replace("x", "");

        // tentative 1 : matching strict (capture + promotion + désambiguïsation)
        for (Coup c : legaux) {
            if (getPieceChar(c.pieceDeplacee()) != pieceChar)                 continue;
            if (!c.arrivee().versAlgebrique().equalsIgnoreCase(destination))  continue;
            if (captureAnnoncee && !c.estCapture())                           continue;
            if (!matchPromotion(c, promotionChar))                            continue;
            if (!matchDesambiguation(c.depart(), desambiguation))             continue;
            return c;
        }

        // tentative 2 : repli permissif (SAN non standard)
        for (Coup c : legaux) {
            if (getPieceChar(c.pieceDeplacee()) != pieceChar)                continue;
            if (!c.arrivee().versAlgebrique().equalsIgnoreCase(destination)) continue;
            if (!matchPromotion(c, promotionChar))                           continue;
            if (!matchDesambiguation(c.depart(), desambiguation))            continue;
            return c;
        }

        return null;
    }

    // trouve le coup de roque correspondant dans la liste légale
    private static Coup trouverRoque(List<Coup> legaux, Couleur trait, boolean petitRoque) {
        for (Coup c : legaux) {
            if (!c.estRoque()) continue;
            int arrivee = c.arrivee().indice();
            if (trait == Couleur.BLANC) {
                if (petitRoque  && arrivee == 62) return c; // e1→g1
                if (!petitRoque && arrivee == 58) return c; // e1→c1
            } else {
                if (petitRoque  && arrivee == 6)  return c; // e8→g8
                if (!petitRoque && arrivee == 2)  return c; // e8→c8
            }
        }
        return null;
    }

    // normalise un token SAN (variantes de roque, annotations, en passant)
    private static String normaliserSanPourMatching(String san) {
        if (san == null) return "";
        san = san.trim();
        if (san.isEmpty()) return "";

        // unification des variantes de notation du roque
        san = san.replace("0-0-0", "O-O-O").replace("0-0", "O-O");

        // suppression des annotations finales (+, #, ?, !)
        while (!san.isEmpty()) {
            char dernier = san.charAt(san.length() - 1);
            if (dernier == '+' || dernier == '#' || dernier == '!' || dernier == '?') {
                san = san.substring(0, san.length() - 1);
            } else {
                break;
            }
        }

        san = san.replace("e.p.", "").replace("e.p", "").replace(" ", "");
        return san;
    }

    // vérifie si une chaîne est une case algébrique valide (ex: "e4")
    private static boolean estCaseAlgebrique(String s) {
        if (s == null || s.length() != 2) return false;
        char col  = Character.toLowerCase(s.charAt(0));
        char rang = s.charAt(1);
        return col >= 'a' && col <= 'h' && rang >= '1' && rang <= '8';
    }

    // vérifie que la promotion du coup correspond au caractère attendu
    private static boolean matchPromotion(Coup coup, char promotionChar) {
        if (promotionChar == 0) {
            return !coup.estPromotion();
        }
        if (!coup.estPromotion() || coup.piecePromotion() == null) {
            return false;
        }
        return getPieceChar(coup.piecePromotion()) == promotionChar;
    }

    // vérifie que la case de départ satisfait la désambiguïsation SAN
    private static boolean matchDesambiguation(Case depart, String desambiguation) {
        if (desambiguation == null || desambiguation.isEmpty()) {
            return true;
        }

        String dep = depart.versAlgebrique();
        if (desambiguation.length() == 1) {
            char d = Character.toLowerCase(desambiguation.charAt(0));
            if (d >= 'a' && d <= 'h') return dep.charAt(0) == d;
            if (d >= '1' && d <= '8') return dep.charAt(1) == d;
            return false;
        }

        if (desambiguation.length() == 2) {
            return dep.equalsIgnoreCase(desambiguation);
        }

        // SAN long non standard (ex: b8d7 → désambiguïsation = b8)
        String queue = desambiguation.substring(desambiguation.length() - 2);
        if (estCaseAlgebrique(queue)) {
            return dep.equalsIgnoreCase(queue);
        }
        return dep.startsWith(desambiguation.substring(0, 1).toLowerCase());
    }

    // nettoie un token SAN (numéros de coup, résultats, espaces)
    private static String nettoyerSAN(String san) {
        san = san.trim();
        san = san.replaceAll("^\\d+\\.+\\*?", "");
        san = san.replaceAll("1-0", "").replaceAll("0-1", "").replaceAll("1/2-1/2", "");
        san = san.replaceAll("\\*", "");
        return san.trim();
    }

    // extrait les tags PGN (clé → valeur) depuis le texte d'une partie
    private static Map<String, String> extraireTags(String pgn) {
        Map<String, String> tags = new HashMap<>();
        String[] lignes = pgn.split("\n");
        for (String ligne : lignes) {
            ligne = ligne.trim();
            if (ligne.startsWith("[") && ligne.endsWith("]")) {
                int indicEspace = ligne.indexOf(" ");
                int indicFin    = ligne.lastIndexOf("]");
                if (indicEspace > 0 && indicFin > indicEspace) {
                    String cle    = ligne.substring(1, indicEspace);
                    String valeur = ligne.substring(indicEspace + 1, indicFin).replace("\"", "");
                    tags.put(cle, valeur);
                }
            }
        }
        return tags;
    }

    // extrait les tokens de coups depuis le texte PGN (après les tags)
    private static String[] extraireCoups(String pgn) {
        int indice = pgn.indexOf("1.");
        if (indice < 0) return new String[0];

        String coupsStr = pgn.substring(indice);
        coupsStr = coupsStr.replaceAll("\\s*(1-0|0-1|1/2-1/2|\\*)\\s*$", "");
        return coupsStr.split("\\s+");
    }

    // parse un elo ("?" → 0, valeur invalide → 0)
    private static int parseElo(String elo) {
        if (elo == null || elo.equals("?")) return 0;
        try {
            return Integer.parseInt(elo);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // écrit le fichier binaire .book avec toutes les positions et coups collectés
    private static void ecrireBook(String path) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(path)))) {

            dos.writeInt(1);                  // version
            dos.writeInt(POSITIONS.size());   // nombre de positions

            int nombreEntrees = 0;

            for (Map.Entry<Long, List<BookEntry>> e : POSITIONS.entrySet()) {
                List<BookEntry> entrees = e.getValue();
                if (entrees.isEmpty()) continue;

                // tri par fréquence décroissante avant écriture
                entrees.sort((a, b) -> Integer.compare(b.frequency, a.frequency));

                for (BookEntry entree : entrees) {
                    dos.writeLong(e.getKey());
                    dos.writeByte(entree.fromSquare);
                    dos.writeByte(entree.toSquare);
                    dos.writeByte(entree.promoPiece);
                    dos.writeUTF(entree.openingName != null ? entree.openingName : "Unknown");
                    dos.writeInt(entree.frequency);
                    nombreEntrees++;
                }
            }

            System.out.println("Entrées écrites: " + nombreEntrees);
        }
    }

    // retourne le caractère SAN d'une pièce (K/Q/R/B/N/P)
    private static char getPieceChar(Piece p) {
        if (p == null) return '?';
        return switch (p) {
            case ROI_BLANC,      ROI_NOIR      -> 'K';
            case DAME_BLANCHE,   DAME_NOIRE    -> 'Q';
            case TOUR_BLANC,     TOUR_NOIRE    -> 'R';
            case FOU_BLANC,      FOU_NOIR      -> 'B';
            case CAVALIER_BLANC, CAVALIER_NOIR -> 'N';
            case PION_BLANC,     PION_NOIR     -> 'P';
            default -> '?';
        };
    }

    // retourne le code de promotion d'un coup (1=Dame, 2=Tour, 3=Fou, 4=Cavalier)
    private static int getPromoCode(Coup c) {
        if (!c.estPromotion()) return 0;
        Piece p = c.piecePromotion();
        if (p == null) return 0;
        return switch (p) {
            case DAME_BLANCHE,   DAME_NOIRE    -> 1;
            case TOUR_BLANC,     TOUR_NOIRE    -> 2;
            case FOU_BLANC,      FOU_NOIR      -> 3;
            case CAVALIER_BLANC, CAVALIER_NOIR -> 4;
            default -> 0;
        };
    }

    // entrée interne pour stocker coup + métadonnées avant écriture dans le .book
    private static class BookEntry {
        byte   fromSquare;
        byte   toSquare;
        byte   promoPiece;
        int    frequency        = 1;
        String openingName      = "Unknown";
        int    recalculatedScore = 0; // score avec heuristiques (réservé)
    }
}
