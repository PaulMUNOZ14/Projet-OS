package Pont;

/**
 * Directions de circulation gérées par la simulation.
 * Les deux premières concernent les véhicules terrestres, les deux suivantes les bateaux.
 */
public enum Direction {
    /** Véhicule terrestre circulant de la gauche vers la droite. */
    GAUCHE_DROITE,
    /** Véhicule terrestre circulant de la droite vers la gauche. */
    DROITE_GAUCHE,
    /** Bateau circulant du nord vers le sud. */
    NORD_SUD,
    /** Bateau circulant du sud vers le nord. */
    SUD_NORD
}
