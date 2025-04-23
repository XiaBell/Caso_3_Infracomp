import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorDelegadoRunnable implements Runnable {
    private final int puerto;
    private final int servicioId;

    public ServidorDelegadoRunnable(int puerto, int servicioId) {
        this.puerto = puerto;
        this.servicioId = servicioId;
    }

    @Override
    public void run() {
        try (ServerSocket servidor = new ServerSocket(puerto)) {
            System.out.println("[DELEGADO " + servicioId + "] Escuchando en puerto " + puerto);
            while (true) {
                Socket cliente = servidor.accept();
                try (
                    DataInputStream in = new DataInputStream(cliente.getInputStream());
                    DataOutputStream out = new DataOutputStream(cliente.getOutputStream())
                ) {
                    // Leer tipo de consulta y código de vuelo enviados por el cliente
                    String tipoConsulta = in.readUTF();
                    String codigoVuelo = in.readUTF();

                    String respuesta = switch (tipoConsulta) {
                        case "CONSULTA_ESTADO" -> consultaEstado(codigoVuelo);
                        case "DISPONIBILIDAD"   -> disponibilidad(codigoVuelo);
                        case "COSTO_VUELO"      -> costoVuelo(codigoVuelo);
                        default -> "Consulta no reconocida.";
                    };

                    out.writeUTF(respuesta);
                }
            }
        } catch (IOException e) {
            System.err.println("[DELEGADO " + servicioId + "] Error: " + e.getMessage());
        }
    }

    // === Métodos personalizados por consulta ===

    private String consultaEstado(String codigo) {
        if (codigo == null || codigo.isEmpty()) {
            return "El estado del vuelo es: A TIEMPO.";
        }
        return "El estado del vuelo " + codigo + " es: A TIEMPO.";
    }

    private String disponibilidad(String codigo) {
        if (codigo == null || codigo.isEmpty()) {
            return "Hay vuelos disponibles a las 10:00, 15:30 y 20:00.";
        }
        return "El vuelo " + codigo + " TIENE DISPONIBILIDAD.";
    }

    private String costoVuelo(String codigo) {
        if (codigo == null || codigo.isEmpty()) {
            return "El vuelo cuesta $350.000 COP.";
        }
        return "El costo del vuelo " + codigo + " es: $320.000.";
    }
}
