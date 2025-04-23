import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ClienteConsulta {
    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("localhost", 5000);
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        System.out.println("[Fase 1 - Paso 1] Enviando saludo HELLO al servidor.");
        out.writeUTF("HELLO");

        System.out.println("[Fase 1 - Paso 2] Recibiendo reto del servidor.");
        byte[] reto = new byte[in.readInt()];
        in.readFully(reto);

        System.out.println("[Fase 1 - Paso 3] Firmando el reto con K_w- (privada cliente).");
        byte[] firmado = ProtocoloSeguridad.firmarRSA(reto, "keys/private.key");

        System.out.println("[Fase 1 - Paso 4] Enviando firma al servidor.");
        out.writeInt(firmado.length);
        out.write(firmado);

        System.out.println("[Fase 1 - Paso 5] Esperando validación de firma...");
        String respuesta = in.readUTF();
        if (!"OK".equals(respuesta)) throw new SecurityException("Firma inválida");

        System.out.println("[Fase 2 - Paso 6] Recibiendo p, g y g^a mod p, y firma.");
        ProtocoloSeguridad.DHResultado dh = new ProtocoloSeguridad.DHResultado();
        ProtocoloSeguridad.recibirParametrosDH(in, dh);
        BigInteger gx = new BigInteger(in.readUTF());
        byte[] firmaDH = new byte[in.readInt()];
        in.readFully(firmaDH);

        System.out.println("[Fase 2 - Paso 6] Verificando firma de parámetros DH.");
        if (!ProtocoloSeguridad.validarFirmaParametros(dh.p, dh.g, gx, firmaDH, "keys/public.key")) {
            throw new SecurityException("Firma inválida sobre parámetros DH.");
        }

        System.out.println("[Fase 2 - Paso 7] Generando g^b mod p y enviando.");
        dh.gx = dh.g.modPow(dh.x, dh.p);
        dh.gy = gx;
        out.writeUTF(dh.gx.toString());

        System.out.println("[Fase 2 - Paso 7] Derivando claves simétricas.");
        ProtocoloSeguridad.ClavesSesion claves = ProtocoloSeguridad.generarLlavesSesion(dh);

        System.out.println("[Fase 3 - Paso 8] Recibiendo tabla cifrada + HMAC.");
        ProtocoloSeguridad.MensajeSeguro tabla = ProtocoloSeguridad.recibirMensajeSeguro(in, claves);
        System.out.println("Servicios disponibles:\n" + new String(tabla.getContenido()));

        System.out.println("[Fase 3 - Paso 9] Enviando ID de servicio cifrado + HMAC.");
        int id;

        
        // Solicitando al usuario que ingrese un número válido (1, 2 o 3)
        java.util.Scanner scanner = new java.util.Scanner(System.in);
        do {
            System.out.print("Ingrese el id del servicio (1, 2 o 3): ");
            try {
            id = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
            id = -1; // Valor inválido para forzar la repetición del bucle
            }
        } while (id < 1 || id > 3);
        scanner.close();

        byte[] idBytes = ByteBuffer.allocate(4).putInt(id).array();
        ProtocoloSeguridad.enviarMensajeSeguro(out, idBytes, claves);

        System.out.println("[Fase 3 - Paso 10] Recibiendo IP/puerto cifrado + HMAC.");
        ProtocoloSeguridad.MensajeSeguro resp = ProtocoloSeguridad.recibirMensajeSeguro(in, claves);
        System.out.println("Respuesta: " + new String(resp.getContenido()));

        socket.close();
    }
}
