package fr.moteurechecs.plateau;

/**
 * Représente l'état complet d'une position d'échecs via des bitboards 64 bits.
 *
 * Chaque type de pièce est encodé dans un {@code long} séparé (12 bitboards au total).
 * Des bitboards dérivés (occupes, blancs, noirs, vides) sont recalculés après chaque coup
 * pour accélérer la génération de coups et les tests d'attaque.
 *
 * Convention d'indice : 0 = a8, 63 = h1 (ligne 8 en haut, ligne 1 en bas).
 */
public final class Plateau {

    // ===== bitboards des pièces (un par type) =====
    private long pionsBlancs;
    private long cavaliersBlancs;
    private long fousBlancs;
    private long toursBlanches;
    private long reineBlanche;
    private long roiBlanc;

    private long pionsNoirs;
    private long cavaliersNoirs;
    private long fousNoirs;
    private long toursNoires;
    private long reineNoire;
    private long roiNoir;

    // ===== état du jeu =====
    private Couleur trait;
    private boolean roqueBlancRoi;
    private boolean roqueBlancReine;
    private boolean roqueNoirRoi;
    private boolean roqueNoirReine;

    private long enPassant; // bitboard d'une case ou 0

    // ===== bitboards dérivés (recalculés après chaque coup pour la performance) =====
    private long occupes;
    private long blancs;
    private long noirs;
    private long vides;

    /**
     * Construit un plateau vide (utile pour les tests).
     * Trait = BLANC, aucun droit de roque, pas d'en passant.
     */
    public Plateau() {
        this.trait           = Couleur.BLANC;
        this.roqueBlancRoi   = false;
        this.roqueBlancReine = false;
        this.roqueNoirRoi    = false;
        this.roqueNoirReine  = false;
        this.enPassant       = 0L;
        recalculerDerives();
    }

    /**
     * Crée une copie profonde du plateau courant.
     * Utilisée pour la recherche parallèle (chaque thread travaille sur sa propre copie).
     *
     * @return copie indépendante de ce plateau
     */
    public Plateau copie() {
        Plateau copie = new Plateau();
        copie.pionsBlancs      = this.pionsBlancs;
        copie.cavaliersBlancs  = this.cavaliersBlancs;
        copie.fousBlancs       = this.fousBlancs;
        copie.toursBlanches    = this.toursBlanches;
        copie.reineBlanche     = this.reineBlanche;
        copie.roiBlanc         = this.roiBlanc;
        copie.pionsNoirs       = this.pionsNoirs;
        copie.cavaliersNoirs   = this.cavaliersNoirs;
        copie.fousNoirs        = this.fousNoirs;
        copie.toursNoires      = this.toursNoires;
        copie.reineNoire       = this.reineNoire;
        copie.roiNoir          = this.roiNoir;
        copie.trait            = this.trait;
        copie.roqueBlancRoi    = this.roqueBlancRoi;
        copie.roqueBlancReine  = this.roqueBlancReine;
        copie.roqueNoirRoi     = this.roqueNoirRoi;
        copie.roqueNoirReine   = this.roqueNoirReine;
        copie.enPassant        = this.enPassant;
        copie.recalculerDerives();
        return copie;
    }

    /**
     * Retourne la case en passant courante.
     *
     * @return bitboard d'une case, ou 0 si aucune case en passant
     */
    public long getEnPassant() {
        return enPassant;
    }

    /**
     * Indique si les Blancs ont encore le droit de roquer côté roi (petit roque).
     *
     * @return {@code true} si le droit est conservé
     */
    public boolean getRoqueBlancRoi() {
        return roqueBlancRoi;
    }

    /**
     * Indique si les Blancs ont encore le droit de roquer côté reine (grand roque).
     *
     * @return {@code true} si le droit est conservé
     */
    public boolean getRoqueBlancReine() {
        return roqueBlancReine;
    }

    /**
     * Indique si les Noirs ont encore le droit de roquer côté roi (petit roque).
     *
     * @return {@code true} si le droit est conservé
     */
    public boolean getRoqueNoirRoi() {
        return roqueNoirRoi;
    }

