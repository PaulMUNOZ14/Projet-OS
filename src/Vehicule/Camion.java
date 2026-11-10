package Vehicule;

import Pont.Direction;
import Pont.Pont;

public class Camion extends VehiculeTerrestre {
    public Camion(String nom, Direction direction, Pont pont) {
        super(nom, direction, pont, 2, false);
    }
}