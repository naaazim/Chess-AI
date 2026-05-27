package fr.moteurechecs.ia;

import fr.moteurechecs.ia.evaluation.Materiel;
import fr.moteurechecs.ouverture.ClePosition;
import fr.moteurechecs.ouverture.LivreOuvertures;
import fr.moteurechecs.plateau.Coup;
import fr.moteurechecs.plateau.Couleur;
import fr.moteurechecs.plateau.EtatPlateau;
import fr.moteurechecs.plateau.GenerateurCoups;
import fr.moteurechecs.plateau.Piece;
import fr.moteurechecs.plateau.Plateau;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Sélecteur statique de coups issus du livre d'ouvertures et du principe de roque.
 *
 * Encapsule toute la logique d'heuristique d'ouverture : score de fréquence,
 * bonus positionnels (centre, développement, roque), pénalités (reines précoces,
 * pions de bord), vérifications de sécurité matérielle et sélection diversifiée
 * par tirage pondéré.
 */
public final class SelecteurCoupLivre {

    // ===== Pondérations heuristiques du livre =====
    private static final int BOOK_TOP_K                  = 6;
    private static final int BOOK_FREQUENCY_WEIGHT       = 12;
    private static final int BOOK_BONUS_CENTRE           = 35;
    private static final int BOOK_BONUS_PRESSION_CENTRE  = 18;
    private static final int BOOK_BONUS_DEVELOPPEMENT    = 24;
    private static final int BOOK_BONUS_ROQUE            = 42;
    private static final int BOOK_PENALITE_PION_BORD     = 26;
    private static final int BOOK_PENALITE_REINE_TOT     = 30;
    private static final int BOOK_PENALITE_REDEPLACEMENT = 22;
    private static final int BOOK_PENALITE_OUVERTURE_ROI = 16;
    private static final int BOOK_DIVERSITY_WINDOW_EARLY = 120;
    private static final int BOOK_DIVERSITY_WINDOW_MID   = 70;
    private static final int BOOK_DIVERSITY_WINDOW_LATE  = 40;

    // ===== Seuils de sécurité =====
    private static final int SAFETY_MATERIAL_DROP_CP   = 180;
    private static final int SAFETY_EVAL_DROP_CP       = 180;
    private static final int SAFETY_HANGING_CAPTURE_CP = 150;
    private static final int ROQUE_MENACE_MAJOR_CP     = Evaluation.VALEUR_CAVALIER;
    private static final int ROQUE_MENACE_MATERIAL_CP  = Evaluation.VALEUR_PION;

    private SelecteurCoupLivre() {}

    // ===== API publique =====

    /**
     * Tente de trouver un coup dans le livre d'ouvertures.
     *
     * Met à jour {@code openingPhase} en interne :
     * {@code onBookMoveChosen} si un coup sain est trouvé, {@code disable} sinon.
     *
     * @param plateau            position courante
     * @param coups              coups légaux disponibles
     * @param niveau             niveau de difficulté
     * @param openingPhase       gestionnaire de phase d'ouverture
     * @param openingBookEnabled indique si le livre est activé globalement
     * @return coup du livre enveloppé dans un Optional, ou {@code Optional.empty()}
     */
    public static Optional<Coup> chercherCoupDansLivre(
            Plateau plateau, List<Coup> coups, Niveau niveau,
            GestionnairePhaseOuverture openingPhase, boolean openingBookEnabled) {

        if (!openingBookEnabled || !openingPhase.shouldUseBook() || !niveau.useOpeningBook()) {
            return Optional.empty();
        }

        LivreOuvertures book = LivreOuvertures.getInstance();
        if (book == null || !book.isLoaded()) {
            openingPhase.disable();
            return Optional.empty();
        }

        ClePosition cle = ClePosition.fromPlateau(plateau);
        List<LivreOuvertures.BookCandidate> candidats = book.getCandidates(cle, coups, BOOK_TOP_K);
        CandidatScore selectionne = choisirCandidatSain(plateau, candidats, openingPhase);

        if (selectionne != null) {
            String nom = sanitizeOpeningName(selectionne.candidate.getOpeningName());
            openingPhase.onBookMoveChosen(nom);
            System.out.println("[IA] Opening: " + nom + " -> " + selectionne.candidate.getMove());
            return Optional.of(selectionne.candidate.getMove());
        }

        openingPhase.disable();
        return Optional.empty();
    }

