import java.util.*;

public class TablaServicios {
    private final Map<Integer, String> servicios = new HashMap<>();
    private final Map<Integer, String> descripciones = new HashMap<>();

    public TablaServicios() {
        // Asociaciones reales
        servicios.put(1, "192.168.1.10:6000");
        servicios.put(2, "192.168.1.11:6001");
        servicios.put(3, "192.168.1.12:6002");

        // Descripciones visibles para el usuario
        descripciones.put(1, "Estado vuelo");
        descripciones.put(2, "Disponibilidad vuelos");
        descripciones.put(3, "Costo de un vuelo");
    }

    public String obtenerIPyPuerto(int id) {
        return servicios.getOrDefault(id, "Servicio no encontrado");
    }

    public String obtenerDescripcion(int id) {
        return descripciones.getOrDefault(id, "Descripción no disponible");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        descripciones.forEach((id, nombre) -> sb.append(id).append(" → ").append(nombre).append("\n"));
        return sb.toString();
    }
}
