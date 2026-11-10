package Pont;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

public class Pont {

    public static final int N = 10;

    private static final int DUREE_PAS = 300;
    private static final int DUREE_OUVERTURE = 600;
    private static final int DUREE_BATEAU = 700;

    private final String[][] segments = new String[2][N];

    private final int[] vehiculesSurPont = new int[2];

    private Direction convoiDirection = null;

    private Etat etat = Etat.FERME;

    private boolean entreesBloquees = false;

    private final Queue<Thread> fileBateaux = new LinkedList<>();
    private boolean bateauEnTraversee = false;

    private boolean ouvertureAutorisee = true;
    private boolean arret = false;

    // partie 5 : délai minimal entre deux ouvertures

    /** Délai minimal entre la fermeture complète et la prochaine ouverture (1 minute pour la simulation, 60 min en réel). */
    private static final long DELAI_ENTRE_OUVERTURES_MS = 60_000;
    private static final DateTimeFormatter FORMAT_HEURE = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Heure exacte de la dernière fermeture complète (null tant que le pont ne s'est jamais ouvert). */
    private LocalTime heureFermetureComplete = null;
    /** Heure à partir de laquelle une nouvelle ouverture est autorisée (null = autorisée immédiatement). */
    private LocalTime heureProchaineOuverture = null;
    /** Timer (daemon) qui lève l'interdiction d'ouverture à l'heure prévue. */
    private final Timer timerOuverture = new Timer("TimerOuverture", true);

    // partie 6 : véhicules prioritaires (ambulances, pompiers)

    private static final int DUREE_PAS_PRIORITAIRE = 120;

    /**
     * Nombre de véhicules prioritaires présents (en attente ou en train de traverser) par voie.
     * Tant que > 0, les véhicules de cette voie n'avancent plus (bande d'urgence) et aucun nouveau n'entre.
     */
    private final int[] urgenceVoie = new int[2];
    /** Vrai si un véhicule prioritaire est en train de traverser la voie (un seul à la fois par voie). */
    private final boolean[] prioritaireSurVoie = new boolean[2];
    /** Position du véhicule prioritaire par voie (affichage uniquement : il n'occupe aucun segment réservé). */
    private final String[][] prioritaire = new String[2][N];
    /** Sens du convoi exceptionnel actuellement engagé sur le pont (null s'il n'y en a pas). */
    private Direction convoiSurPont = null;

    private int voie(Direction d) {
        return d == Direction.GAUCHE_DROITE ? 0 : 1;
    }

    private Direction opposee(Direction d) {
        return d == Direction.GAUCHE_DROITE ? Direction.DROITE_GAUCHE : Direction.GAUCHE_DROITE;
    }

    public void traverserTerrestre(String nom, Direction dir, int longueur, boolean exceptionnel)
            throws InterruptedException {
        int voie = voie(dir);
        boolean entre = false;
        try {
            entrer(nom, dir, longueur, exceptionnel);
            entre = true;
            int tete = longueur - 1;
            while (tete < N - 1) {
                Thread.sleep(DUREE_PAS);
                avancer(nom, voie, tete, longueur);
                tete++;
            }

            for (int s = tete - longueur + 1; s <= tete; s++) {
                Thread.sleep(DUREE_PAS);
                liberer(nom, voie, s);
            }
        } finally {
            if (entre) {
                quitter(nom, voie, exceptionnel);
            }
        }
    }

    private boolean peutEntrer(Direction dir, int longueur, boolean exceptionnel) {
        int voie = voie(dir);
        if (etat != Etat.FERME || entreesBloquees) {
            return false;
        }
        if (urgenceVoie[voie] > 0) {
            return false; // un véhicule prioritaire est annoncé dans ce sens
        }
        if (exceptionnel) {

            if (vehiculesSurPont[1 - voie] > 0) {
                return false;
            }
        } else if (convoiDirection != null && convoiDirection != dir) {

            return false;
        }

        for (int i = 0; i < longueur; i++) {
            if (segments[voie][i] != null) {
                return false;
            }
        }
        return true;
    }

    private synchronized void entrer(String nom, Direction dir, int longueur, boolean exceptionnel)
            throws InterruptedException {
        int voie = voie(dir);
        if (exceptionnel) {
            while (convoiDirection != null) {
                wait();
            }
            convoiDirection = dir;
            log(nom + " arrive : entrée " + opposee(dir) + " bloquée");
        }
        try {
            while (!peutEntrer(dir, longueur, exceptionnel)) {
                wait();
            }
        } catch (InterruptedException e) {
            if (exceptionnel) {
                convoiDirection = null;
                notifyAll();
            }
            throw e;
        }
        for (int i = 0; i < longueur; i++) {
            segments[voie][i] = nom;
        }
        vehiculesSurPont[voie]++;
        if (exceptionnel) {
            convoiSurPont = dir;
        }
        log(nom + " entre sur le pont (" + longueur + " segment(s))");
    }

    private synchronized void avancer(String nom, int voie, int tete, int longueur)
            throws InterruptedException {
        boolean range = false;
        // Le véhicule s'arrête si un prioritaire est présent dans son sens ou si le segment suivant est occupé.
        // Il conserve ses segments (association logique) et reprend exactement où il s'était arrêté.
        while (urgenceVoie[voie] > 0 || segments[voie][tete + 1] != null) {
            if (urgenceVoie[voie] > 0 && !range) {
                range = true;
                log(nom + " se range sur la bande d'urgence");
            }
            wait();
        }
        if (range) {
            log(nom + " reprend sa progression");
        }
        segments[voie][tete + 1] = nom;
        segments[voie][tete - longueur + 1] = null;
        log(nom + " avance");
        notifyAll();
    }

    private synchronized void liberer(String nom, int voie, int index) throws InterruptedException {
        boolean range = false;
        while (urgenceVoie[voie] > 0) {
            if (!range) {
                range = true;
                log(nom + " se range sur la bande d'urgence");
            }
            wait();
        }
        if (range) {
            log(nom + " reprend sa progression");
        }
        if (nom.equals(segments[voie][index])) {
            segments[voie][index] = null;
        }
        notifyAll();
    }

    private synchronized void quitter(String nom, int voie, boolean exceptionnel) {
        for (int i = 0; i < N; i++) {
            if (nom.equals(segments[voie][i])) {
                segments[voie][i] = null;
            }
        }
        vehiculesSurPont[voie]--;
        if (exceptionnel) {
            convoiDirection = null;
            convoiSurPont = null;
            log(nom + " a quitté le pont : sens opposé débloqué");
        } else {
            log(nom + " a quitté le pont");
        }
        notifyAll();
    }

    // fin partie 3

    // partie 6

    /**
     * Un véhicule prioritaire peut entrer si le pont est FERME (même si une ouverture est demandée mais
     * pas encore commencée), si aucun autre prioritaire n'est sur sa voie et si aucun convoi exceptionnel
     * engagé ne circule dans le sens opposé (sa largeur l'interdit).
     */
    private boolean peutEntrerPrioritaire(int voie, Direction dir) {
        return etat == Etat.FERME
                && !prioritaireSurVoie[voie]
                && (convoiSurPont == null || convoiSurPont == dir);
    }

    /**
     * Traversée d'un véhicule prioritaire dans le sens {@code dir}.
     * Dès son arrivée, les véhicules du même sens s'arrêtent (bande d'urgence) en gardant leurs segments,
     * et plus aucun nouveau véhicule de ce sens n'entre. Le prioritaire traverse sans occuper de segment réservé.
     * Les véhicules du sens opposé ne sont pas gênés. À sa sortie, les véhicules interrompus reprennent.
     */
    public void traverserPrioritaire(String nom, Direction dir) throws InterruptedException {
        int voie = voie(dir);
        boolean declare = false;
        boolean surPont = false;
        try {
            synchronized (this) {
                urgenceVoie[voie]++;
                declare = true;
                log(nom + " (prioritaire) arrive : véhicules " + dir + " à l'arrêt");
                notifyAll();
                while (!peutEntrerPrioritaire(voie, dir)) {
                    wait();
                }
                prioritaireSurVoie[voie] = true;
                vehiculesSurPont[voie]++;
                surPont = true;
                log(nom + " entre sur le pont");
            }
            for (int i = 0; i < N; i++) {
                Thread.sleep(DUREE_PAS_PRIORITAIRE);
                synchronized (this) {
                    if (i > 0) {
                        prioritaire[voie][i - 1] = null;
                    }
                    prioritaire[voie][i] = nom;
                    log(nom + " avance");
                }
            }
            Thread.sleep(DUREE_PAS_PRIORITAIRE);
        } finally {
            synchronized (this) {
                for (int i = 0; i < N; i++) {
                    if (nom.equals(prioritaire[voie][i])) {
                        prioritaire[voie][i] = null;
                    }
                }
                if (surPont) {
                    prioritaireSurVoie[voie] = false;
                    vehiculesSurPont[voie]--;
                }
                if (declare) {
                    urgenceVoie[voie]--;
                }
                log(nom + " a quitté le pont" + (urgenceVoie[voie] == 0 ? " : véhicules interrompus repartent" : ""));
                notifyAll();
            }
        }
    }

    // partie 4

    public void traverserBateau(String nom, Direction dir) throws InterruptedException {
        Thread moi = Thread.currentThread();
        synchronized (this) {
            fileBateaux.add(moi);
            log(nom + " (" +dir + ") s'enregistre dans la file (" + fileBateaux.size() + " en attente)");
            notifyAll();
            try {
                while (!(etat == Etat.OUVERT && fileBateaux.peek() == moi && !bateauEnTraversee)) {
                    wait();
                }
            } catch (InterruptedException e) {
                fileBateaux.remove(moi);
                notifyAll();
                throw e;
            }
            fileBateaux.poll();
            bateauEnTraversee = true;
            log(nom+ " passe sous le pont");
        }
        try {
            Thread.sleep(DUREE_BATEAU);
        } finally {
            synchronized (this) {
                bateauEnTraversee = false;
                log(nom + " a fini de passer,");
                notifyAll();
            }
        }
    }

    public void executerControleur() throws InterruptedException {
        while (true) {
            synchronized (this) {
                while (!arret && !(ouvertureAutorisee && !fileBateaux.isEmpty())) {
                    wait();
                }
                if (arret) {
                    return;
                }
                entreesBloquees = true;
                log("Ouverture prise en charge : entrées bloquées");
                // les prioritaires annoncés passent d'abord (ils comptent dans vehiculesSurPont une fois entrés)
                while (vehiculesSurPont[0] > 0 || vehiculesSurPont[1] > 0
                        || urgenceVoie[0] > 0 || urgenceVoie[1] > 0) {
                    wait();
                }
                etat = Etat.OUVERTURE;
                log("Pont : FERME -> OUVERTURE");
            }
            Thread.sleep(DUREE_OUVERTURE);
            synchronized (this) {
                etat = Etat.OUVERT;
                log("Pont : OUVERTURE -> OUVERT");
                notifyAll();
                while (!fileBateaux.isEmpty() || bateauEnTraversee) {
                    wait();
                }

                etat = Etat.FERMETURE;
                log("Pont : OUVERT -> FERMETURE (file vide)");
            }
            Thread.sleep(DUREE_OUVERTURE);
            synchronized (this) {
                etat =Etat.FERME;
                entreesBloquees = false;
                log("Pont : FERMETURE -> FERME");
                planifierProchaineOuverture();
                notifyAll();
            }
        }
    }

    /**
     * Appelée (monitor déjà détenu) à la fin de chaque fermeture complète :
     * mémorise l'heure de fermeture, calcule T_prochaine_ouverture = T_fermeture + délai,
     * interdit toute ouverture d'ici là et programme un TimerTask qui la ré-autorise.
     */
    private void planifierProchaineOuverture() {
        heureFermetureComplete = LocalTime.now();
        heureProchaineOuverture = heureFermetureComplete.plusNanos(DELAI_ENTRE_OUVERTURES_MS * 1_000_000L);
        ouvertureAutorisee = false;
        log("Fermeture complète à " + heureFermetureComplete.format(FORMAT_HEURE)
                + " : prochaine ouverture à " + heureProchaineOuverture.format(FORMAT_HEURE));

        timerOuverture.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (Pont.this) {
                    ouvertureAutorisee = true;
                    log("Délai écoulé (" + FORMAT_HEURE.format(LocalTime.now())
                            + ") : ouverture à nouveau autorisée"
                            + (fileBateaux.isEmpty() ? "" : " (" + fileBateaux.size() + " bateau(x) en attente)"));
                    Pont.this.notifyAll();
                }
            }
        }, DELAI_ENTRE_OUVERTURES_MS);
    }

    /** Heure prévue de la prochaine ouverture autorisée (null si une ouverture est possible dès maintenant). */
    public synchronized LocalTime getHeureProchaineOuverture() {
        return ouvertureAutorisee ? null : heureProchaineOuverture;
    }

    public synchronized void setOuvertureAutorisee(boolean autorisee) {
        this.ouvertureAutorisee = autorisee;
        notifyAll();
    }

    public synchronized void arreter() {
        arret = true;
        timerOuverture.cancel();
        notifyAll();
    }

    private String code(String nom) {
        if (nom==null) {
            return ".";
        }
        return nom.length() > 3 ? nom.substring(0, 2) + nom.substring(nom.lastIndexOf(' ') + 1) : nom;
    }

    private String ligne(int voie, boolean inverse) {
        StringBuilder sb = new StringBuilder("[");
        for (int k = 0; k<N; k++) {
            int i = inverse ? N -1 -k : k;
            if (prioritaire[voie][i] != null) {
                sb.append(String.format("%-4s", "*" + code(prioritaire[voie][i])));
            } else {
                sb.append(String.format("%-4s", code(segments[voie][i])));
            }
        }
        return sb.append("]").toString();
    }

    private void log(String msg) {
        System.out.println(String.format("%-58s G->D %s  D->G %s", msg, ligne(0, false),ligne(1, true)));
    }
}