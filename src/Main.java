import Pont.Controleur;
import Pont.Direction;
import Pont.Pont;
import Vehicule.Ambulance;
import Vehicule.Bateau;
import Vehicule.Camion;
import Vehicule.ConvoiExceptionnel;
import Vehicule.Pompier;
import Vehicule.Voiture;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Pont pont = new Pont();
        new Controleur(pont).start();

        Thread[] acteurs = {
                new Voiture("Voiture 1", Direction.GAUCHE_DROITE, pont),
                new Voiture("Voiture 2", Direction.DROITE_GAUCHE, pont),
                new Camion("Camion 1", Direction.GAUCHE_DROITE, pont),
                new Camion("Camion 2", Direction.DROITE_GAUCHE, pont),
                new ConvoiExceptionnel("Convoi 1", Direction.GAUCHE_DROITE, pont),
                new Voiture("Voiture 3", Direction.DROITE_GAUCHE, pont),
                new Voiture("Voiture 4", Direction.GAUCHE_DROITE, pont),
                new Bateau("Bateau 1", Direction.NORD_SUD, pont),
                new Bateau("Bateau 2", Direction.SUD_NORD, pont),
                new ConvoiExceptionnel("Convoi 2", Direction.DROITE_GAUCHE, pont),
                new Voiture("Voiture 5", Direction.DROITE_GAUCHE, pont),
                new Ambulance("Ambulance 1", Direction.GAUCHE_DROITE, pont),
                new Pompier("Pompier 1", Direction.DROITE_GAUCHE, pont),
        };
        for (Thread t : acteurs) {
            t.start();
            Thread.sleep(150);
        }
        for (Thread t : acteurs) {
            t.join();
        }
        pont.arreter();
        System.out.println("Simulation terminée !");
    }
}