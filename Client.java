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
    private static final String HOST = "localhost";
    private static final int PORT = 12345;
    
    // Composants réseau
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    
    // Données de dessin
    private static List<LineData> lines = Collections.synchronizedList(new ArrayList<>());
    private static Color currentColor = Color.BLACK;
    private static ArrayList<Point> currentLine = new ArrayList<>();
    
    // Interface graphique
    private JButton selectedColorButton = null;
    private JTextArea chatArea;
    private DrawingPanel drawingPanel;
    private JTextField chatInput;
    
    public Client() {
        setTitle("Dessiner c'est Gagné");
        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Layout principal
        setLayout(new BorderLayout());
        
        // Panneau principal divisé en deux
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        // Panneau gauche (dessin + couleurs)
        JPanel leftPanel = new JPanel(new BorderLayout());
        
        // Palette de couleurs
        JPanel colorPanel = createColorPanel();
        leftPanel.add(colorPanel, BorderLayout.NORTH);
        
        // Panneau de dessin
        drawingPanel = new DrawingPanel();
        drawingPanel.setPreferredSize(new Dimension(600, 600));
        leftPanel.add(drawingPanel, BorderLayout.CENTER);
        
        // Panneau droit (chat)
        JPanel rightPanel = new JPanel(new BorderLayout());
        
        // Zone de chat
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setPreferredSize(new Dimension(300, 600));
        
        // Zone de saisie du chat
        chatInput = new JTextField();
        chatInput.addActionListener(e -> {
            String message = chatInput.getText();
            if (!message.trim().isEmpty()) {
                out.println(message);
                chatInput.setText("");
            }
        });
        
        rightPanel.add(scrollPane, BorderLayout.CENTER);
        rightPanel.add(chatInput, BorderLayout.SOUTH);
        
        // Ajout des panneaux au splitPane
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        splitPane.setResizeWeight(0.7);
        
        add(splitPane);
        
        // Configuration des événements de la souris
        setupMouseListeners();
        
        // Connexion au serveur
        connectToServer();
    }
    
    private JPanel createColorPanel() {
        JPanel colorPanel = new JPanel();
        Color[] colors = {
            Color.RED, Color.ORANGE, Color.YELLOW,
            new Color(0, 100, 0), // Vert foncé
            new Color(144, 238, 144), // Vert clair
            Color.CYAN, Color.BLUE,
            new Color(128, 0, 128), // Violet
            Color.PINK, new Color(139, 69, 19), // Marron
            Color.GRAY, Color.BLACK, Color.WHITE // Gomme
        };
        
        for (Color color : colors) {
            JButton colorButton = new JButton();
            colorButton.setPreferredSize(new Dimension(30, 30));
            colorButton.setBackground(color);
            colorButton.setOpaque(true);
            colorButton.setBorderPainted(false);
            
            colorButton.addActionListener(e -> {
                if (selectedColorButton != null) {
                    selectedColorButton.setPreferredSize(new Dimension(30, 30));
                }
                selectedColorButton = colorButton;
                colorButton.setPreferredSize(new Dimension(40, 40));
                currentColor = color;
                colorPanel.revalidate();
            });
            
            if (color == Color.BLACK) {
                colorButton.setPreferredSize(new Dimension(40, 40));
                selectedColorButton = colorButton;
            }
            
            colorPanel.add(colorButton);
        }
        
        return colorPanel;
    }
    
    private void setupMouseListeners() {
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
                lines.add(new LineData(new ArrayList<>(currentLine), currentColor));
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
    
    private void connectToServer() {
        try {
            socket = new Socket(HOST, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            
            // Thread pour recevoir les messages
            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        if (message.startsWith("DRAW:")) {
                            // Traitement des données de dessin
                            try {
                                byte[] data = Base64.getDecoder().decode(message.substring(5));
                                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                                ObjectInputStream ois = new ObjectInputStream(bais);
                                LineData receivedLineData = (LineData) ois.readObject();
                                synchronized (lines) {
                                    lines.add(receivedLineData);
                                }
                                SwingUtilities.invokeLater(() -> drawingPanel.repaint());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            // Messages du chat et du jeu
                            final String finalMessage = message;
                            SwingUtilities.invokeLater(() -> {
                                chatArea.append(finalMessage + "\n");
                                chatArea.setCaretPosition(chatArea.getDocument().getLength());
                            });
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
            
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Impossible de se connecter au serveur.");
            System.exit(1);
        }
    }
    
    private void sendDrawingData() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(new LineData(new ArrayList<>(currentLine), currentColor));
            out.println("DRAW:" + Base64.getEncoder().encodeToString(baos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Ajout des getters statiques nécessaires
    public static List<LineData> getLines() {
        return lines;
    }

    public static Color getCurrentColor() {
        return currentColor;
    }

    public static ArrayList<Point> getCurrentLine() {
        return currentLine;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Client client = new Client();
            client.setVisible(true);
        });
    }
}