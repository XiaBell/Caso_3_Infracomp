import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import java.math.BigInteger;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.Arrays;

public class ProtocoloSeguridad {

    // === FIRMA RSA ===
    public static byte[] firmarRSA(byte[] datos, String privateKeyPath) throws Exception {
        PrivateKey priv = cargarLlavePrivada(privateKeyPath);
        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initSign(priv);
        sign.update(datos);
        return sign.sign();
    }

    public static boolean validarRSA(byte[] datos, byte[] firma, String publicKeyPath) throws Exception {
        PublicKey pub = cargarLlavePublica(publicKeyPath);
        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initVerify(pub);
        sign.update(datos);
        return sign.verify(firma);
    }

    // === FIRMA DE PARÁMETROS DH ===
    public static byte[] firmarParametros(BigInteger p, BigInteger g, BigInteger gx, String privateKeyPath) throws Exception {
        byte[] datos = (p.toString() + "," + g.toString() + "," + gx.toString()).getBytes();
        return firmarRSA(datos, privateKeyPath);
    }

    public static boolean validarFirmaParametros(BigInteger p, BigInteger g, BigInteger gx, byte[] firma, String publicKeyPath) throws Exception {
        byte[] datos = (p.toString() + "," + g.toString() + "," + gx.toString()).getBytes();
        return validarRSA(datos, firma, publicKeyPath);
    }

    // === DIFFIE-HELLMAN ===
    public static DHResultado generarDH() {
        BigInteger p = new BigInteger(1024, new SecureRandom());
        BigInteger g = BigInteger.valueOf(2);
        BigInteger x = new BigInteger(1024, new SecureRandom());
        BigInteger gx = g.modPow(x, p);
        return new DHResultado(p, g, x, gx);
    }

    public static void enviarParametrosDH(DataOutputStream out, DHResultado dh) throws IOException {
        out.writeUTF(dh.p.toString());
        out.writeUTF(dh.g.toString());
    }

    public static void recibirParametrosDH(DataInputStream in, DHResultado dh) throws IOException {
        dh.p = new BigInteger(in.readUTF());
        dh.g = new BigInteger(in.readUTF());
    }

    public static ClavesSesion generarLlavesSesion(DHResultado dh) throws Exception {
        BigInteger k_master = dh.gy.modPow(dh.x, dh.p);
        System.out.println("[DEBUG] K_master (hex): " + k_master.toString(16));
        byte[] digest = MessageDigest.getInstance("SHA-512").digest(k_master.toByteArray());

        SecretKey aes = new SecretKeySpec(Arrays.copyOfRange(digest, 0, 32), "AES");
        SecretKey hmac = new SecretKeySpec(Arrays.copyOfRange(digest, 32, 64), "HmacSHA256");

        return new ClavesSesion(aes, hmac);
    }

    // === CIFRADO SEGURO CON HMAC ===
    public static void enviarMensajeSeguro(DataOutputStream out, byte[] data, ClavesSesion claves) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, claves.aes, new IvParameterSpec(iv));
        byte[] cifrado = cipher.doFinal(data);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(claves.hmac);
        byte[] hmac = mac.doFinal(cifrado);

        // DEBUG
        System.out.println("[DEBUG] --- ENVIANDO ---");
        System.out.println("[DEBUG] IV (hex): " + bytesToHex(iv));
        System.out.println("[DEBUG] Cifrado (hex): " + bytesToHex(cifrado));
        System.out.println("[DEBUG] HMAC (hex): " + bytesToHex(hmac));

        out.writeInt(iv.length); out.write(iv);
        out.writeInt(cifrado.length); out.write(cifrado);
        out.writeInt(hmac.length); out.write(hmac);
    }

    public static MensajeSeguro recibirMensajeSeguro(DataInputStream in, ClavesSesion claves) throws Exception {
        byte[] iv = new byte[in.readInt()]; in.readFully(iv);
        byte[] cifrado = new byte[in.readInt()]; in.readFully(cifrado);
        byte[] hmacRecibido = new byte[in.readInt()]; in.readFully(hmacRecibido);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(claves.hmac);
        byte[] hmacCalculado = mac.doFinal(cifrado);

        // DEBUG
        System.out.println("[DEBUG] --- RECIBIDO ---");
        System.out.println("[DEBUG] IV (hex): " + bytesToHex(iv));
        System.out.println("[DEBUG] Cifrado (hex): " + bytesToHex(cifrado));
        System.out.println("[DEBUG] HMAC Recibido (hex): " + bytesToHex(hmacRecibido));
        System.out.println("[DEBUG] HMAC Calculado (hex): " + bytesToHex(hmacCalculado));

        if (!MessageDigest.isEqual(hmacCalculado, hmacRecibido)) {
            throw new SecurityException("HMAC inválido");
        }

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, claves.aes, new IvParameterSpec(iv));
        byte[] claro = cipher.doFinal(cifrado);
        return new MensajeSeguro(claro);
    }

    // === UTILIDADES ===
    public static PrivateKey cargarLlavePrivada(String path) throws Exception {
        byte[] bytes = Files.readAllBytes(Path.of(path));
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(bytes));
    }

    public static PublicKey cargarLlavePublica(String path) throws Exception {
        byte[] bytes = Files.readAllBytes(Path.of(path));
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // === CLASES INTERNAS ===
    public static class DHResultado {
        public BigInteger p, g, x, gx, gy;

        // Constructor para el cliente (solo genera x)
        public DHResultado() {
            this.x = new BigInteger(1024, new SecureRandom());
        }

        // Constructor completo (usado por el servidor)
        public DHResultado(BigInteger p, BigInteger g, BigInteger x, BigInteger gx) {
            this.p = p;
            this.g = g;
            this.x = x;
            this.gx = gx;
        }

        public void recibirGy(BigInteger gy) {
            this.gy = gy;
        }
    }

    public static class ClavesSesion {
        public SecretKey aes, hmac;
        public ClavesSesion(SecretKey aes, SecretKey hmac) {
            this.aes = aes;
            this.hmac = hmac;
        }
    }

    public static class MensajeSeguro {
        private final byte[] contenido;
        public MensajeSeguro(byte[] contenido) {
            this.contenido = contenido;
        }
        public byte[] getContenido() {
            return contenido;
        }
    }
}
