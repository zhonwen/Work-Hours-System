public class HashGen {
  public static void main(String[] a) {
    System.out.println(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12).encode("Admin123"));
  }
}
