package fr.moteurechecs.plateau;

import java.util.ArrayList;
import java.util.List;

/**
 * Générateur de coups pseudo-légaux et légaux pour le joueur au trait.
 *
 * Deux niveaux de génération :
 * - PSEUDO-LÉGAUX : respectent les mouvements des pièces, mais peuvent
 *       laisser le roi en échec.
 * - LÉGAUX : pseudo-légaux filtrés — les coups laissant le roi en échec
 *       sont supprimés via jouer/tester/annuler.
 *
 * Performance : utilise des bitboards 64 bits, des tables pré-calculées pour
 * le roi et le cavalier ({@link Masques}), et des rayons glissants pour les pièces
 * longue portée ({@link Rayons}).
 */
public final class GenerateurCoups {

    private GenerateurCoups() {}

    /* ============================================================
     * API publique
     * ============================================================ */

    /**
     * Génère tous les coups pseudo-légaux pour le joueur au trait.
     *
     * @param plateau position courante (non null)
     * @return liste de coups pseudo-légaux (peut contenir des coups laissant le roi en échec)
     * @throws IllegalArgumentException si {@code plateau} est null
     */
    public static List<Coup> genererPseudoLegaux(Plateau plateau) {
        if (plateau == null) throw new IllegalArgumentException("plateau null");

        // capacité approximative : rarement plus de 80 coups dans une position normale
        List<Coup> coups = new ArrayList<>(96);
        Couleur    trait = plateau.trait();

        genererCoupsPions(plateau, trait, coups);
        genererCoupsCavaliers(plateau, trait, coups);
        genererCoupsFous(plateau, trait, coups);
        genererCoupsTours(plateau, trait, coups);
        genererCoupsDames(plateau, trait, coups);
        genererCoupsRoiEtRoques(plateau, trait, coups);

        return coups;
    }

    /**
     * Génère tous les coups légaux pour le joueur au trait.
     *
     * Filtre les pseudo-légaux : pour chaque coup, on joue, on vérifie que le roi
     * du joueur qui vient de jouer n'est pas en échec, puis on annule.
     *
     * @param plateau position courante (non null)
     * @return liste de coups légaux
     * @throws IllegalArgumentException si {@code plateau} est null
     */
    public static List<Coup> genererLegaux(Plateau plateau) {
        if (plateau == null) throw new IllegalArgumentException("plateau null");

        Couleur    joueurQuiJoue = plateau.trait();
        List<Coup> pseudoLegaux  = genererPseudoLegaux(plateau);
        List<Coup> legaux        = new ArrayList<>(pseudoLegaux.size());

        for (Coup coup : pseudoLegaux) {
            EtatPlateau sauvegarde = plateau.jouerAvecSauvegarde(coup);
            boolean     roiSauf   = !Arbitre.estEnEchec(plateau, joueurQuiJoue);
            plateau.annuler(sauvegarde);
            if (roiSauf) {
                legaux.add(coup);
            }
        }

        return legaux;
    }

    /* ============================================================
     * PIONS (avance, capture, double pas, promotion, en passant)
     * ============================================================ */

    // génère tous les coups de pions pour la couleur au trait
    private static void genererCoupsPions(Plateau plateau, Couleur trait, List<Coup> coups) {
        long pions      = (trait == Couleur.BLANC) ? plateau.bitboard(Piece.PION_BLANC)  : plateau.bitboard(Piece.PION_NOIR);
        long adversaires = (trait == Couleur.BLANC) ? plateau.noirs()  : plateau.blancs();
        long occupes     = plateau.occupes();
        long enPassant   = plateau.getEnPassant(); // bitboard 0 ou 1 case

        long curseur = pions;
        while (curseur != 0L) {
            long bit           = curseur & -curseur;
            int  indiceDepart  = Long.numberOfTrailingZeros(bit);

            if (trait == Couleur.BLANC) {
                genererPionBlancDepuis(indiceDepart, occupes, adversaires, enPassant, coups);
            } else {
                genererPionNoirDepuis(indiceDepart, occupes, adversaires, enPassant, coups);
            }

            curseur ^= bit;
        }
    }

