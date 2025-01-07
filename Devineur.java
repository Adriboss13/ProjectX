// La classe Devineur étend la classe Joueur et ajoute une fonctionnalité spécifique pour permettre à un devineur de proposer un mot.

class Devineur extends Joueur {

    // Constructeur qui initialise le nom du devineur en appelant le constructeur de la classe parent (Joueur)
    public Devineur(String nom) {
        super(nom); // Appelle le constructeur de la classe parent pour initialiser le nom du joueur
    }

    // Méthode qui permet au devineur de proposer un mot en affichant un message dans la console.
    public void proposerMot(String mot) {
        System.out.println(getNom() + " propose : " + mot);
        // Affiche un message indiquant que le devineur propose le mot passé en paramètre
    }
}