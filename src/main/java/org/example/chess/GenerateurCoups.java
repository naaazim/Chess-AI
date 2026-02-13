package org.example.chess;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * {@code GenerateurCoups} est responsable de la création des coups possibles.
 * </p>
 *
 * <p>
 * Deux niveaux :
 * </p>
 * <ul>
 *   <li><b>Pseudo-légaux</b> : respectent les mouvements des pièces, mais peuvent laisser le roi en échec.</li>
 *   <li><b>Légaux</b> : pseudo-légaux + filtre : on retire ceux qui laissent son roi en échec.</li>
 * </ul>
 *
 * <p>
 * Cette classe est conçue pour gérer un <b>grand facteur de branchement</b> :
 * </p>
 * <ul>
 *   <li>Bitboards (long)</li>
 *   <li>Tables pré-calculées (roi/cavalier)</li>
 *   <li>Rayons pour les pièces glissantes (fou/tour/dame)</li>
 *   <li>Filtrage légal : jouer → tester échec → annuler</li>
 * </ul>
 *
 * <p>
 * Dépendances attendues dans {@link Plateau} :
 * </p>
 * <ul>
 *   <li>{@code long getEnPassant()}</li>
 *   <li>{@code boolean getRoqueBlancRoi()}</li>
 *   <li>{@code boolean getRoqueBlancReine()}</li>
 *   <li>{@code boolean getRoqueNoirRoi()}</li>
 *   <li>{@code boolean getRoqueNoirReine()}</li>
 * </ul>
 */
public final class GenerateurCoups {

    private GenerateurCoups() {}

    /* ============================================================
     * API publique
     * ============================================================ */

    /**
     * <p>Génère tous les coups pseudo-légaux pour le joueur au trait.</p>
     *
     * @param plateau plateau courant
     * @return liste de coups pseudo-légaux
     */
    public static List<Coup> genererPseudoLegaux(Plateau plateau) {
        if (plateau == null) throw new IllegalArgumentException("plateau null");

        // Capacité approximative : souvent < 80 coups
        List<Coup> coups = new ArrayList<>(96);

        Couleur trait = plateau.trait();

        genererCoupsPions(plateau, trait, coups);
        genererCoupsCavaliers(plateau, trait, coups);
        genererCoupsFous(plateau, trait, coups);
        genererCoupsTours(plateau, trait, coups);
        genererCoupsDames(plateau, trait, coups);
        genererCoupsRoiEtRoques(plateau, trait, coups);

        return coups;
    }

    /**
     * <p>Génère tous les coups légaux pour le joueur au trait.</p>
     *
     * <p>
     * Filtre :
     * </p>
     * <ul>
     *   <li>On joue le coup</li>
     *   <li>On vérifie si le roi du joueur qui vient de jouer est en échec</li>
     *   <li>On annule</li>
     * </ul>
     *
     * @param plateau plateau courant
     * @return liste de coups légaux
     */
    public static List<Coup> genererLegaux(Plateau plateau) {
        if (plateau == null) throw new IllegalArgumentException("plateau null");

        Couleur joueurQuiJoue = plateau.trait();
        List<Coup> pseudo = genererPseudoLegaux(plateau);

        List<Coup> legaux = new ArrayList<>(pseudo.size());

        for (Coup coup : pseudo) {
            EtatPlateau s = plateau.jouerAvecSauvegarde(coup);

            boolean roiSafe = !Arbitre.estEnEchec(plateau, joueurQuiJoue);

            plateau.annuler(s);

            if (roiSafe) {
                legaux.add(coup);
            }
        }

        return legaux;
    }

    /* ============================================================
     * PIONS (complet : avance, capture, double pas, promotion, EP)
     * ============================================================ */

    private static void genererCoupsPions(Plateau plateau, Couleur trait, List<Coup> coups) {
        long pions = (trait == Couleur.BLANC)
                ? plateau.bitboard(Piece.PION_BLANC)
                : plateau.bitboard(Piece.PION_NOIR);

        long adversaires = (trait == Couleur.BLANC) ? plateau.noirs() : plateau.blancs();
        long occupes = plateau.occupes();
        long ep = plateau.getEnPassant(); // bitboard (0 ou 1 case)

        long tmp = pions;
        while (tmp != 0L) {
            long lsb = tmp & -tmp;
            int from = Long.numberOfTrailingZeros(lsb);

            if (trait == Couleur.BLANC) {
                genererPionBlancDepuis(from, occupes, adversaires, ep, coups);
            } else {
                genererPionNoirDepuis(from, occupes, adversaires, ep, coups);
            }

            tmp ^= lsb;
        }
    }

