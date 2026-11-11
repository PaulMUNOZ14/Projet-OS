package Vehicule;

import Pont.Direction;
import Pont.Pont;

/** Ambulance : véhicule prioritaire. */
public class Ambulance extends VehiculePrioritaire {

    /**
     * @param nom       nom de l'ambulance
     * @param direction sens de circulation
     * @param pont      pont partagé
     */
    public Ambulance(String nom, Direction direction, Pont pont) {
        super(nom, direction, pont);
    }
}
