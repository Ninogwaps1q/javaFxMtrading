package Table;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class User {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty uname = new SimpleStringProperty();
    private final StringProperty role = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();

    public User(int id, String name, String email, String uname, String role, String status) {
        this.id.set(id);
        this.name.set(name);
        this.email.set(email);
        this.uname.set(uname);
        this.role.set(role);
        this.status.set(status);
    }

    public int getId() { return id.get(); }
    public IntegerProperty idProperty() { return id; }
    public void setId(int id) { this.id.set(id); }

    public String getName() { return name.get(); }
    public StringProperty nameProperty() { return name; }
    public void setName(String name) { this.name.set(name); }

    public String getEmail() { return email.get(); }
    public StringProperty emailProperty() { return email; }
    public void setEmail(String email) { this.email.set(email); }

    public String getUname() { return uname.get(); }
    public StringProperty unameProperty() { return uname; }
    public void setUname(String uname) { this.uname.set(uname); }

    public String getRole() { return role.get(); }
    public StringProperty roleProperty() { return role; }
    public void setRole(String role) { this.role.set(role); }

    public String getStatus() { return status.get(); }
    public StringProperty statusProperty() { return status; }
    public void setStatus(String status) { this.status.set(status); }
}
