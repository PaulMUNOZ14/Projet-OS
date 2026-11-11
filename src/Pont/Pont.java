package Pont;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Ressource partagée représentant le pont mobile.
 *
 * <p>Le pont est protégé par son propre verrou (méthodes synchronisées et blocs
 * {@code synchronized (this)}). Il gère :</p>
 * <ul>
 *   <li>les quatre états {@link Etat} et leurs transitions (partie 2) ;</li>
 *   <li>les {@link #N} segments de chaque sens de circulation (partie 3) ;</li>
 *   <li>la file FIFO des bateaux et le cycle ouverture/fermeture (partie 4) ;</li>
 *   <li>le délai minimal d'une minute entre deux ouvertures, via un {@link Timer} (partie 5) ;</li>
 *   <li>les véhicules prioritaires et la bande d'urgence (partie 6).</li>
 * </ul>
 *
 * <p>L'affichage console (animation ANSI) est regroupé en fin de classe ; il n'a aucune
 * incidence sur la synchronisation.</p>
 */
public class Pont {

    /** Nombre de segments consécutifs qui composent le pont (S1..SN). */
    public static final int N = 10;

    // --- Durées de la simulation (en millisecondes) ---

    /** Durée d'un pas d'avancement d'un véhicule terrestre sur un segment. */
    private static final int DUREE_PAS = 300;
    /** Durée de chacune des phases OUVERTURE et FERMETURE du pont. */
    private static final int DUREE_TRANSITION = 1500;
    /** Durée de traversée d'un bateau sous le pont. */
    private static final int DUREE_BATEAU = 700;
    /** Durée d'un pas d'avancement d'un véhicule prioritaire (plus rapide). */
    private static final int DUREE_PAS_PRIORITAIRE = 120;

    /** Occupant de chaque segment : segments[sens][i] vaut le nom du véhicule ou null si libre. */
    private final String[][] segments = new String[2][N];

    /** Nombre de véhicules terrestres actuellement sur le pont, par sens (0 = G->D, 1 = D->G). */
    private final int[] vehiculesSurPont = new int[2];

    /** Sens du convoi exceptionnel annoncé ou engagé (null s'il n'y en a pas) : bloque le sens opposé. */
    private Direction convoiDirection = null;

    /** État courant du pont. */
    private Etat etat = Etat.FERME;

    /** Vrai lorsque les entrées du pont sont bloquées aux nouveaux véhicules (ouverture en cours). */
    private boolean entreesBloquees = false;

    /** File d'attente FIFO commune à tous les bateaux, quelle que soit leur direction. */
    private final Queue<Thread> fileBateaux = new LinkedList<>();
    /** Vrai tant qu'un bateau occupe l'espace sous le pont (un seul bateau à la fois). */
    private boolean bateauEnTraversee = false;

    /** Vrai si une nouvelle ouverture est autorisée (partie 5). */
    private boolean ouvertureAutorisee = true;
    /** Vrai lorsque la simulation doit s'arrêter. */
    private boolean arret = false;

    // partie 5 : délai minimal entre deux ouvertures

    /** Délai minimal entre la fermeture complète et la prochaine ouverture (1 minute pour la simulation, 60 min en réel). */
    private static final long DELAI_ENTRE_OUVERTURES_MS = 60_000;
    /** Format d'affichage des heures (HH:mm:ss). */
    private static final DateTimeFormatter FORMAT_HEURE = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Heure exacte de la dernière fermeture complète (null tant que le pont ne s'est jamais ouvert). */
    private LocalTime heureFermetureComplete = null;
    /** Heure à partir de laquelle une nouvelle ouverture est autorisée (null = autorisée immédiatement). */
    private LocalTime heureProchaineOuverture = null;
    /** Timer (daemon) qui lève l'interdiction d'ouverture à l'heure prévue. */
    private final Timer timerOuverture = new Timer("TimerOuverture", true);
    /** Timer (daemon) qui rafraîchit l'affichage une fois par seconde (décompte non figé). */
    private final Timer timerAffichage = new Timer("TimerAffichage", true);

    // partie 6 : véhicules prioritaires (ambulances, pompiers)

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

    /**
     * Construit le pont et démarre un timer (daemon) qui rafraîchit l'affichage une fois par seconde,
     * afin que le décompte avant la prochaine ouverture ne soit pas figé entre deux événements.
     */
    public Pont() {
        timerAffichage.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                synchronized (Pont.this) {
                    afficher();
                }
            }
        }, 1000, 1000);
    }

    /** Indice de voie associé à une direction : 0 pour G->D, 1 pour D->G. */
    private int voie(Direction d) {
        return d == Direction.GAUCHE_DROITE ? 0 : 1;
    }

    /** Direction opposée à celle passée en paramètre. */
    private Direction opposee(Direction d) {
        return d == Direction.GAUCHE_DROITE ? Direction.DROITE_GAUCHE : Direction.GAUCHE_DROITE;
    }

    /**
     * Fait traverser un véhicule terrestre (voiture, camion ou convoi exceptionnel).
     *
     * <p>Le véhicule réserve d'abord ses {@code longueur} premiers segments, puis avance segment
     * par segment jusqu'à la sortie, en libérant progressivement ceux qu'il quitte.</p>
     *
     * @param nom          nom du véhicule (pour l'affichage)
     * @param dir          sens de circulation
     * @param longueur     nombre de segments occupés (1 voiture, 2 camion, 4 convoi)
     * @param exceptionnel vrai pour un convoi exceptionnel (bloque le sens opposé)
     */
    public void traverserTerrestre(String nom, Direction dir, int longueur, boolean exceptionnel)
            throws InterruptedException {
        int voie = voie(dir);
        boolean entre = false;
        try {
            entrer(nom, dir, longueur, exceptionnel);
            entre = true;
            int tete = longueur - 1;
            // Avance jusqu'à ce que la tête du véhicule atteigne le dernier segment.
            while (tete < N - 1) {
                Thread.sleep(DUREE_PAS);
                avancer(nom, voie, tete, longueur);
                tete++;
            }

            // Sortie du pont : libère un à un les segments encore occupés.
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

    /**
     * Indique si un véhicule terrestre peut entrer immédiatement : pont FERMÉ, entrées ouvertes,
     * segments nécessaires libres et contraintes de priorité (convoi, prioritaire) respectées.
     */
    private boolean peutEntrer(Direction dir, int longueur, boolean exceptionnel) {
        int voie = voie(dir);
        if (etat != Etat.FERME || entreesBloquees) {
            return false;
        }
        if (urgenceVoie[voie] > 0) {
            return false; // un véhicule prioritaire est annoncé dans ce sens
        }
        if (exceptionnel) {
            // Un convoi ne peut entrer que si aucun véhicule ne circule dans le sens opposé.
            if (vehiculesSurPont[1 - voie] > 0) {
                return false;
            }
        } else if (convoiDirection != null && convoiDirection != dir) {
            // Un convoi engagé dans l'autre sens interdit l'entrée.
            return false;
        }

        for (int i = 0; i < longueur; i++) {
            if (segments[voie][i] != null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Réservation des segments d'entrée d'un véhicule terrestre.
     * Bloque ({@code wait}) tant que les conditions d'entrée ne sont pas réunies. Pour un convoi
     * exceptionnel, réserve d'abord le droit exclusif de traversée (sens opposé bloqué).
     */
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

    /**
     * Fait avancer d'un segment un véhicule terrestre déjà engagé : réserve le segment suivant
     * et libère le premier segment occupé. Le véhicule attend si le segment suivant est occupé
     * ou si un véhicule prioritaire est présent dans son sens (bande d'urgence).
     */
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

    /**
     * Libère un segment lors de la sortie du pont. Attend la fin du passage d'un éventuel
     * véhicule prioritaire dans le même sens (bande d'urgence) avant de libérer.
     */
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

    /**
     * Termine la traversée d'un véhicule terrestre : libère tous les segments qu'il occupe encore,
     * décrémente le compteur de véhicules sur le pont et, s'il s'agit d'un convoi exceptionnel,
     * débloque le sens opposé.
     */
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
                log(nom + " (prioritaire) arrive : " + dir + " à l'arrêt");
                notifyAll();
                while (!peutEntrerPrioritaire(voie, dir)) {
                    wait();
                }
                prioritaireSurVoie[voie] = true;
                vehiculesSurPont[voie]++;
                surPont = true;
                log(nom + " entre sur le pont");
            }
            // Traversée du prioritaire, segment par segment (affichage uniquement).
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

    /**
     * Traversée d'un bateau. Le bateau s'enregistre dans la file FIFO commune puis attend que
     * le pont soit complètement OUVERT et qu'il soit en tête de file. Un seul bateau passe à la fois.
     */
    public void traverserBateau(String nom, Direction dir) throws InterruptedException {
        Thread moi = Thread.currentThread();
        synchronized (this) {
            fileBateaux.add(moi);
            log(nom + " s'enregistre (" + fileBateaux.size() + " en attente)");
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
            bateauAffiche = nom;
            log(nom + " passe sous le pont");
        }
        try {
            Thread.sleep(DUREE_BATEAU);
        } finally {
            synchronized (this) {
                bateauEnTraversee = false;
                bateauAffiche = null;
                log(nom + " a fini de passer");
                notifyAll();
            }
        }
    }

    /**
     * Boucle du contrôleur (thread daemon) : gère le cycle d'ouverture/fermeture du pont.
     *
     * <p>Dès qu'une ouverture est autorisée et qu'au moins un bateau attend, il bloque les entrées,
     * attend que le pont soit vide, passe en OUVERTURE puis OUVERT, laisse passer tous les bateaux
     * dans l'ordre FIFO, puis repasse en FERMETURE et FERMÉ en programmant le délai avant la
     * prochaine ouverture.</p>
     */
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
            Thread.sleep(DUREE_TRANSITION);
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
            Thread.sleep(DUREE_TRANSITION);
            synchronized (this) {
                etat = Etat.FERME;
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
                    int enAttente = fileBateaux.size();
                    log("Délai écoulé : ouverture autorisée"
                            + (enAttente == 0 ? "" : " (" + enAttente + (enAttente == 1 ? " bateau)" : " bateaux)")));
                    Pont.this.notifyAll();
                }
            }
        }, DELAI_ENTRE_OUVERTURES_MS);
    }

    /** Heure prévue de la prochaine ouverture autorisée (null si une ouverture est possible dès maintenant). */
    public synchronized LocalTime getHeureProchaineOuverture() {
        return ouvertureAutorisee ? null : heureProchaineOuverture;
    }

    /** Force l'autorisation (ou non) d'ouvrir le pont (utilitaire, notamment pour les tests). */
    public synchronized void setOuvertureAutorisee(boolean autorisee) {
        this.ouvertureAutorisee = autorisee;
        notifyAll();
    }

    /** Arrête la simulation : réveille le contrôleur et annule les timers (ouverture et affichage). */
    public synchronized void arreter() {
        arret = true;
        timerOuverture.cancel();
        timerAffichage.cancel();
        notifyAll();
    }

    // ------------------------------------------------------------------
    // Affichage console (animation ANSI) : aucune incidence sur la synchronisation.
    // Toutes les méthodes ci-dessous sont appelées sous le verrou du Pont (via log).
    // ------------------------------------------------------------------

    /** Nombre d'événements récents affichés sous le schéma (limité pour tenir dans ~20 lignes). */
    private static final int NB_EVENEMENTS_AFFICHES = 5;
    /** Derniers événements (affichage uniquement). */
    private final LinkedList<String> derniersEvenements = new LinkedList<>();
    /** Nom du bateau en train de passer, null sinon (affichage uniquement : il a déjà quitté la file). */
    private String bateauAffiche = null;
    /** Largeur maximale d'une ligne pour ne pas dépasser un terminal de 80 colonnes. */
    private static final int LARGEUR_MAX = 79;
    /** Faux tant que l'écran n'a pas été effacé une première fois (évite les résidus d'un ancien affichage). */
    private boolean premierAffichage = true;

    /** Code court d'un acteur pour tenir dans une cellule : "Voiture 1" -> "Vo1". */
    private String code(String nom) {
        if (nom == null) {
            return "";
        }
        if (nom.length() <= 3) {
            return nom;
        }
        int espace = nom.lastIndexOf(' ');
        return espace < 0 ? nom.substring(0, 3) : nom.substring(0, 2) + nom.substring(espace + 1);
    }

    /** Largeur d'une case du pont, bordures exclues. */
    private static final int LARGEUR_CASE = 5;
    private static final String MARGE = "  ";

    /**
     * Ajoute une ligne à l'affichage : tronquée à {@link #LARGEUR_MAX} colonnes pour ne pas passer
     * à la ligne, et terminée par la séquence ANSI {@code "\033[K"} qui efface la fin de la ligne
     * (ce qui évite le clignotement d'un effacement complet de l'écran).
     */
    private void ajouterLigne(StringBuilder s, String contenu) {
        String ligne = contenu.length() > LARGEUR_MAX ? contenu.substring(0, LARGEUR_MAX) : contenu;
        s.append(ligne).append("\033[K\n");
    }

    /** Ligne de séparation horizontale entre deux voies : +-----+-----+ ... */
    private void ajouterBordure(StringBuilder s) {
        StringBuilder b = new StringBuilder(MARGE).append("        ");
        for (int k = 0; k < N; k++) {
            b.append('+');
            for (int c = 0; c < LARGEUR_CASE; c++) {
                b.append('-');
            }
        }
        b.append('+');
        ajouterLigne(s, b.toString());
    }

    /** Dessine une voie en coordonnées physiques : la voie D->G est affichée de droite à gauche. */
    private void ajouterVoie(StringBuilder s, int voie, boolean inverse) {
        StringBuilder b = new StringBuilder(MARGE);
        b.append(inverse ? "G <- D  " : "G -> D  ");
        for (int k = 0; k < N; k++) {
            int i = inverse ? N - 1 - k : k;
            String contenu = "";
            if (prioritaire[voie][i] != null) {
                contenu = "*" + code(prioritaire[voie][i]);
            } else if (segments[voie][i] != null) {
                contenu = code(segments[voie][i]);
            }
            b.append(String.format("|%-" + LARGEUR_CASE + "s", contenu));
        }
        b.append('|');
        ajouterLigne(s, b.toString());
    }

    /** Situation des prioritaires sur une voie : libre, en attente, ou nom du véhicule qui traverse. */
    private String urgence(int voie) {
        for (int i = 0; i < N; i++) {
            if (prioritaire[voie][i] != null) {
                return prioritaire[voie][i];
            }
        }
        if (prioritaireSurVoie[voie]) {
            return "entre sur le pont";
        }
        return urgenceVoie[voie] > 0 ? "en attente" : "libre";
    }

    /** Texte décrivant le délai avant la prochaine ouverture autorisée. */
    private String delaiAvantOuverture() {
        if (ouvertureAutorisee) {
            return "possible maintenant";
        }
        if (heureProchaineOuverture == null) {
            return "indeterminee";
        }
        long secondes = Duration.between(LocalTime.now(), heureProchaineOuverture).getSeconds();
        if (secondes < -12 * 3600) {
            secondes += 24 * 3600; // passage de minuit
        }
        return "dans " + Math.max(0, secondes + 1) + " s (a " + FORMAT_HEURE.format(heureProchaineOuverture) + ")";
    }

    /** Début d'une ligne d'information : étiquette alignée sur 19 caractères. */
    private String info(String etiquette) {
        return MARGE + String.format("%-19s : ", etiquette);
    }

    /** Noms des bateaux actuellement dans la file, séparés par des virgules ("vide" si aucun). */
    private String fileBateauxTexte() {
        StringBuilder file = new StringBuilder();
        for (Thread t : fileBateaux) {
            if (file.length() > 0) {
                file.append(", ");
            }
            file.append(t.getName());
        }
        return file.length() == 0 ? "vide" : file.toString();
    }

    /**
     * Redessine l'état complet du pont (environ 20 lignes). Le curseur est replacé en haut à gauche
     * ({@code "\033[H"}) sans effacer tout l'écran, et chaque ligne est terminée par {@code "\033[K"} :
     * l'affichage ne clignote donc plus, contrairement à un effacement complet à chaque événement.
     */
    private void afficher() {
        String doubleTrait = "=".repeat(72);
        String trait = "-".repeat(72);
        StringBuilder s = new StringBuilder();
        if (premierAffichage) {
            s.append("\033[2J"); // efface l'écran une seule fois pour supprimer les résidus
            premierAffichage = false;
        }
        s.append("\033[H");

        ajouterLigne(s, doubleTrait);
        ajouterLigne(s, String.format("%s%-62s%s", MARGE, "PONT MOBILE - SIMULATION", FORMAT_HEURE.format(LocalTime.now())));
        ajouterLigne(s, doubleTrait);

        ajouterLigne(s, info("Etat du pont") + etat
                + (entreesBloquees ? "  (entrees bloquees)" : "  (circulation autorisee)"));
        ajouterLigne(s, info("Prochaine ouverture") + delaiAvantOuverture());
        ajouterLigne(s, info("Prioritaires") + "G->D " + urgence(0) + "  |  D->G " + urgence(1));

        ajouterBordure(s);
        ajouterVoie(s, 0, false);
        ajouterBordure(s);
        ajouterVoie(s, 1, true);
        ajouterBordure(s);

        ajouterLigne(s, info("Riviere") + (bateauAffiche == null ? "libre" : bateauAffiche + " passe"));
        ajouterLigne(s, info("File des bateaux") + fileBateauxTexte());

        ajouterLigne(s, trait);
        int dernier = derniersEvenements.size() - 1, k = 0;
        for (String evenement : derniersEvenements) {
            ajouterLigne(s, (k++ == dernier ? "> " : "  ") + evenement);
        }
        ajouterLigne(s, MARGE + "Legende : Vo voiture, Ca camion, Co convoi, *Am ambulance, *Po pompier");

        System.out.print(s);
        System.out.flush();
    }

    /** Enregistre un événement (horodaté), ne garde que les plus récents et redessine l'affichage. */
    private void log(String msg) {
        derniersEvenements.add(FORMAT_HEURE.format(LocalTime.now()) + "  " + msg);
        if (derniersEvenements.size() > NB_EVENEMENTS_AFFICHES) {
            derniersEvenements.removeFirst();
        }
        afficher();
    }
}
