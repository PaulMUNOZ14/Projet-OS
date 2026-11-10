package Vehicule;

import Pont.Direction;
import Pont.Pont;

public abstract class VehiculeTerrestre extends Thread implements IVehicule {
    private final Direction direction;
    private final Pont pont;
    private final int longueur;
    private final boolean exceptionnel;

    protected VehiculeTerrestre(String nom, Direction direction, Pont pont, int longueur, boolean exceptionnel) {
        super(nom);
        this.direction =direction;
        this.pont=pont;
        this.longueur = longueur;
        this.exceptionnel =exceptionnel;
    }

    @Override
    public void run() {
        traverser();
    }

    @Override
    public void traverser() {
        try {
            pont.traverserTerrestre(getName(), direction,longueur, exceptionnel);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}