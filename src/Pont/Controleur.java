package Pont;

/**
 * Thread « contrôleur » du pont : il exécute en boucle la logique d'ouverture/fermeture
 * du pont (voir {@link Pont#executerControleur()}). C'est un thread daemon : il s'arrête
 * avec le programme et n'empêche donc pas la JVM de se terminer.
 */
public class Controleur extends Thread {

    /** Le pont dont ce contrôleur gère les ouvertures/fermetures. */
    private final Pont pont;

    /**
     * @param pont le pont à contrôler
     */
    public Controleur(Pont pont) {
        super("Controleur");
        this.pont = pont;
        setDaemon(true);
    }

    /** Lance la boucle du contrôleur et restaure le drapeau d'interruption en cas d'arrêt. */
    @Override
    public void run() {
        try {
            pont.executerControleur();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
