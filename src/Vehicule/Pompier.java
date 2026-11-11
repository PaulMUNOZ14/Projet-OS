package Vehicule;

import Pont.Direction;
import Pont.Pont;

/** Véhicule de pompiers : véhicule prioritaire. */
public class Pompier extends VehiculePrioritaire {

    /**
     * @param nom       nom du véhicule de pompiers
     * @param direction sens de circulation
     * @param pont      pont partagé
     */
    public Pompier(String nom, Direction direction, Pont pont) {
        super(nom, direction, pont);
    }
}
