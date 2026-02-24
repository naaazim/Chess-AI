package org.example.chess;

/**
 * <p>
 * La classe {@code Plateau} stocke l’état complet d’une position d’échecs.
 * </p>
 *
 * <p>
 * Elle utilise des bitboards (long 64 bits) pour être rapide, car l’IA va
 * générer
 * énormément de coups (grand facteur de branchement).
 * </p>
 *
 * <p>
 * Convention d’index :
 * </p>
 * <ul>
 * <li>0 = a8</li>
 * <li>63 = h1</li>
 * </ul>
 */
public final class Plateau {

    // Bitboards pièces
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

    // État du jeu
    private Couleur trait;
    private boolean roqueBlancRoi;
    private boolean roqueBlancReine;
    private boolean roqueNoirRoi;
    private boolean roqueNoirReine;

    private long enPassant; // bitboard 1 case ou 0

    // Dérivés (performance)
    private long occupes;
    private long blancs;
    private long noirs;
    private long vides;

    /**
     * <p>
     * Construit un plateau vide (debug/tests) :
     * </p>
     * <ul>
     * <li>trait = BLANC</li>
     * <li>pas de roques</li>
     * <li>pas d’en passant</li>
     * </ul>
     */
    public Plateau() {
        this.trait = Couleur.BLANC;
        this.roqueBlancRoi = false;
        this.roqueBlancReine = false;
        this.roqueNoirRoi = false;
        this.roqueNoirReine = false;
        this.enPassant = 0L;
        recalculerDerives();
    }

    /**
     * <p>
     * Créé une copie profonde du plateau courant.
     * </p>
     * Utile pour la recherche concurrente.
     */
    public Plateau copie() {
        Plateau copie = new Plateau();
        copie.pionsBlancs = this.pionsBlancs;
        copie.cavaliersBlancs = this.cavaliersBlancs;
        copie.fousBlancs = this.fousBlancs;
        copie.toursBlanches = this.toursBlanches;
        copie.reineBlanche = this.reineBlanche;
        copie.roiBlanc = this.roiBlanc;
        copie.pionsNoirs = this.pionsNoirs;
        copie.cavaliersNoirs = this.cavaliersNoirs;
        copie.fousNoirs = this.fousNoirs;
        copie.toursNoires = this.toursNoires;
        copie.reineNoire = this.reineNoire;
        copie.roiNoir = this.roiNoir;
        copie.trait = this.trait;
        copie.roqueBlancRoi = this.roqueBlancRoi;
        copie.roqueBlancReine = this.roqueBlancReine;
        copie.roqueNoirRoi = this.roqueNoirRoi;
        copie.roqueNoirReine = this.roqueNoirReine;
        copie.enPassant = this.enPassant;
        copie.recalculerDerives();
        return copie;
    }

    public long getEnPassant() {
        return enPassant;
    }

    /**
     * <p>
     * Vrai si le blanc a encore le droit de roquer côté roi (petit roque).
     * </p>
     */
    public boolean getRoqueBlancRoi() {
        return roqueBlancRoi;
    }

    /**
     * <p>
     * Vrai si le blanc a encore le droit de roquer côté reine (grand roque).
     * </p>
     */
    public boolean getRoqueBlancReine() {
        return roqueBlancReine;
    }

    /**
     * <p>
     * Vrai si le noir a encore le droit de roquer côté roi (petit roque).
     * </p>
     */
    public boolean getRoqueNoirRoi() {
        return roqueNoirRoi;
    }

    /**
     * <p>
     * Vrai si le noir a encore le droit de roquer côté reine (grand roque).
     * </p>
     */
    public boolean getRoqueNoirReine() {
        return roqueNoirReine;
    }

