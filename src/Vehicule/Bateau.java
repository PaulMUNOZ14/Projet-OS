package Vehicule;

import Pont.Pont;

public class Bateau extends Thread implements IVehicule{
    private String direction;
    private Pont pont;

    public Bateau(String nom, String direction, Pont pont){
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
