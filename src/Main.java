import Pont.Pont;
import Vehicule.Voiture;
import Vehicule.Bateau;

public class Main {

    public static void main(String[] args) {

        Pont pont = new Pont();
        Voiture v1 = new Voiture("Voiture 1", "Gauche-Droite", pont);
        Voiture v2 = new Voiture("Voiture 2", "Gauche-Droite", pont);
        Voiture v3 = new Voiture("Voiture 3", "Droite-Gauche", pont);
        Voiture v4 = new Voiture("Voiture 4", "Droite-Gauche", pont);
        Bateau b1 = new Bateau("Bateau 1", "Nord-Sud", pont);
        Bateau b2 = new Bateau("Bateau 2", "Sud-Nord", pont);

        // Démarrage des threads
        v1.start();
        v2.start();
        v3.start();
        v4.start();

        b1.start();
        b2.start();

        // Attendre la fin de tous les threads
        try {
            v1.join();
            v2.join();
            v3.join();
            v4.join();

            b1.join();
            b2.join();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Simulation terminée !");
    }
}