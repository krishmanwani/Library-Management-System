import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.*;
import java.util.Collections;
import java.util.stream.Collectors;

public class StudentDashboard extends JFrame {
    private JTable borrowTable;
    private DefaultTableModel tableModel;
    private int studentId;
    private JButton issueBookBtn, returnBookBtn, wishlistBtn, renewBtn;
    private JTabbedPane tabbedPane;
    private JComboBox<String> filterCombo;
    private JTextField searchField;
    private List<Integer> wishlist = new ArrayList<>();
    private boolean isDarkMode = false;
    private JToggleButton darkModeToggle;
    private static final Preferences prefs = Preferences.userNodeForPackage(StudentDashboard.class);

    private JTable availableBooksTable;
    private DefaultTableModel availableBooksModel;
    private JTable wishlistTable;
    private DefaultTableModel wishlistModel;

    private final Color LIGHT_BG = new Color(240, 240, 240);
    private final Color LIGHT_FG = Color.BLACK;
    private final Color DARK_BG = new Color(50, 50, 50);
    private final Color DARK_FG = Color.WHITE;
    private final Color DARK_COMPONENT_BG = new Color(70, 70, 70);
    private final Color DARK_BORDER = new Color(100, 100, 100);
    private final Color DARK_TABLE_BG = new Color(60, 60, 60);
    private final Color DARK_DIALOG_BG = new Color(80, 80, 80);

    public StudentDashboard(int studentId) {
        this.studentId = studentId;
        setTitle("Student Dashboard - Library System");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Load dark mode preference
        isDarkMode = prefs.getBoolean("darkMode", false);

        // Initialize components first
        initializeComponents();

        // Then load data
        loadWishlist();
        refreshAllTabs();

        // Apply theme
        applyTheme();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void initializeComponents() {
        // Title Panel
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        titlePanel.add(new JLabel("Welcome, Student!"));
        add(titlePanel, BorderLayout.NORTH);

        // Main Content Panel
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Tabbed Pane for different functions
        tabbedPane = new JTabbedPane();

        // Tab 1: Borrowed Books
        JPanel borrowedBooksPanel = createBorrowedBooksPanel();
        tabbedPane.addTab("My Borrowed Books", borrowedBooksPanel);

        // Tab 2: Issue New Books
        JPanel issueBookPanel = createIssueBookPanel();
        tabbedPane.addTab("Issue New Books", issueBookPanel);

        // Tab 3: Wishlist
        JPanel wishlistPanel = createWishlistPanel();
        tabbedPane.addTab("My Wishlist", wishlistPanel);

        contentPanel.add(tabbedPane, BorderLayout.CENTER);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshAllTabs());
        
        JButton notifyBtn = new JButton("View Notifications");
        notifyBtn.addActionListener(e -> showNotifications());
        
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.addActionListener(e -> {
            dispose();
            new LoginScreen();
        });

        buttonPanel.add(refreshBtn);
        buttonPanel.add(notifyBtn);
        buttonPanel.add(logoutBtn);

        // Dark Mode Toggle
        darkModeToggle = new JToggleButton("Dark Mode");
        darkModeToggle.setSelected(isDarkMode);
        darkModeToggle.addActionListener(e -> toggleDarkMode());
        buttonPanel.add(darkModeToggle);

        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(contentPanel, BorderLayout.CENTER);
    }

    private void toggleDarkMode() {
        isDarkMode = darkModeToggle.isSelected();
        prefs.putBoolean("darkMode", isDarkMode);
        applyTheme();
    }

    private void applyTheme() {
        applyThemeToContainer(this.getContentPane());
        SwingUtilities.updateComponentTreeUI(this);
    }

