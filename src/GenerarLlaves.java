import java.io.FileOutputStream;
import java.security.*;
import java.security.spec.*;

public class GenerarLlaves {
    public static void main(String[] args) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        KeyPair pair = generator.generateKeyPair();

        try (FileOutputStream out = new FileOutputStream("keys/private.key")) {
            out.write(new PKCS8EncodedKeySpec(pair.getPrivate().getEncoded()).getEncoded());
        }

        try (FileOutputStream out = new FileOutputStream("keys/public.key")) {
            out.write(new X509EncodedKeySpec(pair.getPublic().getEncoded()).getEncoded());
        }

        System.out.println("Llaves generadas correctamente.");
    }
}

