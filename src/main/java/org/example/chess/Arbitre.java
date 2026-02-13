package org.example.chess;

/**
 * <p>
 * {@code Arbitre} contient les règles "globales" :
 * </p>
 * <ul>
 *   <li>échec</li>
 *   <li>cases attaquées</li>
 *   <li>plus tard : mat, pat (quand on aura la génération de coups légaux)</li>
 * </ul>
 */
public final class Arbitre {

    private Arbitre() {}

    /**
     * <p>Retourne vrai si la couleur donnée est en échec.</p>
     *
     * @param plateau plateau
     * @param couleur couleur testée
     * @return vrai si en échec
     */
    public static boolean estEnEchec(Plateau plateau, Couleur couleur) {
        long roi = (couleur == Couleur.BLANC) ? plateau.roiBlancBitboard() : plateau.roiNoirBitboard();
        long attaquesAdverses = casesAttaqueesPar(plateau, couleur.inverse());
        return (attaquesAdverses & roi) != 0L;
    }

    /**
     * <p>
     * Calcule un bitboard des cases attaquées par la couleur donnée.
     * </p>
     *
     * @param plateau plateau
     * @param couleur attaquant
     * @return bitboard des cases attaquées
     */
    public static long casesAttaqueesPar(Plateau plateau, Couleur couleur) {
        long occ = plateau.occupes();

        long attaques = 0L;

        // PIONS
        if (couleur == Couleur.BLANC) {
            long p = plateau.pionsBlancsBitboard();
            attaques |= (p >>> 7) & ~Masques.FILE_A;
            attaques |= (p >>> 9) & ~Masques.FILE_H;
        } else {
            long p = plateau.pionsNoirsBitboard();
            attaques |= (p << 7) & ~Masques.FILE_H;
            attaques |= (p << 9) & ~Masques.FILE_A;
        }

        // CAVALIERS
        long cav = (couleur == Couleur.BLANC) ? plateau.cavaliersBlancsBitboard() : plateau.cavaliersNoirsBitboard();
        long tmp = cav;
        while (tmp != 0L) {
            long lsb = tmp & -tmp;
            int idx = Long.numberOfTrailingZeros(lsb);
            attaques |= Masques.attaquesCavalier(idx);
            tmp ^= lsb;
        }

        // ROI
        long roi = (couleur == Couleur.BLANC) ? plateau.roiBlancBitboard() : plateau.roiNoirBitboard();
        if (roi != 0L) {
            int idxR = Long.numberOfTrailingZeros(roi);
            attaques |= Masques.attaquesRoi(idxR);
        }

        // FOUS + DAMES (diagonales)
        long diag = (couleur == Couleur.BLANC)
                ? (plateau.fousBlancsBitboard() | plateau.reineBlancheBitboard())
                : (plateau.fousNoirsBitboard() | plateau.reineNoireBitboard());

        tmp = diag;
        while (tmp != 0L) {
            long lsb = tmp & -tmp;
            int idx = Long.numberOfTrailingZeros(lsb);
            attaques |= Rayons.attaquesDiagonales(idx, occ);
            tmp ^= lsb;
        }

        // TOURS + DAMES (h/v)
        long hv = (couleur == Couleur.BLANC)
                ? (plateau.toursBlanchesBitboard() | plateau.reineBlancheBitboard())
                : (plateau.toursNoiresBitboard() | plateau.reineNoireBitboard());

        tmp = hv;
        while (tmp != 0L) {
            long lsb = tmp & -tmp;
            int idx = Long.numberOfTrailingZeros(lsb);
            attaques |= Rayons.attaquesHorizontalesEtVerticales(idx, occ);
            tmp ^= lsb;
        }

        return attaques;
    }

    /**
     * <p>Mat = en échec + aucun coup légal.</p>
     *
     * <p>
     * Cette méthode sera implémentée quand on aura un générateur de coups légaux.
     * </p>
     */
    public static boolean estMat(Plateau plateau) {
        throw new UnsupportedOperationException("estMat() nécessite la génération de coups légaux (à venir).");
    }

    /**
     * <p>Pat = pas en échec + aucun coup légal.</p>
     *
     * <p>
     * Cette méthode sera implémentée quand on aura un générateur de coups légaux.
     * </p>
     */
    public static boolean estPat(Plateau plateau) {
        throw new UnsupportedOperationException("estPat() nécessite la génération de coups légaux (à venir).");
    }
}
