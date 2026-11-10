package Pont;

public class Controleur extends Thread {
    private final Pont pont;

    public Controleur(Pont pont) {
        super("Controleur");
        this.pont=pont;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            pont.executerControleur();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}