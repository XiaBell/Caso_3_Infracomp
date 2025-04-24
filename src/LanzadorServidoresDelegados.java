import java.util.Scanner;

/**
 * Lanza múltiples servidores delegados en puertos consecutivos desde 6000 en adelante.
 * Cada servidor escucha en su propio puerto y responde a un solo cliente.
 * 
 * Ejemplo:
 * ¿Cuántos servidores deseas lanzar? 4
 * => Se crean ServidorDelegadoRunnable(6000, 1), (6001, 2), ..., (6003, 4)
 */
public class LanzadorServidoresDelegados {
    private static int numServidores = 0;

    public static int getNumServidores() {
        return numServidores;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("¿Cuántos servidores deseas lanzar? ");
        numServidores = sc.nextInt(); // <--- actualiza la variable estática
        sc.close();

        for (int i = 0; i < numServidores; i++) {
            int puerto = 6000 + i;
            int idServicio = (i % 3) + 1;
            new Thread(new ServidorDelegadoRunnable(puerto, idServicio)).start();
        }

        System.out.println("[SERVIDORES DELEGADOS] " + numServidores + " servidores lanzados.");
    }
}