    /**
     * Cherche un roque prioritaire sûr si la position le justifie.
     *
     * @param plateau     position courante
     * @param coups       coups légaux
     * @param currentPly  nombre de demi-coups déjà joués
     * @param maxRoquePly limite de ply au-delà de laquelle le roque n'est plus prioritaire
     * @return roque prioritaire, ou {@code null} si aucun n'est approprié
     */
    public static Coup trouverRoquePrioritaire(
            Plateau plateau, List<Coup> coups, int currentPly, int maxRoquePly) {

        if (currentPly > maxRoquePly) return null;

        List<Coup> roques = filtrerRoques(coups);
        if (roques.isEmpty()) return null;

        Couleur trait        = plateau.trait();
        int     evalAvant    = orienterScore(Evaluation.evaluer(plateau), trait);
        int     materielAvant = orienterScore(Materiel.scoreMateriel(plateau), trait);

        return meilleurRoqueSur(plateau, roques, trait, evalAvant, materielAvant);
    }

    // ===== Sélection depuis le livre =====

    private static CandidatScore choisirCandidatSain(
            Plateau plateau, List<LivreOuvertures.BookCandidate> candidats,
            GestionnairePhaseOuverture openingPhase) {

        if (candidats == null || candidats.isEmpty()) return null;

        Couleur trait         = plateau.trait();
        int     ply           = openingPhase.getCurrentPly();
        int     evalAvant     = orienterScore(Evaluation.evaluer(plateau), trait);
        int     materielAvant = orienterScore(Materiel.scoreMateriel(plateau), trait);

        List<CandidatScore> ordonnes = scorerEtTrier(candidats, trait, ply);
        marquerSurs(ordonnes, plateau, trait, evalAvant, materielAvant);

        List<CandidatScore> surs = filtrerSurs(ordonnes);
        return surs.isEmpty() ? null : choisirDiversifie(surs, ply);
    }

    private static List<CandidatScore> scorerEtTrier(
            List<LivreOuvertures.BookCandidate> candidats, Couleur trait, int ply) {
        List<CandidatScore> ordonnes = new ArrayList<>(candidats.size());
        for (LivreOuvertures.BookCandidate c : candidats) {
            int score = c.getMoveCount() * BOOK_FREQUENCY_WEIGHT
                    + scoreHeuristiqueOuverture(c.getMove(), trait, ply);
            ordonnes.add(new CandidatScore(c, score));
        }
        ordonnes.sort((a, b) -> Integer.compare(b.score, a.score));
        return ordonnes;
    }

    private static void marquerSurs(List<CandidatScore> ordonnes, Plateau plateau,
            Couleur trait, int evalAvant, int materielAvant) {
        for (CandidatScore cs : ordonnes) {
            if (passeSafetyCheck(plateau, cs.candidate.getMove(), trait, evalAvant, materielAvant)) {
                cs.safe = true;
            }
        }
    }

    private static List<CandidatScore> filtrerSurs(List<CandidatScore> ordonnes) {
        List<CandidatScore> surs = new ArrayList<>();
        for (CandidatScore cs : ordonnes) {
            if (cs.safe) surs.add(cs);
        }
        return surs;
    }

    // ===== Vérifications de sécurité =====

    private static boolean passeSafetyCheck(Plateau plateau, Coup coup,
            Couleur joueur, int evalAvant, int materielAvant) {
        Plateau copie = plateau.copie();
        copie.jouer(coup);
        int materielApres = orienterScore(Materiel.scoreMateriel(copie), joueur);
        if (materielApres - materielAvant <= -SAFETY_MATERIAL_DROP_CP) return false;
        if (aCaptureAdverseDangereuse(copie, joueur, materielApres)) return false;
        int evalApres = orienterScore(Evaluation.evaluer(copie), joueur);
        return evalApres - evalAvant > -SAFETY_EVAL_DROP_CP;
    }

