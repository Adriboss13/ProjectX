import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Base64;
import javax.swing.*;

public class Client extends JFrame {
    private static final int PORT = 12345;
    private static final String SERVER = "127.0.0.1";
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static final List<LineData> lines = Collections.synchronizedList(new ArrayList<>());
    private ArrayList<Point> currentLine = new ArrayList<>();
    private Color currentColor = Color.BLACK;
    private JButton selectedColorButton = null;

    public Client() {
        setTitle("Collaborative Drawing");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        DrawingPanel drawingPanel = new DrawingPanel();
        add(drawingPanel, BorderLayout.CENTER);

        // Ajouter une palette de couleurs avec des carrés
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

        for (Color color : colors) {
            JButton colorButton = new JButton();
            colorButton.setPreferredSize(new Dimension(30, 30));
            colorButton.setBackground(color);
            colorButton.setOpaque(true);
            colorButton.setBorderPainted(false);
            colorButton.addActionListener(e -> {
                if (selectedColorButton != null) {
                    selectedColorButton.setPreferredSize(new Dimension(30, 30)); // Réduire la taille du bouton précédent
                }
                selectedColorButton = colorButton; // Mettre à jour le bouton sélectionné
                colorButton.setPreferredSize(new Dimension(40, 40)); // Agrandir le bouton sélectionné
                currentColor = color; // Mettre à jour la couleur sélectionnée
                colorPanel.revalidate();
            });
            
            // Si c'est la couleur noire, l'agrandir par défaut
            if (color == Color.BLACK) {
                colorButton.setPreferredSize(new Dimension(40, 40));
                selectedColorButton = colorButton;
            }
            
            colorPanel.add(colorButton);
        }
        add(colorPanel, BorderLayout.NORTH);

        try {
            socket = new Socket(SERVER, PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            new MessageListener().start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        drawingPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                currentLine.clear();
                currentLine.add(e.getPoint());
                sendDrawingData();
                drawingPanel.repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                lines.add(new LineData(currentLine, currentColor));
                currentLine = new ArrayList<>();
                sendDrawingData();
                drawingPanel.repaint();
            }
        });

        drawingPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                currentLine.add(e.getPoint());
                sendDrawingData();
                drawingPanel.repaint();
            }
        });
    }

    private void sendDrawingData() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(new LineData(currentLine, currentColor));
            out.println(Base64.getEncoder().encodeToString(baos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class MessageListener extends Thread {
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    byte[] data = Base64.getDecoder().decode(message);
                    ByteArrayInputStream bais = new ByteArrayInputStream(data);
                    ObjectInputStream ois = new ObjectInputStream(bais);
                    LineData receivedLineData = (LineData) ois.readObject();
                    synchronized (lines) {
                        lines.add(receivedLineData);
                    }
                    SwingUtilities.invokeLater(() -> repaint());
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private class DrawingPanel extends JPanel {
        public DrawingPanel() {
            setBackground(Color.WHITE); // Définit le fond en blanc
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            synchronized (lines) {
                for (LineData lineData : lines) {
                    g2d.setColor(lineData.color);
                    g2d.setStroke(new BasicStroke(lineData.color == Color.WHITE ? 10 : 4)); // Gomme plus épaisse
                    for (int i = 0; i < lineData.line.size() - 1; i++) {
                        Point p1 = lineData.line.get(i);
                        Point p2 = lineData.line.get(i + 1);
                        g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                    }
                }
                g2d.setColor(currentColor);
                g2d.setStroke(new BasicStroke(currentColor == Color.WHITE ? 10 : 4)); // Gomme plus épaisse
                for (int i = 0; i < currentLine.size() - 1; i++) {
                    Point p1 = currentLine.get(i);
                    Point p2 = currentLine.get(i + 1);
                    g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }
    }

    private static class LineData implements Serializable {
        ArrayList<Point> line;
        Color color;

        LineData(ArrayList<Point> line, Color color) {
            this.line = line;
            this.color = color;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Client client = new Client();
            client.setVisible(true);
        });
    }
}
