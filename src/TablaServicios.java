import java.net.InetAddress;

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
