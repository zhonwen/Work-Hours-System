import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class HashGen {
  public static void main(String[] args) {
    BCryptPasswordEncoder e = new BCryptPasswordEncoder(12);
    String h = e.encode("Admin123");
    System.out.println(h);
    System.out.println(e.matches("Admin123", h));
    System.out.println(e.matches("Admin123", "$2a$12$CQpisUhdgrlmuz.76H79Y.jJGFR3dGribbfDyrCASJzkUsJkWVXfG"));
  }
}
