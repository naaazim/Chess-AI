package fr.moteurechecs.tournoi;

import fr.moteurechecs.ia.Niveau;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Sauvegarde incrémentale d'un tournoi : chaque partie est écrite immédiatement
 * sur disque dès qu'elle se termine, afin de ne rien perdre en cas d'interruption.
 *
 * Format des lignes :
 * <pre>
 *   PARTIE|niveauA|niveauB|numeroPart|blanc|noir|resultat
 *   MATCHUP_TERMINE|niveauA|niveauB
 * </pre>
 */
public final class SauvegardeTournoi implements Closeable {

    private final BufferedWriter writer;

    /**
     * Ouvre le fichier en mode append.
     * Crée le fichier s'il n'existe pas.
     */
    public SauvegardeTournoi(String cheminFichier) throws IOException {
        this.writer = new BufferedWriter(new FileWriter(cheminFichier, true));
    }

    /**
     * Enregistre le résultat d'une partie immédiatement sur disque puis flush.
     *
     * Format :
     * {@code PARTIE|niveauA|niveauB|numeroPart|blanc|noir|resultat}
     */
    public synchronized void enregistrerPartie(
            Niveau niveauA, Niveau niveauB,
            int numeroPart,
            Niveau blanc, Niveau noir,
            Tournoi.Resultat resultat) throws IOException {

        writer.write("PARTIE|" + niveauA.name() + "|" + niveauB.name() + "|"
                + numeroPart + "|" + blanc.name() + "|" + noir.name() + "|"
                + resultat.name());
        writer.newLine();
        writer.flush();
    }

    /**
     * Marque un matchup comme entièrement terminé.
     *
     * Format : {@code MATCHUP_TERMINE|niveauA|niveauB}
     */
    public synchronized void marquerMatchupTermine(
            Niveau niveauA, Niveau niveauB) throws IOException {

        writer.write("MATCHUP_TERMINE|" + niveauA.name() + "|" + niveauB.name());
        writer.newLine();
        writer.flush();
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
