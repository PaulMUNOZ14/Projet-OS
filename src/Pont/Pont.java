package Pont;

import static java.lang.Thread.sleep;

public class Pont {

    private int voituresSurPont = 0;
    private int bateauSousPont = 0;
    private Direction directionVoitures = null;
    private Direction directionBateaux = null;
    private int bateauxEnAttente = 0;
    private Etat etat = Etat.FERME;
    private boolean ouvertureDemandee = false;
    private final int N = 10;
    private final String[] segments = new String[10];
    private Direction directionActive = null;
    private int vehiculesEngages = 0;

    public Pont() {
    }

    public synchronized void entrerVoiture(Direction direction, String nom) {

        while(etat != Etat.FERME || bateauSousPont > 0 || bateauxEnAttente > 0 || (voituresSurPont > 0 && !directionVoitures.equals(direction))){
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        voituresSurPont ++;

        directionVoitures = direction;

        System.out.println(nom + " entre sur le pont de " + direction.toString());

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

    public synchronized void entrerBateau(Direction direction, String nom) {

        bateauxEnAttente++;
        ouvertureDemandee = true;

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

        etat = Etat.OUVERT;

        System.out.println("Pont : OUVERTURE -> OUVERT");

        bateauxEnAttente--;

        bateauSousPont++;

        directionBateaux = direction;

        System.out.println(nom + " passe sous le pont de " + direction.toString());

        try {
            sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        sortirBateau(nom);

    }

    public synchronized void sortirBateau(String nom){
        bateauSousPont--;

        System.out.println(nom + " quitte le pont");

        if (bateauSousPont == 0){
            directionBateaux = null;
        }

        notifyAll();
    }

    private boolean estDirectionTerrestre(Direction direction) {

        return direction == Direction.GAUCHE_DROITE
                || direction == Direction.DROITE_GAUCHE;
    }

    private int pas(Direction direction) {

        if (direction == Direction.GAUCHE_DROITE) {
            return 1;
        }

        return -1;
    }

    private synchronized void reserverSegments(String nom, int debut, int longueur, int pas)throws InterruptedException{
        while(true){
            boolean disponible= true;
            for (int i = 0; i < longueur; i++) {
                int indice = debut + i * pas;
                if (indice < 0 || indice >= N || segments[indice] != null) {
                    disponible = false;
                    break;
                }
            }
            if(disponible){
                for (int i = 0; i < longueur; i++) {

                    int indice = debut + i * pas;

                    segments[indice] = nom;
                }
                return;
            }
            wait();
        }




    }
    private synchronized void libererSegments(String nom, int debut, int longueur, int pas){
        for (int i = 0; i < longueur; i++) {
            int indice = debut + i * pas;

            if (indice >= 0 && indice < N && nom.equals(segments[indice])) {

                segments[indice] = null;
            }
        }
        notifyAll();
    }



}
