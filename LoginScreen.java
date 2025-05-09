// LoginScreen.java
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.prefs.Preferences;

public class LoginScreen extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleBox;
    private boolean isDarkMode = false;
    private JToggleButton darkModeToggle;
    private static final Preferences prefs = Preferences.userNodeForPackage(LoginScreen.class);

    // Define color schemes
    private final Color LIGHT_BG = new Color(240, 240, 240);
    private final Color LIGHT_FG = Color.BLACK;
    private final Color DARK_BG = new Color(50, 50, 50);
    private final Color DARK_FG = Color.WHITE;
    private final Color DARK_COMPONENT_BG = new Color(70, 70, 70);
    private final Color DARK_BORDER = new Color(100, 100, 100);

    public LoginScreen() {
        setTitle("Library Management System");
        setSize(450, 450); // Slightly taller to accommodate dark mode toggle
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Load dark mode preference
        isDarkMode = prefs.getBoolean("darkMode", false);

        // Title
        JLabel titleLabel = new JLabel("Library Management System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(titleLabel, gbc);

        // Username
        gbc.gridy++;
        gbc.gridwidth = 1;
        add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(15);
        add(usernameField, gbc);

        // Password
        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        add(passwordField, gbc);

        // Role
        gbc.gridy++;
        gbc.gridx = 0;
        add(new JLabel("Select Role:"), gbc);
        gbc.gridx = 1;
        roleBox = new JComboBox<>(new String[]{"Admin", "Librarian", "Student"});
        add(roleBox, gbc);

        // Dark Mode Toggle
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.CENTER;
        JPanel darkModePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        darkModeToggle = new JToggleButton("Dark Mode");
        darkModeToggle.setSelected(isDarkMode);
        darkModeToggle.addActionListener(e -> toggleDarkMode());
        darkModePanel.add(darkModeToggle);
        add(darkModePanel, gbc);

        // Buttons
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton loginButton = new JButton("Login");
        loginButton.setPreferredSize(new Dimension(100, 30));
        loginButton.addActionListener(e -> login());
        buttonPanel.add(loginButton);

        JButton registerButton = new JButton("Register");
        registerButton.setPreferredSize(new Dimension(100, 30));
        registerButton.addActionListener(e -> register());
        buttonPanel.add(registerButton);
        add(buttonPanel, gbc);

        setLocationRelativeTo(null);
        
        // Apply theme based on saved preference
        applyTheme();
        
        setVisible(true);
    }

    private void toggleDarkMode() {
        isDarkMode = darkModeToggle.isSelected();
        // Save preference
        prefs.putBoolean("darkMode", isDarkMode);
        applyTheme();
    }

    private void applyTheme() {
        // Get all components in the frame
        applyThemeToContainer(this.getContentPane());
        
        // Set frame background
        this.getContentPane().setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        
        // Refresh UI
        SwingUtilities.updateComponentTreeUI(this);
    }
    
    private void applyThemeToContainer(Container container) {
        // Set background and foreground for this container
        container.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        container.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
        
        // Apply to all components in the container
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel) {
                comp.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
            } 
            else if (comp instanceof JTextField || comp instanceof JPasswordField || comp instanceof JComboBox) {
                comp.setBackground(isDarkMode ? DARK_COMPONENT_BG : Color.WHITE);
                comp.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
                if (comp instanceof JTextField || comp instanceof JPasswordField) {
                    ((JTextField)comp).setCaretColor(isDarkMode ? DARK_FG : LIGHT_FG);
                }
            }
            else if (comp instanceof JButton || comp instanceof JToggleButton) {
                comp.setBackground(isDarkMode ? DARK_COMPONENT_BG : null);
                comp.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
            }
            else if (comp instanceof JPanel) {
                comp.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
                applyThemeToContainer((Container)comp);
            }
            
            // For other containers, recursively apply
            if (comp instanceof Container) {
                applyThemeToContainer((Container)comp);
            }
        }
    }
    
    private void login() {
        String username = usernameField.getText();
        String password = String.valueOf(passwordField.getPassword());
        String role = (String) roleBox.getSelectedItem();

        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT * FROM users WHERE username=? AND password=? AND role=? AND active=1";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, role);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                User user;
                switch (role) {
                    case "Admin": user = new Admin(id, name, username, password); break;
                    case "Librarian": user = new Librarian(id, name, username, password); break;
                    default: user = new Student(id, name, username, password); break;
                }
                
                // Set dark mode preference in the User class
                prefs.putBoolean("darkMode", isDarkMode);
                
                dispose();
                
                // Call the original openDashboard() method to maintain backward compatibility
                user.openDashboard();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials or user not active.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage());
        }
    }

    private void register() {
        JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5));
        
        // Apply the current theme to registration panel
        panel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        panel.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
        
        JTextField nameField = new JTextField();
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JComboBox<String> roleBox = new JComboBox<>(new String[]{"Admin", "Librarian", "Student"});
        
        // Apply theme to components
        if (isDarkMode) {
            nameField.setBackground(DARK_COMPONENT_BG);
            nameField.setForeground(DARK_FG);
            nameField.setCaretColor(DARK_FG);
            
            usernameField.setBackground(DARK_COMPONENT_BG);
            usernameField.setForeground(DARK_FG);
            usernameField.setCaretColor(DARK_FG);
            
            passwordField.setBackground(DARK_COMPONENT_BG);
            passwordField.setForeground(DARK_FG);
            passwordField.setCaretColor(DARK_FG);
            
            roleBox.setBackground(DARK_COMPONENT_BG);
            roleBox.setForeground(DARK_FG);
        }

        JLabel nameLabel = new JLabel("Full Name:");
        JLabel usernameLabel = new JLabel("Username:");
        JLabel passwordLabel = new JLabel("Password:");
        JLabel roleLabel = new JLabel("Role:");
        
        // Apply theme to labels
        if (isDarkMode) {
            nameLabel.setForeground(DARK_FG);
            usernameLabel.setForeground(DARK_FG);
            passwordLabel.setForeground(DARK_FG);
            roleLabel.setForeground(DARK_FG);
        }
        
        panel.add(nameLabel);
        panel.add(nameField);
        panel.add(usernameLabel);
        panel.add(usernameField);
        panel.add(passwordLabel);
        panel.add(passwordField);
        panel.add(roleLabel);
        panel.add(roleBox);

        // Create option pane with dark mode
        JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, 
                JOptionPane.OK_CANCEL_OPTION);
        
        // Theme the option pane
        if (isDarkMode) {
            UIManager.put("OptionPane.background", DARK_BG);
            UIManager.put("Panel.background", DARK_BG);
            UIManager.put("OptionPane.messageForeground", DARK_FG);
        } else {
            UIManager.put("OptionPane.background", LIGHT_BG);
            UIManager.put("Panel.background", LIGHT_BG);
            UIManager.put("OptionPane.messageForeground", LIGHT_FG);
        }

        JDialog dialog = optionPane.createDialog(this, "Register New User");
        dialog.setVisible(true);
        
        Object selectedValue = optionPane.getValue();
        if (selectedValue != null && selectedValue.equals(JOptionPane.OK_OPTION)) {
            String name = nameField.getText();
            String username = usernameField.getText();
            String password = String.valueOf(passwordField.getPassword());
            String role = (String) roleBox.getSelectedItem();

            if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required!");
                return;
            }

            try (Connection conn = DBConnection.getConnection()) {
                String checkQuery = "SELECT * FROM users WHERE username=?";
                PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
                checkStmt.setString(1, username);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "Username already exists!");
                    return;
                }

                String insertQuery = "INSERT INTO users (name, username, password, role, active) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
                insertStmt.setString(1, name);
                insertStmt.setString(2, username);
                insertStmt.setString(3, password);
                insertStmt.setString(4, role);
                insertStmt.setBoolean(5, true);
                insertStmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Registration successful! You can now login.");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Registration failed: " + ex.getMessage());
            }
        }
    }
}