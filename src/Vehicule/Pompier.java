package Vehicule;

import Pont.Direction;
import Pont.Pont;

/** Véhicule de pompiers : véhicule prioritaire. */
public class Pompier extends VehiculePrioritaire {
    public Pompier(String nom, Direction direction, Pont pont) {
        super(nom, direction, pont);
    }
}
