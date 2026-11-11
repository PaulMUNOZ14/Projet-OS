package Vehicule;

import Pont.Direction;
import Pont.Pont;

/**
 * Véhicule terrestre générique (thread). Il occupe {@code longueur} segments consécutifs du pont
 * dans un sens donné : 1 pour une voiture, 2 pour un camion, 4 pour un convoi exceptionnel.
 * La logique de circulation est entièrement déléguée à {@link Pont#traverserTerrestre}.
 */
public abstract class VehiculeTerrestre extends Thread implements IVehicule {
    /** Sens de circulation du véhicule. */
    private final Direction direction;
    /** Pont partagé sur lequel circule le véhicule. */
    private final Pont pont;
    /** Nombre de segments occupés simultanément. */
    private final int longueur;
    /** Vrai pour un convoi exceptionnel (bloque le sens opposé). */
    private final boolean exceptionnel;

    /**
     * @param nom          nom du véhicule (nom du thread, utilisé pour l'affichage)
     * @param direction    sens de circulation
     * @param pont         pont partagé
     * @param longueur     nombre de segments occupés (1, 2 ou 4)
     * @param exceptionnel vrai pour un convoi exceptionnel
     */
    protected VehiculeTerrestre(String nom, Direction direction, Pont pont, int longueur, boolean exceptionnel) {
        super(nom);
        this.direction = direction;
        this.pont = pont;
        this.longueur = longueur;
        this.exceptionnel = exceptionnel;
    }

    /** Point d'entrée du thread : lance la traversée. */
    @Override
    public void run() {
        traverser();
    }

    /** Lance la traversée du pont et restaure le drapeau d'interruption en cas d'arrêt. */
    @Override
    public void traverser() {
        try {
            pont.traverserTerrestre(getName(), direction, longueur, exceptionnel);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
