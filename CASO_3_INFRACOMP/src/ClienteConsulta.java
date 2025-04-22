import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ClienteConsulta {
    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("localhost", 5000);
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        out.writeUTF("HELLO");

        byte[] reto = new byte[in.readInt()];
        in.readFully(reto);

        byte[] firmado = ProtocoloSeguridad.firmarRSA(reto, "keys/private.key");
        out.writeInt(firmado.length);
        out.write(firmado);

        if (!"OK".equals(in.readUTF())) {
            throw new SecurityException("Firma rechazada");
        }

        ProtocoloSeguridad.DHResultado dh = new ProtocoloSeguridad.DHResultado(); // solo genera x
        ProtocoloSeguridad.recibirParametrosDH(in, dh); // recibe p y g
        dh.gy = new BigInteger(in.readUTF()); // recibe gx del servidor
        dh.gx = dh.g.modPow(dh.x, dh.p);
        out.writeUTF(dh.gx.toString());

        ProtocoloSeguridad.ClavesSesion claves = ProtocoloSeguridad.generarLlavesSesion(dh);

        ProtocoloSeguridad.MensajeSeguro tabla = ProtocoloSeguridad.recibirMensajeSeguro(in, claves);
        System.out.println("Servicios disponibles:\n" + new String(tabla.getContenido()));

        int id = 2;
        byte[] idBytes = ByteBuffer.allocate(4).putInt(id).array();
        ProtocoloSeguridad.enviarMensajeSeguro(out, idBytes, claves);

        ProtocoloSeguridad.MensajeSeguro resp = ProtocoloSeguridad.recibirMensajeSeguro(in, claves);
        System.out.println("Respuesta: " + new String(resp.getContenido()));

        socket.close();
    }
}
