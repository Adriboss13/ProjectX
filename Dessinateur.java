// La classe Dessinateur étend la classe Joueur et ajoute une fonctionnalité spécifique pour permettre à un dessinateur de dessiner un mot.

class Dessinateur extends Joueur {

    // Constructeur qui initialise le nom du dessinateur en appelant le constructeur de la classe parent (Joueur)
    public Dessinateur(String nom) {
        super(nom); // Appelle le constructeur de la classe parent pour initialiser le nom du joueur
    }

    // Méthode qui permet au dessinateur de dessiner un mot en affichant un message dans la console.
    public void dessiner(String mot) {
        System.out.println(getNom() + " dessine : " + mot);
        // Affiche un message indiquant que le dessinateur dessine le mot passé en paramètre
    }
}