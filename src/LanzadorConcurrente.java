import java.util.Scanner;

/**
 * La clase LanzadorConcurrente es responsable de lanzar múltiples hilos de clientes
 * de manera concurrente. Permite al usuario especificar la cantidad de clientes a lanzar
 * mediante entrada por consola o utiliza un valor predeterminado si no se proporciona uno válido.
 * Cada cliente se ejecuta como un hilo separado, y el programa mide el tiempo total de ejecución
 * de todos los clientes.
 * 
 * Características:
 * - Permite configurar dinámicamente el número de clientes a lanzar.
 * - Ejecuta cada cliente en un hilo separado.
 * - Mide y muestra el tiempo total de ejecución de todos los clientes.
 * - Maneja entradas inválidas de manera adecuada utilizando un valor predeterminado.
 * 
 * Uso:
 * java LanzadorConcurrente
 * El programa solicitará al usuario que ingrese el número de clientes a lanzar.
 * 
 * Notas:
 * - La clase ClienteConsulta debe estar implementada y ser accesible para que este programa funcione.
 * - Asegúrese de manejar excepciones adecuadamente en la clase ClienteConsulta para evitar errores en tiempo de ejecución.
 * 
 * Ejemplo de salida:
 * ¿Cuántos clientes deseas lanzar? 10
 * [INICIO] Lanzando 10 clientes...
 * [FIN] Todos los clientes terminaron.
 * Tiempo total: 1234 ms
 * 
 */

public class LanzadorConcurrente {

    private static int NUM_CLIENTES = 16; // Cambia a 4, 32, 64, etc.

    private static boolean Lanzadoractivo = false;

    public static boolean getLanzadoractivo() {
        return Lanzadoractivo;
    }

    public static int getNumClientes() {
        return NUM_CLIENTES;
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
