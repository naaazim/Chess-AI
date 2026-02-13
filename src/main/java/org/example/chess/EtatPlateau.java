package org.example.chess;

/**
 * <p>
 * Sauvegarde complète d’un état de plateau.
 * </p>
 *
 * <p>
 * Utile pour l’IA : on joue un coup, on explore, puis on annule rapidement.
 * </p>
 */
public record EtatPlateau(
        long pionsBlancs, long cavaliersBlancs, long fousBlancs, long toursBlanches, long reineBlanche, long roiBlanc,
        long pionsNoirs, long cavaliersNoirs, long fousNoirs, long toursNoires, long reineNoire, long roiNoir,
        Couleur trait,
        boolean roqueBlancRoi, boolean roqueBlancReine, boolean roqueNoirRoi, boolean roqueNoirReine,
        long enPassant
) {}