    // génère les coups d'un pion blanc depuis une case de départ donnée
    private static void genererPionBlancDepuis(int indiceDepart,
                                               long occupes,
                                               long adversaires,
                                               long enPassant,
                                               List<Coup> coups) {

        // avance d'une case (indice décroissant = vers le haut)
        int  indiceArrivee1  = indiceDepart - 8;
        if (indiceArrivee1 >= 0) {
            long bitArrivee1 = 1L << indiceArrivee1;
            if ((occupes & bitArrivee1) == 0L) {
                if (indiceArrivee1 <= 7) {
                    // promotion sur la rangée 8 (indices 0..7)
                    ajouterPromotions(coups, indiceDepart, indiceArrivee1, Piece.PION_BLANC, false, null, false);
                } else {
                    coups.add(creerCoup(indiceDepart, indiceArrivee1, Piece.PION_BLANC, false, null, false, null, false, false));

                    // avance de deux cases depuis la rangée 2 (indices 48..55)
                    if (indiceDepart >= 48 && indiceDepart <= 55) {
                        int  indiceArrivee2 = indiceDepart - 16;
                        long bitArrivee2    = 1L << indiceArrivee2;
                        if ((occupes & bitArrivee2) == 0L) {
                            coups.add(creerCoup(indiceDepart, indiceArrivee2, Piece.PION_BLANC, false, null, false, null, false, false));
                        }
                    }
                }
            }
        }

        // capture diagonale gauche (colonne a exclue)
        if (indiceDepart % 8 != 0) {
            int  indiceArrivee = indiceDepart - 9;
            if (indiceArrivee >= 0) {
                long bitArrivee = 1L << indiceArrivee;
                if ((adversaires & bitArrivee) != 0L) {
                    if (indiceArrivee <= 7) ajouterPromotions(coups, indiceDepart, indiceArrivee, Piece.PION_BLANC, true, null, false);
                    else coups.add(creerCoup(indiceDepart, indiceArrivee, Piece.PION_BLANC, true, null, false, null, false, false));
                }
                // en passant diagonal gauche
                if (enPassant != 0L && (enPassant & bitArrivee) != 0L) {
                    coups.add(creerCoup(indiceDepart, indiceArrivee, Piece.PION_BLANC, true, Piece.PION_NOIR, false, null, false, true));
                }
            }
        }

        // capture diagonale droite (colonne h exclue)
        if (indiceDepart % 8 != 7) {
            int  indiceArrivee = indiceDepart - 7;
            if (indiceArrivee >= 0) {
                long bitArrivee = 1L << indiceArrivee;
                if ((adversaires & bitArrivee) != 0L) {
                    if (indiceArrivee <= 7) ajouterPromotions(coups, indiceDepart, indiceArrivee, Piece.PION_BLANC, true, null, false);
                    else coups.add(creerCoup(indiceDepart, indiceArrivee, Piece.PION_BLANC, true, null, false, null, false, false));
                }
                // en passant diagonal droite
                if (enPassant != 0L && (enPassant & bitArrivee) != 0L) {
                    coups.add(creerCoup(indiceDepart, indiceArrivee, Piece.PION_BLANC, true, Piece.PION_NOIR, false, null, false, true));
                }
            }
        }
    }

