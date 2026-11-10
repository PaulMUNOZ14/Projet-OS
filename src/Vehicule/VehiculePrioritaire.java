package Vehicule;

import Pont.Direction;
import Pont.Pont;

/**
 * Véhicule prioritaire (ambulance, pompiers) : thread qui traverse le pont en priorité.
 * Les véhicules du même sens se rangent sur la bande d'urgence pendant son passage.
 */
public abstract class VehiculePrioritaire extends Thread implements IVehicule {
    private final Direction direction;
    private final Pont pont;

    protected VehiculePrioritaire(String nom, Direction direction, Pont pont) {
        super(nom);
        this.direction = direction;
        this.pont = pont;
    }

    @Override
    public void run() {
        traverser();
    }

    @Override
    public void traverser() {
        try {
            pont.traverserPrioritaire(getName(), direction);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
