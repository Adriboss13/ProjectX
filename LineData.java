// La classe LineData représente une ligne composée de plusieurs points (ArrayList de Points) et d'une couleur.
// Elle est sérialisable pour pouvoir être enregistrée ou envoyée via des flux de données (par exemple, pour le réseau).

import java.awt.*;
import java.io.*;
import java.util.ArrayList;

public class LineData implements Serializable {

    // Liste de points représentant la ligne (chaque point a une position x et y).
    ArrayList<Point> line;

    // Couleur associée à la ligne.
    Color color;

    // Constructeur de la classe qui initialise les propriétés de la ligne (les points et la couleur).
    LineData(ArrayList<Point> line, Color color) {
        this.line = line; // Assigne la liste des points à la variable d'instance 'line'
        this.color = color; // Assigne la couleur à la variable d'instance 'color'
    }
}