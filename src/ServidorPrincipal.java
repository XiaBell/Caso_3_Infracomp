import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.math.BigInteger;


public class ServidorPrincipal {

    public static void main(String[] args) throws Exception {
        TablaServicios.inicializarIP();

        new Thread(new ServidorDelegadoRunnable(6000, 1)).start();
        new Thread(new ServidorDelegadoRunnable(6001, 2)).start();
        new Thread(new ServidorDelegadoRunnable(6002, 3)).start();

        try (ServerSocket servidor = new ServerSocket(5000)) {
            System.out.println("[SERVIDOR PRINCIPAL] Iniciando y escuchando...");
            while (true) {
                Socket cliente = servidor.accept();
                new Thread(() -> {
                    try {
                        manejarCliente(cliente);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
    }

    private static void manejarCliente(Socket cliente) throws Exception {
        DataInputStream in = new DataInputStream(cliente.getInputStream());
        DataOutputStream out = new DataOutputStream(cliente.getOutputStream());

        System.out.println("[Fase 1 - Paso 1] Recibiendo saludo HELLO.");
        String saludo = in.readUTF();
        if (!"HELLO".equals(saludo)) {
            out.writeUTF("ERROR");
            return;
        }

        System.out.println("[Fase 1 - Paso 2] Generando y enviando reto.");
        byte[] reto = new byte[32];
        new java.security.SecureRandom().nextBytes(reto);
        out.writeInt(reto.length);
        out.write(reto);

        System.out.println("[Fase 1 - Paso 3] Recibiendo firma del cliente.");
        byte[] firma = new byte[in.readInt()];
        in.readFully(firma);

        System.out.println("[Fase 1 - Paso 4] Verificando firma con K_w+ (cliente).");
        boolean valido = ProtocoloSeguridad.validarRSA(reto, firma, "keys/public.key");
        out.writeUTF(valido ? "OK" : "ERROR");
        if (!valido) return;

        System.out.println("[Fase 1 - Paso 5] Firma válida, autenticación completada.");

        // DH
        System.out.println("[Fase 2 - Paso 5] Generando parámetros p, g, g^a.");
        ProtocoloSeguridad.DHResultado dh = ProtocoloSeguridad.generarDH();
        out.writeUTF(dh.p.toString());
        out.writeUTF(dh.g.toString());
        out.writeUTF(dh.gx.toString());
        byte[] firmaDH = ProtocoloSeguridad.firmarParametros(dh.p, dh.g, dh.gx, "keys/private.key");
        out.writeInt(firmaDH.length);
        out.write(firmaDH);

        System.out.println("[Fase 2 - Paso 6] Esperando g^b del cliente.");
        dh.recibirGy(new BigInteger(in.readUTF()));
        System.out.println("[Fase 2 - Paso 7] Derivando claves simétricas.");
        ProtocoloSeguridad.ClavesSesion claves = ProtocoloSeguridad.generarLlavesSesion(dh);

        // Tabla de servicios
        System.out.println("[Fase 3 - Paso 8] Enviando tabla cifrada + HMAC.");
        String tabla = """
        1 ? Estado vuelo
        2 ? Disponibilidad vuelos
        3 ? Costo de un vuelo
        """;
        ProtocoloSeguridad.enviarMensajeSeguro(out, tabla.getBytes(), claves);

        System.out.println("[Fase 3 - Paso 9] Recibiendo ID de servicio cifrado + HMAC.");
        ProtocoloSeguridad.MensajeSeguro ms = ProtocoloSeguridad.recibirMensajeSeguro(in, claves);
        int id = ByteBuffer.wrap(ms.getContenido()).getInt();

        System.out.println("[Fase 3 - Paso 10] Enviando IP/puerto cifrado + HMAC.");
        String direccion = TablaServicios.obtenerDireccionPorId(id);
        ProtocoloSeguridad.enviarMensajeSeguro(out, direccion.getBytes(), claves);
    }
}
