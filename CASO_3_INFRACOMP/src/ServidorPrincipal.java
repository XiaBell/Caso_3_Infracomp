import java.io.*;
import java.math.BigInteger;
import java.net.*;

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

                    if (!"HELLO".equals(in.readUTF())) return;

                    byte[] reto = new byte[16];
                    new java.security.SecureRandom().nextBytes(reto);
                    out.writeInt(reto.length);
                    out.write(reto);

                    byte[] firma = new byte[in.readInt()];
                    in.readFully(firma);
                    if (!ProtocoloSeguridad.validarRSA(reto, firma, "keys/public.key")) {
                        out.writeUTF("ERROR");
                        return;
                    }
                    out.writeUTF("OK");

                    ProtocoloSeguridad.DHResultado dh = ProtocoloSeguridad.generarDH(); // genera p, g, x, gx
                    ProtocoloSeguridad.enviarParametrosDH(out, dh);
                    out.writeUTF(dh.gx.toString());
                    dh.recibirGy(new BigInteger(in.readUTF()));

                    ProtocoloSeguridad.ClavesSesion claves = ProtocoloSeguridad.generarLlavesSesion(dh);

                    byte[] datos = tabla.toString().getBytes();
                    ProtocoloSeguridad.enviarMensajeSeguro(out, datos, claves);

                    ProtocoloSeguridad.MensajeSeguro idMsg = ProtocoloSeguridad.recibirMensajeSeguro(in, claves);
                    int id = java.nio.ByteBuffer.wrap(idMsg.getContenido()).getInt();
                    String respuesta = tabla.obtenerIPyPuerto(id);
                    ProtocoloSeguridad.enviarMensajeSeguro(out, respuesta.getBytes(), claves);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
