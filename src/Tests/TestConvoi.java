
package Tests;

import Pont.Pont;
import Pont.Direction;
import Vehicule.Voiture;
import Vehicule.ConvoiExceptionnel;

public class TestConvoi {

    public static void main(String[] args) {

        Pont pont = new Pont();

        // Création des voitures
        Voiture v1 = new Voiture("Voiture 1", Direction.GAUCHE_DROITE, pont);

        Voiture v2 = new Voiture("Voiture 2", Direction.DROITE_GAUCHE, pont);

        Voiture v3 = new Voiture("Voiture 3", Direction.GAUCHE_DROITE, pont);

        Voiture v4 = new Voiture("Voiture 4", Direction.DROITE_GAUCHE, pont);

        // Création des convois exceptionnels
        ConvoiExceptionnel c1 = new ConvoiExceptionnel("Convoi 1", Direction.GAUCHE_DROITE, pont);

        ConvoiExceptionnel c2 = new ConvoiExceptionnel("Convoi 2", Direction.DROITE_GAUCHE, pont);

        System.out.println("===== DEBUT DU TEST =====");

        try {
            // 1. Première voiture
            v1.start();
            Thread.sleep(100);

            // 2. Voiture dans le sens opposé
            v2.start();
            Thread.sleep(100);

            // 3. Arrivée du premier convoi
            c1.start();
            Thread.sleep(100);

            // 4. Nouvelle voiture dans le sens opposé
            v4.start();
            Thread.sleep(100);

            // 5. Nouvelle voiture dans le même sens
            v3.start();
            Thread.sleep(100);

            // 6. Arrivée du deuxième convoi
            c2.start();

            // Attendre la fin de tous les threads
            v1.join();
            v2.join();
            v3.join();
            v4.join();

            c1.join();
            c2.join();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("===== FIN DU TEST =====");
    }
}
