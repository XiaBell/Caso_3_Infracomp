import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.ByteBuffer;

public class ServidorPrincipal {
    public static void main(String[] args) throws Exception {
        ServerSocket server = new ServerSocket(5000);
        TablaServicios tabla = new TablaServicios();
        System.out.println("[SERVIDOR] Escuchando...");

        while (true) {
            Socket cliente = server.accept();
            new Thread(() -> {
                try (DataInputStream in = new DataInputStream(cliente.getInputStream());
                     DataOutputStream out = new DataOutputStream(cliente.getOutputStream())) {

                    System.out.println("[Fase 1 - Paso 1] Recibiendo saludo HELLO.");
                    if (!"HELLO".equals(in.readUTF())) return;

                    System.out.println("[Fase 1 - Paso 2] Generando y enviando reto.");
                    byte[] reto = new byte[16];
                    new java.security.SecureRandom().nextBytes(reto);
                    out.writeInt(reto.length);
                    out.write(reto);

                    System.out.println("[Fase 1 - Paso 3] Recibiendo firma del cliente.");
                    byte[] firma = new byte[in.readInt()];
                    in.readFully(firma);

                    System.out.println("[Fase 1 - Paso 4] Verificando firma con K_w+ (cliente).");
                    if (!ProtocoloSeguridad.validarRSA(reto, firma, "keys/public.key")) {
                        out.writeUTF("ERROR");
                        return;
                    }
                    out.writeUTF("OK");
                    System.out.println("[Fase 1 - Paso 5] Firma válida, autenticación completada.");

                    System.out.println("[Fase 2 - Paso 5] Generando parámetros p, g, g^a.");
                    ProtocoloSeguridad.DHResultado dh = ProtocoloSeguridad.generarDH();

                    System.out.println("[Fase 2 - Paso 5] Enviando p, g, gx con firma.");
                    ProtocoloSeguridad.enviarParametrosDH(out, dh);
                    out.writeUTF(dh.gx.toString());

                    byte[] firmaDH = ProtocoloSeguridad.firmarParametros(dh.p, dh.g, dh.gx, "keys/private.key");
                    out.writeInt(firmaDH.length);
                    out.write(firmaDH);

                    System.out.println("[Fase 2 - Paso 6] Esperando g^b del cliente.");
                    dh.recibirGy(new BigInteger(in.readUTF()));

                    System.out.println("[Fase 2 - Paso 7] Derivando claves simétricas.");
                    ProtocoloSeguridad.ClavesSesion claves = ProtocoloSeguridad.generarLlavesSesion(dh);

                    System.out.println("[Fase 3 - Paso 8] Enviando tabla cifrada + HMAC.");
                    byte[] datos = tabla.toString().getBytes();
                    ProtocoloSeguridad.enviarMensajeSeguro(out, datos, claves);

                    System.out.println("[Fase 3 - Paso 9] Recibiendo ID de servicio cifrado + HMAC.");
                    ProtocoloSeguridad.MensajeSeguro idMsg = ProtocoloSeguridad.recibirMensajeSeguro(in, claves);
                    int id = ByteBuffer.wrap(idMsg.getContenido()).getInt();

                    System.out.println("[Fase 3 - Paso 10] Enviando IP/puerto cifrado + HMAC.");
                    String respuesta = tabla.obtenerIPyPuerto(id);
                    ProtocoloSeguridad.enviarMensajeSeguro(out, respuesta.getBytes(), claves);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
