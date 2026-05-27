package fr.moteurechecs.ia.evaluation;

import fr.moteurechecs.plateau.Case;
import fr.moteurechecs.plateau.Coup;
import fr.moteurechecs.plateau.Couleur;
import fr.moteurechecs.plateau.Piece;
import fr.moteurechecs.plateau.Plateau;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Guide l'IA pendant l'ouverture quand le livre de coups est épuisé.
 *
 * Classe les coups légaux selon trois principes fondamentaux :
 * - Contrôle du centre
 * - Développement des pièces mineures
 * - Sécurité du roi
 */
public final class PrincipesOuverture {

    private static volatile PrincipesOuverture instance;

    private static final int BONUS_CENTRE        = 40;
    private static final int BONUS_DEVELOPPEMENT = 35;
    private static final int BONUS_SECURITE_ROI  = 30;

    // séquences d'ouvertures connues (clé = coups joués, valeur = suites recommandées)
    private final Map<String, List<String>> patternsOuvertures;

    private PrincipesOuverture() {
        this.patternsOuvertures = new HashMap<>();
        chargerPatterns();
    }

    /**
     * Retourne l'instance singleton.
     *
     * @return instance partagée de {@code PrincipesOuverture}
     */
    public static PrincipesOuverture getInstance() {
        if (instance == null) {
            synchronized (PrincipesOuverture.class) {
                if (instance == null) {
                    instance = new PrincipesOuverture();
                }
            }
        }
        return instance;
    }

    /**
     * Retourne le meilleur coup selon les principes d'ouverture.
     *
     * @param plateau la position courante
     * @param coups   liste des coups légaux
     * @return le coup le mieux classé, ou {@code null} si la liste est vide
     */
    public Coup getMeilleurCoupPrincipes(Plateau plateau, List<Coup> coups) {
        if (coups == null || coups.isEmpty()) return null;
        List<Coup> coupsTries = evaluerCoups(plateau, coups);
        return coupsTries.isEmpty() ? null : coupsTries.get(0);
    }

    /**
     * Trie les coups légaux selon les principes d'ouverture (meilleur en premier).
     *
     * @param plateau la position courante
     * @param coups   liste des coups légaux
     * @return nouvelle liste triée par score décroissant
     */
    public List<Coup> evaluerCoups(Plateau plateau, List<Coup> coups) {
        if (coups == null || coups.isEmpty()) return coups;

        List<CoupScore> coupsAvecScore = new ArrayList<>(coups.size());
        for (Coup coup : coups) {
            coupsAvecScore.add(new CoupScore(coup, calculerScoreCoup(plateau, coup)));
        }
        coupsAvecScore.sort((a, b) -> Integer.compare(b.score, a.score));

        List<Coup> resultat = new ArrayList<>(coupsAvecScore.size());
        for (CoupScore cs : coupsAvecScore) {
            resultat.add(cs.coup);
        }
        return resultat;
    }

    /**
     * Retourne true si la position est encore en phase d'ouverture.
     *
     * @param plateau la position à tester
     * @return true si au moins deux pièces mineures ne sont pas développées
     */
    public static boolean estPhaseOuverture(Plateau plateau) {
        long cavaliers = plateau.bitboard(Piece.CAVALIER_BLANC) | plateau.bitboard(Piece.CAVALIER_NOIR);
        long fous      = plateau.bitboard(Piece.FOU_BLANC)      | plateau.bitboard(Piece.FOU_NOIR);

        long casesDepart = 0x0000000000000044L | 0x4400000000000000L;
        long fouDepart   = 0x0000000000000030L | 0x3000000000000000L;

        return (Long.bitCount(cavaliers & casesDepart) + Long.bitCount(fous & fouDepart)) >= 2;
    }

    // score global d'un coup selon les trois principes
    private int calculerScoreCoup(Plateau plateau, Coup coup) {
        Plateau apres = plateau.copie();
        apres.jouer(coup);
        Couleur trait = plateau.trait();

        return evaluerPrincipeCentre(apres, trait)
             + evaluerPrincipeDeveloppement(apres, trait)
             + evaluerPrincipeSecuriteRoi(apres, trait)
             + evaluerPatternConnu(coup)
             + evaluerDeveloppementPiece(plateau, coup)
             - evaluerBloquage(plateau, coup);
    }

    // bonus pour le contrôle du centre après le coup
    private int evaluerPrincipeCentre(Plateau apres, Couleur trait) {
        long centre        = 0x0000001818000000L;
        long centreEtendu  = 0x0000003C3C000000L;
        long pions = (trait == Couleur.BLANC)
            ? apres.bitboard(Piece.PION_BLANC)
            : apres.bitboard(Piece.PION_NOIR);

        int score = Long.bitCount(pions & centreEtendu) * BONUS_CENTRE
                  + Long.bitCount(pions & centre) * BONUS_CENTRE;

        long cavaliers = (trait == Couleur.BLANC)
            ? apres.bitboard(Piece.CAVALIER_BLANC)
            : apres.bitboard(Piece.CAVALIER_NOIR);

        long[] casesCentre = (trait == Couleur.BLANC)
            ? new long[]{0x0000000000000400L, 0x0000000000000800L,
                         0x0000000004000000L, 0x0000000008000000L}
            : new long[]{0x0400000000000000L, 0x0800000000000000L,
                         0x0004000000000000L, 0x0008000000000000L};

        for (long c : casesCentre) {
            if ((cavaliers & c) != 0) score += BONUS_CENTRE;
        }
        return score;
    }

