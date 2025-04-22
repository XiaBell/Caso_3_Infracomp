public class LanzadorConcurrente {

    private static final int NUM_CLIENTES = 16; // Cambia a 4, 32, 64, etc.

    public static void main(String[] args) {
        Thread[] clientes = new Thread[NUM_CLIENTES];
        long inicio = System.currentTimeMillis();

        System.out.println("[INICIO] Lanzando " + NUM_CLIENTES + " clientes...");

        for (int i = 0; i < NUM_CLIENTES; i++) {
            int id = i;
            clientes[i] = new Thread(() -> {
                try {
                    ClienteConsulta.main(null); // ejecuta la clase cliente
                } catch (Exception e) {
                    System.err.println("[ERROR] Cliente " + id + ": " + e.getMessage());
                }
            });
            clientes[i].start();
        }

        for (int i = 0; i < NUM_CLIENTES; i++) {
            try {
                clientes[i].join(); // espera a que todos terminen
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long fin = System.currentTimeMillis();
        System.out.println("[FIN] Todos los clientes terminaron.");
        System.out.println("Tiempo total: " + (fin - inicio) + " ms");
    }
}
