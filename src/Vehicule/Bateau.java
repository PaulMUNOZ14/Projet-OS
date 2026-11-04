package Vehicule;

import Pont.Pont;
import Pont.Direction;

public class Bateau extends Thread implements IVehicule{
    private Direction direction;
    private Pont pont;

    public Bateau(String nom, Direction direction, Pont pont){
        super(nom);
        this.direction = direction;
        this.pont = pont;
    }

    @Override
    public void run(){
        traverser();
    }
    @Override
    public void traverser() {
        pont.entrerBateau(direction, getName());
    }
}
