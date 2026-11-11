package Vehicule;

/**
 * Contrat commun à tous les acteurs de la simulation (véhicules terrestres, bateaux et
 * véhicules prioritaires) : savoir traverser le pont.
 */
public interface IVehicule {
    /** Déclenche la traversée du pont par l'acteur. */
    void traverser();
}
