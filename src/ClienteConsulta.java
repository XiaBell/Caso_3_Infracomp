import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ClienteConsulta {
    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("localhost", 5000);

        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        System.out.println("[Fase 1] Enviando saludo HELLO");
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
        System.out.println("Servicios disponibles:\n" + new String(tabla.getContenido()));

                // Selección del servicio
                int id;
                if (LanzadorConcurrente.getLanzadoractivo()) {
                    id = 1 + (int) (Math.random() * 3);
                    System.out.println("ID aleatorio seleccionado: " + id);
                } else {
                    java.util.Scanner sc = new java.util.Scanner(System.in);
                    do {
                        System.out.print("Ingrese el ID del servicio (1 a 3): ");
                        try {
                            id = Integer.parseInt(sc.nextLine());
                        } catch (NumberFormatException e) {
                            id = -1;
                        }
                    } while (id < 1 || id > 3);
                   // sc.close();
                }


        byte[] idBytes = ByteBuffer.allocate(4).putInt(id).array();
        ProtocoloSeguridad.enviarMensajeSeguro(out, idBytes, claves);

        ProtocoloSeguridad.MensajeSeguro resp = ProtocoloSeguridad.recibirMensajeSeguro(in, claves);
        String direccion = new String(resp.getContenido());
        System.out.println("Dirección del servidor delegado: " + direccion);
        socket.close();

        String[] partes = direccion.split(":");
        Socket delegado = new Socket(partes[0], Integer.parseInt(partes[1]));
        DataOutputStream outDel = new DataOutputStream(delegado.getOutputStream());
        DataInputStream inDel = new DataInputStream(delegado.getInputStream());

        String tipoConsulta;
        String codigoVuelo = "";
        
        if (LanzadorConcurrente.getLanzadoractivo()) {
            tipoConsulta = switch (id) {
                case 1 -> "CONSULTA_ESTADO";
                case 2 -> "DISPONIBILIDAD";
                case 3 -> "COSTO_VUELO";
                default -> "DESCONOCIDA";
            };
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Ingrese el código del vuelo: ");
            codigoVuelo = reader.readLine();
            tipoConsulta = switch (id) {
                case 1 -> "CONSULTA_ESTADO";
                case 2 -> "DISPONIBILIDAD";
                case 3 -> "COSTO_VUELO";
                default -> "DESCONOCIDA";
            };
        }
        
        // Enviar tipo de consulta y código del vuelo como dos líneas
        outDel.writeUTF(tipoConsulta);
        outDel.writeUTF(codigoVuelo);  // puede estar vacío si es lanzador
        

        String resultado = inDel.readUTF();
        System.out.println("Respuesta del delegado: " + resultado);
        delegado.close();
    }
}
