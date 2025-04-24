import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Scanner;

public class ClienteIterativo {

    private static int consultas = 1;

    public static int getConsultas() {
    return consultas;
}


    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.print("¿Cuántas consultas desea ejecutar?: ");
        consultas = sc.nextInt();
        sc.close();

        if (consultas < 1) {
            System.out.println("Dado que el número es mejor a 1, se ejecutará 1 consulta por defexto.");
            consultas = 1;
        }

        for (int i = 0; i < consultas; i++) {
            System.out.println("\n--- CONSULTA #" + (i + 1) + " ---");

            Socket socket = new Socket("localhost", 5000);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            out.writeUTF("HELLO");

            byte[] reto = new byte[in.readInt()];
            in.readFully(reto);

            byte[] firmado = ProtocoloSeguridad.firmarRSA(reto, "keys/private.key");
            out.writeInt(firmado.length);
            out.write(firmado);

            String respuesta = in.readUTF();
            if (!"OK".equals(respuesta)) throw new SecurityException("Firma inválida");

            ProtocoloSeguridad.DHResultado dh = new ProtocoloSeguridad.DHResultado();
            ProtocoloSeguridad.recibirParametrosDH(in, dh);
            BigInteger gx = new BigInteger(in.readUTF());
            byte[] firmaDH = new byte[in.readInt()];
            in.readFully(firmaDH);

            if (!ProtocoloSeguridad.validarFirmaParametros(dh.p, dh.g, gx, firmaDH, "keys/public.key")) {
                throw new SecurityException("Firma inválida sobre parámetros DH.");
            }

            dh.gx = dh.g.modPow(dh.x, dh.p);
            dh.gy = gx;
            out.writeUTF(dh.gx.toString());

            ProtocoloSeguridad.ClavesSesion claves = ProtocoloSeguridad.generarLlavesSesion(dh);
            ProtocoloSeguridad.MensajeSeguro tabla = ProtocoloSeguridad.recibirMensajeSeguro(in, claves);

            int id = 1 + (int) (Math.random() * 3); // Aleatorio entre 1 y 3
            byte[] idBytes = ByteBuffer.allocate(4).putInt(id).array();
            ProtocoloSeguridad.enviarMensajeSeguro(out, idBytes, claves);

            ProtocoloSeguridad.MensajeSeguro resp = ProtocoloSeguridad.recibirMensajeSeguro(in, claves);
            String direccion = new String(resp.getContenido());
            System.out.println("Delegado: " + direccion);

            socket.close();

            String[] partes = direccion.split(":");
            Socket delegado = new Socket(partes[0], Integer.parseInt(partes[1]));
            DataOutputStream outDel = new DataOutputStream(delegado.getOutputStream());
            DataInputStream inDel = new DataInputStream(delegado.getInputStream());

            String tipoConsulta = switch (id) {
                case 1 -> "CONSULTA_ESTADO";
                case 2 -> "DISPONIBILIDAD";
                case 3 -> "COSTO_VUELO";
                default -> "DESCONOCIDA";
            };

            String codigoVuelo = "ABC" + (100 + i); // Generación automática de código para que no hayan retrasos en el proceso xd
            outDel.writeUTF(tipoConsulta);
            outDel.writeUTF(codigoVuelo);

            String resultado = inDel.readUTF();
            System.out.println("Respuesta del delegado: " + resultado);
            delegado.close();
        }
    }
}
