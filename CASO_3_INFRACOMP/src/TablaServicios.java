import java.util.*;

public class TablaServicios {
    private final Map<Integer, String> servicios = new HashMap<>();

    public TablaServicios() {
        servicios.put(1, "192.168.1.10:6000");
        servicios.put(2, "192.168.1.11:6001");
        servicios.put(3, "192.168.1.12:6002");
    }

    public String obtenerIPyPuerto(int id) {
        return servicios.getOrDefault(id, "Servicio no encontrado");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        servicios.forEach((k, v) -> sb.append(k).append(" → ").append(v).append("\n"));
        return sb.toString();
    }
}
