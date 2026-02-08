package Table;

public class User {

    int id;
    String name, email, uname, role, status, image;

    public User(int id, String name, String email, String uname,
                String role, String status, String image) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.uname = uname;
        this.role = role;
        this.status = status;
        this.image = image;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getUname() { return uname; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public String getImage() { return image; }
}