    private static void genererPionBlancDepuis(int from,
                                               long occupes,
                                               long adversaires,
                                               long ep,
                                               List<Coup> coups) {

        // Avance 1 : from -> from-8
        int to1 = from - 8;
        if (to1 >= 0) {
            long bitTo1 = 1L << to1;
            if ((occupes & bitTo1) == 0L) {
                // Promotion si arrivée sur rangée 8 (indices 0..7)
                if (to1 <= 7) {
                    ajouterPromotions(coups, from, to1, Piece.PION_BLANC, false, null, false);
                } else {
                    coups.add(coupSimple(from, to1, Piece.PION_BLANC, false, null, false, null, false, false));

                    // Avance 2 : depuis rangée 2 (48..55)
                    if (from >= 48 && from <= 55) {
                        int to2 = from - 16;
                        long bitTo2 = 1L << to2;
                        if ((occupes & bitTo2) == 0L) {
                            coups.add(coupSimple(from, to2, Piece.PION_BLANC, false, null, false, null, false, false));
                        }
                    }
                }
            }
        }

        // Capture diag gauche : from -> from-9 (si pas colonne a)
        if (from % 8 != 0) {
            int to = from - 9;
            if (to >= 0) {
                long bitTo = 1L << to;

                if ((adversaires & bitTo) != 0L) {
                    if (to <= 7) ajouterPromotions(coups, from, to, Piece.PION_BLANC, true, null, false);
                    else coups.add(coupSimple(from, to, Piece.PION_BLANC, true, null, false, null, false, false));
                }

                // En passant : destination == EP
                if (ep != 0L && (ep & bitTo) != 0L) {
                    coups.add(coupSimple(from, to, Piece.PION_BLANC, true, Piece.PION_NOIR, false, null, false, true));
                }
            }
        }

        // Capture diag droite : from -> from-7 (si pas colonne h)
        if (from % 8 != 7) {
            int to = from - 7;
            if (to >= 0) {
                long bitTo = 1L << to;

                if ((adversaires & bitTo) != 0L) {
                    if (to <= 7) ajouterPromotions(coups, from, to, Piece.PION_BLANC, true, null, false);
                    else coups.add(coupSimple(from, to, Piece.PION_BLANC, true, null, false, null, false, false));
                }

                if (ep != 0L && (ep & bitTo) != 0L) {
                    coups.add(coupSimple(from, to, Piece.PION_BLANC, true, Piece.PION_NOIR, false, null, false, true));
                }
            }
        }
    }

    private static void genererPionNoirDepuis(int from,
                                              long occupes,
                                              long adversaires,
                                              long ep,
                                              List<Coup> coups) {

        // Avance 1 : from -> from+8
        int to1 = from + 8;
        if (to1 <= 63) {
            long bitTo1 = 1L << to1;
            if ((occupes & bitTo1) == 0L) {
                // Promotion si arrivée sur rangée 1 (56..63)
                if (to1 >= 56) {
                    ajouterPromotions(coups, from, to1, Piece.PION_NOIR, false, null, false);
                } else {
                    coups.add(coupSimple(from, to1, Piece.PION_NOIR, false, null, false, null, false, false));

                    // Avance 2 : depuis rangée 7 (8..15)
                    if (from >= 8 && from <= 15) {
                        int to2 = from + 16;
                        long bitTo2 = 1L << to2;
                        if ((occupes & bitTo2) == 0L) {
                            coups.add(coupSimple(from, to2, Piece.PION_NOIR, false, null, false, null, false, false));
                        }
                    }
                }
            }
        }

        // Capture diag gauche (noir) : from -> from+7 (si pas colonne a)
        if (from % 8 != 0) {
            int to = from + 7;
            if (to <= 63) {
                long bitTo = 1L << to;

                if ((adversaires & bitTo) != 0L) {
                    if (to >= 56) ajouterPromotions(coups, from, to, Piece.PION_NOIR, true, null, false);
                    else coups.add(coupSimple(from, to, Piece.PION_NOIR, true, null, false, null, false, false));
                }

                if (ep != 0L && (ep & bitTo) != 0L) {
                    coups.add(coupSimple(from, to, Piece.PION_NOIR, true, Piece.PION_BLANC, false, null, false, true));
                }
            }
        }

        // Capture diag droite (noir) : from -> from+9 (si pas colonne h)
        if (from % 8 != 7) {
            int to = from + 9;
            if (to <= 63) {
                long bitTo = 1L << to;

                if ((adversaires & bitTo) != 0L) {
                    if (to >= 56) ajouterPromotions(coups, from, to, Piece.PION_NOIR, true, null, false);
                    else coups.add(coupSimple(from, to, Piece.PION_NOIR, true, null, false, null, false, false));
                }

                if (ep != 0L && (ep & bitTo) != 0L) {
                    coups.add(coupSimple(from, to, Piece.PION_NOIR, true, Piece.PION_BLANC, false, null, false, true));
                }
            }
        }
    }

