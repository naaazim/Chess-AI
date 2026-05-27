package fr.moteurechecs.ia.recherche;

import fr.moteurechecs.ia.Evaluation;
import fr.moteurechecs.ia.Niveau;
import fr.moteurechecs.plateau.Coup;
import fr.moteurechecs.plateau.Couleur;
import fr.moteurechecs.plateau.EtatPlateau;
import fr.moteurechecs.plateau.GenerateurCoups;
import fr.moteurechecs.plateau.Plateau;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Algorithme minimax avec élagage alpha-bêta et table de transposition.
 *
 * Le comportement est piloté par les capacités du {@link Niveau} passé en paramètre :
 * - {@link Niveau#useAlphaBeta()} — active ou non les coupures alpha/bêta
 * - {@link Niveau#useMoveSorting()} — active ou non le tri MVV-LVA des coups
 * - {@link Niveau#useTranspositionTable()} — active ou non la table de transposition
 * - {@link Niveau#getFonctionEvaluation()} — détermine la fonction d'évaluation statique
 */
public final class RecursionAlphaBeta {

    private RecursionAlphaBeta() {
    }

    /**
     * Lance la recherche minimax récursive avec détection de répétition dans la ligne courante.
     *
     * La pile {@code pileHistorique} contient les empreintes Zobrist de toutes les positions
     * jouées depuis le début de la partie jusqu'au nœud parent, ainsi que les positions de la
     * ligne de recherche courante. Si la position actuelle apparaît déjà deux fois dans cette
     * pile (troisième visite = triple répétition), le score 0 est retourné immédiatement pour
     * éviter que l'IA ne tourne en rond dans une position gagnante.
     *
     * @param plateau        position courante (modifiée puis restaurée pendant la recherche)
     * @param profondeur     profondeur restante à explorer
     * @param alpha          borne inférieure de la fenêtre
     * @param beta           borne supérieure de la fenêtre
     * @param tempsEcoule    signal d'arrêt par dépassement de temps
     * @param niveau         niveau de l'IA (capacités actives et fonction d'évaluation)
     * @param pileHistorique tableau d'empreintes Zobrist partagé entre les appels récursifs ;
     *                       les indices {@code [0, indicePile)} contiennent les positions déjà vues
     * @param indicePile     indice courant dans {@code pileHistorique} où écrire l'empreinte de
     *                       la position actuelle avant de descendre plus loin
     * @return score minimax de la position
     * @throws ExceptionTempsDepasse si le temps alloué est écoulé
     */
    public static int minimax(Plateau plateau, int profondeur, int alpha, int beta,
                              AtomicBoolean tempsEcoule, Niveau niveau,
                              long[] pileHistorique, int indicePile) {
        if (tempsEcoule.get())
            throw new ExceptionTempsDepasse();

        // ── Détection de répétition dans la ligne de recherche courante ───────
        // Le hash est calculé systématiquement, même si la TT est désactivée,
        // car la détection de répétition en a toujours besoin.
        long empreinte = TableDeTransposition.hash(plateau);
        pileHistorique[indicePile] = empreinte;
        int occurrences = 0;
        for (int i = 0; i < indicePile; i++) {
            if (pileHistorique[i] == empreinte) occurrences++;
        }
        if (occurrences >= 2) return 0; // triple répétition dans la ligne → nul

        // ── Table de transposition (conditionnelle) ───────────────────────────
        boolean useTT = niveau.useTranspositionTable();
        long empreintePlateau = useTT ? empreinte : 0L;
        TableDeTransposition tt = useTT ? TableDeTransposition.getInstance() : null;
        TableDeTransposition.EntreeTT entree = (tt != null) ? tt.probe(empreintePlateau) : null;

        int alphaOriginal = alpha;
        int betaOriginale = beta;

        if (useTT) {
            alpha = appliquerEntreeTT(entree, profondeur, alpha, beta);
            if (entree != null && entree.profondeur >= profondeur && estCoupure(alpha, beta)) {
                return entree.score;
            }
        }

        // ── Profondeur zéro : basculer sur la quiescence ─────────────────────
        if (profondeur == 0) {
            return RechercheQuiescence.quiescenceSearch(plateau, alpha, beta, tempsEcoule, niveau);
        }

        List<Coup> coups = GenerateurCoups.genererLegaux(plateau);

        if (coups.isEmpty()) {
            return evaluerPositionTerminale(plateau, profondeur, tt, empreintePlateau);
        }

        // ── Tri des coups (conditionnel) ──────────────────────────────────────
        if (niveau.useMoveSorting()) {
            TrieurDeCoups.trierCoups(coups, plateau);
            if (entree != null && entree.meilleurCoup != null) {
                TrieurDeCoups.placerEnPremier(coups, entree.meilleurCoup);
            }
        }

        boolean joueurMax = (plateau.trait() == Couleur.BLANC);
        return joueurMax
            ? rechercherMax(plateau, coups, profondeur, alpha, beta, tempsEcoule,
                            tt, empreintePlateau, alphaOriginal, betaOriginale, niveau,
                            pileHistorique, indicePile)
            : rechercherMin(plateau, coups, profondeur, alpha, beta, tempsEcoule,
                            tt, empreintePlateau, alphaOriginal, betaOriginale, niveau,
                            pileHistorique, indicePile);
    }

    // application des bornes de la table de transposition sur alpha/beta
    private static int appliquerEntreeTT(TableDeTransposition.EntreeTT entree,
                                         int profondeur, int alpha, int beta) {
        if (entree == null || entree.profondeur < profondeur) return alpha;
        switch (entree.type) {
            case TableDeTransposition.EXACT:       return entree.score;
            case TableDeTransposition.BORNE_INFERIEURE: return Math.max(alpha, entree.score);
            case TableDeTransposition.BORNE_SUPERIEURE: beta = Math.min(beta, entree.score); break;
        }
        return alpha;
    }

    // vérifie si la fenêtre est fermée après application des bornes TT
    private static boolean estCoupure(int alpha, int beta) {
        return alpha >= beta;
    }

    // évaluation d'une position terminale (mat ou pat)
    private static int evaluerPositionTerminale(Plateau plateau, int profondeur,
                                                 TableDeTransposition tt, long empreintePlateau) {
        int score;
        if (plateau.estEnEchec(plateau.trait())) {
            // mat : préférer les mats les plus rapides
            score = (plateau.trait() == Couleur.BLANC)
                ? -Evaluation.SCORE_MAT + (20 - profondeur)
                :  Evaluation.SCORE_MAT - (20 - profondeur);
        } else {
            score = 0; // pat
        }
        if (tt != null) tt.store(empreintePlateau, score, profondeur, TableDeTransposition.EXACT, null);
        return score;
    }

    // recherche pour le joueur maximiseur (Blancs)
    private static int rechercherMax(Plateau plateau, List<Coup> coups,
                                     int profondeur, int alpha, int beta,
                                     AtomicBoolean tempsEcoule,
                                     TableDeTransposition tt, long empreintePlateau,
                                     int alphaOriginal, int betaOriginale,
                                     Niveau niveau,
                                     long[] pileHistorique, int indicePile) {
        int meilleur = Integer.MIN_VALUE;
        Coup meilleurCoupLocal = coups.get(0);

        for (Coup coup : coups) {
            EtatPlateau sauvegarde = plateau.jouerAvecSauvegarde(coup);
            int score = minimax(plateau, profondeur - 1, alpha, beta, tempsEcoule, niveau,
                                pileHistorique, indicePile + 1);
            plateau.annuler(sauvegarde);

            if (score > meilleur) {
                meilleur = score;
                meilleurCoupLocal = coup;
            }
            alpha = Math.max(alpha, meilleur);
            if (niveau.useAlphaBeta() && alpha >= beta) break; // coupe bêta
        }

        if (tt != null) {
            int typeTT = determinerTypeTT(meilleur, alphaOriginal, betaOriginale);
            tt.store(empreintePlateau, meilleur, profondeur, typeTT, meilleurCoupLocal);
        }
        return meilleur;
    }

    // recherche pour le joueur minimiseur (Noirs)
    private static int rechercherMin(Plateau plateau, List<Coup> coups,
                                     int profondeur, int alpha, int beta,
                                     AtomicBoolean tempsEcoule,
                                     TableDeTransposition tt, long empreintePlateau,
                                     int alphaOriginal, int betaOriginale,
                                     Niveau niveau,
                                     long[] pileHistorique, int indicePile) {
        int meilleur = Integer.MAX_VALUE;
        Coup meilleurCoupLocal = coups.get(0);

        for (Coup coup : coups) {
            EtatPlateau sauvegarde = plateau.jouerAvecSauvegarde(coup);
            int score = minimax(plateau, profondeur - 1, alpha, beta, tempsEcoule, niveau,
                                pileHistorique, indicePile + 1);
            plateau.annuler(sauvegarde);

            if (score < meilleur) {
                meilleur = score;
                meilleurCoupLocal = coup;
            }
            beta = Math.min(beta, meilleur);
            if (niveau.useAlphaBeta() && alpha >= beta) break; // coupe alpha
        }

        if (tt != null) {
            int typeTT = determinerTypeTT(meilleur, alphaOriginal, betaOriginale);
            tt.store(empreintePlateau, meilleur, profondeur, typeTT, meilleurCoupLocal);
        }
        return meilleur;
    }

    // type d'entrée TT : EXACT, BORNE_INFERIEURE ou BORNE_SUPERIEURE
    private static int determinerTypeTT(int meilleur, int alphaOriginal, int betaOriginale) {
        if (meilleur >= betaOriginale)  return TableDeTransposition.BORNE_INFERIEURE;
        if (meilleur <= alphaOriginal)  return TableDeTransposition.BORNE_SUPERIEURE;
        return TableDeTransposition.EXACT;
    }
}
