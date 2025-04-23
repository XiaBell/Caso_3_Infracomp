import java.io.FileOutputStream;
import java.security.*;
import java.security.spec.*;

/**
 * La clase GenerarLlaves es responsable de generar un par de llaves RSA
 * (privada y pública) y guardarlas en archivos dentro del directorio "keys".
 * 
 * Este programa utiliza el algoritmo RSA para generar un par de llaves de 1024 bits.
 * La llave privada se guarda en formato PKCS8, y la llave pública se guarda en
 * formato X.509. Ambas llaves se escriben en archivos separados llamados 
 * "private.key" y "public.key" respectivamente.
 * 
 * Uso:
 * - Asegúrese de que el directorio "keys" exista en el directorio de trabajo antes de ejecutar el programa.
 * - Las llaves generadas pueden ser utilizadas para operaciones criptográficas como cifrado, descifrado,
 *   firma y verificación.
 * 
 * Excepciones:
 * - El programa puede lanzar excepciones si hay problemas con la entrada/salida de archivos o la generación de llaves.
 * 
 * Nota:
 * - El tamaño de llave de 1024 bits se utiliza con fines de demostración. Para uso en producción,
 *   considere usar un tamaño de llave mayor (por ejemplo, 2048 o 4096 bits) para mayor seguridad.
 * 
 */

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

