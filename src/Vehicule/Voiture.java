package Vehicule;

import Pont.Direction;
import Pont.Pont;

/** Voiture : véhicule terrestre qui occupe un seul segment du pont. */
public class Voiture extends VehiculeTerrestre {

    /**
     * @param nom       nom de la voiture
     * @param direction sens de circulation
     * @param pont      pont partagé
     */
    public Voiture(String nom, Direction direction, Pont pont) {
        super(nom, direction, pont, 1, false);
    }
}
