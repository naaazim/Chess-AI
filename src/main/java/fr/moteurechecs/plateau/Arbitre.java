package fr.moteurechecs.plateau;

/**
 * Règles globales du jeu d'échecs : échec, pat et mat.
 *
 * Délégué par {@link Plateau} pour les vérifications d'échec et le calcul
 * des cases attaquées. Utilise uniquement des bitboards pour la performance.
 */
public final class Arbitre {

    private Arbitre() {}

    /**
     * Indique si la couleur donnée est en échec dans la position courante.
     *
     * @param plateau position à examiner
     * @param couleur couleur dont on teste le roi
     * @return {@code true} si le roi de cette couleur est attaqué
     */
    public static boolean estEnEchec(Plateau plateau, Couleur couleur) {
        long roi              = (couleur == Couleur.BLANC) ? plateau.roiBlancBitboard() : plateau.roiNoirBitboard();
        long attaquesAdverses = casesAttaqueesPar(plateau, couleur.inverse());
        return (attaquesAdverses & roi) != 0L;
    }

    /**
     * Calcule le bitboard de toutes les cases attaquées par une couleur.
     *
     * @param plateau  position à examiner
     * @param couleur  couleur des pièces attaquantes
     * @return bitboard des cases couvertes par les attaques de cette couleur
     */
    public static long casesAttaqueesPar(Plateau plateau, Couleur couleur) {
        long occupes = plateau.occupes();
        long attaques = 0L;

        // ===== pions : attaques diagonales avant =====
        if (couleur == Couleur.BLANC) {
            long pions = plateau.pionsBlancsBitboard();
            attaques |= (pions >>> 7) & ~Masques.FILE_A;
            attaques |= (pions >>> 9) & ~Masques.FILE_H;
        } else {
            long pions = plateau.pionsNoirsBitboard();
            attaques |= (pions << 7) & ~Masques.FILE_H;
            attaques |= (pions << 9) & ~Masques.FILE_A;
        }

        // ===== cavaliers : sauts en L depuis les tables pré-calculées =====
        long cavaliers = (couleur == Couleur.BLANC)
                ? plateau.cavaliersBlancsBitboard()
                : plateau.cavaliersNoirsBitboard();
        long curseur = cavaliers;
        while (curseur != 0L) {
            long bit    = curseur & -curseur;
            int  indice = Long.numberOfTrailingZeros(bit);
            attaques   |= Masques.attaquesCavalier(indice);
            curseur    ^= bit;
        }

        // ===== roi : cases adjacentes =====
        long roi = (couleur == Couleur.BLANC) ? plateau.roiBlancBitboard() : plateau.roiNoirBitboard();
        if (roi != 0L) {
            int indice = Long.numberOfTrailingZeros(roi);
            attaques |= Masques.attaquesRoi(indice);
        }

        // ===== fous et dame : diagonales =====
        long fousEtDame = (couleur == Couleur.BLANC)
                ? (plateau.fousBlancsBitboard()  | plateau.reineBlancheBitboard())
                : (plateau.fousNoirsBitboard()   | plateau.reineNoireBitboard());
        curseur = fousEtDame;
        while (curseur != 0L) {
            long bit    = curseur & -curseur;
            int  indice = Long.numberOfTrailingZeros(bit);
            attaques   |= Rayons.attaquesDiagonales(indice, occupes);
            curseur    ^= bit;
        }

        // ===== tours et dame : horizontales et verticales =====
        long toursEtDame = (couleur == Couleur.BLANC)
                ? (plateau.toursBlanchesBitboard() | plateau.reineBlancheBitboard())
                : (plateau.toursNoiresBitboard()   | plateau.reineNoireBitboard());
        curseur = toursEtDame;
        while (curseur != 0L) {
            long bit    = curseur & -curseur;
            int  indice = Long.numberOfTrailingZeros(bit);
            attaques   |= Rayons.attaquesHorizontalesEtVerticales(indice, occupes);
            curseur    ^= bit;
        }

        return attaques;
    }

    /**
     * Indique si la position est un mat pour la couleur donnée.
     *
     * @param plateau position à examiner
     * @return {@code true} si la couleur au trait est en mat
     * @throws UnsupportedOperationException toujours (non encore implémenté)
     */
    public static boolean estMat(Plateau plateau) {
        throw new UnsupportedOperationException("estMat() nécessite la génération de coups légaux (à venir).");
    }

    /**
     * Indique si la position est un pat pour la couleur donnée.
     *
     * @param plateau position à examiner
     * @return {@code true} si la couleur au trait est en pat
     * @throws UnsupportedOperationException toujours (non encore implémenté)
     */
    public static boolean estPat(Plateau plateau) {
        throw new UnsupportedOperationException("estPat() nécessite la génération de coups légaux (à venir).");
    }
}