    // génère les coups d'un pion noir depuis une case de départ donnée
    private static void genererPionNoirDepuis(int indiceDepart,
                                              long occupes,
                                              long adversaires,
                                              long enPassant,
                                              List<Coup> coups) {

        // avance d'une case (indice croissant = vers le bas)
        int  indiceArrivee1 = indiceDepart + 8;
        if (indiceArrivee1 <= 63) {
            long bitArrivee1 = 1L << indiceArrivee1;
            if ((occupes & bitArrivee1) == 0L) {
                if (indiceArrivee1 >= 56) {
                    // promotion sur la rangée 1 (indices 56..63)
                    ajouterPromotions(coups, indiceDepart, indiceArrivee1, Piece.PION_NOIR, false, null, false);
                } else {
                    coups.add(creerCoup(indiceDepart, indiceArrivee1, Piece.PION_NOIR, false, null, false, null, false, false));

                    // avance de deux cases depuis la rangée 7 (indices 8..15)
                    if (indiceDepart >= 8 && indiceDepart <= 15) {
                        int  indiceArrivee2 = indiceDepart + 16;
                        long bitArrivee2    = 1L << indiceArrivee2;
                        if ((occupes & bitArrivee2) == 0L) {
                            coups.add(creerCoup(indiceDepart, indiceArrivee2, Piece.PION_NOIR, false, null, false, null, false, false));
                        }
                    }
                }
            }
        }

        // capture diagonale gauche (colonne a exclue)
        if (indiceDepart % 8 != 0) {
            int  indiceArrivee = indiceDepart + 7;
            if (indiceArrivee <= 63) {
                long bitArrivee = 1L << indiceArrivee;
                if ((adversaires & bitArrivee) != 0L) {
                    if (indiceArrivee >= 56) ajouterPromotions(coups, indiceDepart, indiceArrivee, Piece.PION_NOIR, true, null, false);
                    else coups.add(creerCoup(indiceDepart, indiceArrivee, Piece.PION_NOIR, true, null, false, null, false, false));
                }
                if (enPassant != 0L && (enPassant & bitArrivee) != 0L) {
                    coups.add(creerCoup(indiceDepart, indiceArrivee, Piece.PION_NOIR, true, Piece.PION_BLANC, false, null, false, true));
                }
            }
        }

        // capture diagonale droite (colonne h exclue)
        if (indiceDepart % 8 != 7) {
            int  indiceArrivee = indiceDepart + 9;
            if (indiceArrivee <= 63) {
                long bitArrivee = 1L << indiceArrivee;
                if ((adversaires & bitArrivee) != 0L) {
                    if (indiceArrivee >= 56) ajouterPromotions(coups, indiceDepart, indiceArrivee, Piece.PION_NOIR, true, null, false);
                    else coups.add(creerCoup(indiceDepart, indiceArrivee, Piece.PION_NOIR, true, null, false, null, false, false));
                }
                if (enPassant != 0L && (enPassant & bitArrivee) != 0L) {
                    coups.add(creerCoup(indiceDepart, indiceArrivee, Piece.PION_NOIR, true, Piece.PION_BLANC, false, null, false, true));
                }
            }
        }
    }

    // ajoute les 4 variantes de promotion (dame, tour, fou, cavalier)
    private static void ajouterPromotions(List<Coup> coups,
                                          int indiceDepart,
                                          int indiceArrivee,
                                          Piece pion,
                                          boolean capture,
                                          Piece pieceCapturee,
                                          boolean enPassant) {

        Piece dame     = (pion == Piece.PION_BLANC) ? Piece.DAME_BLANCHE   : Piece.DAME_NOIRE;
        Piece tour     = (pion == Piece.PION_BLANC) ? Piece.TOUR_BLANC     : Piece.TOUR_NOIRE;
        Piece fou      = (pion == Piece.PION_BLANC) ? Piece.FOU_BLANC      : Piece.FOU_NOIR;
        Piece cavalier = (pion == Piece.PION_BLANC) ? Piece.CAVALIER_BLANC : Piece.CAVALIER_NOIR;

        coups.add(creerCoup(indiceDepart, indiceArrivee, pion, capture, pieceCapturee, true, dame,     false, enPassant));
        coups.add(creerCoup(indiceDepart, indiceArrivee, pion, capture, pieceCapturee, true, tour,     false, enPassant));
        coups.add(creerCoup(indiceDepart, indiceArrivee, pion, capture, pieceCapturee, true, fou,      false, enPassant));
        coups.add(creerCoup(indiceDepart, indiceArrivee, pion, capture, pieceCapturee, true, cavalier, false, enPassant));
    }

