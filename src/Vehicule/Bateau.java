package Vehicule;

import Pont.Direction;
import Pont.Pont;

/**
 * Bateau : thread qui traverse l'espace situé sous le pont. Il s'enregistre dans la file FIFO
 * commune gérée par le {@link Pont} et ne peut passer que lorsque le pont est complètement OUVERT
 * et qu'il est en tête de file.
 */
public class Bateau extends Thread implements IVehicule {
    /** Sens de navigation du bateau (NORD_SUD ou SUD_NORD). */
    private final Direction direction;
    /** Pont partagé sous lequel passe le bateau. */
    private final Pont pont;

    /**
     * @param nom       nom du bateau (nom du thread, utilisé pour l'affichage)
     * @param direction sens de navigation
     * @param pont      pont partagé
     */
    public Bateau(String nom, Direction direction, Pont pont) {
        super(nom);
        this.direction = direction;
        this.pont = pont;
    }

    /** Point d'entrée du thread : lance la traversée. */
    @Override
    public void run() {
        traverser();
    }

    /** S'enregistre dans la file des bateaux et attend son tour ; restaure le drapeau d'interruption. */
    @Override
    public void traverser() {
        try {
            pont.traverserBateau(getName(), direction);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
