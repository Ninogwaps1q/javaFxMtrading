package Table;

public class User {

    private int id;
    private String name;
    private String email;
    private String uname;
    private String role;
    private String status;
    private String image;
    private String phone;      // ✅ NEW
    private String address;    // ✅ NEW

    // ================= CONSTRUCTOR =================
    public User(int id, String name, String email, String uname,
                String role, String status, String image,
                String phone, String address) {

        this.id = id;
        this.name = name;
        this.email = email;
        this.uname = uname;
        this.role = role;
        this.status = status;
        this.image = image;
        this.phone = phone;
        this.address = address;
    }

    // ================= GETTERS =================
    public int getId() { return id; }

    public String getName() { return name; }

    public String getEmail() { return email; }

    public String getUname() { return uname; }

    public String getRole() { return role; }

    public String getStatus() { return status; }

    public String getImage() { return image; }

    public String getPhone() { return phone; }      // ✅ NEW

    public String getAddress() { return address; }  // ✅ NEW


    // ================= SETTERS (Optional but Recommended) =================
    public void setId(int id) { this.id = id; }

    public void setName(String name) { this.name = name; }

    public void setEmail(String email) { this.email = email; }

    public void setUname(String uname) { this.uname = uname; }

    public void setRole(String role) { this.role = role; }

    public void setStatus(String status) { this.status = status; }

    public void setImage(String image) { this.image = image; }

    public void setPhone(String phone) { this.phone = phone; }      // ✅ NEW

    public void setAddress(String address) { this.address = address; } // ✅ NEW
}