    /* ============================================================
     * CAVALIERS
     * ============================================================ */

    // génère tous les coups de cavaliers pour la couleur au trait
    private static void genererCoupsCavaliers(Plateau plateau, Couleur trait, List<Coup> coups) {
        long cavaliers  = (trait == Couleur.BLANC) ? plateau.bitboard(Piece.CAVALIER_BLANC) : plateau.bitboard(Piece.CAVALIER_NOIR);
        long allies     = (trait == Couleur.BLANC) ? plateau.blancs() : plateau.noirs();
        long ennemis    = (trait == Couleur.BLANC) ? plateau.noirs()  : plateau.blancs();
        Piece piece     = (trait == Couleur.BLANC) ? Piece.CAVALIER_BLANC : Piece.CAVALIER_NOIR;

        long curseur = cavaliers;
        while (curseur != 0L) {
            long bit           = curseur & -curseur;
            int  indiceDepart  = Long.numberOfTrailingZeros(bit);

            // masque des cases attaquées par ce cavalier moins les cases alliées
            long destinations = Masques.attaquesCavalier(indiceDepart) & ~allies;

            long cibles = destinations;
            while (cibles != 0L) {
                long bitDestination = cibles & -cibles;
                int  indiceArrivee  = Long.numberOfTrailingZeros(bitDestination);
                boolean capture     = (ennemis & bitDestination) != 0L;
                coups.add(creerCoup(indiceDepart, indiceArrivee, piece, capture, null, false, null, false, false));
                cibles ^= bitDestination;
            }

            curseur ^= bit;
        }
    }

    /* ============================================================
     * FOUS (diagonales)
     * ============================================================ */

    // génère tous les coups de fous pour la couleur au trait via les rayons diagonaux
    private static void genererCoupsFous(Plateau plateau, Couleur trait, List<Coup> coups) {
        long fous    = (trait == Couleur.BLANC) ? plateau.bitboard(Piece.FOU_BLANC)  : plateau.bitboard(Piece.FOU_NOIR);
        long allies  = (trait == Couleur.BLANC) ? plateau.blancs() : plateau.noirs();
        long ennemis = (trait == Couleur.BLANC) ? plateau.noirs()  : plateau.blancs();
        long occupes = plateau.occupes();
        Piece piece  = (trait == Couleur.BLANC) ? Piece.FOU_BLANC : Piece.FOU_NOIR;

        long curseur = fous;
        while (curseur != 0L) {
            long bit          = curseur & -curseur;
            int  indiceDepart = Long.numberOfTrailingZeros(bit);

            long destinations = Rayons.attaquesDiagonales(indiceDepart, occupes) & ~allies;

            long cibles = destinations;
            while (cibles != 0L) {
                long bitDestination = cibles & -cibles;
                int  indiceArrivee  = Long.numberOfTrailingZeros(bitDestination);
                boolean capture     = (ennemis & bitDestination) != 0L;
                coups.add(creerCoup(indiceDepart, indiceArrivee, piece, capture, null, false, null, false, false));
                cibles ^= bitDestination;
            }

            curseur ^= bit;
        }
    }

    /* ============================================================
     * TOURS (horizontales et verticales)
     * ============================================================ */

    // génère tous les coups de tours pour la couleur au trait via les rayons HV
    private static void genererCoupsTours(Plateau plateau, Couleur trait, List<Coup> coups) {
        long tours   = (trait == Couleur.BLANC) ? plateau.bitboard(Piece.TOUR_BLANC)  : plateau.bitboard(Piece.TOUR_NOIRE);
        long allies  = (trait == Couleur.BLANC) ? plateau.blancs() : plateau.noirs();
        long ennemis = (trait == Couleur.BLANC) ? plateau.noirs()  : plateau.blancs();
        long occupes = plateau.occupes();
        Piece piece  = (trait == Couleur.BLANC) ? Piece.TOUR_BLANC : Piece.TOUR_NOIRE;

        long curseur = tours;
        while (curseur != 0L) {
            long bit          = curseur & -curseur;
            int  indiceDepart = Long.numberOfTrailingZeros(bit);

            long destinations = Rayons.attaquesHorizontalesEtVerticales(indiceDepart, occupes) & ~allies;

            long cibles = destinations;
            while (cibles != 0L) {
                long bitDestination = cibles & -cibles;
                int  indiceArrivee  = Long.numberOfTrailingZeros(bitDestination);
                boolean capture     = (ennemis & bitDestination) != 0L;
                coups.add(creerCoup(indiceDepart, indiceArrivee, piece, capture, null, false, null, false, false));
                cibles ^= bitDestination;
            }

            curseur ^= bit;
        }
    }

