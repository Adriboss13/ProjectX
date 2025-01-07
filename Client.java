// Cette classe représente un client pour une application de dessin collaboratif en réseau.
// Elle permet à plusieurs utilisateurs de dessiner ensemble en temps réel, chaque action de dessin étant synchronisée via un serveur central.

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import javax.swing.*;

public class Client extends JFrame {
    // Configuration réseau
    private static final int PORT = 12345; // Port utilisé pour se connecter au serveur
    private static final String SERVER = "127.0.0.1"; // Adresse du serveur (localhost dans ce cas)

    // Gestion de la connexion et des messages
    private static Socket socket; // Socket pour la connexion au serveur
    private static PrintWriter out; // Flux pour envoyer des données au serveur
    public static BufferedReader in; // Flux pour recevoir des données du serveur

    // Données de dessin
    public static List<LineData> lines = Collections.synchronizedList(new ArrayList<>()); // Liste des lignes dessinées (partagée entre threads)
    public static Color currentColor = Color.BLACK; // Couleur actuellement sélectionnée pour dessiner
    public static ArrayList<Point> currentLine = new ArrayList<>(); // Points de la ligne en cours de dessin

    // Interface graphique
    private JButton selectedColorButton = null; // Bouton correspondant à la couleur sélectionnée

    public Client() {
        // Configuration de la fenêtre principale
        setTitle("Collaborative Drawing");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Panneau de dessin
        DrawingPanel drawingPanel = new DrawingPanel();
        add(drawingPanel, BorderLayout.CENTER);

        // Création de la palette de couleurs
        JPanel colorPanel = new JPanel();
        Color[] colors = {
            Color.RED, Color.ORANGE, Color.YELLOW,
            new Color(0, 100, 0), // Vert foncé
            new Color(144, 238, 144), // Vert clair
            Color.CYAN, Color.BLUE,
            new Color(128, 0, 128), // Violet
            Color.PINK, new Color(139, 69, 19), // Marron
            Color.GRAY, Color.BLACK, Color.WHITE // Ajout de la gomme
        };

        // Ajouter un bouton pour chaque couleur dans la palette
        for (Color color : colors) {
            JButton colorButton = new JButton();
            colorButton.setPreferredSize(new Dimension(30, 30));
            colorButton.setBackground(color);
            colorButton.setOpaque(true);
            colorButton.setBorderPainted(false);

            // Listener pour changer la couleur actuelle
            colorButton.addActionListener(e -> {
                if (selectedColorButton != null) {
                    selectedColorButton.setPreferredSize(new Dimension(30, 30)); // Réduire la taille du bouton précédent
                }
                selectedColorButton = colorButton; // Mettre à jour le bouton sélectionné
                colorButton.setPreferredSize(new Dimension(40, 40)); // Agrandir le bouton sélectionné
                currentColor = color; // Mettre à jour la couleur actuelle
                colorPanel.revalidate(); // Rafraîchir l'affichage
            });

            // Si la couleur est noire, la sélectionner par défaut
            if (color == Color.BLACK) {
                colorButton.setPreferredSize(new Dimension(40, 40));
                selectedColorButton = colorButton;
            }

            colorPanel.add(colorButton); // Ajouter le bouton à la palette
        }
        add(colorPanel, BorderLayout.NORTH);

        // Initialisation de la connexion au serveur
        try {
            socket = new Socket(SERVER, PORT); // Connexion au serveur
            out = new PrintWriter(socket.getOutputStream(), true); // Flux pour envoyer des messages
            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Flux pour recevoir des messages

            // Lancer un thread pour écouter les messages du serveur
            MessageListener messageListener = new MessageListener(this);
            messageListener.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Gestion des événements de la souris sur le panneau de dessin
        drawingPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Commencer une nouvelle ligne
                currentLine.clear();
                currentLine.add(e.getPoint());
                sendDrawingData(); // Envoyer la ligne au serveur
                drawingPanel.repaint(); // Rafraîchir l'affichage
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // Ajouter la ligne à la liste des lignes finales
                lines.add(new LineData(currentLine, currentColor));
                currentLine = new ArrayList<>(); // Réinitialiser la ligne en cours
                sendDrawingData(); // Envoyer les données au serveur
                drawingPanel.repaint(); // Rafraîchir l'affichage
            }
        });

        drawingPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                // Ajouter un point à la ligne en cours de dessin
                currentLine.add(e.getPoint());
                sendDrawingData(); // Envoyer les données au serveur
                drawingPanel.repaint(); // Rafraîchir l'affichage
            }
        });
    }

    // Méthode pour envoyer les données de dessin au serveur
    private void sendDrawingData() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(new LineData(currentLine, currentColor)); // Sérialiser la ligne et sa couleur
            out.println(Base64.getEncoder().encodeToString(baos.toByteArray())); // Envoyer les données encodées
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Point d'entrée du programme
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Client client = new Client(); // Créer une nouvelle instance du client
            client.setVisible(true); // Afficher l'interface graphique
        });
    }

    // Méthodes utilitaires pour accéder aux données depuis d'autres classes
    public static List<LineData> getLines() {
        return lines;
    }

    public static Color getCurrentColor() {
        return currentColor;
    }

    public static ArrayList<Point> getCurrentLine() {
        return currentLine;
    }

    public static BufferedReader getIn() {
        return in;
    }
}