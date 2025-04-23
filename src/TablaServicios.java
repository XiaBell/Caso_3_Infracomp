import java.net.InetAddress;

/**
 * La clase TablaServicios proporciona funcionalidad para gestionar y recuperar combinaciones
 * de direcciones IP del servidor y puertos basadas en un identificador dado. Detecta automáticamente
 * la dirección IP del servidor y permite la recuperación de direcciones específicas de servicios.
 */
public class TablaServicios {
    private static String ipServidor;

    public static void inicializarIP() throws Exception {
        ipServidor = InetAddress.getLocalHost().getHostAddress(); // Detecta IP automáticamente
        System.out.println("[SERVIDOR] IP detectada: " + ipServidor);
    }

    public static String obtenerDireccionPorId(int id) {
        return switch (id) {
            case 1 -> ipServidor + ":6000";
            case 2 -> ipServidor + ":6001";
            case 3 -> ipServidor + ":6002";
            default -> ipServidor + ":9999";
        };
    }

    public static String getIP() {
        return ipServidor;
    }
}