    /* ============================================================
     * DAMES (diagonales + horizontales/verticales)
     * ============================================================ */

    // génère tous les coups de dames pour la couleur au trait (union des rayons fou + tour)
    private static void genererCoupsDames(Plateau plateau, Couleur trait, List<Coup> coups) {
        long dames   = (trait == Couleur.BLANC) ? plateau.bitboard(Piece.DAME_BLANCHE) : plateau.bitboard(Piece.DAME_NOIRE);
        long allies  = (trait == Couleur.BLANC) ? plateau.blancs() : plateau.noirs();
        long ennemis = (trait == Couleur.BLANC) ? plateau.noirs()  : plateau.blancs();
        long occupes = plateau.occupes();
        Piece piece  = (trait == Couleur.BLANC) ? Piece.DAME_BLANCHE : Piece.DAME_NOIRE;

        long curseur = dames;
        while (curseur != 0L) {
            long bit          = curseur & -curseur;
            int  indiceDepart = Long.numberOfTrailingZeros(bit);

            long destinations = (Rayons.attaquesDiagonales(indiceDepart, occupes)
                    | Rayons.attaquesHorizontalesEtVerticales(indiceDepart, occupes))
                    & ~allies;

            long cibles = destinations;
            while (cibles != 0L) {
                long bitDestination = cibles & -cibles;
                int  indiceArrivee  = Long.numberOfTrailingZeros(bitDestination);
                boolean capture     = (ennemis & bitDestination) != 0L;
                coups.add(creerCoup(indiceDepart, indiceArrivee, piece, capture, null, false, null, false, false));
                cibles ^= bitDestination;
            }

            curseur ^= bit;
        }
    }

    /* ============================================================
     * ROI + ROQUES
     * ============================================================ */

    // génère les coups normaux du roi et tente de générer les roques
    private static void genererCoupsRoiEtRoques(Plateau plateau, Couleur trait, List<Coup> coups) {
        Piece roiPiece = (trait == Couleur.BLANC) ? Piece.ROI_BLANC : Piece.ROI_NOIR;
        long  roi      = plateau.bitboard(roiPiece);

        if (roi == 0L) return; // position invalide : éviter le crash

        long allies  = (trait == Couleur.BLANC) ? plateau.blancs() : plateau.noirs();
        long ennemis = (trait == Couleur.BLANC) ? plateau.noirs()  : plateau.blancs();

        int  indiceDepart = Long.numberOfTrailingZeros(roi);
        long destinations = Masques.attaquesRoi(indiceDepart) & ~allies;

        // coups normaux du roi (cases adjacentes libres ou capturables)
        long cibles = destinations;
        while (cibles != 0L) {
            long bitDestination = cibles & -cibles;
            int  indiceArrivee  = Long.numberOfTrailingZeros(bitDestination);
            boolean capture     = (ennemis & bitDestination) != 0L;
            coups.add(creerCoup(indiceDepart, indiceArrivee, roiPiece, capture, null, false, null, false, false));
            cibles ^= bitDestination;
        }

        // roques (pseudo-légaux avec vérification partielle des attaques)
        genererRoques(plateau, trait, coups);
    }

