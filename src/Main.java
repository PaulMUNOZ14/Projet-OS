import Pont.Pont;
import Vehicule.ConvoiExceptionnel;
import Vehicule.Voiture;
import Vehicule.Bateau;
import Pont.Direction;

public class Main {

    public static void main(String[] args) {

        Pont pont = new Pont();
        Voiture v1 = new Voiture("Voiture 1", Direction.GAUCHE_DROITE, pont);
        Voiture v2 = new Voiture("Voiture 2", Direction.GAUCHE_DROITE, pont);
        Voiture v3 = new Voiture("Voiture 3", Direction.DROITE_GAUCHE, pont);
        Voiture v4 = new Voiture("Voiture 4", Direction.DROITE_GAUCHE, pont);
        Bateau b1 = new Bateau("Bateau 1", Direction.NORD_SUD, pont);
        Bateau b2 = new Bateau("Bateau 2", Direction.SUD_NORD, pont);
        ConvoiExceptionnel c1 = new ConvoiExceptionnel("Convoi 1", Direction.GAUCHE_DROITE, pont);
        ConvoiExceptionnel c2 = new ConvoiExceptionnel("Convoi 2", Direction.DROITE_GAUCHE, pont);

        // Démarrage des threads
        v1.start();
        v2.start();
        v3.start();
        v4.start();

        b1.start();
        b2.start();

        c1.start();
        c2.start();

        // Attendre la fin de tous les threads
        try {
            v1.join();
            v2.join();
            v3.join();
            v4.join();

            b1.join();
            b2.join();

            c1.join();
            c2.join();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Simulation terminée !");
    }
}