    private static void ajouterPromotions(List<Coup> coups,
                                          int from,
                                          int to,
                                          Piece pion,
                                          boolean capture,
                                          Piece pieceCapturee,
                                          boolean enPassant) {

        Piece dame = (pion == Piece.PION_BLANC) ? Piece.DAME_BLANCHE : Piece.DAME_NOIRE;
        Piece tour = (pion == Piece.PION_BLANC) ? Piece.TOUR_BLANC : Piece.TOUR_NOIR;
        Piece fou  = (pion == Piece.PION_BLANC) ? Piece.FOU_BLANC  : Piece.FOU_NOIR;
        Piece cav  = (pion == Piece.PION_BLANC) ? Piece.CAVALIER_BLANC : Piece.CAVALIER_NOIR;

        coups.add(coupSimple(from, to, pion, capture, pieceCapturee, true, dame, false, enPassant));
        coups.add(coupSimple(from, to, pion, capture, pieceCapturee, true, tour, false, enPassant));
        coups.add(coupSimple(from, to, pion, capture, pieceCapturee, true, fou,  false, enPassant));
        coups.add(coupSimple(from, to, pion, capture, pieceCapturee, true, cav,  false, enPassant));
    }

    /* ============================================================
     * CAVALIERS
     * ============================================================ */

    private static void genererCoupsCavaliers(Plateau plateau, Couleur trait, List<Coup> coups) {
        long cavaliers = (trait == Couleur.BLANC)
                ? plateau.bitboard(Piece.CAVALIER_BLANC)
                : plateau.bitboard(Piece.CAVALIER_NOIR);

        long allies = (trait == Couleur.BLANC) ? plateau.blancs() : plateau.noirs();
        long ennemis = (trait == Couleur.BLANC) ? plateau.noirs() : plateau.blancs();

        Piece piece = (trait == Couleur.BLANC) ? Piece.CAVALIER_BLANC : Piece.CAVALIER_NOIR;

        long tmp = cavaliers;
        while (tmp != 0L) {
            long lsb = tmp & -tmp;
            int from = Long.numberOfTrailingZeros(lsb);

            long attaques = Masques.attaquesCavalier(from);
            long dests = attaques & ~allies;

            long d = dests;
            while (d != 0L) {
                long dLsb = d & -d;
                int to = Long.numberOfTrailingZeros(dLsb);

                boolean capture = (ennemis & dLsb) != 0L;
                coups.add(coupSimple(from, to, piece, capture, null, false, null, false, false));

                d ^= dLsb;
            }

            tmp ^= lsb;
        }
    }

    /* ============================================================
     * FOUS (diagonales)
     * ============================================================ */

    private static void genererCoupsFous(Plateau plateau, Couleur trait, List<Coup> coups) {
        long fous = (trait == Couleur.BLANC)
                ? plateau.bitboard(Piece.FOU_BLANC)
                : plateau.bitboard(Piece.FOU_NOIR);

        long allies = (trait == Couleur.BLANC) ? plateau.blancs() : plateau.noirs();
        long ennemis = (trait == Couleur.BLANC) ? plateau.noirs() : plateau.blancs();

        long occ = plateau.occupes();
        Piece piece = (trait == Couleur.BLANC) ? Piece.FOU_BLANC : Piece.FOU_NOIR;

        long tmp = fous;
        while (tmp != 0L) {
            long lsb = tmp & -tmp;
            int from = Long.numberOfTrailingZeros(lsb);

            long attaques = Rayons.attaquesDiagonales(from, occ);
            long dests = attaques & ~allies;

            long d = dests;
            while (d != 0L) {
                long dLsb = d & -d;
                int to = Long.numberOfTrailingZeros(dLsb);

                boolean capture = (ennemis & dLsb) != 0L;
                coups.add(coupSimple(from, to, piece, capture, null, false, null, false, false));

                d ^= dLsb;
            }

            tmp ^= lsb;
        }
    }

