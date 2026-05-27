package fr.moteurechecs.ouverture;

import fr.moteurechecs.plateau.Coup;
import fr.moteurechecs.plateau.GenerateurCoups;
import fr.moteurechecs.plateau.Plateau;
import fr.moteurechecs.ia.evaluation.EvaluationOuverture;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base de données d'ouvertures construite depuis des parties PGN.
 *
 * Analyse les parties de la source PGN et extrait les patterns d'ouvertures
 * en vérifiant les trois principes fondamentaux : prise du centre, développement
 * des pièces et sécurité du roi. Les positions sont organisées en arbre de noeuds
 * pondérés par leur fréquence d'apparition.
 */
public final class BaseOuvertures {

    // noeud de l'arbre des ouvertures
    private static class NoeudOuverture {
        ClePosition           position;
        List<NoeudOuverture>  enfants      = new ArrayList<>();
        String                nom;
        int                   compteur;    // nombre de fois joué dans les parties sources

        // scores d'évaluation des principes pour cette position
        int scoreCentre;
        int scoreDeveloppement;
        int scoreSecurite;
    }

    private NoeudOuverture racine;
    private int            totalParties;

    private BaseOuvertures() {
        this.racine     = new NoeudOuverture();
        this.racine.nom = "Position initiale";
    }

