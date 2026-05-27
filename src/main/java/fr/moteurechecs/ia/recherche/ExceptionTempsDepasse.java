package fr.moteurechecs.ia.recherche;

/**
 * Exception levée quand le temps alloué à la recherche est écoulé.
 *
 * Utilisée comme signal de contrôle non-checked pour interrompre
 * la recherche minimax depuis n'importe quelle profondeur de récursion.
 */
public class ExceptionTempsDepasse extends RuntimeException {
}
