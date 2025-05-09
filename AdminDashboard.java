// AdminDashboard.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

public class AdminDashboard extends JFrame {
    private JTable librarianTable;
    private DefaultTableModel tableModel;
    private JTabbedPane tabbedPane;
    private boolean darkMode;
    private JToggleButton darkModeToggle;
    private static final Preferences prefs = Preferences.userNodeForPackage(User.class);
    
    // Colors from User class for consistent theming
    private static final Color LIGHT_BG = new Color(240, 240, 240);
    private static final Color LIGHT_FG = Color.BLACK;
    private static final Color DARK_BG = new Color(50, 50, 50);
    private static final Color DARK_FG = Color.WHITE;
    private static final Color DARK_COMPONENT_BG = new Color(70, 70, 70);
    private static final Color DARK_BORDER = new Color(100, 100, 100);

    public AdminDashboard() {
        this(false); // Default to light mode
    }
    
    public AdminDashboard(boolean darkMode) {
        this.darkMode = darkMode;
        
        setTitle("Admin Dashboard");
        setSize(800, 600);
        setLayout(new BorderLayout());

        // Top panel for dark mode toggle
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        darkModeToggle = new JToggleButton("Dark Mode");
        darkModeToggle.setSelected(darkMode);
        darkModeToggle.addActionListener(e -> {
            this.darkMode = darkModeToggle.isSelected();
            // Save preference
            prefs.putBoolean("darkMode", this.darkMode);
            // Apply theme
            applyTheme(getContentPane());
            // Refresh UI
            SwingUtilities.updateComponentTreeUI(this);
        });
        topPanel.add(darkModeToggle);
        add(topPanel, BorderLayout.NORTH);

        // Create tabbed pane for different functionalities
        tabbedPane = new JTabbedPane();
        
        // Librarian Management Tab
        JPanel librarianPanel = createLibrarianManagementPanel();
        tabbedPane.addTab("Librarians", librarianPanel);
        
        // User Information Tab
        JPanel userInfoPanel = createUserInformationPanel();
        tabbedPane.addTab("User Information", userInfoPanel);
        
        // Reports Tab
        JPanel reportsPanel = createReportsPanel();
        tabbedPane.addTab("Reports", reportsPanel);
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Logout button at bottom
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> {
            dispose();
            new LoginScreen();
        });
        add(logoutBtn, BorderLayout.SOUTH);

