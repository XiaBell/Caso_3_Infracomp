import java.util.Scanner;

public class LanzadorConcurrente {

    private static int NUM_CLIENTES = 16; // Cambia a 4, 32, 64, etc.

    private static boolean Lanzadoractivo = false;

    public static boolean getLanzadoractivo() {
        return Lanzadoractivo;
    }

    public static void main(String[] args) {

        Lanzadoractivo = true;

            Scanner scanner = new Scanner(System.in);
            System.out.print("¿Cuántos clientes deseas lanzar? ");
            int numClientesInput = scanner.nextInt();
            scanner.close();

            if (numClientesInput > 0) {
                NUM_CLIENTES = numClientesInput;
            } else {
                System.out.println("[ADVERTENCIA] Número inválido, se usará el valor predeterminado: " + NUM_CLIENTES);
            }

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