    /**
     * <p>
     * Crée la position initiale standard.
     * </p>
     *
     * @return plateau initial
     */
    public static Plateau positionInitiale() {
        return depuisFEN("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    }

    /**
     * <p>
     * Crée un plateau depuis une chaîne FEN (version simple).
     * </p>
     *
     * @param fen chaîne FEN
     * @return plateau correspondant
     */
    public static Plateau depuisFEN(String fen) {
        if (fen == null || fen.isBlank()) {
            throw new IllegalArgumentException("FEN invalide (vide)");
        }

        Plateau p = new Plateau();

        // reset
        p.pionsBlancs = p.cavaliersBlancs = p.fousBlancs = p.toursBlanches = p.reineBlanche = p.roiBlanc = 0L;
        p.pionsNoirs = p.cavaliersNoirs = p.fousNoirs = p.toursNoires = p.reineNoire = p.roiNoir = 0L;
        p.enPassant = 0L;
        p.roqueBlancRoi = p.roqueBlancReine = p.roqueNoirRoi = p.roqueNoirReine = false;

        String[] parts = fen.trim().split("\\s+");
        if (parts.length < 4) {
            throw new IllegalArgumentException("FEN invalide (au moins 4 champs)");
        }

        String placement = parts[0];
        String traitStr = parts[1];
        String roquesStr = parts[2];
        String epStr = parts[3];

        // placement
        int indice = 0; // 0=a8
        for (int i = 0; i < placement.length(); i++) {
            char c = placement.charAt(i);
            if (c == '/')
                continue;

            if (Character.isDigit(c)) {
                indice += (c - '0');
                continue;
            }

            if (indice < 0 || indice > 63) {
                throw new IllegalArgumentException("FEN invalide (indice hors [0..63])");
            }

            long bit = 1L << indice;
            switch (c) {
                case 'P' -> p.pionsBlancs |= bit;
                case 'N' -> p.cavaliersBlancs |= bit;
                case 'B' -> p.fousBlancs |= bit;
                case 'R' -> p.toursBlanches |= bit;
                case 'Q' -> p.reineBlanche |= bit;
                case 'K' -> p.roiBlanc |= bit;

                case 'p' -> p.pionsNoirs |= bit;
                case 'n' -> p.cavaliersNoirs |= bit;
                case 'b' -> p.fousNoirs |= bit;
                case 'r' -> p.toursNoires |= bit;
                case 'q' -> p.reineNoire |= bit;
                case 'k' -> p.roiNoir |= bit;

                default -> throw new IllegalArgumentException("FEN invalide (pièce inconnue) : " + c);
            }
            indice++;
        }

        // trait
        p.trait = "w".equals(traitStr) ? Couleur.BLANC : Couleur.NOIR;

        // roques
        if (!"-".equals(roquesStr)) {
            p.roqueBlancRoi = roquesStr.contains("K");
            p.roqueBlancReine = roquesStr.contains("Q");
            p.roqueNoirRoi = roquesStr.contains("k");
            p.roqueNoirReine = roquesStr.contains("q");
        }

        // en passant
        if (!"-".equals(epStr)) {
            Case ep = Case.depuisAlgebrique(epStr);
            p.enPassant = ep.bit();
        }

        p.recalculerDerives();
        return p;
    }

    /**
     * <p>
     * Retourne la couleur au trait.
     * </p>
     */
    public Couleur trait() {
        return trait;
    }

    /**
     * <p>
     * Bitboard d’une pièce exacte.
     * </p>
     *
     * @param piece pièce exacte
     * @return bitboard correspondant
     */
    public long bitboard(Piece piece) {
        return switch (piece) {
            case PION_BLANC -> pionsBlancs;
            case CAVALIER_BLANC -> cavaliersBlancs;
            case FOU_BLANC -> fousBlancs;
            case TOUR_BLANC -> toursBlanches;
            case DAME_BLANCHE -> reineBlanche;
            case ROI_BLANC -> roiBlanc;

            case PION_NOIR -> pionsNoirs;
            case CAVALIER_NOIR -> cavaliersNoirs;
            case FOU_NOIR -> fousNoirs;
            case TOUR_NOIR -> toursNoires;
            case DAME_NOIRE -> reineNoire;
            case ROI_NOIR -> roiNoir;
        };
    }

    public boolean estOccupe(Case c) {
        long b = c.bit();
        return (occupes & b) != 0L;
    }

    public boolean estVide(Case c) {
        return !estOccupe(c);
    }

    public boolean contientPiece(Case c, Piece p) {
        return (bitboard(p) & c.bit()) != 0L;
    }

    public Piece pieceEn(Case c) {
        long b = c.bit();

        if ((pionsBlancs & b) != 0)
            return Piece.PION_BLANC;
        if ((cavaliersBlancs & b) != 0)
            return Piece.CAVALIER_BLANC;
        if ((fousBlancs & b) != 0)
            return Piece.FOU_BLANC;
        if ((toursBlanches & b) != 0)
            return Piece.TOUR_BLANC;
        if ((reineBlanche & b) != 0)
            return Piece.DAME_BLANCHE;
        if ((roiBlanc & b) != 0)
            return Piece.ROI_BLANC;

        if ((pionsNoirs & b) != 0)
            return Piece.PION_NOIR;
        if ((cavaliersNoirs & b) != 0)
            return Piece.CAVALIER_NOIR;
        if ((fousNoirs & b) != 0)
            return Piece.FOU_NOIR;
        if ((toursNoires & b) != 0)
            return Piece.TOUR_NOIR;
        if ((reineNoire & b) != 0)
            return Piece.DAME_NOIRE;
        if ((roiNoir & b) != 0)
            return Piece.ROI_NOIR;

        return null;
    }

    public long blancs() {
        return blancs;
    }

    public long noirs() {
        return noirs;
    }

    public long occupes() {
        return occupes;
    }

    public long vides() {
        return vides;
    }

    /**
     * <p>
     * Joue un coup (modifie le plateau).
     * </p>
     */
    public void jouer(Coup coup) {
        jouerSansSauvegarde(coup);
    }

    /**
     * <p>
     * Joue un coup et retourne une sauvegarde pour pouvoir annuler.
     * </p>
     */
    public EtatPlateau jouerAvecSauvegarde(Coup coup) {
        EtatPlateau s = new EtatPlateau(
                pionsBlancs, cavaliersBlancs, fousBlancs, toursBlanches, reineBlanche, roiBlanc,
                pionsNoirs, cavaliersNoirs, fousNoirs, toursNoires, reineNoire, roiNoir,
                trait, roqueBlancRoi, roqueBlancReine, roqueNoirRoi, roqueNoirReine, enPassant);
        jouerSansSauvegarde(coup);
        return s;
    }

    /**
     * <p>
     * Annule en restaurant la sauvegarde.
     * </p>
     */
    public void annuler(EtatPlateau sauvegarde) {
        this.pionsBlancs = sauvegarde.pionsBlancs();
        this.cavaliersBlancs = sauvegarde.cavaliersBlancs();
        this.fousBlancs = sauvegarde.fousBlancs();
        this.toursBlanches = sauvegarde.toursBlanches();
        this.reineBlanche = sauvegarde.reineBlanche();
        this.roiBlanc = sauvegarde.roiBlanc();

        this.pionsNoirs = sauvegarde.pionsNoirs();
        this.cavaliersNoirs = sauvegarde.cavaliersNoirs();
        this.fousNoirs = sauvegarde.fousNoirs();
        this.toursNoires = sauvegarde.toursNoires();
        this.reineNoire = sauvegarde.reineNoire();
        this.roiNoir = sauvegarde.roiNoir();

        this.trait = sauvegarde.trait();
        this.roqueBlancRoi = sauvegarde.roqueBlancRoi();
        this.roqueBlancReine = sauvegarde.roqueBlancReine();
        this.roqueNoirRoi = sauvegarde.roqueNoirRoi();
        this.roqueNoirReine = sauvegarde.roqueNoirReine();
        this.enPassant = sauvegarde.enPassant();

        recalculerDerives();
    }

    /**
     * <p>
     * Échec pour une couleur ? (délégué à l’arbitre).
     * </p>
     */
    public boolean estEnEchec(Couleur couleur) {
        return Arbitre.estEnEchec(this, couleur);
    }

    /**
     * <p>
     * Cases attaquées par une couleur (délégué à l’arbitre).
     * </p>
     */
    public long casesAttaqueesPar(Couleur couleur) {
        return Arbitre.casesAttaqueesPar(this, couleur);
    }

    /**
     * <p>
     * Affichage ASCII simple pour debug.
     * </p>
     */
    public String versASCII() {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < 8; row++) {
            sb.append(8 - row).append("  ");
            for (int col = 0; col < 8; col++) {
                int idx = row * 8 + col;
                Piece p = pieceEn(Case.depuisIndice(idx));
                sb.append(p == null ? ". " : (p.caractereFEN() + " "));
            }
            sb.append('\n');
        }
        sb.append("\n   a b c d e f g h\n");
        sb.append("Trait: ").append(trait).append('\n');
        return sb.toString();
    }

    @Override
    public String toString() {
        return versASCII();
    }

    /*
     * ===========================
     * Getters bitboards (utilisés par Arbitre)
     * ===========================
     */

    long pionsBlancsBitboard() {
        return pionsBlancs;
    }

    long cavaliersBlancsBitboard() {
        return cavaliersBlancs;
    }

    long fousBlancsBitboard() {
        return fousBlancs;
    }

    long toursBlanchesBitboard() {
        return toursBlanches;
    }

    long reineBlancheBitboard() {
        return reineBlanche;
    }

    long roiBlancBitboard() {
        return roiBlanc;
    }

    long pionsNoirsBitboard() {
        return pionsNoirs;
    }

    long cavaliersNoirsBitboard() {
        return cavaliersNoirs;
    }

    long fousNoirsBitboard() {
        return fousNoirs;
    }

    long toursNoiresBitboard() {
        return toursNoires;
    }

    long reineNoireBitboard() {
        return reineNoire;
    }

    long roiNoirBitboard() {
        return roiNoir;
    }

    /*
     * ===========================
     * Internes
     * ===========================
     */

    private void recalculerDerives() {
        blancs = pionsBlancs | cavaliersBlancs | fousBlancs | toursBlanches | reineBlanche | roiBlanc;
        noirs = pionsNoirs | cavaliersNoirs | fousNoirs | toursNoires | reineNoire | roiNoir;
        occupes = blancs | noirs;
        vides = ~occupes;
    }

    private void jouerSansSauvegarde(Coup coup) {
        if (coup == null)
            throw new IllegalArgumentException("Coup null");

        Piece piece = coup.pieceDeplacee();
        long depart = coup.depart().bit();
        long arrivee = coup.arrivee().bit();

        // 1) Capture (case d'arrivée)
        if (coup.estCapture()) {
            Piece cap = coup.pieceCapturee();
            if (cap == null) {
                supprimerPieceSurMasque(arrivee);
            } else {
                supprimerPiece(cap, arrivee);
            }
        }

        // 2) En passant
        if (coup.estEnPassant()) {
            int idxArr = coup.arrivee().indice();
            int idxPionCapture = (trait == Couleur.BLANC) ? (idxArr + 8) : (idxArr - 8);
            long bitPionCapture = 1L << idxPionCapture;
            if (trait == Couleur.BLANC)
                pionsNoirs &= ~bitPionCapture;
            else
                pionsBlancs &= ~bitPionCapture;
        }

        // 3) Déplacement de la pièce
        deplacerPiece(piece, depart, arrivee);

        // 4) Promotion
        if (coup.estPromotion()) {
            Piece promo = coup.piecePromotion();
            if (promo == null)
                throw new IllegalArgumentException("Promotion sans piècePromotion");

            if (piece == Piece.PION_BLANC)
                pionsBlancs &= ~arrivee;
            if (piece == Piece.PION_NOIR)
                pionsNoirs &= ~arrivee;

            ajouterPiece(promo, arrivee);
        }

        // 5) Roque : déplacer la tour
        if (coup.estRoque()) {
            if (piece == Piece.ROI_BLANC) {
                int dep = coup.depart().indice();
                int arr = coup.arrivee().indice();
                if (dep == 60 && arr == 62) { // petit roque
                    long h1 = 1L << 63, f1 = 1L << 61;
                    toursBlanches = (toursBlanches & ~h1) | f1;
                } else if (dep == 60 && arr == 58) { // grand roque
                    long a1 = 1L << 56, d1 = 1L << 59;
                    toursBlanches = (toursBlanches & ~a1) | d1;
                }
            } else if (piece == Piece.ROI_NOIR) {
                int dep = coup.depart().indice();
                int arr = coup.arrivee().indice();
                if (dep == 4 && arr == 6) { // petit roque
                    long h8 = 1L << 7, f8 = 1L << 5;
                    toursNoires = (toursNoires & ~h8) | f8;
                } else if (dep == 4 && arr == 2) { // grand roque
                    long a8 = 1L << 0, d8 = 1L << 3;
                    toursNoires = (toursNoires & ~a8) | d8;
                }
            }
        }

        // 6) En passant (mise à jour)
        if (piece == Piece.PION_BLANC) {
            int dep = coup.depart().indice();
            int arr = coup.arrivee().indice();
            if (dep - arr == 16)
                enPassant = 1L << (dep - 8);
            else
                enPassant = 0L;
        } else if (piece == Piece.PION_NOIR) {
            int dep = coup.depart().indice();
            int arr = coup.arrivee().indice();
            if (arr - dep == 16)
                enPassant = 1L << (dep + 8);
            else
                enPassant = 0L;
        } else {
            enPassant = 0L;
        }

        // 7) Droits de roque
        mettreAJourDroitsRoque(coup);

        // 8) Trait
        trait = trait.inverse();

        // 9) Dérivés
        recalculerDerives();
    }

    private void deplacerPiece(Piece piece, long depart, long arrivee) {
        switch (piece) {
            case PION_BLANC -> pionsBlancs = (pionsBlancs & ~depart) | arrivee;
            case CAVALIER_BLANC -> cavaliersBlancs = (cavaliersBlancs & ~depart) | arrivee;
            case FOU_BLANC -> fousBlancs = (fousBlancs & ~depart) | arrivee;
            case TOUR_BLANC -> toursBlanches = (toursBlanches & ~depart) | arrivee;
            case DAME_BLANCHE -> reineBlanche = (reineBlanche & ~depart) | arrivee;
            case ROI_BLANC -> roiBlanc = (roiBlanc & ~depart) | arrivee;

            case PION_NOIR -> pionsNoirs = (pionsNoirs & ~depart) | arrivee;
            case CAVALIER_NOIR -> cavaliersNoirs = (cavaliersNoirs & ~depart) | arrivee;
            case FOU_NOIR -> fousNoirs = (fousNoirs & ~depart) | arrivee;
            case TOUR_NOIR -> toursNoires = (toursNoires & ~depart) | arrivee;
            case DAME_NOIRE -> reineNoire = (reineNoire & ~depart) | arrivee;
            case ROI_NOIR -> roiNoir = (roiNoir & ~depart) | arrivee;
        }
    }

    private void ajouterPiece(Piece piece, long masque) {
        switch (piece) {
            case PION_BLANC -> pionsBlancs |= masque;
            case CAVALIER_BLANC -> cavaliersBlancs |= masque;
            case FOU_BLANC -> fousBlancs |= masque;
            case TOUR_BLANC -> toursBlanches |= masque;
            case DAME_BLANCHE -> reineBlanche |= masque;
            case ROI_BLANC -> roiBlanc |= masque;

            case PION_NOIR -> pionsNoirs |= masque;
            case CAVALIER_NOIR -> cavaliersNoirs |= masque;
            case FOU_NOIR -> fousNoirs |= masque;
            case TOUR_NOIR -> toursNoires |= masque;
            case DAME_NOIRE -> reineNoire |= masque;
            case ROI_NOIR -> roiNoir |= masque;
        }
    }

    private void supprimerPiece(Piece piece, long masque) {
        switch (piece) {
            case PION_BLANC -> pionsBlancs &= ~masque;
            case CAVALIER_BLANC -> cavaliersBlancs &= ~masque;
            case FOU_BLANC -> fousBlancs &= ~masque;
            case TOUR_BLANC -> toursBlanches &= ~masque;
            case DAME_BLANCHE -> reineBlanche &= ~masque;
            case ROI_BLANC -> roiBlanc &= ~masque;

            case PION_NOIR -> pionsNoirs &= ~masque;
            case CAVALIER_NOIR -> cavaliersNoirs &= ~masque;
            case FOU_NOIR -> fousNoirs &= ~masque;
            case TOUR_NOIR -> toursNoires &= ~masque;
            case DAME_NOIRE -> reineNoire &= ~masque;
            case ROI_NOIR -> roiNoir &= ~masque;
        }
    }

    private void supprimerPieceSurMasque(long masque) {
        pionsBlancs &= ~masque;
        cavaliersBlancs &= ~masque;
        fousBlancs &= ~masque;
        toursBlanches &= ~masque;
        reineBlanche &= ~masque;
        roiBlanc &= ~masque;

        pionsNoirs &= ~masque;
        cavaliersNoirs &= ~masque;
        fousNoirs &= ~masque;
        toursNoires &= ~masque;
        reineNoire &= ~masque;
        roiNoir &= ~masque;
    }

    private void mettreAJourDroitsRoque(Coup coup) {
        Piece piece = coup.pieceDeplacee();
        int dep = coup.depart().indice();
        long arrivee = coup.arrivee().bit();

        if (piece == Piece.ROI_BLANC) {
            roqueBlancRoi = false;
            roqueBlancReine = false;
        } else if (piece == Piece.ROI_NOIR) {
            roqueNoirRoi = false;
            roqueNoirReine = false;
        }

        if (piece == Piece.TOUR_BLANC) {
            if (dep == 63)
                roqueBlancRoi = false; // h1
            if (dep == 56)
                roqueBlancReine = false; // a1
        } else if (piece == Piece.TOUR_NOIR) {
            if (dep == 7)
                roqueNoirRoi = false; // h8
            if (dep == 0)
                roqueNoirReine = false; // a8
        }

        if ((arrivee & (1L << 63)) != 0)
            roqueBlancRoi = false;
        if ((arrivee & (1L << 56)) != 0)
            roqueBlancReine = false;
        if ((arrivee & (1L << 7)) != 0)
            roqueNoirRoi = false;
        if ((arrivee & (1L << 0)) != 0)
            roqueNoirReine = false;
    }
}