    private void applyThemeToContainer(Container container) {
        container.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        container.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);

        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel) {
                comp.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
            } else if (comp instanceof JButton || comp instanceof JToggleButton) {
                comp.setBackground(isDarkMode ? DARK_COMPONENT_BG : null);
                comp.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
            } else if (comp instanceof JPanel) {
                comp.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
                applyThemeToContainer((Container) comp);
            }

            if (comp instanceof Container) {
                applyThemeToContainer((Container) comp);
            }
        }
    }

    private JPanel createBorrowedBooksPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);

        // Search and filter panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);

        searchField = new JTextField(20);
        searchField.setBackground(isDarkMode ? DARK_COMPONENT_BG : Color.WHITE);
        searchField.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
        searchField.setCaretColor(isDarkMode ? DARK_FG : LIGHT_FG);
        searchField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                filterBorrowedBooks();
            }
        });

        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);

        filterCombo = new JComboBox<>(new String[]{"All", "Borrowed", "Returned", "Overdue"});
        filterCombo.setBackground(isDarkMode ? DARK_COMPONENT_BG : Color.WHITE);
        filterCombo.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
        filterCombo.addActionListener(e -> filterBorrowedBooks());
        searchPanel.add(new JLabel("Filter:"));
        searchPanel.add(filterCombo);

        panel.add(searchPanel, BorderLayout.NORTH);

        // Table for borrowed books
        String[] columns = {"Book ID", "Title", "Author", "Borrow Date", "Due Date", "Status", "Fine (Rs.)"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        borrowTable = new JTable(tableModel);
        borrowTable.setRowHeight(25);
        borrowTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        borrowTable.setBackground(isDarkMode ? DARK_COMPONENT_BG : Color.WHITE);
        borrowTable.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
        JScrollPane scrollPane = new JScrollPane(borrowTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Action buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        actionPanel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);

        returnBookBtn = new JButton("Return Selected Book");
        returnBookBtn.setBackground(isDarkMode ? DARK_COMPONENT_BG : null);
        returnBookBtn.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
        returnBookBtn.addActionListener(e -> returnSelectedBook());

        renewBtn = new JButton("Renew Selected Book");
        renewBtn.setBackground(isDarkMode ? DARK_COMPONENT_BG : null);
        renewBtn.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
        renewBtn.addActionListener(e -> renewSelectedBook());

        actionPanel.add(returnBookBtn);
        actionPanel.add(renewBtn);
        panel.add(actionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createIssueBookPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);

        JTextField bookSearchField = new JTextField(20);
        bookSearchField.setBackground(isDarkMode ? DARK_COMPONENT_BG : Color.WHITE);
        bookSearchField.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
        bookSearchField.setCaretColor(isDarkMode ? DARK_FG : LIGHT_FG);

        JButton searchBtn = new JButton("Search");
        searchBtn.setBackground(isDarkMode ? DARK_COMPONENT_BG : null);
        searchBtn.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
        searchPanel.add(new JLabel("Search Books:"));
        searchPanel.add(bookSearchField);
        searchPanel.add(searchBtn);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(searchPanel, gbc);

        // Table for available books
        availableBooksModel = new DefaultTableModel(new String[]{"ID", "Title", "Author", "Genre"}, 0);
        availableBooksTable = new JTable(availableBooksModel);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(availableBooksModel);
        availableBooksTable.setRowSorter(sorter);

        JScrollPane availableScrollPane = new JScrollPane(availableBooksTable);
        availableBooksTable.setBackground(isDarkMode ? DARK_COMPONENT_BG : Color.WHITE);
        availableBooksTable.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);

        searchBtn.addActionListener(e -> {
            String query = bookSearchField.getText().trim();
            if (query.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + query));
            }
        });

        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        panel.add(availableScrollPane, gbc);

        // Action buttons
        issueBookBtn = new JButton("Issue Selected Book");
        issueBookBtn.setBackground(isDarkMode ? DARK_COMPONENT_BG : null);
        issueBookBtn.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
        issueBookBtn.addActionListener(e -> issueBook(availableBooksTable));

        wishlistBtn = new JButton("Add to Wishlist");
        wishlistBtn.setBackground(isDarkMode ? DARK_COMPONENT_BG : null);
        wishlistBtn.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
        wishlistBtn.addActionListener(e -> addToWishlist(availableBooksTable));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        btnPanel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);
        btnPanel.add(issueBookBtn);
        btnPanel.add(wishlistBtn);

        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.weighty = 0;
        panel.add(btnPanel, gbc);

        loadAvailableBooks(availableBooksModel);

        return panel;
    }

    private JPanel createWishlistPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);

        wishlistModel = new DefaultTableModel(new String[]{"ID", "Title", "Author", "Status"}, 0);
        wishlistTable = new JTable(wishlistModel);
        wishlistTable.setBackground(isDarkMode ? DARK_COMPONENT_BG : Color.WHITE);
        wishlistTable.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
        JScrollPane scrollPane = new JScrollPane(wishlistTable);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Action buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        btnPanel.setBackground(isDarkMode ? DARK_BG : LIGHT_BG);

        JButton removeWishlistBtn = new JButton("Remove from Wishlist");
        removeWishlistBtn.setBackground(isDarkMode ? DARK_COMPONENT_BG : null);
        removeWishlistBtn.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
        removeWishlistBtn.addActionListener(e -> removeFromWishlist(wishlistTable, wishlistModel));

        JButton issueWishlistBtn = new JButton("Issue Selected Book");
        issueWishlistBtn.setBackground(isDarkMode ? DARK_COMPONENT_BG : null);
        issueWishlistBtn.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
        issueWishlistBtn.addActionListener(e -> issueFromWishlist(wishlistTable));

        btnPanel.add(removeWishlistBtn);
        btnPanel.add(issueWishlistBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        loadWishlistBooks(wishlistModel);

        return panel;
    }

    private void loadWishlist() {
		wishlist.clear();
		try (Connection conn = DBConnection.getConnection()) {
			String query = "SELECT wishlist FROM users WHERE id=?";
			PreparedStatement stmt = conn.prepareStatement(query);
			stmt.setInt(1, studentId);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				String wishlistStr = rs.getString("wishlist");
				if (wishlistStr != null && !wishlistStr.isEmpty()) {
					String[] ids = wishlistStr.split(",");
					for (String id : ids) {
						if (!id.isEmpty()) {
							wishlist.add(Integer.parseInt(id.trim()));
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, "Error loading wishlist: " + ex.getMessage());
		}
		
		// Refresh wishlist display if the table exists
		if (wishlistTable != null && wishlistModel != null) {
			loadWishlistBooks(wishlistModel);
		}
	} 

    private void loadBorrowedBooks() {
        tableModel.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT b.id, b.title, b.author, bb.borrow_date, bb.due_date, " +
                         "CASE WHEN bb.return_date IS NULL THEN 'Borrowed' ELSE 'Returned' END AS status, " +
                         "IFNULL(bb.fine, 0) as fine " +
                         "FROM books b JOIN borrowed_books bb ON b.id = bb.book_id " +
                         "WHERE bb.student_id = ? ORDER BY bb.borrow_date DESC";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, studentId);
            ResultSet rs = stmt.executeQuery();
    
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getDate("borrow_date"),
                    rs.getDate("due_date"),
                    rs.getString("status"),
                    rs.getDouble("fine")
                });
            }
            
            // Reapply dark mode for table rows after loading data (if theme is switched dynamically)
            borrowTable.setBackground(isDarkMode ? DARK_COMPONENT_BG : Color.WHITE);
            borrowTable.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading borrowed books: " + ex.getMessage());
        }
    }    
	
    private void loadAvailableBooks(DefaultTableModel model) {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String query = "SELECT id, title, author, genre FROM books WHERE isAvailable=TRUE";
            ResultSet rs = conn.createStatement().executeQuery(query);
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("author"),
                    rs.getString("genre")
                });
            }
    
            // Reapply dark mode for table rows after loading data (if theme is switched dynamically)
            availableBooksTable.setBackground(isDarkMode ? DARK_COMPONENT_BG : Color.WHITE);
            availableBooksTable.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading available books: " + ex.getMessage());
        }
    }
    
    private void loadWishlistBooks(DefaultTableModel model) {
		model.setRowCount(0); // Clear existing data
		if (wishlist.isEmpty()) return;

		try (Connection conn = DBConnection.getConnection()) {
			// Create comma-separated placeholders for the SQL IN clause
			String placeholders = String.join(",", java.util.Collections.nCopies(wishlist.size(), "?"));
			
			String query = "SELECT id, title, author, isAvailable FROM books WHERE id IN (" + placeholders + ")";
			PreparedStatement stmt = conn.prepareStatement(query);
			
			// Set each book ID parameter
			for (int i = 0; i < wishlist.size(); i++) {
				stmt.setInt(i + 1, wishlist.get(i));
			}
			
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				model.addRow(new Object[]{
					rs.getInt("id"),
					rs.getString("title"),
					rs.getString("author"),
					rs.getBoolean("isAvailable") ? "Available" : "Borrowed"
				});
			}
			
			// Apply theme to the table
			if (isDarkMode) {
				wishlistTable.setBackground(DARK_TABLE_BG);
				wishlistTable.setForeground(DARK_FG);
			} else {
				wishlistTable.setBackground(Color.WHITE);
				wishlistTable.setForeground(Color.BLACK);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, "Error loading wishlist books: " + ex.getMessage());
		}
	}
	
    private void issueBook(JTable availableBooksTable) {
        int selectedRow = availableBooksTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to issue.");
            return;
        }
    
        int bookId = (int) availableBooksTable.getValueAt(selectedRow, 0);
    
        try (Connection conn = DBConnection.getConnection()) {
            LocalDate today = LocalDate.now();
            LocalDate dueDate = today.plusDays(14);
    
            // Insert the borrowed book record
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO borrowed_books (student_id, book_id, borrow_date, due_date) VALUES (?, ?, ?, ?)"
            );
            stmt.setInt(1, studentId);
            stmt.setInt(2, bookId);
            stmt.setDate(3, Date.valueOf(today));
            stmt.setDate(4, Date.valueOf(dueDate));
            stmt.executeUpdate();
    
            // Update the book status to not available
            PreparedStatement updateBook = conn.prepareStatement("UPDATE books SET isAvailable=FALSE WHERE id=?");
            updateBook.setInt(1, bookId);
            updateBook.executeUpdate();
    
            // Remove from wishlist if it was there
            if (wishlist.contains(bookId)) {
                wishlist.remove(Integer.valueOf(bookId));
                updateWishlistInDB();
            }
    
            // Refresh the UI to reflect changes
            refreshAllTabs();
            JOptionPane.showMessageDialog(this, "Book issued successfully! Due date: " + dueDate);
    
            // Apply dark mode styling to the components after issuing
            availableBooksTable.setBackground(isDarkMode ? DARK_COMPONENT_BG : Color.WHITE);
            availableBooksTable.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
            refreshAllTabs();
    
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error issuing book: " + ex.getMessage());
        }
    }    

    private void returnSelectedBook() {
        int selectedRow = borrowTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to return.");
            return;
        }
    
        int bookId = (int) tableModel.getValueAt(selectedRow, 0);
        String status = (String) tableModel.getValueAt(selectedRow, 5);
    
        if ("Returned".equals(status)) {
            JOptionPane.showMessageDialog(this, "This book is already returned.");
            return;
        }
    
        try (Connection conn = DBConnection.getConnection()) {
            LocalDate today = LocalDate.now();
            Date dueDate = (Date) tableModel.getValueAt(selectedRow, 4);
            double fine = 0;
    
            // Calculate fine if overdue
            if (today.isAfter(dueDate.toLocalDate())) {
                long daysLate = today.toEpochDay() - dueDate.toLocalDate().toEpochDay();
                fine = daysLate * 5; // Rs. 5 per day
            }
    
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE borrowed_books SET return_date=?, fine=? WHERE student_id=? AND book_id=? AND return_date IS NULL"
            );
            stmt.setDate(1, Date.valueOf(today));
            stmt.setDouble(2, fine);
            stmt.setInt(3, studentId);
            stmt.setInt(4, bookId);
            stmt.executeUpdate();
    
            PreparedStatement updateBook = conn.prepareStatement("UPDATE books SET isAvailable=TRUE WHERE id=?");
            updateBook.setInt(1, bookId);
            updateBook.executeUpdate();
    
            refreshAllTabs();
    
            if (fine > 0) {
                JOptionPane.showMessageDialog(this, 
                    String.format("Book returned successfully! Fine: Rs. %.2f", fine));
            } else {
                JOptionPane.showMessageDialog(this, "Book returned successfully with no fine.");
            }
    
            // Apply dark mode styling to dialogs and components after return
            JOptionPane.getRootFrame().setBackground(isDarkMode ? DARK_COMPONENT_BG : Color.WHITE);
    
            // Optionally, update the table background and text colors
            borrowTable.setBackground(isDarkMode ? DARK_COMPONENT_BG : Color.WHITE);
            borrowTable.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
    
            // Refresh the table after styling
            refreshAllTabs();
    
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error returning book: " + ex.getMessage());
        }
    }    
    private void renewSelectedBook() {
        int selectedRow = borrowTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to renew.");
            return;
        }
    
        int bookId = (int) tableModel.getValueAt(selectedRow, 0);
        String status = (String) tableModel.getValueAt(selectedRow, 5);
    
        if ("Returned".equals(status)) {
            JOptionPane.showMessageDialog(this, "Cannot renew a returned book.");
            return;
        }
    
        try (Connection conn = DBConnection.getConnection()) {
            // Check if book is reserved by someone else
            String checkQuery = "SELECT * FROM borrowed_books WHERE book_id=? AND return_date IS NULL AND student_id != ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setInt(1, bookId);
            checkStmt.setInt(2, studentId);
            ResultSet rs = checkStmt.executeQuery();
    
            if (rs.next()) {
                JOptionPane.showMessageDialog(this, "Cannot renew - this book has been reserved by another student.");
                return;
            }
    
            LocalDate newDueDate = LocalDate.now().plusDays(14);
            PreparedStatement stmt = conn.prepareStatement(
                "UPDATE borrowed_books SET due_date=? WHERE student_id=? AND book_id=? AND return_date IS NULL"
            );
            stmt.setDate(1, Date.valueOf(newDueDate));
            stmt.setInt(2, studentId);
            stmt.setInt(3, bookId);
            stmt.executeUpdate();
    
            refreshAllTabs();
            JOptionPane.showMessageDialog(this, "Book renewed successfully! New due date: " + newDueDate);
    
            // Apply dark mode styling to dialogs and components after renewal
            JOptionPane.getRootFrame().setBackground(isDarkMode ? DARK_COMPONENT_BG : Color.WHITE);
    
            // Optionally, update the table background and text colors
            borrowTable.setBackground(isDarkMode ? DARK_COMPONENT_BG : Color.WHITE);
            borrowTable.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
    
            // Refresh the table after styling
            refreshAllTabs();
    
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error renewing book: " + ex.getMessage());
        }
    }    

    private void addToWishlist(JTable availableBooksTable) {
        int selectedRow = availableBooksTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to add to wishlist.");
            return;
        }
    
        int bookId = (int) availableBooksTable.getValueAt(selectedRow, 0);
    
        if (wishlist.contains(bookId)) {
            JOptionPane.showMessageDialog(this, "This book is already in your wishlist.");
            return;
        }
    
        wishlist.add(bookId);
        updateWishlistInDB();
        JOptionPane.showMessageDialog(this, "Book added to wishlist!");
    
        // Apply dark mode styling to dialog and table
        JOptionPane.getRootFrame().setBackground(isDarkMode ? DARK_COMPONENT_BG : Color.WHITE);
        availableBooksTable.setBackground(isDarkMode ? DARK_COMPONENT_BG : Color.WHITE);
        availableBooksTable.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
    }
    
    private void removeFromWishlist(JTable wishlistTable, DefaultTableModel model) {
        int selectedRow = wishlistTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to remove from wishlist.");
            return;
        }
    
        int bookId = (int) wishlistTable.getValueAt(selectedRow, 0);
        wishlist.remove(Integer.valueOf(bookId));
        updateWishlistInDB();
        model.removeRow(selectedRow);
        JOptionPane.showMessageDialog(this, "Book removed from wishlist.");
    
        // Apply dark mode styling to dialog and table
        JOptionPane.getRootFrame().setBackground(isDarkMode ? DARK_COMPONENT_BG : Color.WHITE);
        wishlistTable.setBackground(isDarkMode ? DARK_COMPONENT_BG : Color.WHITE);
        wishlistTable.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
    }
    
    private void issueFromWishlist(JTable wishlistTable) {
        int selectedRow = wishlistTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a book to issue.");
            return;
        }
    
        String status = (String) wishlistTable.getValueAt(selectedRow, 3);
        if (!"Available".equals(status)) {
            JOptionPane.showMessageDialog(this, "This book is not currently available.");
            return;
        }
    
        int bookId = (int) wishlistTable.getValueAt(selectedRow, 0);
    
        try (Connection conn = DBConnection.getConnection()) {
            // Verify availability again
            PreparedStatement checkStmt = conn.prepareStatement(
                "SELECT isAvailable FROM books WHERE id=?"
            );
            checkStmt.setInt(1, bookId);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && !rs.getBoolean("isAvailable")) {
                JOptionPane.showMessageDialog(this, "This book was just borrowed by someone else.");
                refreshAllTabs();
                return;
            }
    
            // Proceed with issuing
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
    
            // Remove from wishlist
            wishlist.remove(Integer.valueOf(bookId));
            updateWishlistInDB();
    
            refreshAllTabs();
            tabbedPane.setSelectedIndex(0); // Switch to borrowed books tab
            JOptionPane.showMessageDialog(this, "Book issued successfully! Due date: " + dueDate);
            
            // Apply dark mode styling to dialog and table
            JOptionPane.getRootFrame().setBackground(isDarkMode ? DARK_COMPONENT_BG : Color.WHITE);
            wishlistTable.setBackground(isDarkMode ? DARK_COMPONENT_BG : Color.WHITE);
            wishlistTable.setForeground(isDarkMode ? DARK_FG : LIGHT_FG);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error issuing book: " + ex.getMessage());
        }
    }    
    private void updateWishlistInDB() {
		try (Connection conn = DBConnection.getConnection()) {
			// Convert wishlist to comma-separated string
			String wishlistStr = wishlist.stream()
				.map(String::valueOf)
				.collect(java.util.stream.Collectors.joining(","));
			
			// Update database
			String query = "UPDATE users SET wishlist=? WHERE id=?";
			PreparedStatement stmt = conn.prepareStatement(query);
			stmt.setString(1, wishlistStr);
			stmt.setInt(2, studentId);
			stmt.executeUpdate();
			
			// Refresh the display
			loadWishlistBooks(wishlistModel);
		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(this, "Error updating wishlist: " + ex.getMessage());
		}
	} 

    private void filterBorrowedBooks() {
        String searchText = searchField.getText().toLowerCase();
        String filter = (String) filterCombo.getSelectedItem();
        
        // Set up row sorter for the borrowed books table
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        borrowTable.setRowSorter(sorter);
        
        List<RowFilter<Object, Object>> filters = new ArrayList<>();
        
        // Apply search filter
        if (!searchText.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + searchText));
        }
        
        // Apply status filter
        if (!"All".equals(filter)) {
            if ("Overdue".equals(filter)) {
                filters.add(new RowFilter<Object, Object>() {
                    public boolean include(Entry<? extends Object, ? extends Object> entry) {
                        String status = (String) entry.getValue(5);
                        Date dueDate = (Date) entry.getValue(4);
                        return "Borrowed".equals(status) && 
                               LocalDate.now().isAfter(dueDate.toLocalDate());
                    }
                });
            } else {
                filters.add(RowFilter.regexFilter("^" + filter + "$", 5));
            }
        }
        
        // Apply combined filters to the row sorter
        if (!filters.isEmpty()) {
            sorter.setRowFilter(RowFilter.andFilter(filters));
        } else {
            sorter.setRowFilter(null);
        }
    
        // Dark mode styling for search field and filter combo
        if (isDarkMode) {
            searchField.setBackground(DARK_COMPONENT_BG);
            searchField.setForeground(Color.WHITE);
            filterCombo.setBackground(DARK_COMPONENT_BG);
            filterCombo.setForeground(Color.WHITE);
            borrowTable.setBackground(DARK_TABLE_BG);
            borrowTable.setForeground(Color.WHITE);
        } else {
            searchField.setBackground(Color.WHITE);
            searchField.setForeground(Color.BLACK);
            filterCombo.setBackground(Color.WHITE);
            filterCombo.setForeground(Color.BLACK);
            borrowTable.setBackground(Color.WHITE);
            borrowTable.setForeground(Color.BLACK);
        }
    }
    
    private void showNotifications() {
        StringBuilder notifications = new StringBuilder();
        try (Connection conn = DBConnection.getConnection()) {
            // Current borrowed books
            String currentSql = "SELECT b.title, bb.due_date FROM books b " +
                                 "JOIN borrowed_books bb ON b.id = bb.book_id " +
                                 "WHERE bb.student_id = ? AND bb.return_date IS NULL";
            PreparedStatement currentStmt = conn.prepareStatement(currentSql);
            currentStmt.setInt(1, studentId);
            ResultSet currentRs = currentStmt.executeQuery();
    
            if (!currentRs.next()) {
                notifications.append("You have no currently borrowed books.\n");
            } else {
                notifications.append("=== Currently Borrowed Books ===\n");
                do {
                    Date dueDate = currentRs.getDate("due_date");
                    notifications.append("Book: ").append(currentRs.getString("title"))
                                 .append("\nDue Date: ").append(dueDate);
                    
                    if (LocalDate.now().isAfter(dueDate.toLocalDate())) {
                        long daysLate = LocalDate.now().toEpochDay() - dueDate.toLocalDate().toEpochDay();
                        notifications.append(" (OVERDUE by ").append(daysLate).append(" days)");
                    }
                    notifications.append("\n--------------------------\n");
                } while (currentRs.next());
            }
    
            // Wishlist availability
            if (!wishlist.isEmpty()) {
                String placeholders = String.join(",", java.util.Collections.nCopies(wishlist.size(), "?"));
                String wishlistSql = "SELECT id, title FROM books WHERE id IN (" + placeholders + ") AND isAvailable=TRUE";
                PreparedStatement wishlistStmt = conn.prepareStatement(wishlistSql);
                for (int i = 0; i < wishlist.size(); i++) {
                    wishlistStmt.setInt(i + 1, wishlist.get(i));
                }
                ResultSet wishlistRs = wishlistStmt.executeQuery();
                
                if (wishlistRs.next()) {
                    notifications.append("\n=== Wishlist Books Now Available ===\n");
                    do {
                        notifications.append("Book: ").append(wishlistRs.getString("title"))
                                     .append("\n--------------------------\n");
                    } while (wishlistRs.next());
                }
            }
    
            // Show notifications in dark mode compatible dialog
            if (isDarkMode) {
                UIManager.put("OptionPane.background", DARK_DIALOG_BG);
                UIManager.put("Panel.background", DARK_DIALOG_BG);
                UIManager.put("OptionPane.messageForeground", Color.WHITE);
            }
    
            JOptionPane.showMessageDialog(this, notifications.toString(), "Notifications", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading notifications: " + ex.getMessage());
        }
    }
    

    private void refreshAllTabs() {
        loadBorrowedBooks();
        loadWishlist();
        
        // Refresh each tab's content with dark mode consideration
        if (tabbedPane.getTabCount() > 0) {
            Component borrowedTab = tabbedPane.getComponentAt(0);
            if (borrowedTab instanceof JScrollPane) {
                JTable table = (JTable) ((JScrollPane) borrowedTab).getViewport().getView();
                ((DefaultTableModel) table.getModel()).fireTableDataChanged();
                if (isDarkMode) {
                    table.setBackground(DARK_TABLE_BG);
                    table.setForeground(Color.WHITE);
                }
            }
        }
        
        if (tabbedPane.getTabCount() > 1) {
            Component issueTab = tabbedPane.getComponentAt(1);
            if (issueTab instanceof JPanel) {
                for (Component comp : ((JPanel) issueTab).getComponents()) {
                    if (comp instanceof JScrollPane) {
                        JTable table = (JTable) ((JScrollPane) comp).getViewport().getView();
                        loadAvailableBooks((DefaultTableModel) table.getModel());
                        if (isDarkMode) {
                            table.setBackground(DARK_TABLE_BG);
                            table.setForeground(Color.WHITE);
                        }
                    }
                }
            }
        }
        
        if (tabbedPane.getTabCount() > 2) {
            Component wishlistTab = tabbedPane.getComponentAt(2);
            if (wishlistTab instanceof JScrollPane) {
                JTable table = (JTable) ((JScrollPane) wishlistTab).getViewport().getView();
                loadWishlistBooks((DefaultTableModel) table.getModel());
                if (isDarkMode) {
                    table.setBackground(DARK_TABLE_BG);
                    table.setForeground(Color.WHITE);
                }
            }
        }
    }
}