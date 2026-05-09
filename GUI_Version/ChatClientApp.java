import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatClientApp {
    
    static Socket socket;
    static PrintWriter out;
    static BufferedReader in;
    static String myUsername;
    
    // Using objects instead of raw HTML string so we can easily delete messages
    static class ChatMessage {
        String sender, time, text;
        public ChatMessage(String s, String ti, String te) { sender=s; time=ti; text=te; }
        public String toHTML() {
            // Using a full width HTML table ensures the time is perfectly aligned to the right, far from the text.
            return "<table width='100%' cellpadding='0' cellspacing='0' style='margin-bottom: 15px; font-family: \"Segoe UI\", sans-serif;'>"
                 + "<tr>"
                 + "<td style='line-height: 1.6; word-wrap: break-word;'>"
                 + "<span style='font-weight: bold; color: #111; font-size: 15px;'>" + sender + ": </span>"
                 + "<span style='color: #222; font-size: 15px;'>" + text + "</span>"
                 + "</td>"
                 + "<td align='right' valign='bottom' width='80'>"
                 + "<span style='color: #999; font-size: 11px;'>" + time + "</span>"
                 + "</td>"
                 + "</tr>"
                 + "</table>";
        }
    }
    
    static Map<String, List<ChatMessage>> roomChats = new HashMap<>();
    
    static JFrame frame;
    static JPanel cards;
    static JList<String> roomList;
    static DefaultListModel<String> roomModel = new DefaultListModel<>();
    static JTextPane chatArea;
    static RoundTextField inputField;
    
    public static void main(String[] args) {
        setupTheme();
        SwingUtilities.invokeLater(ChatClientApp::buildGUI);
    }
    
    static void setupTheme() {
        UIManager.put("Panel.background", new Color(245, 245, 245));
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("SplitPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("SplitPaneDivider.border", BorderFactory.createEmptyBorder());
    }

    // --- CUSTOM MODERN COMPONENTS ---
    static class RoundButton extends JButton {
        private Color normalColor = Color.BLACK;
        private Color hoverColor = new Color(50, 50, 50);

        public RoundButton(String text) {
            super(text);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setForeground(Color.WHITE);
            setBackground(normalColor);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 20, 10, 20));

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { setBackground(hoverColor); repaint(); }
                public void mouseExited(MouseEvent e) { setBackground(normalColor); repaint(); }
                public void mousePressed(MouseEvent e) { setBackground(Color.DARK_GRAY); repaint(); }
                public void mouseReleased(MouseEvent e) { setBackground(hoverColor); repaint(); }
            });
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            super.paintComponent(g);
            g2.dispose();
        }
    }

    static class RoundTextField extends JTextField {
        private String placeholder;
        public RoundTextField(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setBorder(new EmptyBorder(12, 20, 12, 20));
            setFont(new Font("Segoe UI", Font.PLAIN, 15));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, getHeight(), getHeight());
            super.paintComponent(g);
            if (getText().isEmpty() && placeholder != null && !isFocusOwner()) {
                g2.setColor(new Color(150, 150, 150));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(placeholder, getInsets().left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isFocusOwner() ? Color.BLACK : new Color(220, 220, 220));
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, getHeight(), getHeight());
            g2.dispose();
        }
    }

    static class RoundPasswordField extends JPasswordField {
        private String placeholder;
        public RoundPasswordField(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setBorder(new EmptyBorder(12, 20, 12, 20));
            setFont(new Font("Segoe UI", Font.PLAIN, 15));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, getHeight(), getHeight());
            super.paintComponent(g);
            if (getPassword().length == 0 && placeholder != null && !isFocusOwner()) {
                g2.setColor(new Color(150, 150, 150));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(placeholder, getInsets().left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            }
            g2.dispose();
        }
        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isFocusOwner() ? Color.BLACK : new Color(220, 220, 220));
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, getHeight(), getHeight());
            g2.dispose();
        }
    }

    static class ModernListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, false);
            label.setBorder(new EmptyBorder(18, 25, 18, 25));
            label.setFont(new Font("Segoe UI", Font.BOLD, 15));
            if (isSelected) {
                label.setBackground(new Color(235, 235, 235));
                label.setForeground(Color.BLACK);
            } else {
                label.setBackground(Color.WHITE);
                label.setForeground(new Color(100, 100, 100));
            }
            return label;
        }
    }

    // --- GUI BUILDER ---
    static void buildGUI() {
        frame = new JFrame("Chat Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(950, 650);
        frame.setMinimumSize(new Dimension(850, 500));
        
        cards = new JPanel(new CardLayout());
        cards.setBackground(new Color(245, 245, 245));
        
        // --- SPLIT-SCREEN LOGIN PANEL ---
        JPanel loginPanel = new JPanel(new GridLayout(1, 2));
        
        // Left Side (Branding / Dark Theme)
        JPanel leftSide = new JPanel(new GridBagLayout());
        leftSide.setBackground(new Color(20, 20, 20)); // Deep dark color
        JLabel brandLabel = new JLabel("<html><div style='text-align: center; font-family: \"Segoe UI\", sans-serif;'>"
            + "<h1 style='font-size: 45px; color: white; margin-bottom: 5px;'><nobr>Nexus Chat</nobr></h1>"
            + "<p style='font-size: 16px; color: #aaaaaa; margin-top: 0;'><nobr>Connect instantly.</nobr><br><nobr>Communicate seamlessly.</nobr></p>"
            + "</div></html>");
        leftSide.add(brandLabel);
        
        // Right Side (Form / Clean Light Theme)
        JPanel rightSide = new JPanel(new GridBagLayout());
        rightSide.setBackground(Color.WHITE);
        
        JPanel formBox = new JPanel();
        formBox.setOpaque(false);
        formBox.setLayout(new BoxLayout(formBox, BoxLayout.Y_AXIS));
        
        JLabel titleLabel = new JLabel("Sign In");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 38));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel subLabel = new JLabel("Welcome back! Please enter your details.");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subLabel.setForeground(new Color(130, 130, 130));
        subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        RoundTextField userField = new RoundTextField("Username");
        userField.setPreferredSize(new Dimension(380, 55));
        userField.setMaximumSize(new Dimension(380, 55));
        userField.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        RoundPasswordField passField = new RoundPasswordField("Password");
        passField.setPreferredSize(new Dimension(380, 55));
        passField.setMaximumSize(new Dimension(380, 55));
        passField.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        RoundButton loginBtn = new RoundButton("Log In");
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        loginBtn.setPreferredSize(new Dimension(380, 55));
        loginBtn.setMaximumSize(new Dimension(380, 55));
        loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        formBox.add(titleLabel);
        formBox.add(Box.createVerticalStrut(10));
        formBox.add(subLabel);
        formBox.add(Box.createVerticalStrut(45));
        formBox.add(userField);
        formBox.add(Box.createVerticalStrut(25));
        formBox.add(passField);
        formBox.add(Box.createVerticalStrut(45));
        formBox.add(loginBtn);
        
        rightSide.add(formBox, new GridBagConstraints()); // Centers formBox vertically and horizontally
        
        loginPanel.add(leftSide);
        loginPanel.add(rightSide);
        cards.add(loginPanel, "LOGIN");
        
        // --- CHAT PANEL ---
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(new Color(245, 245, 245));
        
        // Left Sidebar
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBackground(Color.WHITE);
        leftPanel.setPreferredSize(new Dimension(280, 0));
        
        JPanel leftHeader = new JPanel(new BorderLayout());
        leftHeader.setBackground(Color.WHITE);
        leftHeader.setBorder(new EmptyBorder(25, 25, 15, 25));
        
        JLabel appTitle = new JLabel("Chats", SwingConstants.LEFT);
        appTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        RoundButton logoutBtn = new RoundButton("Logout");
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoutBtn.setBorder(new EmptyBorder(6, 14, 6, 14));
        
        leftHeader.add(appTitle, BorderLayout.WEST);
        leftHeader.add(logoutBtn, BorderLayout.EAST);
        leftPanel.add(leftHeader, BorderLayout.NORTH);
        
        roomList = new JList<>(roomModel);
        roomList.setCellRenderer(new ModernListRenderer());
        roomList.setBackground(Color.WHITE);
        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomList.addListSelectionListener(e -> {
            if(!e.getValueIsAdjusting()) {
                String sel = roomList.getSelectedValue();
                if(sel != null) refreshChatArea(sel);
            }
        });
        
        leftPanel.add(new JScrollPane(roomList), BorderLayout.CENTER);
        
        // Right Main Area
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(new Color(248, 249, 250)); // Very light gray background
        
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setContentType("text/html");
        chatArea.setBackground(new Color(248, 249, 250));
        rightPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        
        // Input Area
        JPanel inputPanel = new JPanel(new BorderLayout(15, 0));
        inputPanel.setBackground(Color.WHITE);
        inputPanel.setBorder(new EmptyBorder(20, 25, 20, 25));
        
        inputField = new RoundTextField("Type your message...");
        inputField.setPreferredSize(new Dimension(0, 50));
        
        RoundButton sendBtn = new RoundButton("Send");
        sendBtn.setPreferredSize(new Dimension(110, 50));
        sendBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);
        rightPanel.add(inputPanel, BorderLayout.SOUTH);
        
        chatPanel.add(leftPanel, BorderLayout.WEST);
        chatPanel.add(rightPanel, BorderLayout.CENTER);
        
        cards.add(chatPanel, "CHAT");
        frame.add(cards);
        
        // --- EVENT LISTENERS ---
        logoutBtn.addActionListener(e -> {
            try { if(socket != null) socket.close(); } catch(Exception ex){}
            ((CardLayout)cards.getLayout()).show(cards, "LOGIN");
            frame.setTitle("Chat Client");
            userField.setText("");
            passField.setText("");
        });
        
        loginBtn.addActionListener(e -> {
            String u = userField.getText().trim();
            String p = new String(passField.getPassword()).trim();
            if(!u.isEmpty() && !p.isEmpty()) connectToServer(u, p);
        });
        
        Action sendAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = inputField.getText().trim();
                String target = roomList.getSelectedValue();
                if(!text.isEmpty() && target != null && out != null) {
                    out.println("MSG|" + target + "|" + text);
                    inputField.setText("");
                }
            }
        };
        sendBtn.addActionListener(sendAction);
        inputField.addActionListener(sendAction);
        
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    // Renders the entire chat log for a specific room to the JTextPane
    static void refreshChatArea(String room) {
        if (!roomChats.containsKey(room)) return;
        
        StringBuilder fullHtml = new StringBuilder();
        for (ChatMessage msg : roomChats.get(room)) {
            fullHtml.append(msg.toHTML());
        }
        
        String htmlWrap = "<html><body style='font-family: \"Segoe UI\", sans-serif; margin: 25px;'>" 
                        + fullHtml.toString() + "</body></html>";
        
        chatArea.setText(htmlWrap);
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    static void appendMessage(String target, String sender, String time, String text) {
        if(!roomChats.containsKey(target)) return;
        
        ChatMessage newMsg = new ChatMessage(sender, time, text);
        roomChats.get(target).add(newMsg);
        
        SwingUtilities.invokeLater(() -> {
            if(target.equals(roomList.getSelectedValue())) {
                refreshChatArea(target);
            }
        });
    }

    // --- NETWORK LOGIC ---
    static void connectToServer(String u, String p) {
        try {
            socket = new Socket("localhost", 20774);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            out.println("LOGIN|" + u + "|" + p);
            
            new Thread(() -> {
                try {
                    String line;
                    while((line = in.readLine()) != null) {
                        
                        if(line.startsWith("AUTH_OK|")) {
                            myUsername = u;
                            String[] parts = line.split("\\|");
                            SwingUtilities.invokeLater(() -> {
                                roomModel.clear();
                                roomChats.clear();
                                if(parts.length > 1) {
                                    for(String r : parts[1].split(",")) {
                                        roomModel.addElement(r);
                                        roomChats.put(r, new ArrayList<>());
                                    }
                                }
                                if(roomModel.getSize() > 0) roomList.setSelectedIndex(0);
                                ((CardLayout)cards.getLayout()).show(cards, "CHAT");
                                frame.setTitle("Chat Client - " + myUsername);
                            });
                            
                        } else if(line.startsWith("AUTH_FAIL|")) {
                            String reason = line.split("\\|")[1];
                            JOptionPane.showMessageDialog(frame, "Login Failed: " + reason, "Error", JOptionPane.ERROR_MESSAGE);
                            socket.close();
                            break;
                            
                        } else if (line.startsWith("UPDATE_GROUPS|")) {
                            String[] parts = line.split("\\|");
                            SwingUtilities.invokeLater(() -> {
                                String sel = roomList.getSelectedValue();
                                roomModel.clear();
                                if(parts.length > 1) {
                                    for(String r : parts[1].split(",")) {
                                        roomModel.addElement(r);
                                        if(!roomChats.containsKey(r)) roomChats.put(r, new ArrayList<>());
                                    }
                                }
                                if(sel != null && roomChats.containsKey(sel)) {
                                    roomList.setSelectedValue(sel, true);
                                } else if(roomModel.getSize() > 0) {
                                    roomList.setSelectedIndex(0);
                                }
                            });
                            
                        } else if(line.startsWith("MSG|")) {
                            String[] pParts = line.split("\\|", 5);
                            if(pParts.length == 5) {
                                String target = pParts[1];
                                String sender = pParts[2];
                                String time = pParts[3];
                                String text = pParts[4];
                                appendMessage(target, sender, time, text);
                            }
                            
                        } else if(line.startsWith("CLEAR_CHAT|")) {
                            // Server commanded to clear a whole room
                            String target = line.split("\\|")[1];
                            if(roomChats.containsKey(target)) {
                                roomChats.get(target).clear();
                                SwingUtilities.invokeLater(() -> {
                                    if(target.equals(roomList.getSelectedValue())) refreshChatArea(target);
                                });
                            }
                            
                        } else if(line.startsWith("CLEAR_USER|")) {
                            // Server commanded to clear all messages from a specific user
                            String user = line.split("\\|")[1];
                            for (List<ChatMessage> list : roomChats.values()) {
                                list.removeIf(msg -> msg.sender.equals(user));
                            }
                            SwingUtilities.invokeLater(() -> {
                                String sel = roomList.getSelectedValue();
                                if(sel != null) refreshChatArea(sel);
                            });
                            
                        } else if(line.startsWith("KICK|")) {
                            JOptionPane.showMessageDialog(frame, "Disconnected by the administrator.", "Kicked", JOptionPane.WARNING_MESSAGE);
                            socket.close();
                            SwingUtilities.invokeLater(() -> {
                                ((CardLayout)cards.getLayout()).show(cards, "LOGIN");
                                frame.setTitle("Chat Client");
                            });
                            break;
                        }
                    }
                } catch(Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame, "Connection lost.", "Error", JOptionPane.ERROR_MESSAGE);
                        ((CardLayout)cards.getLayout()).show(cards, "LOGIN");
                        frame.setTitle("Chat Client");
                    });
                }
            }).start();
            
        } catch(Exception ex) {
            JOptionPane.showMessageDialog(frame, "Cannot connect to server. Ensure it is running.");
        }
    }
}
