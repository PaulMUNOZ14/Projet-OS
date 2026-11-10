package Vehicule;

import Pont.Direction;
import Pont.Pont;

public class Voiture extends VehiculeTerrestre {
    public Voiture(String nom, Direction direction, Pont pont) {
        super(nom, direction, pont, 1, false);
    }
}