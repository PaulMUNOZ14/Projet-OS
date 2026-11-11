import Pont.Controleur;
import Pont.Direction;
import Pont.Pont;
import Vehicule.Ambulance;
import Vehicule.Bateau;
import Vehicule.Camion;
import Vehicule.ConvoiExceptionnel;
import Vehicule.Pompier;
import Vehicule.Voiture;

/**
 * Point d'entrée de la simulation du pont mobile.
 *
 * <p>Crée le pont, lance le thread contrôleur (gestion de l'ouverture/fermeture) puis démarre un
 * jeu de véhicules et de bateaux représentatif de toutes les catégories du sujet. Un dernier
 * bateau est lancé après la première fermeture complète pour illustrer le délai minimal d'une
 * minute entre deux ouvertures (partie 5).</p>
 *
 * <p>À exécuter dans un vrai terminal (les séquences ANSI ne sont pas interprétées par la console
 * « Run » d'IntelliJ) : {@code java -jar PontMobile.jar}</p>
 */
public class Main {

    /**
     * Crée les acteurs, lance la simulation et attend sa fin.
     *
     * @param args arguments de ligne de commande (non utilisés)
     * @throws InterruptedException si l'attente des threads est interrompue
     */
    public static void main(String[] args) throws InterruptedException {
        Pont pont = new Pont();
        new Controleur(pont).start();

        // Acteurs représentatifs : voitures, camions, convois, bateaux et véhicules prioritaires,
        // dans les deux sens, lancés progressivement pour créer de la concurrence sur le pont.
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
            Thread.sleep(150); // léger décalage pour que les véhicules arrivent les uns après les autres
        }
        for (Thread t : acteurs) {
            t.join();
        }

        // Bateau tardif : arrivé après la première fermeture complète, il doit attendre la fin du
        // délai d'une minute avant que le pont ne se rouvre (partie 5).
        Bateau tardif = new Bateau("Bateau tardif", Direction.NORD_SUD, pont);
        tardif.start();
        tardif.join();

        // Laisse le contrôleur terminer la fermeture du pont (visible : FERMETURE -> FERME)
        // avant d'arrêter la simulation.
        Thread.sleep(2000);

        pont.arreter();
        System.out.println("Simulation terminée !");
    }
}
