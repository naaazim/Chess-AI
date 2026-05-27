package fr.moteurechecs.ia;

import fr.moteurechecs.plateau.Couleur;
import fr.moteurechecs.plateau.Coup;
import fr.moteurechecs.plateau.Piece;
import fr.moteurechecs.plateau.Plateau;
import fr.moteurechecs.ia.evaluation.Materiel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Système d'ouvertures structuré avec lignes principales.
 *
 * Priorité d'utilisation : (1) jouer les premiers coups depuis la base,
 * (2) développer les pièces sans perdre de matériel, (3) comparer avec
 * le score alpha-bêta avant de valider, (4) roquer si le score le permet.
 *
 * Le singleton est initialisé de manière paresseuse et thread-safe.
 */
public final class BaseOuverturesIA {

    private static volatile BaseOuverturesIA instance;

    // lignes d'ouvertures : premier coup (UCI) → liste de séquences possibles
    private final Map<String, List<SequenceOuverture>> lignesOuvertures;

    private BaseOuverturesIA() {
        this.lignesOuvertures = new HashMap<>();
        initialiserLignes();
    }

    /**
     * Retourne l'instance singleton (double-checked locking).
     *
     * @return l'instance unique de {@code BaseOuverturesIA}
     */
    public static BaseOuverturesIA getInstance() {
        if (instance == null) {
            synchronized (BaseOuverturesIA.class) {
                if (instance == null) {
                    instance = new BaseOuverturesIA();
                }
            }
        }
        return instance;
    }

    // initialise les lignes d'ouvertures principales pour e4, d4 et c4
    private void initialiserLignes() {
        // ===== BLANCS : Premier coup = e4 =====
        List<SequenceOuverture> e4 = new ArrayList<>();
        e4.add(new SequenceOuverture("e4", "e5", "Partie centrale"));
        e4.add(new SequenceOuverture("e4", "c5", "Sicilienne"));
        e4.add(new SequenceOuverture("e4", "e6", "Française"));
        e4.add(new SequenceOuverture("e4", "c6", "Caro-Kann"));
        e4.add(new SequenceOuverture("e4", "d6", "Pirc/Moderne"));
        e4.add(new SequenceOuverture("e4", "d5", "Scandinave"));
        e4.add(new SequenceOuverture("e4", "g6", "Ongrais"));
        lignesOuvertures.put("e2e4", e4);

        // ===== BLANCS : Premier coup = d4 =====
        List<SequenceOuverture> d4 = new ArrayList<>();
        d4.add(new SequenceOuverture("d4", "d5", "Gambit Dame"));
        d4.add(new SequenceOuverture("d4", "g6", "Indienne"));
        d4.add(new SequenceOuverture("d4", "e6", "Française-Dame"));
        d4.add(new SequenceOuverture("d4", "c5", "Benoni"));
        d4.add(new SequenceOuverture("d4", "f5", "Hollandaise"));
        lignesOuvertures.put("d2d4", d4);

        // ===== BLANCS : Premier coup = c4 =====
        List<SequenceOuverture> c4 = new ArrayList<>();
        c4.add(new SequenceOuverture("c4", "c5", "Anglaise symétrique"));
        c4.add(new SequenceOuverture("c4", "e5", "Anglaise"));
        c4.add(new SequenceOuverture("c4", "e6", "Anglaise reverse"));
        c4.add(new SequenceOuverture("c4", "g6", "Anglaise hypermoderne"));
        lignesOuvertures.put("c2c4", c4);
    }

    /**
     * Représente une séquence d'ouverture avec le coup principal et la réponse attendue.
     */
    private static class SequenceOuverture {
        String coup;
        String reponseAttendue;
        String nom;

        SequenceOuverture(String coup, String reponseAttendue, String nom) {
            this.coup             = coup;
            this.reponseAttendue  = reponseAttendue;
            this.nom              = nom;
        }
    }

    /**
     * Trouve le meilleur coup d'ouverture selon le numéro de demi-coup.
     *
     * @param plateau      position courante
     * @param coupsLegaux  liste des coups légaux disponibles
     * @param plyNumber    numéro du demi-coup courant (0 = premier coup)
     * @return le coup d'ouverture choisi, ou {@code null} si inapplicable
     */
    public Coup getCoupOuverture(Plateau plateau, List<Coup> coupsLegaux, int plyNumber) {
        if (plyNumber > 4) {
            return null;
        }

        // au premier coup : jouer un coup de centre
        if (plyNumber == 0) {
            return getCoupPremier(plateau, coupsLegaux);
        }

        return null;
    }

    // joue le premier coup en priorité : e4 > d4 > c4 > f4
    private Coup getCoupPremier(Plateau plateau, List<Coup> coupsLegaux) {
        String[] priorites = {"e4", "d4", "c4", "f4"};
        for (String cible : priorites) {
            for (Coup coup : coupsLegaux) {
                if (coup.pieceDeplacee() == Piece.PION_BLANC
                        && coup.arrivee().versAlgebrique().equals(cible)) {
                    return coup;
                }
            }
        }
        return null;
    }

    /**
     * Vérifie si un coup de développement est sûr (ne perd pas de matériel).
     *
     * @param plateau        position courante
     * @param coup           coup de développement à tester
     * @param alphaBetaScore score de référence issu de la recherche alpha-bêta
     * @return {@code true} si le coup ne dégrade pas significativement la position
     */
    public boolean estCoupDeveloppementSur(Plateau plateau, Coup coup, int alphaBetaScore) {
        Plateau copie = plateau.copie();
        copie.jouer(coup);

        // coup illégal s'il laisse le roi en échec
        if (copie.estEnEchec(plateau.trait())) {
            return false;
        }

        int matAvant = Materiel.scoreMateriel(plateau);
        int matApres = Materiel.scoreMateriel(copie);

        // rejeter si le coup cause une perte de matériel
        if ((plateau.trait() == Couleur.BLANC && matApres < matAvant)
                || (plateau.trait() == Couleur.NOIR && matApres > matAvant)) {
            return false;
        }

        int scoreApres = Evaluation.evaluer(copie);

        // accepter si le score reste dans une marge acceptable par rapport à alpha-bêta
        if (plateau.trait() == Couleur.BLANC) {
            return scoreApres > alphaBetaScore - 50;
        } else {
            return scoreApres < alphaBetaScore + 50;
        }
    }

