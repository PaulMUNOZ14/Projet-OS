package Vehicule;

import Pont.Direction;
import Pont.Pont;

/** Camion : véhicule terrestre qui occupe deux segments consécutifs du pont. */
public class Camion extends VehiculeTerrestre {

    /**
     * @param nom       nom du camion
     * @param direction sens de circulation
     * @param pont      pont partagé
     */
    public Camion(String nom, Direction direction, Pont pont) {
        super(nom, direction, pont, 2, false);
    }
}
