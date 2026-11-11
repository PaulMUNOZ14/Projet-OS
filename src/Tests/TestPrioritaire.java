package Tests;

import Pont.Direction;
import Pont.Pont;
import Vehicule.Ambulance;
import Vehicule.Camion;
import Vehicule.Pompier;
import Vehicule.Voiture;

/**
 * Test de la partie 6 : un camion et des voitures sont sur le pont quand une ambulance (G->D)
 * puis un véhicule de pompiers (D->G) arrivent. Les véhicules du même sens que le prioritaire
 * doivent se ranger puis reprendre ; ceux de l'autre sens ne sont pas gênés.
 *
 * <p>Exécution : {@code java -cp PontMobile.jar Tests.TestPrioritaire}</p>
 */
public class TestPrioritaire {

    /** Lance le scénario et attend la fin de tous les acteurs. */
    public static void main(String[] args) throws InterruptedException {
        Pont pont = new Pont();

        Thread[] acteurs = {
                new Camion("Camion 1", Direction.GAUCHE_DROITE, pont),
                new Voiture("Voiture 1", Direction.DROITE_GAUCHE, pont),
                new Voiture("Voiture 2", Direction.GAUCHE_DROITE, pont),
                new Ambulance("Ambulance 1", Direction.GAUCHE_DROITE, pont),
                new Voiture("Voiture 3", Direction.GAUCHE_DROITE, pont),
                new Pompier("Pompier 1", Direction.DROITE_GAUCHE, pont),
        };

        System.out.println("===== DEBUT DU TEST =====");
        for (Thread t : acteurs) {
            t.start();
            Thread.sleep(500);
        }
        for (Thread t : acteurs) {
            t.join();
        }
        System.out.println("===== FIN DU TEST =====");
    }
}