    private static boolean aCaptureAdverseDangereuse(
            Plateau positionApres, Couleur notreCouleur, int materielApres) {
        List<Coup> reponsesAdverses = GenerateurCoups.genererLegaux(positionApres);
        if (reponsesAdverses.isEmpty()) return false;
        long casesDefendues = positionApres.casesAttaqueesPar(notreCouleur);
        for (Coup reponse : reponsesAdverses) {
            if (!reponse.estCapture()) continue;
            boolean nonDefendue = (casesDefendues & reponse.arrivee().bit()) == 0L;
            if (nonDefendue && valeurPieceCapturee(positionApres, reponse) >= SAFETY_HANGING_CAPTURE_CP) {
                return true;
            }
            EtatPlateau etat = positionApres.jouerAvecSauvegarde(reponse);
            int materielApresReponse = orienterScore(Materiel.scoreMateriel(positionApres), notreCouleur);
            positionApres.annuler(etat);
            if (materielApres - materielApresReponse >= SAFETY_MATERIAL_DROP_CP) return true;
        }
        return false;
    }

    // ===== Sélection diversifiée =====

    private static CandidatScore choisirDiversifie(List<CandidatScore> surs, int ply) {
        if (surs.size() == 1) return surs.get(0);
        int fenetre = (ply <= 4) ? BOOK_DIVERSITY_WINDOW_EARLY
                    : (ply <= 10) ? BOOK_DIVERSITY_WINDOW_MID : BOOK_DIVERSITY_WINDOW_LATE;
        int meilleurScore = surs.get(0).score;
        List<CandidatScore> listeCourte = new ArrayList<>();
        for (CandidatScore cs : surs) {
            if (cs.score >= meilleurScore - fenetre) listeCourte.add(cs);
        }
        return tiragePondere(listeCourte);
    }

    private static CandidatScore tiragePondere(List<CandidatScore> liste) {
        if (liste.size() == 1) return liste.get(0);
        int scoreMin = liste.get(liste.size() - 1).score;
        long   totalPoids = 0L;
        long[] poids      = new long[liste.size()];
        for (int i = 0; i < liste.size(); i++) {
            int  n    = Math.max(1, liste.get(i).score - scoreMin + 10);
            poids[i]  = (long) n * n;
            totalPoids += poids[i];
        }
        long tirage = ThreadLocalRandom.current().nextLong(totalPoids);
        long cumul  = 0L;
        for (int i = 0; i < liste.size(); i++) {
            cumul += poids[i];
            if (tirage < cumul) return liste.get(i);
        }
        return liste.get(0);
    }

    // ===== Roque prioritaire =====

    private static List<Coup> filtrerRoques(List<Coup> coups) {
        List<Coup> roques = new ArrayList<>(2);
        for (Coup c : coups) {
            if (c.estRoque()) roques.add(c);
        }
        return roques;
    }

    private static Coup meilleurRoqueSur(Plateau plateau, List<Coup> roques,
            Couleur trait, int evalAvant, int materielAvant) {
        Coup meilleurRoque = null;
        int  meilleurScore = Integer.MIN_VALUE;
        for (Coup roque : roques) {
            if (!passeSafetyCheck(plateau, roque, trait, evalAvant, materielAvant)) continue;
            if (aMenacesImmediatesApresRoque(plateau, roque, trait)) continue;
            Plateau copie  = plateau.copie();
            copie.jouer(roque);
            int evalApres  = orienterScore(Evaluation.evaluer(copie), trait);
            int score = (estPetitRoque(roque, trait) ? 40 : 25) + (evalApres - evalAvant) / 4;
            if (score > meilleurScore) { meilleurScore = score; meilleurRoque = roque; }
        }
        return meilleurRoque;
    }