    /**
     * Indique si les Noirs ont encore le droit de roquer côté reine (grand roque).
     *
     * @return {@code true} si le droit est conservé
     */
    public boolean getRoqueNoirReine() {
        return roqueNoirReine;
    }

    /**
     * Crée la position initiale standard des échecs.
     *
     * @return plateau en position initiale
     */
    public static Plateau positionInitiale() {
        return depuisFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    /**
     * Crée un plateau depuis une chaîne FEN (Forsyth-Edwards Notation).
     *
     * @param fen chaîne FEN à parser (au moins 4 champs obligatoires)
     * @return plateau correspondant
     * @throws IllegalArgumentException si la FEN est nulle, vide ou malformée
     */
    public static Plateau depuisFEN(String fen) {
        if (fen == null || fen.isBlank()) {
            throw new IllegalArgumentException("FEN invalide (vide)");
        }

        Plateau nouveau = new Plateau();
        // réinitialisation complète de tous les bitboards
        nouveau.pionsBlancs = nouveau.cavaliersBlancs = nouveau.fousBlancs
                = nouveau.toursBlanches = nouveau.reineBlanche = nouveau.roiBlanc = 0L;
        nouveau.pionsNoirs = nouveau.cavaliersNoirs = nouveau.fousNoirs
                = nouveau.toursNoires = nouveau.reineNoire = nouveau.roiNoir = 0L;
        nouveau.enPassant = 0L;
        nouveau.roqueBlancRoi = nouveau.roqueBlancReine = nouveau.roqueNoirRoi = nouveau.roqueNoirReine = false;

        String[] champsfen = fen.trim().split("\\s+");
        if (champsfen.length < 4) {
            throw new IllegalArgumentException("FEN invalide (au moins 4 champs requis)");
        }

        analyserPlacement(nouveau, champsfen[0]);
        nouveau.trait = "w".equals(champsfen[1]) ? Couleur.BLANC : Couleur.NOIR;
        analyserRoques(nouveau, champsfen[2]);
        analyserEnPassant(nouveau, champsfen[3]);

        nouveau.recalculerDerives();
        return nouveau;
    }

    /**
     * Retourne la couleur du joueur au trait.
     *
     * @return couleur au trait
     */
    public Couleur trait() {
        return trait;
    }

    /**
     * Retourne le bitboard d'une pièce exacte.
     *
     * @param piece pièce dont on veut le bitboard
     * @return bitboard correspondant (0 si la pièce est absente)
     */
    public long bitboard(Piece piece) {
        return switch (piece) {
            case PION_BLANC     -> pionsBlancs;
            case CAVALIER_BLANC -> cavaliersBlancs;
            case FOU_BLANC      -> fousBlancs;
            case TOUR_BLANC     -> toursBlanches;
            case DAME_BLANCHE   -> reineBlanche;
            case ROI_BLANC      -> roiBlanc;
            case PION_NOIR      -> pionsNoirs;
            case CAVALIER_NOIR  -> cavaliersNoirs;
            case FOU_NOIR       -> fousNoirs;
            case TOUR_NOIRE     -> toursNoires;
            case DAME_NOIRE     -> reineNoire;
            case ROI_NOIR       -> roiNoir;
        };
    }

    /**
     * Indique si une case est occupée.
     *
     * @param c case à tester
     * @return {@code true} si la case contient une pièce
     */
    public boolean estOccupe(Case c) {
        return (occupes & c.bit()) != 0L;
    }

    /**
     * Indique si une case est vide.
     *
     * @param c case à tester
     * @return {@code true} si la case est libre
     */
    public boolean estVide(Case c) {
        return !estOccupe(c);
    }

    /**
     * Indique si une case contient une pièce précise.
     *
     * @param c case à tester
     * @param p pièce attendue
     * @return {@code true} si la case contient exactement cette pièce
     */
    public boolean contientPiece(Case c, Piece p) {
        return (bitboard(p) & c.bit()) != 0L;
    }

    /**
     * Retourne la pièce présente sur une case.
     *
     * @param c case à interroger
     * @return pièce présente, ou {@code null} si la case est vide
     */
    public Piece pieceEn(Case c) {
        long masque = c.bit();

        if ((pionsBlancs      & masque) != 0) return Piece.PION_BLANC;
        if ((cavaliersBlancs  & masque) != 0) return Piece.CAVALIER_BLANC;
        if ((fousBlancs       & masque) != 0) return Piece.FOU_BLANC;
        if ((toursBlanches    & masque) != 0) return Piece.TOUR_BLANC;
        if ((reineBlanche     & masque) != 0) return Piece.DAME_BLANCHE;
        if ((roiBlanc         & masque) != 0) return Piece.ROI_BLANC;

        if ((pionsNoirs       & masque) != 0) return Piece.PION_NOIR;
        if ((cavaliersNoirs   & masque) != 0) return Piece.CAVALIER_NOIR;
        if ((fousNoirs        & masque) != 0) return Piece.FOU_NOIR;
        if ((toursNoires      & masque) != 0) return Piece.TOUR_NOIRE;
        if ((reineNoire       & masque) != 0) return Piece.DAME_NOIRE;
        if ((roiNoir          & masque) != 0) return Piece.ROI_NOIR;

        return null;
    }

    /**
     * Bitboard de toutes les pièces blanches.
     *
     * @return bitboard des blancs
     */
    public long blancs() { return blancs; }

    /**
     * Bitboard de toutes les pièces noires.
     *
     * @return bitboard des noirs
     */
    public long noirs()   { return noirs;   }

    /**
     * Bitboard de toutes les cases occupées.
     *
     * @return bitboard des cases occupées
     */
    public long occupes() { return occupes; }

    /**
     * Bitboard de toutes les cases vides.
     *
     * @return bitboard des cases libres
     */
    public long vides()   { return vides;   }

    /**
     * Joue un coup en modifiant le plateau en place.
     *
     * @param coup coup à jouer (non null)
     */
    public void jouer(Coup coup) {
        jouerSansSauvegarde(coup);
    }

    /**
     * Capture l'état actuel du plateau dans un objet sauvegardable.
     *
     * @return snapshot de l'état courant
     */
    public EtatPlateau sauvegarderEtat() {
        return new EtatPlateau(
                pionsBlancs, cavaliersBlancs, fousBlancs, toursBlanches, reineBlanche, roiBlanc,
                pionsNoirs, cavaliersNoirs, fousNoirs, toursNoires, reineNoire, roiNoir,
                trait, roqueBlancRoi, roqueBlancReine, roqueNoirRoi, roqueNoirReine, enPassant);
    }

    /**
     * Joue un coup et retourne une sauvegarde pour pouvoir l'annuler ensuite.
     *
     * @param coup coup à jouer
     * @return snapshot de l'état avant le coup
     */
    public EtatPlateau jouerAvecSauvegarde(Coup coup) {
        EtatPlateau sauvegarde = sauvegarderEtat();
        jouerSansSauvegarde(coup);
        return sauvegarde;
    }

    /**
     * Annule un coup en restaurant une sauvegarde précédente.
     *
     * @param sauvegarde état à restaurer
     */
    public void annuler(EtatPlateau sauvegarde) {
        this.pionsBlancs     = sauvegarde.pionsBlancs();
        this.cavaliersBlancs = sauvegarde.cavaliersBlancs();
        this.fousBlancs      = sauvegarde.fousBlancs();
        this.toursBlanches   = sauvegarde.toursBlanches();
        this.reineBlanche    = sauvegarde.reineBlanche();
        this.roiBlanc        = sauvegarde.roiBlanc();
        this.pionsNoirs      = sauvegarde.pionsNoirs();
        this.cavaliersNoirs  = sauvegarde.cavaliersNoirs();
        this.fousNoirs       = sauvegarde.fousNoirs();
        this.toursNoires     = sauvegarde.toursNoires();
        this.reineNoire      = sauvegarde.reineNoire();
        this.roiNoir         = sauvegarde.roiNoir();
        this.trait           = sauvegarde.trait();
        this.roqueBlancRoi   = sauvegarde.roqueBlancRoi();
        this.roqueBlancReine = sauvegarde.roqueBlancReine();
        this.roqueNoirRoi    = sauvegarde.roqueNoirRoi();
        this.roqueNoirReine  = sauvegarde.roqueNoirReine();
        this.enPassant       = sauvegarde.enPassant();
        recalculerDerives();
    }

    /**
     * Indique si une couleur est en échec dans la position courante.
     *
     * @param couleur couleur dont on teste le roi
     * @return {@code true} si le roi est attaqué
     */
    public boolean estEnEchec(Couleur couleur) {
        return Arbitre.estEnEchec(this, couleur);
    }

    /**
     * Retourne un bitboard des cases attaquées par une couleur.
     *
     * @param couleur couleur attaquante
     * @return bitboard des cases couvertes
     */
    public long casesAttaqueesPar(Couleur couleur) {
        return Arbitre.casesAttaqueesPar(this, couleur);
    }

    /**
     * Représentation ASCII du plateau (utile pour le débogage).
     *
     * @return chaîne multi-lignes avec les pièces en notation FEN
     */
    public String versASCII() {
        StringBuilder sb = new StringBuilder();
        for (int ligne = 0; ligne < 8; ligne++) {
            sb.append(8 - ligne).append("  ");
            for (int colonne = 0; colonne < 8; colonne++) {
                int    indice = ligne * 8 + colonne;
                Piece  piece  = pieceEn(Case.depuisIndice(indice));
                sb.append(piece == null ? ". " : (piece.caractereFEN() + " "));
            }
            sb.append('\n');
        }
        sb.append("\n   a b c d e f g h\n");
        sb.append("Trait: ").append(trait).append('\n');
        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return versASCII();
    }

    /*
     * ===========================
     * Getters bitboards (accès package pour Arbitre)
     * ===========================
     */

    long pionsBlancsBitboard()      { return pionsBlancs;     }
    long cavaliersBlancsBitboard()  { return cavaliersBlancs; }
    long fousBlancsBitboard()       { return fousBlancs;      }
    long toursBlanchesBitboard()    { return toursBlanches;   }
    long reineBlancheBitboard()     { return reineBlanche;    }
    long roiBlancBitboard()         { return roiBlanc;        }
    long pionsNoirsBitboard()       { return pionsNoirs;      }
    long cavaliersNoirsBitboard()   { return cavaliersNoirs;  }
    long fousNoirsBitboard()        { return fousNoirs;       }
    long toursNoiresBitboard()      { return toursNoires;     }
    long reineNoireBitboard()       { return reineNoire;      }
    long roiNoirBitboard()          { return roiNoir;         }

    /*
     * ===========================
     * Méthodes privées
     * ===========================
     */

    // recalcule les bitboards dérivés après chaque modification du plateau
    private void recalculerDerives() {
        blancs  = pionsBlancs | cavaliersBlancs | fousBlancs | toursBlanches | reineBlanche | roiBlanc;
        noirs   = pionsNoirs  | cavaliersNoirs  | fousNoirs  | toursNoires   | reineNoire   | roiNoir;
        occupes = blancs | noirs;
        vides   = ~occupes;
    }

    // applique un coup au plateau sans sauvegarder l'état précédent
    private void jouerSansSauvegarde(Coup coup) {
        if (coup == null) throw new IllegalArgumentException("Coup null");

        Piece piece         = coup.pieceDeplacee();
        long  masqueDepart  = coup.depart().bit();
        long  masqueArrivee = coup.arrivee().bit();

        // 1) capture : retirer la pièce adverse sur la case d'arrivée
        if (coup.estCapture()) {
            traiterCapture(coup, masqueArrivee);
        }

        // 2) en passant : supprimer le pion capturé derrière la case d'arrivée
        if (coup.estEnPassant()) {
            traiterEnPassantCapture(coup.arrivee().indice());
        }

        // 3) déplacer la pièce de la case de départ vers la case d'arrivée
        deplacerPiece(piece, masqueDepart, masqueArrivee);

        // 4) promotion : remplacer le pion par la pièce choisie
        if (coup.estPromotion()) {
            traiterPromotion(piece, coup.piecePromotion(), masqueArrivee);
        }

        // 5) roque : déplacer la tour du côté correspondant
        if (coup.estRoque()) {
            deplacerTourApresRoque(piece, coup.depart().indice(), coup.arrivee().indice());
        }

        // 6) mise à jour de la case en passant pour le prochain coup
        mettreAJourEnPassant(piece, coup.depart().indice(), coup.arrivee().indice());

        // 7) mise à jour des droits de roque
        mettreAJourDroitsRoque(coup);

        // 8) changement de trait
        trait = trait.inverse();

        // 9) recalcul des bitboards dérivés
        recalculerDerives();
    }

    // retire la pièce capturée de la case d'arrivée
    private void traiterCapture(Coup coup, long masqueArrivee) {
        Piece pieceCapturee = coup.pieceCapturee();
        if (pieceCapturee == null) {
            // type inconnu : nettoyer tous les bitboards sur cette case
            supprimerPieceSurMasque(masqueArrivee);
        } else {
            supprimerPiece(pieceCapturee, masqueArrivee);
        }
    }

    // supprime le pion adverse capturé en passant (il se trouve sur la rangée derrière l'arrivée)
    private void traiterEnPassantCapture(int indiceArrivee) {
        int  indiceCapture = (trait == Couleur.BLANC) ? (indiceArrivee + 8) : (indiceArrivee - 8);
        long masqueCapture = 1L << indiceCapture;
        if (trait == Couleur.BLANC) {
            pionsNoirs  &= ~masqueCapture;
        } else {
            pionsBlancs &= ~masqueCapture;
        }
    }

    // remplace le pion promu par la pièce choisie sur la case d'arrivée
    private void traiterPromotion(Piece piecePion, Piece piecePromue, long masqueArrivee) {
        if (piecePromue == null) throw new IllegalArgumentException("Promotion sans piècePromotion");
        // retirer le pion de la case d'arrivée
        if (piecePion == Piece.PION_BLANC) pionsBlancs &= ~masqueArrivee;
        if (piecePion == Piece.PION_NOIR)  pionsNoirs  &= ~masqueArrivee;
        // placer la pièce de promotion
        ajouterPiece(piecePromue, masqueArrivee);
    }

    // déplace la tour associée après un roque (petit ou grand, blanc ou noir)
    private void deplacerTourApresRoque(Piece roi, int indiceDepart, int indiceArrivee) {
        if (roi == Piece.ROI_BLANC) {
            if (indiceDepart == 60 && indiceArrivee == 62) {
                // petit roque blanc : h1 → f1
                toursBlanches = (toursBlanches & ~(1L << 63)) | (1L << 61);
            } else if (indiceDepart == 60 && indiceArrivee == 58) {
                // grand roque blanc : a1 → d1
                toursBlanches = (toursBlanches & ~(1L << 56)) | (1L << 59);
            }
        } else if (roi == Piece.ROI_NOIR) {
            if (indiceDepart == 4 && indiceArrivee == 6) {
                // petit roque noir : h8 → f8
                toursNoires = (toursNoires & ~(1L << 7)) | (1L << 5);
            } else if (indiceDepart == 4 && indiceArrivee == 2) {
                // grand roque noir : a8 → d8
                toursNoires = (toursNoires & ~(1L << 0)) | (1L << 3);
            }
        }
    }

    // met à jour la case en passant si un pion vient d'avancer de deux cases
    private void mettreAJourEnPassant(Piece piece, int indiceDepart, int indiceArrivee) {
        if (piece == Piece.PION_BLANC && indiceDepart - indiceArrivee == 16) {
            enPassant = 1L << (indiceDepart - 8); // case passée par le pion blanc
        } else if (piece == Piece.PION_NOIR && indiceArrivee - indiceDepart == 16) {
            enPassant = 1L << (indiceDepart + 8); // case passée par le pion noir
        } else {
            enPassant = 0L;
        }
    }

    // déplace un bitboard de pièce d'une case à une autre
    private void deplacerPiece(Piece piece, long masqueDepart, long masqueArrivee) {
        switch (piece) {
            case PION_BLANC     -> pionsBlancs     = (pionsBlancs     & ~masqueDepart) | masqueArrivee;
            case CAVALIER_BLANC -> cavaliersBlancs = (cavaliersBlancs & ~masqueDepart) | masqueArrivee;
            case FOU_BLANC      -> fousBlancs      = (fousBlancs      & ~masqueDepart) | masqueArrivee;
            case TOUR_BLANC     -> toursBlanches   = (toursBlanches   & ~masqueDepart) | masqueArrivee;
            case DAME_BLANCHE   -> reineBlanche    = (reineBlanche    & ~masqueDepart) | masqueArrivee;
            case ROI_BLANC      -> roiBlanc        = (roiBlanc        & ~masqueDepart) | masqueArrivee;
            case PION_NOIR      -> pionsNoirs      = (pionsNoirs      & ~masqueDepart) | masqueArrivee;
            case CAVALIER_NOIR  -> cavaliersNoirs  = (cavaliersNoirs  & ~masqueDepart) | masqueArrivee;
            case FOU_NOIR       -> fousNoirs       = (fousNoirs       & ~masqueDepart) | masqueArrivee;
            case TOUR_NOIRE     -> toursNoires     = (toursNoires     & ~masqueDepart) | masqueArrivee;
            case DAME_NOIRE     -> reineNoire      = (reineNoire      & ~masqueDepart) | masqueArrivee;
            case ROI_NOIR       -> roiNoir         = (roiNoir         & ~masqueDepart) | masqueArrivee;
        }
    }

    // ajoute une pièce sur un masque (utilisé pour les promotions)
    private void ajouterPiece(Piece piece, long masque) {
        switch (piece) {
            case PION_BLANC     -> pionsBlancs     |= masque;
            case CAVALIER_BLANC -> cavaliersBlancs |= masque;
            case FOU_BLANC      -> fousBlancs      |= masque;
            case TOUR_BLANC     -> toursBlanches   |= masque;
            case DAME_BLANCHE   -> reineBlanche    |= masque;
            case ROI_BLANC      -> roiBlanc        |= masque;
            case PION_NOIR      -> pionsNoirs      |= masque;
            case CAVALIER_NOIR  -> cavaliersNoirs  |= masque;
            case FOU_NOIR       -> fousNoirs       |= masque;
            case TOUR_NOIRE     -> toursNoires     |= masque;
            case DAME_NOIRE     -> reineNoire      |= masque;
            case ROI_NOIR       -> roiNoir         |= masque;
        }
    }

    // retire une pièce d'un masque (capture normale)
    private void supprimerPiece(Piece piece, long masque) {
        switch (piece) {
            case PION_BLANC     -> pionsBlancs     &= ~masque;
            case CAVALIER_BLANC -> cavaliersBlancs &= ~masque;
            case FOU_BLANC      -> fousBlancs      &= ~masque;
            case TOUR_BLANC     -> toursBlanches   &= ~masque;
            case DAME_BLANCHE   -> reineBlanche    &= ~masque;
            case ROI_BLANC      -> roiBlanc        &= ~masque;
            case PION_NOIR      -> pionsNoirs      &= ~masque;
            case CAVALIER_NOIR  -> cavaliersNoirs  &= ~masque;
            case FOU_NOIR       -> fousNoirs       &= ~masque;
            case TOUR_NOIRE     -> toursNoires     &= ~masque;
            case DAME_NOIRE     -> reineNoire      &= ~masque;
            case ROI_NOIR       -> roiNoir         &= ~masque;
        }
    }

    // nettoie tous les bitboards sur un masque (quand le type de pièce est inconnu)
    private void supprimerPieceSurMasque(long masque) {
        pionsBlancs    &= ~masque;
        cavaliersBlancs &= ~masque;
        fousBlancs     &= ~masque;
        toursBlanches  &= ~masque;
        reineBlanche   &= ~masque;
        roiBlanc       &= ~masque;
        pionsNoirs     &= ~masque;
        cavaliersNoirs &= ~masque;
        fousNoirs      &= ~masque;
        toursNoires    &= ~masque;
        reineNoire     &= ~masque;
        roiNoir        &= ~masque;
    }

    // met à jour les droits de roque suite au déplacement d'un roi ou d'une tour
    private void mettreAJourDroitsRoque(Coup coup) {
        Piece piece         = coup.pieceDeplacee();
        int   indiceDepart  = coup.depart().indice();
        long  masqueArrivee = coup.arrivee().bit();

        // déplacement du roi : perte de tous les droits de ce côté
        if (piece == Piece.ROI_BLANC) {
            roqueBlancRoi   = false;
            roqueBlancReine = false;
        } else if (piece == Piece.ROI_NOIR) {
            roqueNoirRoi   = false;
            roqueNoirReine = false;
        }

        // déplacement d'une tour : perte du droit correspondant
        if (piece == Piece.TOUR_BLANC) {
            if (indiceDepart == 63) roqueBlancRoi   = false; // h1
            if (indiceDepart == 56) roqueBlancReine = false; // a1
        } else if (piece == Piece.TOUR_NOIRE) {
            if (indiceDepart == 7) roqueNoirRoi   = false; // h8
            if (indiceDepart == 0) roqueNoirReine = false; // a8
        }

        // capture d'une tour sur sa case initiale : perte du droit correspondant
        if ((masqueArrivee & (1L << 63)) != 0) roqueBlancRoi   = false;
        if ((masqueArrivee & (1L << 56)) != 0) roqueBlancReine = false;
        if ((masqueArrivee & (1L << 7))  != 0) roqueNoirRoi    = false;
        if ((masqueArrivee & (1L << 0))  != 0) roqueNoirReine  = false;
    }

    // analyse la partie placement d'une chaîne FEN et remplit les bitboards
    private static void analyserPlacement(Plateau nouveau, String placement) {
        int indice = 0; // 0 = a8
        for (int i = 0; i < placement.length(); i++) {
            char c = placement.charAt(i);
            if (c == '/') continue;
            if (Character.isDigit(c)) {
                indice += (c - '0');
                continue;
            }
            if (indice < 0 || indice > 63) {
                throw new IllegalArgumentException("FEN invalide (indice hors [0..63])");
            }
            long bit = 1L << indice;
            switch (c) {
                case 'P' -> nouveau.pionsBlancs     |= bit;
                case 'N' -> nouveau.cavaliersBlancs |= bit;
                case 'B' -> nouveau.fousBlancs      |= bit;
                case 'R' -> nouveau.toursBlanches   |= bit;
                case 'Q' -> nouveau.reineBlanche    |= bit;
                case 'K' -> nouveau.roiBlanc        |= bit;
                case 'p' -> nouveau.pionsNoirs      |= bit;
                case 'n' -> nouveau.cavaliersNoirs  |= bit;
                case 'b' -> nouveau.fousNoirs       |= bit;
                case 'r' -> nouveau.toursNoires     |= bit;
                case 'q' -> nouveau.reineNoire      |= bit;
                case 'k' -> nouveau.roiNoir         |= bit;
                default  -> throw new IllegalArgumentException("FEN invalide (pièce inconnue) : " + c);
            }
            indice++;
        }
    }

    // analyse le champ de droits de roque d'une chaîne FEN
    private static void analyserRoques(Plateau nouveau, String roquesStr) {
        if (!"-".equals(roquesStr)) {
            nouveau.roqueBlancRoi   = roquesStr.contains("K");
            nouveau.roqueBlancReine = roquesStr.contains("Q");
            nouveau.roqueNoirRoi    = roquesStr.contains("k");
            nouveau.roqueNoirReine  = roquesStr.contains("q");
        }
    }

    // analyse le champ en passant d'une chaîne FEN et positionne le bitboard
    private static void analyserEnPassant(Plateau nouveau, String epStr) {
        if (!"-".equals(epStr)) {
            Case caseEP = Case.depuisAlgebrique(epStr);
            nouveau.enPassant = caseEP.bit();
        }
    }
}
