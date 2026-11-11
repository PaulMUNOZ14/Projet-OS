package Vehicule;

import Pont.Direction;
import Pont.Pont;

/**
 * Convoi exceptionnel : véhicule terrestre qui occupe quatre segments consécutifs et dont la
 * largeur interdit la présence simultanée de véhicules dans le sens opposé.
 */
public class ConvoiExceptionnel extends VehiculeTerrestre {

    /**
     * @param nom       nom du convoi
     * @param direction sens de circulation
     * @param pont      pont partagé
     */
    public ConvoiExceptionnel(String nom, Direction direction, Pont pont) {
        super(nom, direction, pont, 4, true);
    }
}
