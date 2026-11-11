package Pont;

/**
 * États possibles du pont mobile.
 * Les transitions autorisées sont : FERME -> OUVERTURE -> OUVERT -> FERMETURE -> FERME.
 */
public enum Etat {
    /** Pont fermé : accueille les véhicules, interdit le passage des bateaux. */
    FERME,
    /** Pont en cours d'ouverture : plus aucun nouveau véhicule ne peut entrer. */
    OUVERTURE,
    /** Pont ouvert : seuls les bateaux peuvent passer. */
    OUVERT,
    /** Pont en cours de fermeture : aucun véhicule ne peut entrer avant le retour à FERME. */
    FERMETURE
}