    private static boolean aMenacesImmediatesApresRoque(Plateau plateau, Coup roque, Couleur nous) {
        Plateau copie = plateau.copie();
        copie.jouer(roque);
        Couleur adverse = nous.inverse();
        List<Coup> reponsesAdverses = GenerateurCoups.genererLegaux(copie);
        if (reponsesAdverses.isEmpty()) return false;
        if (aCaptureMajeure(copie, reponsesAdverses)) return true;
        if (aMenaceMaterielleDirect(copie, reponsesAdverses, nous)) return true;
        long attaquesAdverses = copie.casesAttaqueesPar(adverse);
        long attaquesNous     = copie.casesAttaqueesPar(nous);
        return aPieceImportanteEnPriseNonDefendue(copie, nous, attaquesAdverses, attaquesNous);
    }

    private static boolean aCaptureMajeure(Plateau copie, List<Coup> reponses) {
        for (Coup r : reponses) {
            if (r.estCapture() && valeurPieceCapturee(copie, r) >= ROQUE_MENACE_MAJOR_CP) return true;
        }
        return false;
    }

    private static boolean aMenaceMaterielleDirect(Plateau copie, List<Coup> reponses, Couleur nous) {
        int materielAvant = orienterScore(Materiel.scoreMateriel(copie), nous);
        int pirePerte = 0;
        for (Coup r : reponses) {
            EtatPlateau etat = copie.jouerAvecSauvegarde(r);
            int materielApres = orienterScore(Materiel.scoreMateriel(copie), nous);
            copie.annuler(etat);
            pirePerte = Math.max(pirePerte, materielAvant - materielApres);
        }
        return pirePerte >= ROQUE_MENACE_MATERIAL_CP;
    }

    private static boolean aPieceImportanteEnPriseNonDefendue(
            Plateau plateau, Couleur nous, long attaquesAdverses, long attaquesNous) {
        Piece[] importantes = (nous == Couleur.BLANC)
                ? new Piece[]{Piece.CAVALIER_BLANC, Piece.FOU_BLANC, Piece.TOUR_BLANC, Piece.DAME_BLANCHE}
                : new Piece[]{Piece.CAVALIER_NOIR,  Piece.FOU_NOIR,  Piece.TOUR_NOIRE, Piece.DAME_NOIRE};
        for (Piece piece : importantes) {
            long bb = plateau.bitboard(piece);
            while (bb != 0L) {
                long lsb = bb & -bb;
                if ((attaquesAdverses & lsb) != 0L && (attaquesNous & lsb) == 0L) return true;
                bb ^= lsb;
            }
        }
        return false;
    }

    private static boolean estPetitRoque(Coup roque, Couleur trait) {
        int arrivee = roque.arrivee().indice();
        return (trait == Couleur.BLANC && arrivee == 62) || (trait == Couleur.NOIR && arrivee == 6);
    }

    // ===== Score heuristique d'ouverture =====

    private static int scoreHeuristiqueOuverture(Coup coup, Couleur trait, int ply) {
        int score = 0;
        int depart        = coup.depart().indice();
        int arrivee       = coup.arrivee().indice();
        int colonneDepart = depart % 8;
        Piece piece       = coup.pieceDeplacee();

        if (arrivee == 36 || arrivee == 35 || arrivee == 28 || arrivee == 27) score += BOOK_BONUS_CENTRE;
        if (arrivee == 34 || arrivee == 26 || arrivee == 37 || arrivee == 29) score += BOOK_BONUS_PRESSION_CENTRE;
        if (estDeveloppement(piece, depart, trait)) score += BOOK_BONUS_DEVELOPPEMENT;
        if (coup.estRoque()) score += BOOK_BONUS_ROQUE;
        if (estPionDuTrait(piece, trait) && (colonneDepart == 0 || colonneDepart == 7) && ply <= 10)
            score -= BOOK_PENALITE_PION_BORD;
        if (estReineDuTrait(piece, trait) && ply <= 8) score -= BOOK_PENALITE_REINE_TOT;
        if (!estPionDuTrait(piece, trait) && !estCaseDepartNaturelle(piece, depart, trait)
                && !coup.estCapture() && !coup.estRoque() && ply <= 10)
            score -= BOOK_PENALITE_REDEPLACEMENT;
        if (estPionDuTrait(piece, trait)
                && (colonneDepart == 5 || colonneDepart == 6 || colonneDepart == 1)
                && estRangDepartPion(trait, depart) && ply <= 8)
            score -= BOOK_PENALITE_OUVERTURE_ROI;
        return score;
    }

