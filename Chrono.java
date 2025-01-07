// La classe Chrono représente un chronomètre qui suit une durée en secondes et permet de la diminuer à chaque appel de la méthode 'decrementer'.

class Chrono {
    private int tempsRestant; // Temps restant en secondes

    // Constructeur qui initialise la durée du chronomètre à la valeur spécifiée.
    public Chrono(int duree) {
        this.tempsRestant = duree; // Initialise le temps restant à la durée donnée
    }

    // Démarre le chronomètre en affichant le temps restant initial.
    public void demarrer() {
        System.out.println("Chronomètre démarré : " + tempsRestant + " secondes restantes.");
        // Affiche un message indiquant que le chronomètre est démarré avec le temps restant initial
    }

    // Diminue le temps restant de 1 seconde, tant que le temps est supérieur à 0.
    public void decrementer() {
        if (tempsRestant > 0) { // Vérifie que le temps restant est positif
            tempsRestant--; // Diminue le temps restant de 1 seconde
        }
    }

    // Renvoie le temps restant actuel.
    public int getTempsRestant() {
        return tempsRestant; // Retourne la valeur actuelle du temps restant
    }
}