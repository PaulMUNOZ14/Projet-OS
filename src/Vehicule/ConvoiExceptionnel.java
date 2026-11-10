package Vehicule;

import Pont.Direction;
import Pont.Pont;

public class ConvoiExceptionnel extends VehiculeTerrestre {
    public ConvoiExceptionnel(String nom, Direction direction, Pont pont) {
        super(nom, direction, pont, 4, true);
    }
}