    /* ============================================================
     * TOURS (h/v)
     * ============================================================ */

    private static void genererCoupsTours(Plateau plateau, Couleur trait, List<Coup> coups) {
        long tours = (trait == Couleur.BLANC)
                ? plateau.bitboard(Piece.TOUR_BLANC)
                : plateau.bitboard(Piece.TOUR_NOIR);

        long allies = (trait == Couleur.BLANC) ? plateau.blancs() : plateau.noirs();
        long ennemis = (trait == Couleur.BLANC) ? plateau.noirs() : plateau.blancs();

        long occ = plateau.occupes();
        Piece piece = (trait == Couleur.BLANC) ? Piece.TOUR_BLANC : Piece.TOUR_NOIR;

        long tmp = tours;
        while (tmp != 0L) {
            long lsb = tmp & -tmp;
            int from = Long.numberOfTrailingZeros(lsb);

            long attaques = Rayons.attaquesHorizontalesEtVerticales(from, occ);
            long dests = attaques & ~allies;

            long d = dests;
            while (d != 0L) {
                long dLsb = d & -d;
                int to = Long.numberOfTrailingZeros(dLsb);

                boolean capture = (ennemis & dLsb) != 0L;
                coups.add(coupSimple(from, to, piece, capture, null, false, null, false, false));

                d ^= dLsb;
            }

            tmp ^= lsb;
        }
    }

    /* ============================================================
     * DAMES (diagonales + h/v)
     * ============================================================ */

    private static void genererCoupsDames(Plateau plateau, Couleur trait, List<Coup> coups) {
        long dames = (trait == Couleur.BLANC)
                ? plateau.bitboard(Piece.DAME_BLANCHE)
                : plateau.bitboard(Piece.DAME_NOIRE);

        long allies = (trait == Couleur.BLANC) ? plateau.blancs() : plateau.noirs();
        long ennemis = (trait == Couleur.BLANC) ? plateau.noirs() : plateau.blancs();

        long occ = plateau.occupes();
        Piece piece = (trait == Couleur.BLANC) ? Piece.DAME_BLANCHE : Piece.DAME_NOIRE;

        long tmp = dames;
        while (tmp != 0L) {
            long lsb = tmp & -tmp;
            int from = Long.numberOfTrailingZeros(lsb);

            long attaques = Rayons.attaquesDiagonales(from, occ) | Rayons.attaquesHorizontalesEtVerticales(from, occ);
            long dests = attaques & ~allies;

            long d = dests;
            while (d != 0L) {
                long dLsb = d & -d;
                int to = Long.numberOfTrailingZeros(dLsb);

                boolean capture = (ennemis & dLsb) != 0L;
                coups.add(coupSimple(from, to, piece, capture, null, false, null, false, false));

                d ^= dLsb;
            }

            tmp ^= lsb;
        }
    }

    /* ============================================================
     * ROI + ROQUES
     * ============================================================ */

    private static void genererCoupsRoiEtRoques(Plateau plateau, Couleur trait, List<Coup> coups) {
        Piece roiPiece = (trait == Couleur.BLANC) ? Piece.ROI_BLANC : Piece.ROI_NOIR;
        long roi = plateau.bitboard(roiPiece);

        if (roi == 0L) return; // position invalide, mais on évite crash

        long allies = (trait == Couleur.BLANC) ? plateau.blancs() : plateau.noirs();
        long ennemis = (trait == Couleur.BLANC) ? plateau.noirs() : plateau.blancs();

        int from = Long.numberOfTrailingZeros(roi);
        long attaques = Masques.attaquesRoi(from);
        long dests = attaques & ~allies;

        // Coups normaux du roi
        long d = dests;
        while (d != 0L) {
            long dLsb = d & -d;
            int to = Long.numberOfTrailingZeros(dLsb);

            boolean capture = (ennemis & dLsb) != 0L;
            coups.add(coupSimple(from, to, roiPiece, capture, null, false, null, false, false));

            d ^= dLsb;
        }

        // Roques (pseudo-légaux, mais on vérifie déjà les règles liées aux cases attaquées)
        genererRoques(plateau, trait, coups);
    }

