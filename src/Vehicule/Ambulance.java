package Vehicule;

import Pont.Direction;
import Pont.Pont;

/** Ambulance : véhicule prioritaire. */
public class Ambulance extends VehiculePrioritaire {
    public Ambulance(String nom, Direction direction, Pont pont) {
        super(nom, direction, pont);
    }
}
