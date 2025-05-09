// User.java
import javax.swing.table.DefaultTableModel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.prefs.Preferences;

public abstract class User {
    protected int id;
    protected String name;
    protected String username;
    protected String password;
    protected boolean darkMode;
    protected static final Preferences prefs = Preferences.userNodeForPackage(User.class);

    // Define color schemes for consistent theming
    protected static final Color LIGHT_BG = new Color(240, 240, 240);
    protected static final Color LIGHT_FG = Color.BLACK;
    protected static final Color DARK_BG = new Color(50, 50, 50);
    protected static final Color DARK_FG = Color.WHITE;
    protected static final Color DARK_COMPONENT_BG = new Color(70, 70, 70);
    protected static final Color DARK_BORDER = new Color(100, 100, 100);

    public User(int id, String name, String username, String password) {
        this.id = id;
        this.name = name;
        this.username = username;
        this.password = password;
        
        // Load dark mode preference
        this.darkMode = prefs.getBoolean("darkMode", false);
    }

    // Keep the original abstract method for backward compatibility
    public abstract void openDashboard();
    
    // Add overloaded method for dark mode support
    public void openDashboard(boolean darkMode) {
        this.darkMode = darkMode;
        // Save preference
        prefs.putBoolean("darkMode", darkMode);
        
        // Call the original method for backward compatibility
        openDashboard();
    }
    
    // Getter for dark mode
    public boolean isDarkMode() {
        return darkMode;
    }
    
    // Setter for dark mode
    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
        // Save preference
        prefs.putBoolean("darkMode", darkMode);
    }
    
    // Utility method to apply theme to any container
    protected void applyTheme(Container container) {
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
            
            // For other containers, recursively apply
            if (comp instanceof Container) {
                applyTheme((Container)comp);
            }
        }
    }
    
    // Create a dark mode toggle button for any panel
    protected JToggleButton createDarkModeToggle(Container container) {
        JToggleButton darkModeToggle = new JToggleButton("Dark Mode");
        darkModeToggle.setSelected(darkMode);
        darkModeToggle.addActionListener(e -> {
            darkMode = darkModeToggle.isSelected();
            setDarkMode(darkMode);
            applyTheme(container);
            // Refresh UI
            SwingUtilities.updateComponentTreeUI(container);
        });
        return darkModeToggle;
    }
}