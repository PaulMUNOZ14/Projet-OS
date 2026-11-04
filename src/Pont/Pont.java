package Pont;

import static java.lang.Thread.sleep;

public class Pont {

    private int voituresSurPont = 0;
    private int bateauSousPont = 0;
    private String directionVoitures = null;
    private String directionBateaux = null;
    private int bateauxEnAttente = 0;

    public Pont() {
    }

    public synchronized void entrerVoiture(String direction, String nom) {

        while(bateauSousPont > 0 || bateauxEnAttente > 0 || (voituresSurPont > 0 && !directionVoitures.equals(direction))){
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        voituresSurPont ++;

        directionVoitures = direction;

        System.out.println(nom + " entre sur le pont de " + direction);

        try {
            sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        sortirVoiture(nom);
    }

    public synchronized void sortirVoiture(String nom){
        voituresSurPont--;

        System.out.println(nom + " quitte le pont");

        if (voituresSurPont == 0){
            directionVoitures = null;
        }

        notifyAll();
    }

    public synchronized void entrerBateau(String direction, String nom) {

        bateauxEnAttente++;

        while(bateauSousPont > 0 || voituresSurPont > 0){
            try {
                wait();
            } catch (InterruptedException e) {
                bateauxEnAttente--;
                Thread.currentThread().interrupt();
                notifyAll();
                return;
            }
        }

        bateauxEnAttente--;

        bateauSousPont++;

        directionBateaux = direction;

        System.out.println(nom + " passe sous le pont de " + direction);

        try {
            sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        sortirVoiture(nom);

    }

}