    // bonus pour le développement des pièces mineures
    private int evaluerPrincipeDeveloppement(Plateau apres, Couleur trait) {
        long casesDepart = (trait == Couleur.BLANC)
            ? 0x0000000000000044L : 0x4400000000000000L;
        long fouDepart = (trait == Couleur.BLANC)
            ? 0x0000000000000030L : 0x3000000000000000L;

        long cavaliers = (trait == Couleur.BLANC)
            ? apres.bitboard(Piece.CAVALIER_BLANC) : apres.bitboard(Piece.CAVALIER_NOIR);
        long fous = (trait == Couleur.BLANC)
            ? apres.bitboard(Piece.FOU_BLANC) : apres.bitboard(Piece.FOU_NOIR);

        return Long.bitCount(cavaliers & ~casesDepart) * BONUS_DEVELOPPEMENT
             + Long.bitCount(fous & ~fouDepart) * BONUS_DEVELOPPEMENT;
    }

    // bonus pour la sécurité du roi
    private int evaluerPrincipeSecuriteRoi(Plateau apres, Couleur trait) {
        int score = 0;
        boolean aRoque = (trait == Couleur.BLANC)
            ? (apres.getRoqueBlancRoi() || apres.getRoqueBlancReine())
            : (apres.getRoqueNoirRoi()  || apres.getRoqueNoirReine());

        if (aRoque) score += BONUS_SECURITE_ROI;

        long roi = (trait == Couleur.BLANC)
            ? apres.bitboard(Piece.ROI_BLANC) : apres.bitboard(Piece.ROI_NOIR);

        if (roi != 0) {
            int indice  = Long.numberOfTrailingZeros(roi);
            int colonne = indice % 8;
            boolean enSecurite = (colonne <= 1 || colonne >= 6);
            score += enSecurite ? BONUS_SECURITE_ROI / 2 : -BONUS_SECURITE_ROI;
        }
        return score;
    }

    // bonus si le coup correspond à un pattern d'ouverture connu
    private int evaluerPatternConnu(Coup coup) {
        String coupStr = coup.depart().versAlgebrique() + coup.arrivee().versAlgebrique();
        int score = 0;
        for (List<String> suites : patternsOuvertures.values()) {
            if (suites.contains(coupStr)) score += 20;
        }
        return score;
    }

    // bonus si le coup développe une pièce vers l'avant
    private int evaluerDeveloppementPiece(Plateau avant, Coup coup) {
        int score = 0;
        Piece piece = coup.pieceDeplacee();

        if (piece == Piece.CAVALIER_BLANC || piece == Piece.CAVALIER_NOIR) {
            long casesAvant = avant.bitboard(piece);
            if (casesAvant != 0) {
                int rangeeAvant = 8 - (Long.numberOfTrailingZeros(casesAvant) / 8);
                int rangeeApres = coup.arrivee().ligne();
                boolean avance = (avant.trait() == Couleur.BLANC)
                    ? rangeeApres > rangeeAvant : rangeeApres < rangeeAvant;
                if (avance) score += 25;
            }
        }
        if (piece == Piece.FOU_BLANC || piece == Piece.FOU_NOIR) score += 20;
        if (piece == Piece.PION_BLANC || piece == Piece.PION_NOIR) {
            int rangee = coup.arrivee().ligne();
            boolean versCentre = (avant.trait() == Couleur.BLANC) ? rangee >= 4 : rangee <= 4;
            if (versCentre) score += 15;
        }
        return score;
    }

    // pénalité si le coup expose la dame prématurément
    private int evaluerBloquage(Plateau avant, Coup coup) {
        Piece piece = coup.pieceDeplacee();
        if (piece != Piece.DAME_BLANCHE && piece != Piece.DAME_NOIRE) return 0;

        Case arrivee = coup.arrivee();
        int rangee = arrivee.ligne();
        boolean tropAvancee = (avant.trait() == Couleur.BLANC) ? rangee >= 4 : rangee <= 4;
        return tropAvancee ? 30 : 0;
    }

    // chargement des patterns d'ouvertures classiques
    private void chargerPatterns() {
        patternsOuvertures.put("e2e4", Arrays.asList(
            "e7e5","c7c5","e7e6","c7c6","g7g6","b7b6","d7d6","f7f5"));
        patternsOuvertures.put("d2d4", Arrays.asList(
            "d7d5","g7g6","e7e6","c7c5","f7f5","b7b6","c7c6","g8f6"));
        patternsOuvertures.put("e2e4_c7c5", Arrays.asList(
            "g1f3","b1c3","d2d4","f1b5","f1c4","d2d3"));
        patternsOuvertures.put("e2e4_e7e6", Arrays.asList(
            "d2d4","b1c3","g1f3","d1g4"));
        patternsOuvertures.put("e2e4_c7c6", Arrays.asList(
            "d2d4","b1c3","g1f3","c2c4"));
        patternsOuvertures.put("e2e4_d7d6", Arrays.asList(
            "d2d4","g1f3","b1c3","f1c4"));
        patternsOuvertures.put("e2e4_d7d5", Arrays.asList(
            "e4d5","b1c3","g1f3"));
        patternsOuvertures.put("d2d4_d7d5_c2c4", Arrays.asList(
            "c4d5","c4b5","c4f5","c4g5"));
        patternsOuvertures.put("e2e4_e7e5_g1f3_b8c6_f1b5", Arrays.asList(
            "a2a4","d2d3","c2c3","a2a3","h2h3"));
        patternsOuvertures.put("e2e4_e7e5_g1f3_b8c6_f1c4", Arrays.asList(
            "d2d3","c2c3","b1c3","h2h3"));
        patternsOuvertures.put("c2c4", Arrays.asList(
            "e5","c5","e6","g6","c6","d5"));
    }

    // paire (coup, score) pour le tri
    private static final class CoupScore {
        final Coup coup;
        final int score;

        CoupScore(Coup coup, int score) {
            this.coup  = coup;
            this.score = score;
        }
    }
}
