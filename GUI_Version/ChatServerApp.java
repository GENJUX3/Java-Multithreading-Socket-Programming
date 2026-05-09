import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServerApp {
    private static final int PORT = 20774;
    
    // UI Components
    static JTextArea logArea;
    static ModernTable usersTable, groupsTable, activeTable;
    static DefaultTableModel usersTableModel, groupsTableModel, activeTableModel;
    static JComboBox<String> groupUserCombo;
    static JComboBox<String> targetCombo;
    static JComboBox<String> chatUserCombo;
    static JFrame frame;
    static JPanel cards;
    
    // Data Storage Models
    static class UserInfo {
        String username, password, registerTime, lastLoginTime;
        public UserInfo(String u, String p, String r, String l) {
            username=u; password=p; registerTime=r; lastLoginTime=l;
        }
        public UserInfo(String u, String data) {
            username = u;
            String[] p = data.split("\\|");
            password = p[0];
            registerTime = p.length > 1 ? p[1] : getCurrentTime();
            lastLoginTime = p.length > 2 ? p[2] : "Never";
        }
        public String toDataString() { return password + "|" + registerTime + "|" + lastLoginTime; }
    }

    static class GroupInfo {
        String name, createTime;
        List<String> members = new ArrayList<>();
        public GroupInfo(String n, String data) {
            name = n;
            String[] p = data.split("\\|", 2);
            createTime = p[0];
            if (p.length > 1 && !p[1].isEmpty()) {
                members = new ArrayList<>(Arrays.asList(p[1].split(",")));
            }
        }

        public String toDataString() { return createTime + "|" + String.join(",", members); }
    }
    
    static Map<String, UserInfo> userDB = new HashMap<>();
    static Map<String, GroupInfo> groupDB = new HashMap<>();
    static List<String> history = new ArrayList<>();
    
    // Networking
    static ServerSocket serverSocket;
    static List<ClientHandler> activeClients = new CopyOnWriteArrayList<>();
    
    public static void main(String[] args) {
        setupTheme();
        loadData();
        SwingUtilities.invokeLater(ChatServerApp::buildGUI);
        startServer();
    }
    
    static String getCurrentTime() {
        return new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.ENGLISH).format(new Date());
    }
    
    static void setupTheme() {
        UIManager.put("Panel.background", new Color(248, 249, 250));
        UIManager.put("Button.background", Color.BLACK);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.focus", new Color(0, 0, 0, 0));
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("ComboBox.background", Color.WHITE);
        UIManager.put("ComboBox.foreground", Color.BLACK);
        UIManager.put("TableHeader.background", Color.WHITE);
        UIManager.put("Table.selectionBackground", new Color(240, 240, 240));
        UIManager.put("Table.selectionForeground", Color.BLACK);
    }

    // --- CUSTOM MODERN COMPONENTS ---
    static class RoundButton extends JButton {
        public RoundButton(String text) {
            super(text);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setForeground(Color.WHITE);
            setBackground(Color.BLACK);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(10, 20, 10, 20));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { setBackground(new Color(50, 50, 50)); repaint(); }
                public void mouseExited(MouseEvent e) { setBackground(Color.BLACK); repaint(); }
                public void mousePressed(MouseEvent e) { setBackground(Color.DARK_GRAY); repaint(); }
                public void mouseReleased(MouseEvent e) { setBackground(new Color(50, 50, 50)); repaint(); }
            });
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight()); // Pill shape
            super.paintComponent(g);
            g2.dispose();
        }
    }
    
    static class RoundTextField extends JTextField {
        private String placeholder;
        public RoundTextField(String placeholder) {
            this.placeholder = placeholder;
            setOpaque(false);
            setBorder(new EmptyBorder(10, 15, 10, 15));
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
    
    static class ModernListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, false);
            label.setBorder(new EmptyBorder(18, 25, 18, 25));
            label.setFont(new Font("Segoe UI", Font.BOLD, 15));
            if (isSelected) {
                label.setBackground(new Color(230, 230, 230));
                label.setForeground(Color.BLACK);
            } else {
                label.setBackground(Color.WHITE);
                label.setForeground(new Color(90, 90, 90));
            }
            return label;
        }
    }
    
    // --- CUSTOM TABLE WITHOUT LINES ---
    static class ModernTable extends JTable {
        public ModernTable(DefaultTableModel model) {
            super(model);
            setShowGrid(false); // No lines!
            setIntercellSpacing(new Dimension(0, 0));
            setRowHeight(45);
            setFillsViewportHeight(true);
            setBackground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.PLAIN, 15));
            
            getTableHeader().setReorderingAllowed(false);
            getTableHeader().setBackground(Color.WHITE);
            getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 15));
            getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));
            getTableHeader().setPreferredSize(new Dimension(0, 50));
            
            setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    c.setBorder(new EmptyBorder(0, 20, 0, 20)); // Padding inside cells
                    return c;
                }
            });
        }
    }
    
    // --- DATA HANDLING ---
    static void loadData() {
        try {
            File uf = new File("users.db");
            if(uf.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(uf));
                String line;
                while((line = br.readLine()) != null) {
                    String[] p = line.split("=", 2);
                    if(p.length == 2) userDB.put(p[0], new UserInfo(p[0], p[1]));
                }
                br.close();
            }
            File gf = new File("groups.db");
            if(gf.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(gf));
                String line;
                while((line = br.readLine()) != null) {
                    String[] p = line.split("=", 2);
                    if(p.length == 2) groupDB.put(p[0], new GroupInfo(p[0], p[1]));
                }
                br.close();
            }
            File hf = new File("chat_history.log");
            if(hf.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(hf));
                String line;
                while((line = br.readLine()) != null) history.add(line);
                br.close();
            }
        } catch(Exception e) { e.printStackTrace(); }
    }
    
    static void saveUsers() {
        try {
            FileWriter fw = new FileWriter("users.db", false);
            for(UserInfo u : userDB.values()) fw.write(u.username + "=" + u.toDataString() + "\n");
            fw.close();
        } catch(Exception e){}
    }
    
    static void saveGroups() {
        try {
            FileWriter fw = new FileWriter("groups.db", false);
            for(GroupInfo g : groupDB.values()) fw.write(g.name + "=" + g.toDataString() + "\n");
            fw.close();
        } catch(Exception e){}
    }
    
    static void appendHistory(String record) {
        history.add(record);
        saveHistoryToFile();
    }
    
    static void saveHistoryToFile() {
        try {
            FileWriter fw = new FileWriter("chat_history.log", false);
            for(String h : history) fw.write(h + "\n");
            fw.close();
        } catch(Exception e){}
    }
    
    static void log(String msg) {
        String time = new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(new Date());
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + time + "] " + msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    static void updateUILists() {
        SwingUtilities.invokeLater(() -> {
            // Update Users Table
            int uSel = usersTable != null ? usersTable.getSelectedRow() : -1;
            if(usersTableModel != null) {
                usersTableModel.setRowCount(0);
                for(UserInfo u : userDB.values()) {
                    boolean isOnline = activeClients.stream().anyMatch(c -> u.username.equals(c.username));
                    String status = isOnline ? "🟢 Online" : "⚫ Offline";
                    List<String> uGroups = new ArrayList<>();
                    for(GroupInfo g : groupDB.values()) if(g.members.contains(u.username)) uGroups.add(g.name);
                    String grpsStr = uGroups.isEmpty() ? "None" : String.join(", ", uGroups);
                    usersTableModel.addRow(new Object[]{ u.username, status, u.registerTime, u.lastLoginTime, grpsStr });
                }
                if(uSel >= 0 && uSel < usersTable.getRowCount()) usersTable.setRowSelectionInterval(uSel, uSel);
            }
            
            // Update Groups Table
            int gSel = groupsTable != null ? groupsTable.getSelectedRow() : -1;
            if(groupsTableModel != null) {
                groupsTableModel.setRowCount(0);
                for(GroupInfo g : groupDB.values()) {
                    long onlineCount = g.members.stream().filter(m -> activeClients.stream().anyMatch(c -> m.equals(c.username))).count();
                    groupsTableModel.addRow(new Object[]{ g.name, g.createTime, g.members.size(), onlineCount });
                }
                if(gSel >= 0 && gSel < groupsTable.getRowCount()) groupsTable.setRowSelectionInterval(gSel, gSel);
            }
            
            // Update Active Connections Table
            int aSel = activeTable != null ? activeTable.getSelectedRow() : -1;
            if(activeTableModel != null) {
                activeTableModel.setRowCount(0);
                for(ClientHandler ch : activeClients) {
                    if(ch.username != null) {
                        activeTableModel.addRow(new Object[]{ ch.username, ch.loginTime, ch.socket.getInetAddress().getHostAddress() });
                    }
                }
                if(aSel >= 0 && aSel < activeTable.getRowCount()) activeTable.setRowSelectionInterval(aSel, aSel);
            }
            
            // Update Combos
            if(groupUserCombo != null) {
                Object sel = groupUserCombo.getSelectedItem();
                groupUserCombo.removeAllItems();
                for(String k : userDB.keySet()) groupUserCombo.addItem(k);
                if(sel != null) groupUserCombo.setSelectedItem(sel);
            }
            if(targetCombo != null) {
                targetCombo.removeAllItems();
                targetCombo.addItem("Global");
                for(String k : groupDB.keySet()) targetCombo.addItem(k);
            }
            if(chatUserCombo != null) {
                chatUserCombo.removeAllItems();
                for(String k : userDB.keySet()) chatUserCombo.addItem(k);
            }
        });
    }
    
    // --- SERVER GUI ---
    static void buildGUI() {
        frame = new JFrame("Server Dashboard");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 750);
        frame.setMinimumSize(new Dimension(1000, 650));
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // --- LEFT SIDEBAR ---
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(280, 0));
        sidebar.setBackground(Color.WHITE);
        
        JPanel sidebarHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 25));
        sidebarHeader.setBackground(Color.WHITE);
        JLabel title = new JLabel("Admin Panel");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        sidebarHeader.add(title);
        sidebar.add(sidebarHeader, BorderLayout.NORTH);
        
        String[] navItems = {"Logs", "Users Management", "Groups Management", "Active Connections", "Chat Management"};
        JList<String> navList = new JList<>(navItems);
        navList.setCellRenderer(new ModernListRenderer());
        navList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        navList.setSelectedIndex(0);
        sidebar.add(navList, BorderLayout.CENTER);
        
        // --- CENTER CARDS ---
        cards = new JPanel(new CardLayout());
        cards.setBackground(new Color(248, 249, 250));
        
        // 1. Logs
        JPanel logsPanel = new JPanel(new BorderLayout());
        logsPanel.setBorder(new EmptyBorder(20,20,20,20));
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 15));
        logArea.setBackground(Color.WHITE);
        logArea.setBorder(new EmptyBorder(15, 15, 15, 15));
        logsPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        cards.add(logsPanel, "Logs");
        
        // 2. Users Management
        JPanel usersTab = new JPanel(new BorderLayout(20, 20));
        usersTab.setBorder(new EmptyBorder(30,30,30,30));
        
        usersTableModel = new DefaultTableModel(new String[]{"Username", "Status", "Registered", "Last Login", "Groups"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        usersTable = new ModernTable(usersTableModel);
        usersTab.add(new JScrollPane(usersTable), BorderLayout.CENTER);
        
        JPanel userControl = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        userControl.setOpaque(false);
        RoundTextField uField = new RoundTextField("Username");
        uField.setPreferredSize(new Dimension(180, 45));
        RoundTextField pField = new RoundTextField("Password");
        pField.setPreferredSize(new Dimension(180, 45));
        RoundButton addUBtn = new RoundButton("Create New User");
        RoundButton updatePBtn = new RoundButton("Update Password");
        RoundButton delUBtn = new RoundButton("Delete Selected");
        userControl.add(uField); userControl.add(pField); userControl.add(addUBtn); userControl.add(updatePBtn); userControl.add(delUBtn);
        usersTab.add(userControl, BorderLayout.SOUTH);
        cards.add(usersTab, "Users Management");
        
        // 3. Groups Management
        JPanel groupsTab = new JPanel(new BorderLayout(20, 20));
        groupsTab.setBorder(new EmptyBorder(30,30,30,30));
        
        groupsTableModel = new DefaultTableModel(new String[]{"Group Name", "Created At", "Members Count", "Online Members"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        groupsTable = new ModernTable(groupsTableModel);
        groupsTab.add(new JScrollPane(groupsTable), BorderLayout.CENTER);
        
        JPanel groupControl = new JPanel(new GridLayout(2, 1, 0, 15));
        groupControl.setOpaque(false);
        JPanel topG = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        topG.setOpaque(false);
        RoundTextField gField = new RoundTextField("New Group Name");
        gField.setPreferredSize(new Dimension(200, 45));
        RoundButton createGBtn = new RoundButton("Create Group");
        RoundButton delGBtn = new RoundButton("Delete Selected Group");
        topG.add(gField); topG.add(createGBtn); topG.add(delGBtn);
        
        JPanel botG = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        botG.setOpaque(false);
        groupUserCombo = new JComboBox<>();
        groupUserCombo.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        groupUserCombo.setPreferredSize(new Dimension(200, 40));
        RoundButton addGUserBtn = new RoundButton("Add User to Group");
        RoundButton remGUserBtn = new RoundButton("Remove User from Group");
        botG.add(groupUserCombo); botG.add(addGUserBtn); botG.add(remGUserBtn);
        
        groupControl.add(topG); groupControl.add(botG);
        groupsTab.add(groupControl, BorderLayout.SOUTH);
        cards.add(groupsTab, "Groups Management");
        
        // 4. Active Connections
        JPanel activeTab = new JPanel(new BorderLayout(20, 20));
        activeTab.setBorder(new EmptyBorder(30,30,30,30));
        
        activeTableModel = new DefaultTableModel(new String[]{"Username", "Login Time", "IP Address"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        activeTable = new ModernTable(activeTableModel);
        activeTab.add(new JScrollPane(activeTable), BorderLayout.CENTER);
        
        JPanel activeBot = new JPanel(new FlowLayout(FlowLayout.CENTER));
        activeBot.setOpaque(false);
        RoundButton kickBtn = new RoundButton("Kick Selected User");
        activeBot.add(kickBtn);
        activeTab.add(activeBot, BorderLayout.SOUTH);
        cards.add(activeTab, "Active Connections");
        
        // 5. Chat Management
        JPanel chatManTab = new JPanel(new BorderLayout(20, 20));
        chatManTab.setBorder(new EmptyBorder(40, 40, 40, 40));
        
        JPanel controlsPanel = new JPanel(new GridLayout(2, 1, 0, 30));
        controlsPanel.setOpaque(false);
        
        JPanel targetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        targetPanel.setOpaque(false);
        JLabel tLabel = new JLabel("Target Room:");
        tLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        targetCombo = new JComboBox<>();
        targetCombo.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        targetCombo.setPreferredSize(new Dimension(200, 40));
        RoundButton clearTargetBtn = new RoundButton("Clear Entire Room Chat");
        targetPanel.add(tLabel); targetPanel.add(targetCombo); targetPanel.add(clearTargetBtn);
        
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        userPanel.setOpaque(false);
        JLabel uLabel = new JLabel("Specific User:");
        uLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        chatUserCombo = new JComboBox<>();
        chatUserCombo.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        chatUserCombo.setPreferredSize(new Dimension(200, 40));
        RoundButton clearUserBtn = new RoundButton("Delete All Messages By User");
        userPanel.add(uLabel); userPanel.add(chatUserCombo); userPanel.add(clearUserBtn);
        
        controlsPanel.add(targetPanel);
        controlsPanel.add(userPanel);
        
        chatManTab.add(controlsPanel, BorderLayout.NORTH);
        cards.add(chatManTab, "Chat Management");
        
        // Combine layout
        mainPanel.add(sidebar, BorderLayout.WEST);
        mainPanel.add(cards, BorderLayout.CENTER);
        frame.add(mainPanel);
        
        // --- INITIAL DATA LOAD TO UI ---
        updateUILists();
        
        // --- LISTENERS ---
        navList.addListSelectionListener(e -> {
            if(!e.getValueIsAdjusting()) {
                ((CardLayout)cards.getLayout()).show(cards, navList.getSelectedValue());
            }
        });
        
        addUBtn.addActionListener(e -> {
            String u = uField.getText().trim();
            String p = pField.getText().trim();
            if(!u.isEmpty() && !p.isEmpty()) {
                if(!userDB.containsKey(u)) {
                    userDB.put(u, new UserInfo(u, p, getCurrentTime(), "Never"));
                    saveUsers(); updateUILists();
                    uField.setText(""); pField.setText("");
                    log("Created account: " + u);
                } else {
                    JOptionPane.showMessageDialog(frame, "User already exists!");
                }
            }
        });
        
        updatePBtn.addActionListener(e -> {
            int row = usersTable.getSelectedRow();
            if(row >= 0) {
                String u = (String) usersTableModel.getValueAt(row, 0);
                String newPass = pField.getText().trim();
                if(!newPass.isEmpty()) {
                    userDB.get(u).password = newPass;
                    saveUsers();
                    pField.setText("");
                    log("Updated password for: " + u);
                    JOptionPane.showMessageDialog(frame, "Password updated successfully.");
                } else {
                    JOptionPane.showMessageDialog(frame, "Please enter a new password in the field.");
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Please select a user from the table first.");
            }
        });
        
        delUBtn.addActionListener(e -> {
            int row = usersTable.getSelectedRow();
            if(row >= 0) {
                String u = (String) usersTableModel.getValueAt(row, 0);
                userDB.remove(u); saveUsers();
                for(GroupInfo g : groupDB.values()) {
                    if(g.members.remove(u)) saveGroups();
                }
                updateUILists(); kickUser(u);
                log("Deleted account: " + u);
            }
        });
        
        createGBtn.addActionListener(e -> {
            String g = gField.getText().trim();
            if(!g.isEmpty() && !g.contains(",") && !g.contains("|") && !g.equalsIgnoreCase("Global")) {
                if(!groupDB.containsKey(g)) {
                    groupDB.put(g, new GroupInfo(g, getCurrentTime() + "|"));
                    saveGroups(); updateUILists(); gField.setText("");
                    log("Created group: " + g);
                }
            }
        });
        
        delGBtn.addActionListener(e -> {
            int row = groupsTable.getSelectedRow();
            if(row >= 0) {
                String g = (String) groupsTableModel.getValueAt(row, 0);
                groupDB.remove(g); saveGroups(); updateUILists();
                log("Deleted group: " + g);
                for(ClientHandler ch : activeClients) sendGroupUpdate(ch.username);
            }
        });
        
        addGUserBtn.addActionListener(e -> {
            int row = groupsTable.getSelectedRow();
            String u = (String) groupUserCombo.getSelectedItem();
            if(row >= 0 && u != null) {
                String g = (String) groupsTableModel.getValueAt(row, 0);
                GroupInfo info = groupDB.get(g);
                if(!info.members.contains(u)) {
                    info.members.add(u);
                    saveGroups(); updateUILists(); sendGroupUpdate(u);
                    log("Added '" + u + "' to '" + g + "'");
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Select a group from the table and a user from the dropdown.");
            }
        });
        
        remGUserBtn.addActionListener(e -> {
            int row = groupsTable.getSelectedRow();
            String u = (String) groupUserCombo.getSelectedItem();
            if(row >= 0 && u != null) {
                String g = (String) groupsTableModel.getValueAt(row, 0);
                GroupInfo info = groupDB.get(g);
                if(info.members.remove(u)) {
                    saveGroups(); updateUILists(); sendGroupUpdate(u);
                    log("Removed '" + u + "' from '" + g + "'");
                }
            }
        });
        
        kickBtn.addActionListener(e -> {
            int row = activeTable.getSelectedRow();
            if(row >= 0) {
                String u = (String) activeTableModel.getValueAt(row, 0);
                kickUser(u); log("Kicked: " + u); 
            }
        });
        
        clearTargetBtn.addActionListener(e -> {
            String target = (String) targetCombo.getSelectedItem();
            if(target != null) {
                history.removeIf(h -> h.startsWith(target + "|"));
                saveHistoryToFile();
                for(ClientHandler ch : activeClients) ch.send("CLEAR_CHAT|" + target);
                log("Cleared all chat history for room: " + target);
            }
        });
        
        clearUserBtn.addActionListener(e -> {
            String user = (String) chatUserCombo.getSelectedItem();
            if(user != null) {
                history.removeIf(h -> {
                    String[] p = h.split("\\|", 4);
                    return p.length >= 4 && p[1].equals(user);
                });
                saveHistoryToFile();
                for(ClientHandler ch : activeClients) ch.send("CLEAR_USER|" + user);
                log("Deleted all chat messages sent by: " + user);
            }
        });
        
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
    
    // --- SERVER NETWORKING ---
    static void startServer() {
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                log("Server running on port " + PORT);
                while(true) {
                    Socket s = serverSocket.accept();
                    new ClientHandler(s).start();
                }
            } catch(Exception e) { log("Server stopped."); }
        }).start();
    }
    
    static boolean isUserInGroup(String user, String group) {
        if(user == null) return false;
        if(!groupDB.containsKey(group)) return false;
        return groupDB.get(group).members.contains(user);
    }
    
    static void sendGroupUpdate(String username) {
        if(username == null) return;
        StringBuilder myGroups = new StringBuilder("Global");
        for (String g : groupDB.keySet()) if (isUserInGroup(username, g)) myGroups.append(",").append(g);
        for (ClientHandler ch : activeClients) if (username.equals(ch.username)) ch.send("UPDATE_GROUPS|" + myGroups.toString());
    }
    
    static void broadcast(String target, String sender, String msg) {
        String timestamp = new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(new Date());
        String record = target + "|" + sender + "|" + timestamp + "|" + msg;
        appendHistory(record);
        log("[" + target + "] " + sender + ": " + msg);
        
        String outMsg = "MSG|" + target + "|" + sender + "|" + timestamp + "|" + msg;
        for (ClientHandler ch : activeClients) {
            if (target.equals("Global") || isUserInGroup(ch.username, target)) ch.send(outMsg);
        }
    }
    
    static void kickUser(String u) {
        for(ClientHandler ch : activeClients) {
            if(ch.username != null && ch.username.equals(u)) {
                ch.send("KICK|Kicked");
                try { ch.socket.close(); } catch(Exception e){}
            }
        }
    }
    
    static class ClientHandler extends Thread {
        Socket socket; PrintWriter out; BufferedReader in; 
        String username;
        String loginTime;
        
        public ClientHandler(Socket s) { this.socket = s; }
        public void send(String m) { if(out != null) out.println(m); }
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                
                String line = in.readLine();
                if (line != null && line.startsWith("LOGIN|")) {
                    String[] p = line.split("\\|", 3);
                    if(p.length == 3) {
                        String u = p[1], pwd = p[2];
                        if (userDB.containsKey(u) && userDB.get(u).password.equals(pwd)) {
                            boolean alreadyIn = false;
                            for(ClientHandler ch : activeClients) if (u.equals(ch.username)) alreadyIn = true;
                            if(alreadyIn) { send("AUTH_FAIL|Already logged in."); socket.close(); return; }
                            
                            // Successful Login
                            username = u; 
                            loginTime = getCurrentTime();
                            userDB.get(u).lastLoginTime = loginTime;
                            saveUsers();
                            
                            activeClients.add(this); 
                            updateUILists();
                            log("Connected: " + username);
                            
                            StringBuilder myGroups = new StringBuilder("Global");
                            for (String g : groupDB.keySet()) if (isUserInGroup(username, g)) myGroups.append(",").append(g);
                            send("AUTH_OK|" + myGroups.toString());
                            
                            for(String h : history) {
                                String[] hp = h.split("\\|", 4);
                                if(hp.length >= 4 && (hp[0].equals("Global") || isUserInGroup(username, hp[0]))) {
                                    send("MSG|" + hp[0] + "|" + hp[1] + "|" + hp[2] + "|" + hp[3]);
                                }
                            }
                            
                            broadcast("Global", "System", username + " joined.");
                            
                            while((line = in.readLine()) != null) {
                                if(line.startsWith("MSG|")) {
                                    String[] msgParts = line.split("\\|", 3);
                                    if(msgParts.length == 3 && (msgParts[1].equals("Global") || isUserInGroup(username, msgParts[1]))) {
                                        broadcast(msgParts[1], username, msgParts[2]);
                                    }
                                }
                            }
                        } else send("AUTH_FAIL|Invalid credentials.");
                    }
                }
            } catch(Exception e) {} finally {
                if(username != null) {
                    activeClients.remove(this); updateUILists();
                    broadcast("Global", "System", username + " left.");
                    log("Disconnected: " + username);
                }
                try { socket.close(); } catch(Exception e){}
            }
        }
    }
}
