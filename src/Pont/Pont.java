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
            voituresSurPont--;
            if (voituresSurPont == 0) {
                directionVoitures = null;
            }
            notifyAll();
            Thread.currentThread().interrupt();
            return;
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

        System.out.println(nom + " demande l'ouverture du pont.");

        while(bateauSousPont > 0 || voituresSurPont > 0){
            try {
                wait();
            } catch (InterruptedException e) {
                bateauxEnAttente--;
                if (bateauxEnAttente == 0) {
                    ouvertureDemandee = false;
                }
                notifyAll();
                Thread.currentThread().interrupt();
                return;
            }
        }
        if (etat == Etat.FERME) {
            etat = Etat.OUVERTURE;
            System.out.println("Pont : FERME -> OUVERTURE");

            try {
                sleep(500);
            } catch (InterruptedException e) {
                etat = Etat.FERME;
                bateauxEnAttente--;
                if (bateauxEnAttente == 0) {
                    ouvertureDemandee = false;
                }

                notifyAll();
                Thread.currentThread().interrupt();
                return;
            }

            etat = Etat.OUVERT;
            System.out.println("Pont : OUVERTURE -> OUVERT");
        }

        bateauxEnAttente--;

        bateauSousPont++;

        directionBateaux = direction;

        System.out.println(nom + " passe sous le pont de " + direction.toString());

        try {
            sleep(500);
        } catch (InterruptedException e) {
            sortirBateau(nom);
            Thread.currentThread().interrupt();
            return;
        }

        sortirBateau(nom);

    }

    public synchronized void sortirBateau(String nom){
        bateauSousPont--;

        System.out.println(nom + " quitte le pont");

        if (bateauSousPont == 0){
            directionBateaux = null;

            etat = Etat.FERMETURE;
            System.out.println("Pont : OUVERT -> FERMETURE");

            try {
                sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            etat = Etat.FERME;
            System.out.println("Pont : FERMETURE -> FERME");

            if (bateauxEnAttente == 0) {
                ouvertureDemandee = false;
            }
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
