package Vehicule;

import Pont.Direction;
import Pont.Pont;

/**
 * Véhicule prioritaire (ambulance, pompiers) : thread qui traverse le pont en priorité.
 * Les véhicules du même sens se rangent sur la bande d'urgence pendant son passage, puis
 * reprennent leur progression là où ils s'étaient arrêtés. Les véhicules du sens opposé ne
 * sont pas gênés.
 */
public abstract class VehiculePrioritaire extends Thread implements IVehicule {
    /** Sens de circulation du véhicule prioritaire. */
    private final Direction direction;
    /** Pont partagé. */
    private final Pont pont;

    /**
     * @param nom       nom du véhicule prioritaire
     * @param direction sens de circulation
     * @param pont      pont partagé
     */
    protected VehiculePrioritaire(String nom, Direction direction, Pont pont) {
        super(nom);
        this.direction = direction;
        this.pont = pont;
    }

    /** Point d'entrée du thread : lance la traversée prioritaire. */
    @Override
    public void run() {
        traverser();
    }

    /** Lance la traversée prioritaire ; restaure le drapeau d'interruption en cas d'arrêt. */
    @Override
    public void traverser() {
        try {
            pont.traverserPrioritaire(getName(), direction);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