    // génère les roques disponibles en vérifiant les cases vides et non attaquées
    private static void genererRoques(Plateau plateau, Couleur trait, List<Coup> coups) {
        // conditions vérifiées : (1) droit de roque, (2) cases vides, (3) roi pas en échec,
        // (4) roi ne traverse pas une case attaquée, (5) case d'arrivée non attaquée

        Couleur adverse     = trait.inverse();
        long    attaquesAdv = Arbitre.casesAttaqueesPar(plateau, adverse);
        long    occupes     = plateau.occupes();

        if (trait == Couleur.BLANC) {
            if (Arbitre.estEnEchec(plateau, Couleur.BLANC)) return;

            // petit roque blanc : e1(60) → g1(62), tour h1(63) → f1(61)
            if (plateau.getRoqueBlancRoi()) {
                boolean tourPresente = (plateau.bitboard(Piece.TOUR_BLANC) & (1L << 63)) != 0L;
                boolean casesVides   = (occupes     & ((1L << 61) | (1L << 62))) == 0L;
                boolean casesSures   = (attaquesAdv & ((1L << 61) | (1L << 62))) == 0L;
                if (tourPresente && casesVides && casesSures) {
                    coups.add(creerCoup(60, 62, Piece.ROI_BLANC, false, null, false, null, true, false));
                }
            }

            // grand roque blanc : e1(60) → c1(58), tour a1(56) → d1(59)
            if (plateau.getRoqueBlancReine()) {
                boolean tourPresente = (plateau.bitboard(Piece.TOUR_BLANC) & (1L << 56)) != 0L;
                boolean casesVides   = (occupes     & ((1L << 57) | (1L << 58) | (1L << 59))) == 0L;
                boolean casesSures   = (attaquesAdv & ((1L << 59) | (1L << 58))) == 0L; // roi traverse d1 et arrive c1
                if (tourPresente && casesVides && casesSures) {
                    coups.add(creerCoup(60, 58, Piece.ROI_BLANC, false, null, false, null, true, false));
                }
            }

        } else {
            if (Arbitre.estEnEchec(plateau, Couleur.NOIR)) return;

            // petit roque noir : e8(4) → g8(6), tour h8(7) → f8(5)
            if (plateau.getRoqueNoirRoi()) {
                boolean tourPresente = (plateau.bitboard(Piece.TOUR_NOIRE) & (1L << 7)) != 0L;
                boolean casesVides   = (occupes     & ((1L << 5) | (1L << 6))) == 0L;
                boolean casesSures   = (attaquesAdv & ((1L << 5) | (1L << 6))) == 0L;
                if (tourPresente && casesVides && casesSures) {
                    coups.add(creerCoup(4, 6, Piece.ROI_NOIR, false, null, false, null, true, false));
                }
            }

            // grand roque noir : e8(4) → c8(2), tour a8(0) → d8(3)
            if (plateau.getRoqueNoirReine()) {
                boolean tourPresente = (plateau.bitboard(Piece.TOUR_NOIRE) & (1L << 0)) != 0L;
                boolean casesVides   = (occupes     & ((1L << 1) | (1L << 2) | (1L << 3))) == 0L;
                boolean casesSures   = (attaquesAdv & ((1L << 3) | (1L << 2))) == 0L; // roi traverse d8 et arrive c8
                if (tourPresente && casesVides && casesSures) {
                    coups.add(creerCoup(4, 2, Piece.ROI_NOIR, false, null, false, null, true, false));
                }
            }
        }
    }

    /* ============================================================
     * Fabrique de Coup
     * ============================================================ */

    // construit un Coup depuis des indices entiers (évite la création directe de Case à chaque appel)
    private static Coup creerCoup(int indiceDepart,
                                  int indiceArrivee,
                                  Piece pieceDeplacee,
                                  boolean capture,
                                  Piece pieceCapturee,
                                  boolean promotion,
                                  Piece piecePromotion,
                                  boolean roque,
                                  boolean enPassant) {
        return new Coup(
                Case.depuisIndice(indiceDepart),
                Case.depuisIndice(indiceArrivee),
                pieceDeplacee,
                capture,
                pieceCapturee,
                promotion,
                piecePromotion,
                roque,
                enPassant);
    }
}
