package Vehicule;

import Pont.Direction;
import Pont.Pont;

public class Bateau extends Thread implements IVehicule {
    private final Direction direction;
    private final Pont pont;

    public Bateau(String nom, Direction direction, Pont pont) {
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
            pont.traverserBateau(getName(), direction);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}