    private static void genererRoques(Plateau plateau, Couleur trait, List<Coup> coups) {
        // On vérifie :
        // 1) droit de roque
        // 2) cases entre roi et tour vides
        // 3) roi pas en échec
        // 4) roi ne traverse pas une case attaquée
        // 5) case d'arrivée pas attaquée

        Couleur adv = trait.inverse();
        long attaquesAdv = Arbitre.casesAttaqueesPar(plateau, adv);
        long occ = plateau.occupes();

        if (trait == Couleur.BLANC) {
            // Indices : e1=60, g1=62, c1=58, h1=63, a1=56, f1=61, d1=59
            boolean enEchec = Arbitre.estEnEchec(plateau, Couleur.BLANC);
            if (enEchec) return;

            // Petit roque blanc : e1 -> g1, tour h1 -> f1
            if (plateau.getRoqueBlancRoi()) {
                boolean tourPresente = (plateau.bitboard(Piece.TOUR_BLANC) & (1L << 63)) != 0L;
                boolean casesVides = ((occ & ((1L << 61) | (1L << 62))) == 0L);
                boolean casesSafe = ((attaquesAdv & ((1L << 61) | (1L << 62))) == 0L);

                if (tourPresente && casesVides && casesSafe) {
                    coups.add(coupSimple(60, 62, Piece.ROI_BLANC, false, null, false, null, true, false));
                }
            }

            // Grand roque blanc : e1 -> c1, tour a1 -> d1
            if (plateau.getRoqueBlancReine()) {
                boolean tourPresente = (plateau.bitboard(Piece.TOUR_BLANC) & (1L << 56)) != 0L;
                boolean casesVides = ((occ & ((1L << 57) | (1L << 58) | (1L << 59))) == 0L);
                // Le roi traverse d1 (59) ? non : il traverse d1 (59) et arrive c1 (58) — et passe par d1.
                // En pratique : e1->d1->c1 : on teste d1 et c1 (et e1 déjà pas en échec)
                boolean casesSafe = ((attaquesAdv & ((1L << 59) | (1L << 58))) == 0L);

                if (tourPresente && casesVides && casesSafe) {
                    coups.add(coupSimple(60, 58, Piece.ROI_BLANC, false, null, false, null, true, false));
                }
            }

        } else {
            // Indices : e8=4, g8=6, c8=2, h8=7, a8=0, f8=5, d8=3
            boolean enEchec = Arbitre.estEnEchec(plateau, Couleur.NOIR);
            if (enEchec) return;

            // Petit roque noir : e8 -> g8
            if (plateau.getRoqueNoirRoi()) {
                boolean tourPresente = (plateau.bitboard(Piece.TOUR_NOIR) & (1L << 7)) != 0L;
                boolean casesVides = ((occ & ((1L << 5) | (1L << 6))) == 0L);
                boolean casesSafe = ((attaquesAdv & ((1L << 5) | (1L << 6))) == 0L);

                if (tourPresente && casesVides && casesSafe) {
                    coups.add(coupSimple(4, 6, Piece.ROI_NOIR, false, null, false, null, true, false));
                }
            }

            // Grand roque noir : e8 -> c8
            if (plateau.getRoqueNoirReine()) {
                boolean tourPresente = (plateau.bitboard(Piece.TOUR_NOIR) & (1L << 0)) != 0L;
                boolean casesVides = ((occ & ((1L << 1) | (1L << 2) | (1L << 3))) == 0L);
                boolean casesSafe = ((attaquesAdv & ((1L << 3) | (1L << 2))) == 0L);

                if (tourPresente && casesVides && casesSafe) {
                    coups.add(coupSimple(4, 2, Piece.ROI_NOIR, false, null, false, null, true, false));
                }
            }
        }
    }

    /* ============================================================
     * Helper de création de Coup
     * ============================================================ */

    private static Coup coupSimple(int from,
                                   int to,
                                   Piece pieceDeplacee,
                                   boolean capture,
                                   Piece pieceCapturee,
                                   boolean promotion,
                                   Piece piecePromotion,
                                   boolean roque,
                                   boolean enPassant) {
        return new Coup(
                Case.depuisIndice(from),
                Case.depuisIndice(to),
                pieceDeplacee,
                capture,
                pieceCapturee,
                promotion,
                piecePromotion,
                roque,
                enPassant
        );
    }
}
