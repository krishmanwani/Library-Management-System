import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;

// LibrarianDashboard.java - With Dark Mode Support
public class LibrarianDashboard extends JFrame {
    private JTable bookTable;
    private DefaultTableModel tableModel;
    private boolean darkMode;
    private JToggleButton darkModeToggle;
    
    // Colors from User class for consistent theming
    private static final Color LIGHT_BG = new Color(240, 240, 240);
    private static final Color LIGHT_FG = Color.BLACK;
    private static final Color DARK_BG = new Color(50, 50, 50);
    private static final Color DARK_FG = Color.WHITE;
    private static final Color DARK_COMPONENT_BG = new Color(70, 70, 70);
    private static final Color DARK_BORDER = new Color(100, 100, 100);

    public LibrarianDashboard() {
        this(false); // Default to light mode
    }
    
    public LibrarianDashboard(boolean darkMode) {
        this.darkMode = darkMode;
        
        setTitle("Librarian Dashboard - Manage Books");
        setSize(800, 500);
        setLayout(new BorderLayout());

        String[] columns = {"ID", "Title", "Author", "Available"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        bookTable = new JTable(tableModel);
        loadBooks();

        JPanel btnPanel = new JPanel(new FlowLayout());
        JButton addBtn = new JButton("Add Book");
        JButton deleteBtn = new JButton("Delete Book");
        JButton issueBtn = new JButton("Issue Book");
        JButton returnBtn = new JButton("Return Book");
        JButton logoutBtn = new JButton("Logout");
        
        // Add dark mode toggle
        darkModeToggle = new JToggleButton("Dark Mode");
        darkModeToggle.setSelected(darkMode);
        darkModeToggle.addActionListener(e -> {
            this.darkMode = darkModeToggle.isSelected();
            applyTheme(getContentPane());
            // Refresh UI
            SwingUtilities.updateComponentTreeUI(this);
        });
        
        btnPanel.add(addBtn);
        btnPanel.add(deleteBtn);
        btnPanel.add(issueBtn);
        btnPanel.add(returnBtn);
        btnPanel.add(darkModeToggle);
        btnPanel.add(logoutBtn);

        add(new JScrollPane(bookTable), BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        addBtn.addActionListener(e -> addBook());
        deleteBtn.addActionListener(e -> deleteBook());
        issueBtn.addActionListener(e -> issueBook());
        returnBtn.addActionListener(e -> returnBook());
        logoutBtn.addActionListener(e -> {
            dispose();
            new LoginScreen();
        });
        
        // Apply theme initially
        applyTheme(getContentPane());

        setLocationRelativeTo(null);
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
            
            // For other containers, recursively apply
            if (comp instanceof Container) {
                applyTheme((Container)comp);
            }
        }
    }

    private void loadBooks() {
        tableModel.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT * FROM books ORDER BY title";
            ResultSet rs = conn.createStatement().executeQuery(query);
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getBoolean("isAvailable") ? "Yes" : "No"
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading books: " + ex.getMessage());
        }
    }