    private static boolean estDeveloppement(Piece piece, int depart, Couleur trait) {
        if (trait == Couleur.BLANC) {
            return (piece == Piece.CAVALIER_BLANC && (depart == 57 || depart == 62))
                    || (piece == Piece.FOU_BLANC   && (depart == 58 || depart == 61));
        }
        return (piece == Piece.CAVALIER_NOIR && (depart == 1 || depart == 6))
                || (piece == Piece.FOU_NOIR   && (depart == 2 || depart == 5));
    }

    private static boolean estCaseDepartNaturelle(Piece piece, int depart, Couleur trait) {
        return switch (piece) {
            case CAVALIER_BLANC -> depart == 57 || depart == 62;
            case CAVALIER_NOIR  -> depart == 1  || depart == 6;
            case FOU_BLANC      -> depart == 58 || depart == 61;
            case FOU_NOIR       -> depart == 2  || depart == 5;
            case TOUR_BLANC     -> depart == 56 || depart == 63;
            case TOUR_NOIRE     -> depart == 0  || depart == 7;
            case DAME_BLANCHE   -> depart == 59;
            case DAME_NOIRE     -> depart == 3;
            case ROI_BLANC      -> depart == 60;
            case ROI_NOIR       -> depart == 4;
            default             -> estPionDuTrait(piece, trait);
        };
    }

    private static boolean estRangDepartPion(Couleur trait, int depart) {
        int rang = 8 - (depart / 8);
        return (trait == Couleur.BLANC && rang == 2) || (trait == Couleur.NOIR && rang == 7);
    }

    private static boolean estPionDuTrait(Piece piece, Couleur trait) {
        return (trait == Couleur.BLANC && piece == Piece.PION_BLANC)
                || (trait == Couleur.NOIR && piece == Piece.PION_NOIR);
    }

    private static boolean estReineDuTrait(Piece piece, Couleur trait) {
        return (trait == Couleur.BLANC && piece == Piece.DAME_BLANCHE)
                || (trait == Couleur.NOIR && piece == Piece.DAME_NOIRE);
    }

    // ===== Utilitaires =====

    private static int valeurPieceCapturee(Plateau plateau, Coup capture) {
        Piece capturee = capture.pieceCapturee();
        if (capturee == null) capturee = plateau.pieceEn(capture.arrivee());
        return valeurPiece(capturee);
    }

    private static int valeurPiece(Piece piece) {
        if (piece == null) return 0;
        return switch (piece) {
            case PION_BLANC,     PION_NOIR     -> Evaluation.VALEUR_PION;
            case CAVALIER_BLANC, CAVALIER_NOIR -> Evaluation.VALEUR_CAVALIER;
            case FOU_BLANC,      FOU_NOIR      -> Evaluation.VALEUR_FOU;
            case TOUR_BLANC,     TOUR_NOIRE    -> Evaluation.VALEUR_TOUR;
            case DAME_BLANCHE,   DAME_NOIRE    -> Evaluation.VALEUR_DAME;
            case ROI_BLANC,      ROI_NOIR      -> Evaluation.SCORE_MAT;
        };
    }

    private static int orienterScore(int scoreBlanc, Couleur couleur) {
        return couleur == Couleur.BLANC ? scoreBlanc : -scoreBlanc;
    }

    private static String sanitizeOpeningName(String nom) {
        return (nom == null || nom.isBlank()) ? "Unknown" : nom;
    }

    // ===== Classe interne =====

    /** Candidat du livre avec son score heuristique et son statut de sécurité. */
    private static final class CandidatScore {
        final LivreOuvertures.BookCandidate candidate;
        final int score;
        boolean   safe;

        CandidatScore(LivreOuvertures.BookCandidate candidat, int score) {
            this.candidate = candidat;
            this.score     = score;
        }
    }
}