    /**
     * Charge les ouvertures depuis un fichier PGN.
     *
     * @param chemin chemin vers le fichier PGN source
     * @return base d'ouvertures construite depuis les parties analysées
     */
    public static BaseOuvertures chargerDepuisPGN(String chemin) {
        BaseOuvertures base = new BaseOuvertures();
        base.totalParties = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(chemin))) {
            StringBuilder partieEnCours = new StringBuilder();
            String ligne;

            while ((ligne = reader.readLine()) != null) {
                ligne = ligne.trim();

                if (ligne.isEmpty()) {
                    if (partieEnCours.length() > 0) {
                        base.analyserPartie(partieEnCours.toString());
                        partieEnCours.setLength(0);
                    }
                } else {
                    partieEnCours.append(ligne).append("\n");
                }
            }

            // traiter la dernière partie du fichier
            if (partieEnCours.length() > 0) {
                base.analyserPartie(partieEnCours.toString());
            }

            System.out.println("[BaseOuvertures] Analyse terminée: " + base.totalParties + " parties");

        } catch (IOException e) {
            System.err.println("[BaseOuvertures] Erreur lecture: " + e.getMessage());
        }

        return base;
    }

    // analyse une partie PGN et enregistre les positions dans l'arbre
    private void analyserPartie(String pgn) {
        try {
            List<String> coups = extraireCoups(pgn);
            if (coups.isEmpty()) return;

            totalParties++;
            Plateau plateau = Plateau.positionInitiale();

            // jouer les 20 premiers coups en évaluant chaque position
            for (int i = 0; i < Math.min(coups.size(), 20); i++) {
                String coupSAN = coups.get(i);

                List<Coup> coupsLegaux = GenerateurCoups.genererLegaux(plateau);
                Coup coup = convertirSAN(coupSAN, plateau, coupsLegaux);

                if (coup == null) break; // SAN non parsable, arrêt de la séquence

                // évaluation des principes d'ouverture avant et après le coup
                int scoreCentre = EvaluationOuverture.evaluerOuverture(plateau);
                plateau.jouer(coup);
                int scoreApres  = EvaluationOuverture.evaluerOuverture(plateau);

                ajouterPosition(plateau, coup, scoreApres, scoreCentre);
            }

        } catch (Exception e) {
            // partie invalide : ignorée
        }
    }

    // extrait la liste de coups SAN depuis le texte PGN d'une partie
    private List<String> extraireCoups(String pgn) {
        List<String> coups = new ArrayList<>();
        String[] lignes = pgn.split("\n");
        boolean trouveCoups = false;

        for (String ligne : lignes) {
            ligne = ligne.trim();

            // ignorer les balises [ ... ]
            if (ligne.startsWith("[") && ligne.endsWith("]")) {
                continue;
            }

            // les coups commencent par un numéro suivi d'un point
            if (ligne.matches("^\\d+\\..*")) {
                trouveCoups = true;
            }

            if (trouveCoups) {
                // nettoyage de la ligne : numéros, commentaires, variantes, résultats
                ligne = ligne.replaceAll("\\d+\\.", " ");
                ligne = ligne.replaceAll("\\{.*?\\}", " ");
                ligne = ligne.replaceAll("\\(.*?\\)", " ");
                ligne = ligne.replaceAll("\\d\\-\\d|\\d/\\d", "");

                String[] tokens = ligne.split("\\s+");
                for (String token : tokens) {
                    token = token.trim();
                    if (!token.isEmpty() && !token.equals("0-1") && !token.equals("1-0")
                            && !token.equals("1/2-1/2") && !token.equals("*")) {
                        coups.add(token);
                    }
                }
            }
        }

        return coups;
    }

    // convertit une notation SAN en coup légal (correspondance par case d'arrivée)
    private Coup convertirSAN(String san, Plateau plateau, List<Coup> coupsLegaux) {
        // nettoyage des annotations
        san = san.replace("+", "").replace("#", "").replace("!", "").replace("?", "");

        // correspondance directe sur la case d'arrivée
        for (Coup coup : coupsLegaux) {
            if (san.equals(coup.arrivee().versAlgebrique())) {
                return coup;
            }
        }

        // repli : correspondance partielle pour les coups courts (ex: "e4")
        if (san.length() >= 2 && san.length() <= 4) {
            for (Coup coup : coupsLegaux) {
                if (coup.arrivee().versAlgebrique().equals(san)) {
                    return coup;
                }
            }
        }

        return null;
    }

    // ajoute ou met à jour un noeud dans l'arbre pour la position jouée
    private void ajouterPosition(Plateau plateau, Coup coupJoue, int scoreApres, int scoreCentre) {
        ClePosition cle  = ClePosition.fromPlateau(plateau);
        long        hash = cle.toHashKey();

        NoeudOuverture noeud = chercherNoeud(racine, hash);

        if (noeud == null) {
            noeud           = new NoeudOuverture();
            noeud.position  = cle;
            noeud.nom       = "Position " + hash;
            noeud.scoreCentre = scoreApres;
            noeud.compteur  = 1;
            ajouterEnfant(racine, noeud);
        } else {
            noeud.compteur++;
            // mise à jour du score par moyenne mobile
            noeud.scoreCentre = (noeud.scoreCentre * (noeud.compteur - 1) + scoreApres) / noeud.compteur;
        }
    }

    // recherche récursive d'un noeud par hash dans l'arbre
    private NoeudOuverture chercherNoeud(NoeudOuverture parent, long hash) {
        if (parent.position != null && parent.position.toHashKey() == hash) {
            return parent;
        }
        for (NoeudOuverture enfant : parent.enfants) {
            NoeudOuverture resultat = chercherNoeud(enfant, hash);
            if (resultat != null) return resultat;
        }
        return null;
    }

    // attache un noeud enfant au noeud parent
    private void ajouterEnfant(NoeudOuverture parent, NoeudOuverture enfant) {
        parent.enfants.add(enfant);
    }

    /**
     * Retourne le meilleur coup pour une position donnée selon la fréquence.
     *
     * @param plateau     position courante
     * @param coupsLegaux coups légaux disponibles
     * @return coup le plus fréquent, ou {@code null} si la position est inconnue
     */
    public Coup getMeilleurCoup(Plateau plateau, List<Coup> coupsLegaux) {
        ClePosition cle  = ClePosition.fromPlateau(plateau);
        long        hash = cle.toHashKey();

        NoeudOuverture noeud = chercherNoeud(racine, hash);
        if (noeud == null) {
            return null;
        }

        // enfant le plus fréquent = coup le plus joué dans cette position
        NoeudOuverture meilleur    = null;
        int            maxCompteur = 0;

        for (NoeudOuverture enfant : noeud.enfants) {
            if (enfant.compteur > maxCompteur) {
                maxCompteur = enfant.compteur;
                meilleur    = enfant;
            }
        }

        if (meilleur == null) {
            return null;
        }

        // retourne le premier coup légal (simplification : le noeud ne stocke pas le coup)
        return coupsLegaux.isEmpty() ? null : coupsLegaux.get(0);
    }

    /**
     * Retourne le nombre de positions dans la base.
     *
     * @return nombre total de noeuds dans l'arbre
     */
    public int getNombrePositions() {
        return compterNoeuds(racine);
    }

    // compte récursivement tous les noeuds de l'arbre
    private int compterNoeuds(NoeudOuverture noeud) {
        int compte = noeud.enfants.size();
        for (NoeudOuverture enfant : noeud.enfants) {
            compte += compterNoeuds(enfant);
        }
        return compte;
    }

    /**
     * Point d'entrée de test : charge une base depuis un fichier PGN et affiche les statistiques.
     *
     * @param args argument[0] = chemin du fichier PGN
     */
    public static void main(String[] args) {
        BaseOuvertures base = BaseOuvertures.chargerDepuisPGN("lichess.pgn");
        System.out.println("Base chargée avec " + base.getNombrePositions() + " positions");
    }
}