    private void addBook() {
        JTextField titleField = new JTextField();
        JTextField authorField = new JTextField();
        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(new JLabel("Title:")); panel.add(titleField);
        panel.add(new JLabel("Author:")); panel.add(authorField);
        
        // Apply theme to dialog components
        applyTheme(panel);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Book", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            if (titleField.getText().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Title is required!");
                return;
            }
            
            try (Connection conn = DBConnection.getConnection()) {
                String sql = "INSERT INTO books (title, author, isAvailable) VALUES (?, ?, TRUE)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, titleField.getText());
                stmt.setString(2, authorField.getText());
                stmt.executeUpdate();
                loadBooks();
                JOptionPane.showMessageDialog(this, "Book added successfully!");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error adding book: " + ex.getMessage());
            }
        }
    }

    private void deleteBook() {
        int row = bookTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to delete.");
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete this book?", 
            "Confirm Delete", JOptionPane.YES_NO_OPTION);
        
        if (confirm != JOptionPane.YES_OPTION) return;
        
        int id = (int) tableModel.getValueAt(row, 0);
        try (Connection conn = DBConnection.getConnection()) {
            String checkSql = "SELECT * FROM borrowed_books WHERE book_id=? AND return_date IS NULL";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "Cannot delete book that is currently borrowed!");
                return;
            }
            
            String sql = "DELETE FROM books WHERE id=?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, id);
            stmt.executeUpdate();
            loadBooks();
            JOptionPane.showMessageDialog(this, "Book deleted successfully!");
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error deleting book: " + ex.getMessage());
        }
    }

    private void issueBook() {
        JTextField studentIdField = new JTextField();
        JTextField bookIdField = new JTextField();
        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(new JLabel("Student ID:")); panel.add(studentIdField);
        panel.add(new JLabel("Book ID:")); panel.add(bookIdField);
        
        // Apply theme to dialog components
        applyTheme(panel);

        int result = JOptionPane.showConfirmDialog(this, panel, "Issue Book", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                int studentId = Integer.parseInt(studentIdField.getText());
                int bookId = Integer.parseInt(bookIdField.getText());
                
                try (Connection conn = DBConnection.getConnection()) {
                    String studentCheck = "SELECT * FROM users WHERE id=? AND role='Student' AND active=1";
                    PreparedStatement studentStmt = conn.prepareStatement(studentCheck);
                    studentStmt.setInt(1, studentId);
                    ResultSet studentRs = studentStmt.executeQuery();
                    
                    if (!studentRs.next()) {
                        JOptionPane.showMessageDialog(this, "Invalid student ID or student not active!");
                        return;
                    }
                    
                    String bookCheck = "SELECT * FROM books WHERE id=? AND isAvailable=TRUE";
                    PreparedStatement bookStmt = conn.prepareStatement(bookCheck);
                    bookStmt.setInt(1, bookId);
                    ResultSet bookRs = bookStmt.executeQuery();
                    
                    if (!bookRs.next()) {
                        JOptionPane.showMessageDialog(this, "Invalid book ID or book not available!");
                        return;
                    }
                    
                    LocalDate today = LocalDate.now();
                    LocalDate dueDate = today.plusDays(14);

                    PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO borrowed_books (student_id, book_id, borrow_date, due_date) VALUES (?, ?, ?, ?)"
                    );
                    stmt.setInt(1, studentId);
                    stmt.setInt(2, bookId);
                    stmt.setDate(3, Date.valueOf(today));
                    stmt.setDate(4, Date.valueOf(dueDate));
                    stmt.executeUpdate();

                    PreparedStatement updateBook = conn.prepareStatement("UPDATE books SET isAvailable=FALSE WHERE id=?");
                    updateBook.setInt(1, bookId);
                    updateBook.executeUpdate();

                    loadBooks();
                    JOptionPane.showMessageDialog(this, "Book issued successfully! Due date: " + dueDate);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid IDs (numbers only)");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error issuing book: " + ex.getMessage());
            }
        }
    }

    private void returnBook() {
        JTextField studentIdField = new JTextField();
        JTextField bookIdField = new JTextField();
        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(new JLabel("Student ID:")); panel.add(studentIdField);
        panel.add(new JLabel("Book ID:")); panel.add(bookIdField);
        
        // Apply theme to dialog components
        applyTheme(panel);

        int result = JOptionPane.showConfirmDialog(this, panel, "Return Book", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                int studentId = Integer.parseInt(studentIdField.getText());
                int bookId = Integer.parseInt(bookIdField.getText());
                
                try (Connection conn = DBConnection.getConnection()) {
                    LocalDate today = LocalDate.now();

                    PreparedStatement getRecord = conn.prepareStatement(
                        "SELECT due_date FROM borrowed_books WHERE student_id=? AND book_id=? AND return_date IS NULL"
                    );
                    getRecord.setInt(1, studentId);
                    getRecord.setInt(2, bookId);
                    ResultSet rs = getRecord.executeQuery();

                    if (rs.next()) {
                        Date due = rs.getDate("due_date");
                        long daysLate = (today.toEpochDay() - due.toLocalDate().toEpochDay());
                        double fine = daysLate > 0 ? daysLate * 5 : 0;

                        PreparedStatement returnStmt = conn.prepareStatement(
                            "UPDATE borrowed_books SET return_date=?, fine=? WHERE student_id=? AND book_id=? AND return_date IS NULL"
                        );
                        returnStmt.setDate(1, Date.valueOf(today));
                        returnStmt.setDouble(2, fine);
                        returnStmt.setInt(3, studentId);
                        returnStmt.setInt(4, bookId);
                        returnStmt.executeUpdate();

                        PreparedStatement updateBook = conn.prepareStatement("UPDATE books SET isAvailable=TRUE WHERE id=?");
                        updateBook.setInt(1, bookId);
                        updateBook.executeUpdate();

                        loadBooks();
                        if (fine > 0) {
                            JOptionPane.showMessageDialog(this, 
                                String.format("Book returned successfully! Fine: Rs. %.2f", fine));
                        } else {
                            JOptionPane.showMessageDialog(this, "Book returned successfully with no fine.");
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "No active borrowing record found for this student and book.");
                    }
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid IDs (numbers only)");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error returning book: " + ex.getMessage());
            }
        }
    }
}