    /**
     * Détermine si le joueur devrait roquer dans la position actuelle.
     *
     * @param plateau position courante
     * @param couleur couleur du joueur
     * @return {@code true} si le roque est encore possible et le roi n'est pas en sécurité
     */
    public boolean doitRoquer(Plateau plateau, Couleur couleur) {
        boolean peutRoquerRoi;
        boolean peutRoquerDame;

        // vérification des droits de roque selon la couleur
        if (couleur == Couleur.BLANC) {
            peutRoquerRoi  = plateau.getRoqueBlancRoi();
            peutRoquerDame = plateau.getRoqueBlancReine();
        } else {
            peutRoquerRoi  = plateau.getRoqueNoirRoi();
            peutRoquerDame = plateau.getRoqueNoirReine();
        }

        if (!peutRoquerRoi && !peutRoquerDame) {
            return false;
        }

        if (plateau.estEnEchec(couleur)) {
            return false;
        }

        long bb = plateau.bitboard(couleur == Couleur.BLANC ? Piece.ROI_BLANC : Piece.ROI_NOIR);
        if (bb == 0) return false;

        int indice = Long.numberOfTrailingZeros(bb);
        int rangee = 8 - (indice / 8);

        // le roi a bougé si sa rangée ne correspond plus à la rangée de départ
        if (couleur == Couleur.BLANC && rangee > 1)  return true;
        if (couleur == Couleur.NOIR  && rangee < 6)  return true;

        return false;
    }

    /**
     * Trouve le meilleur coup de développement sûr parmi les coups légaux.
     *
     * @param plateau        position courante
     * @param coupsLegaux    liste des coups légaux disponibles
     * @param alphaBetaScore score de référence issu de la recherche alpha-bêta
     * @return le coup de développement recommandé, ou {@code null} si aucun n'est sûr
     */
    public Coup getCoupDeveloppementSur(Plateau plateau, List<Coup> coupsLegaux, int alphaBetaScore) {
        Couleur trait = plateau.trait();

        // filtrer les coups de développement candidates
        List<Coup> coupsDeveloppement = new ArrayList<>();
        for (Coup coup : coupsLegaux) {
            if (estCoupDeveloppement(coup, trait)) {
                coupsDeveloppement.add(coup);
            }
        }

        int meilleurScore = trait == Couleur.BLANC ? Integer.MIN_VALUE : Integer.MAX_VALUE;
        Coup meilleurCoup = null;

        for (Coup coup : coupsDeveloppement) {
            Plateau copie = plateau.copie();
            copie.jouer(coup);

            if (copie.estEnEchec(trait)) {
                continue;
            }

            int matAvant = Materiel.scoreMateriel(plateau);
            int matApres = Materiel.scoreMateriel(copie);

            // rejeter les coups qui perdent du matériel
            if ((trait == Couleur.BLANC && matApres < matAvant)
                    || (trait == Couleur.NOIR && matApres > matAvant)) {
                continue;
            }

            int score = Evaluation.evaluer(copie);

            // vérifier que le coup reste dans la marge acceptable vis-à-vis d'alpha-bêta
            boolean meilleurQueAlphaBeta = (trait == Couleur.BLANC)
                    ? (score > alphaBetaScore - 30)
                    : (score < alphaBetaScore + 30);

            if (meilleurQueAlphaBeta) {
                if (trait == Couleur.BLANC && score > meilleurScore) {
                    meilleurScore = score;
                    meilleurCoup  = coup;
                } else if (trait == Couleur.NOIR && score < meilleurScore) {
                    meilleurScore = score;
                    meilleurCoup  = coup;
                }
            }
        }

        return meilleurCoup;
    }

    // vérifie si un coup constitue un coup de développement au sens large
    private boolean estCoupDeveloppement(Coup coup, Couleur couleur) {
        Piece piece = coup.pieceDeplacee();

        // pion qui avance vers le centre (rangées 4–5 pour blancs, 3–4 pour noirs)
        if (piece == Piece.PION_BLANC || piece == Piece.PION_NOIR) {
            int rangee = coup.arrivee().ligne();
            if (couleur == Couleur.BLANC && rangee >= 4 && rangee <= 5) return true;
            if (couleur == Couleur.NOIR  && rangee >= 3 && rangee <= 4) return true;
        }

        // cavalier développé vers une case centrale
        if (piece == Piece.CAVALIER_BLANC || piece == Piece.CAVALIER_NOIR) {
            String arrivee = coup.arrivee().versAlgebrique();
            if (couleur == Couleur.BLANC) {
                return arrivee.equals("f3") || arrivee.equals("g3") || arrivee.equals("e2")
                        || arrivee.equals("d3") || arrivee.equals("c3");
            } else {
                return arrivee.equals("f6") || arrivee.equals("g6") || arrivee.equals("e7")
                        || arrivee.equals("d6") || arrivee.equals("c6");
            }
        }

        // tout mouvement de fou est un développement
        if (piece == Piece.FOU_BLANC || piece == Piece.FOU_NOIR) {
            return true;
        }

        // le roque est aussi un coup de développement (sécurité du roi)
        if (coup.estRoque()) {
            return true;
        }

        return false;
    }
}