        // Apply theme initially
        applyTheme(getContentPane());

        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }

    // Theme application utility - adapted from User class
    private void applyTheme(Container container) {
        // Set background and foreground for this container
        container.setBackground(darkMode ? DARK_BG : LIGHT_BG);
        container.setForeground(darkMode ? DARK_FG : LIGHT_FG);
        
        // Apply to all components in the container
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel) {
                comp.setForeground(darkMode ? DARK_FG : LIGHT_FG);
            } 
            else if (comp instanceof JTextField || comp instanceof JPasswordField || comp instanceof JComboBox) {
                comp.setBackground(darkMode ? DARK_COMPONENT_BG : Color.WHITE);
                comp.setForeground(darkMode ? DARK_FG : LIGHT_FG);
                if (comp instanceof JTextField || comp instanceof JPasswordField) {
                    ((JTextField)comp).setCaretColor(darkMode ? DARK_FG : LIGHT_FG);
                }
            }
            else if (comp instanceof JButton || comp instanceof JToggleButton) {
                comp.setBackground(darkMode ? DARK_COMPONENT_BG : null);
                comp.setForeground(darkMode ? DARK_FG : LIGHT_FG);
            }
            else if (comp instanceof JPanel) {
                comp.setBackground(darkMode ? DARK_BG : LIGHT_BG);
                applyTheme((Container)comp);
            }
            else if (comp instanceof JTable) {
                JTable table = (JTable)comp;
                table.setBackground(darkMode ? DARK_COMPONENT_BG : Color.WHITE);
                table.setForeground(darkMode ? DARK_FG : LIGHT_FG);
                table.setGridColor(darkMode ? DARK_BORDER : Color.GRAY);
                table.getTableHeader().setBackground(darkMode ? DARK_BG : LIGHT_BG);
                table.getTableHeader().setForeground(darkMode ? DARK_FG : LIGHT_FG);
            }
            else if (comp instanceof JScrollPane) {
                comp.setBackground(darkMode ? DARK_BG : LIGHT_BG);
                JScrollPane scrollPane = (JScrollPane)comp;
                scrollPane.getViewport().setBackground(darkMode ? DARK_BG : LIGHT_BG);
                applyTheme(scrollPane.getViewport());
            }
            else if (comp instanceof JTextArea) {
                comp.setBackground(darkMode ? DARK_COMPONENT_BG : Color.WHITE);
                comp.setForeground(darkMode ? DARK_FG : LIGHT_FG);
                ((JTextArea)comp).setCaretColor(darkMode ? DARK_FG : LIGHT_FG);
            }
            else if (comp instanceof JTabbedPane) {
                comp.setBackground(darkMode ? DARK_BG : LIGHT_BG);
                comp.setForeground(darkMode ? DARK_FG : LIGHT_FG);
                JTabbedPane tabPane = (JTabbedPane)comp;
                for (int i = 0; i < tabPane.getTabCount(); i++) {
                    Component tabComp = tabPane.getComponentAt(i);
                    if (tabComp instanceof Container) {
                        applyTheme((Container)tabComp);
                    }
                }
            }
            
            // For other containers, recursively apply
            if (comp instanceof Container) {
                applyTheme((Container)comp);
            }
        }
    }

    private JPanel createLibrarianManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        String[] columns = {"ID", "Name", "Username", "Active"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        librarianTable = new JTable(tableModel);
        loadLibrarians();

        JPanel btnPanel = new JPanel();
        JButton addBtn = new JButton("Add Librarian");
        JButton deleteBtn = new JButton("Delete Librarian");
        JButton toggleBtn = new JButton("Activate/Deactivate");
        btnPanel.add(addBtn);
        btnPanel.add(deleteBtn);
        btnPanel.add(toggleBtn);

        addBtn.addActionListener(e -> addLibrarian());
        deleteBtn.addActionListener(e -> deleteLibrarian());
        toggleBtn.addActionListener(e -> toggleStatus());

        panel.add(new JScrollPane(librarianTable), BorderLayout.CENTER);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createUserInformationPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // User table
        String[] userColumns = {"ID", "Name", "Username", "Role", "Status"};
        DefaultTableModel userModel = new DefaultTableModel(userColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable userTable = new JTable(userModel);
        
        // Load all users
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT id, name, username, role, active FROM users";
            ResultSet rs = conn.createStatement().executeQuery(query);
            while (rs.next()) {
                userModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("username"),
                    rs.getString("role"),
                    rs.getBoolean("active") ? "Active" : "Inactive"
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading users: " + ex.getMessage());
        }
        
        // Search functionality
        JPanel searchPanel = new JPanel();
        JTextField searchField = new JTextField(20);
        JButton searchBtn = new JButton("Search");
        JComboBox<String> searchType = new JComboBox<>(new String[]{"Name", "Username", "Role"});
        
        searchBtn.addActionListener(e -> {
            String searchText = searchField.getText().trim();
            String type = (String) searchType.getSelectedItem();
            
            if (searchText.isEmpty()) {
                // Reload all users if search is empty
                userModel.setRowCount(0);
                try (Connection conn = DBConnection.getConnection()) {
                    String query = "SELECT id, name, username, role, active FROM users";
                    ResultSet rs = conn.createStatement().executeQuery(query);
                    while (rs.next()) {
                        userModel.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("username"),
                            rs.getString("role"),
                            rs.getBoolean("active") ? "Active" : "Inactive"
                        });
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error loading users: " + ex.getMessage());
                }
                return;
            }
            
            try (Connection conn = DBConnection.getConnection()) {
                String query;
                if (type.equals("Name")) {
                    query = "SELECT id, name, username, role, active FROM users WHERE name LIKE ?";
                } else if (type.equals("Username")) {
                    query = "SELECT id, name, username, role, active FROM users WHERE username LIKE ?";
                } else {
                    query = "SELECT id, name, username, role, active FROM users WHERE role LIKE ?";
                }
                
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setString(1, "%" + searchText + "%");
                
                ResultSet rs = stmt.executeQuery();
                userModel.setRowCount(0); // Clear existing rows
                
                while (rs.next()) {
                    userModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getBoolean("active") ? "Active" : "Inactive"
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error searching users: " + ex.getMessage());
            }
        });
        
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(new JLabel("by:"));
        searchPanel.add(searchType);
        searchPanel.add(searchBtn);
        
        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(userTable), BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Report selection
        JPanel reportSelectionPanel = new JPanel();
        JComboBox<String> reportType = new JComboBox<>(new String[]{
            "User Activity Summary", 
            "Librarian Activity", 
            "User Status Distribution"
        });
        JButton generateBtn = new JButton("Generate Report");
        reportSelectionPanel.add(new JLabel("Select Report:"));
        reportSelectionPanel.add(reportType);
        reportSelectionPanel.add(generateBtn);
        
        // Report display area
        JTextArea reportArea = new JTextArea();
        reportArea.setEditable(false);
        JScrollPane reportScrollPane = new JScrollPane(reportArea);
        
        generateBtn.addActionListener(e -> {
            String selectedReport = (String) reportType.getSelectedItem();
            String reportContent = "";
            
            try (Connection conn = DBConnection.getConnection()) {
                switch (selectedReport) {
                    case "User Activity Summary":
                        reportContent = generateUserActivityReport(conn);
                        break;
                    case "Librarian Activity":
                        reportContent = generateLibrarianActivityReport(conn);
                        break;
                    case "User Status Distribution":
                        reportContent = generateStatusDistributionReport(conn);
                        break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                reportContent = "Error generating report: " + ex.getMessage();
            }
            
            reportArea.setText(selectedReport + ":\n\n" + reportContent);
        });
        
        panel.add(reportSelectionPanel, BorderLayout.NORTH);
        panel.add(reportScrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    private String generateUserActivityReport(Connection conn) throws SQLException {
        StringBuilder report = new StringBuilder();
        
        // Total users
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
        rs.next();
        int totalUsers = rs.getInt(1);
        
        // Active users
        rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE active=TRUE");
        rs.next();
        int activeUsers = rs.getInt(1);
        
        // Inactive users
        int inactiveUsers = totalUsers - activeUsers;
        
        // By role
        rs = stmt.executeQuery("SELECT role, COUNT(*) FROM users GROUP BY role");
        Map<String, Integer> roleCounts = new HashMap<>();
        while (rs.next()) {
            roleCounts.put(rs.getString(1), rs.getInt(2));
        }
        
        report.append("Total Users: ").append(totalUsers).append("\n");
        report.append("Active Users: ").append(activeUsers).append("\n");
        report.append("Inactive Users: ").append(inactiveUsers).append("\n\n");
        report.append("Users by Role:\n");
        for (Map.Entry<String, Integer> entry : roleCounts.entrySet()) {
            report.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        
        return report.toString();
    }

    private String generateLibrarianActivityReport(Connection conn) throws SQLException {
        StringBuilder report = new StringBuilder();
        
        // Librarian counts
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE role='Librarian'");
        rs.next();
        int totalLibrarians = rs.getInt(1);
        
        rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE role='Librarian' AND active=TRUE");
        rs.next();
        int activeLibrarians = rs.getInt(1);
        
        report.append("Total Librarians: ").append(totalLibrarians).append("\n");
        report.append("Active Librarians: ").append(activeLibrarians).append("\n");
        report.append("Inactive Librarians: ").append(totalLibrarians - activeLibrarians).append("\n\n");
        
        // List of librarians
        rs = stmt.executeQuery("SELECT name, username, active FROM users WHERE role='Librarian'");
        report.append("Librarian Details:\n");
        while (rs.next()) {
            report.append("- ").append(rs.getString("name")).append(" (")
                  .append(rs.getString("username")).append("): ")
                  .append(rs.getBoolean("active") ? "Active" : "Inactive").append("\n");
        }
        
        return report.toString();
    }

    private String generateStatusDistributionReport(Connection conn) throws SQLException {
        StringBuilder report = new StringBuilder();
        
        // Status distribution
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT active, COUNT(*) FROM users GROUP BY active");
        
        report.append("User Status Distribution:\n");
        while (rs.next()) {
            report.append("- ").append(rs.getBoolean(1) ? "Active" : "Inactive")
                  .append(": ").append(rs.getInt(2)).append(" users\n");
        }
        
        // Status by role
        rs = stmt.executeQuery("SELECT role, active, COUNT(*) FROM users GROUP BY role, active");
        report.append("\nStatus by Role:\n");
        while (rs.next()) {
            report.append("- ").append(rs.getString("role")).append(" (")
                  .append(rs.getBoolean("active") ? "Active" : "Inactive")
                  .append("): ").append(rs.getInt(3)).append(" users\n");
        }
        
        return report.toString();
    }

    private void loadLibrarians() {
        tableModel.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT id, name, username, active FROM users WHERE role='Librarian'";
            ResultSet rs = conn.createStatement().executeQuery(query);
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("username"),
                    rs.getBoolean("active") ? "Active" : "Inactive"
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading librarians: " + ex.getMessage());
        }
    }

    private void addLibrarian() {
        JTextField nameField = new JTextField();
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JPanel panel = new JPanel(new GridLayout(3, 2));
        panel.add(new JLabel("Name:")); panel.add(nameField);
        panel.add(new JLabel("Username:")); panel.add(usernameField);
        panel.add(new JLabel("Password:")); panel.add(passwordField);

        // Apply theme to dialog components
        applyTheme(panel);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Librarian", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try (Connection conn = DBConnection.getConnection()) {
                String checkSql = "SELECT * FROM users WHERE username=?";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setString(1, usernameField.getText());
                ResultSet rs = checkStmt.executeQuery();
                
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "Username already exists!");
                    return;
                }

                String sql = "INSERT INTO users (name, username, password, role, active) VALUES (?, ?, ?, 'Librarian', TRUE)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, nameField.getText());
                stmt.setString(2, usernameField.getText());
                stmt.setString(3, String.valueOf(passwordField.getPassword()));
                stmt.executeUpdate();
                loadLibrarians();
                JOptionPane.showMessageDialog(this, "Librarian added successfully!");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error adding librarian: " + ex.getMessage());
            }
        }
    }

    private void deleteLibrarian() {
        int row = librarianTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a librarian to delete.");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete this librarian?", 
            "Confirm Delete", JOptionPane.YES_NO_OPTION);
        
        if (confirm != JOptionPane.YES_OPTION) return;
        
        int id = (int) tableModel.getValueAt(row, 0);
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "DELETE FROM users WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            stmt.executeUpdate();
            loadLibrarians();
            JOptionPane.showMessageDialog(this, "Librarian deleted successfully!");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error deleting librarian: " + ex.getMessage());
        }
    }

    private void toggleStatus() {
        int row = librarianTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a librarian to activate/deactivate.");
            return;
        }
        
        int id = (int) tableModel.getValueAt(row, 0);
        String status = (String) tableModel.getValueAt(row, 3);
        boolean newStatus = !status.equals("Active");

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE users SET active=? WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setBoolean(1, newStatus);
            stmt.setInt(2, id);
            stmt.executeUpdate();
            loadLibrarians();
            JOptionPane.showMessageDialog(this, "Librarian status updated!");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating status: " + ex.getMessage());
        }
    }
}