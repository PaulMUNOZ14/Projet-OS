package Vehicule;

import Pont.Pont;
import Pont.Direction;

public class ConvoiExceptionnel extends Thread implements IVehicule {

    private Direction direction;
    private Pont pont;

    public ConvoiExceptionnel(String nom, Direction direction, Pont pont) {
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

        boolean entre = false;

        try {

            System.out.println(getName() + " demande l'accès au pont.");

            pont.entrerConvoi(direction, getName());

            entre = true;

            sleep(2000);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

        } finally {

            if (entre) {
                pont.sortirConvoi(getName());
            }
        }
    }
}
