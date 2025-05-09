// Librarian.java
public class Librarian extends User {
    public Librarian(int id, String name, String username, String password) {
        super(id, name, username, password);
    }

    @Override
    public void openDashboard() {
        new LibrarianDashboard();
    }
}