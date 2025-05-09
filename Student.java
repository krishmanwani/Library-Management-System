// Student.java
public class Student extends User {
    public Student(int id, String name, String username, String password) {
        super(id, name, username, password);
    }

    @Override
    public void openDashboard() {
        new StudentDashboard(id);
    }
}