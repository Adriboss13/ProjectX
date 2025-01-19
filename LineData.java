/******************************************************************************
 * LineData.java
 * Représentation d'une ligne de dessin
 *
 * Cette classe :
 * - Stocke les informations d'une ligne de dessin
 * - Est sérialisable pour la transmission réseau
 * - Gère la couleur et l'épaisseur du trait
 *****************************************************************************/

import java.awt.*;
import java.io.*;
import java.util.ArrayList;

/**
 * Classe représentant une ligne de dessin avec ses propriétés
 * Implémente Serializable pour permettre la transmission réseau
 */
public class LineData implements Serializable {
    //==========================================================================
    // Variables membres
    //==========================================================================
    private final ArrayList<Point> line;      // Points constituant la ligne
    private final Color color;                // Couleur de la ligne
    private final int strokeWidth;            // Épaisseur du trait

    //==========================================================================
    // Constructeur
    //==========================================================================
    /**
     * Crée une nouvelle ligne avec ses propriétés
     * @param line Liste des points constituant la ligne
     * @param color Couleur de la ligne
     * @param strokeWidth Épaisseur du trait
     */
    public LineData(ArrayList<Point> line, Color color, int strokeWidth) {
        this.line = line;
        this.color = color;
        this.strokeWidth = strokeWidth;
    }

    //==========================================================================
    // Getters
    //==========================================================================
    /**
     * Retourne l'épaisseur du trait
     * @return Épaisseur en pixels
     */
    public int getStrokeWidth() {
        return strokeWidth;
    }

    /**
     * Retourne la couleur de la ligne
     * @return Couleur du trait
     */
    public Color getColor() {
        return color;
    }

    /**
     * Retourne la liste des points constituant la ligne
     * @return Liste des points
     */
    public ArrayList<Point> getLine() {
        return line;
    }
}