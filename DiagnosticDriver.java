import java.net.URL;

public class DiagnosticDriver {
    public static void main(String[] args) {
        System.out.println("=== DIAGNOSTIC DRIVER ===");
        try {
            Class<?> cls = Class.forName("org.sokybot.app.AppRunner");
            System.out.println("Class: " + cls.getName());
            java.security.CodeSource cs = cls.getProtectionDomain().getCodeSource();
            if (cs != null) {
                System.out.println("Location: " + cs.getLocation());
            } else {
                System.out.println("Location: null (Maybe system class or unknown source)");
            }
            System.out.println("ClassLoader: " + cls.getClassLoader());
            
            // Resource lookup verify
            URL res = cls.getResource("/org/sokybot/app/AppRunner.class");
            System.out.println("Resource URL: " + res);
            
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.out.println("=========================");
    }
}
