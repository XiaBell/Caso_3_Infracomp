import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.math.BigInteger;
import java.util.Scanner;

/**
 * La clase ServidorPrincipal representa el servidor principal que maneja las conexiones
 * de los clientes y delega las solicitudes de servicios a servidores secundarios. Implementa
 * un protocolo de comunicación segura con los clientes, incluyendo autenticación, intercambio
 * de claves y mensajes seguros.
 * 
 * Funcionalidades clave:
 * - Inicialización de las IPs de la tabla de servicios.
 * - Inicio de servidores delegados en puertos específicos.
 * - Manejo de conexiones de clientes y gestión de comunicación segura.
 * 
 * El protocolo de comunicación consta de las siguientes fases:
 * 1. Autenticación: El servidor autentica al cliente utilizando un mecanismo de desafío-respuesta
 *    con firmas RSA.
 * 2. Intercambio de claves: El servidor y el cliente realizan un intercambio de claves Diffie-Hellman
 *    para derivar claves de sesión simétricas.
 * 3. Solicitud de servicio: El servidor envía una tabla de servicios cifrada al cliente, recibe
 *    el ID del servicio solicitado y responde con la dirección IP y puerto del servicio correspondiente.
 * 
 * Las excepciones se manejan durante la comunicación con los clientes para garantizar que el servidor
 * permanezca operativo incluso si ocurren errores con clientes individuales.
 * 
 * Dependencias:
 * - {@code TablaServicios}: Administra el mapeo de IDs de servicios a direcciones IP/puertos.
 * - {@code ProtocoloSeguridad}: Proporciona utilidades criptográficas para validación RSA, intercambio
 *   de claves Diffie-Hellman y mensajes seguros.
 * - {@code ServidorDelegadoRunnable}: Representa los hilos de los servidores delegados.
 * 
 * Uso:
 * java ServidorPrincipal
 * 
 * @throws Exception si ocurre un error durante la inicialización del servidor o el manejo de clientes.
 */


public class ServidorPrincipal {

    public static void main(String[] args) throws Exception {
        TablaServicios.inicializarIP();

        Scanner sc = new Scanner(System.in);
        System.out.print("¿Desea lanzar servidores delegados concurrentes? (s/n): ");
        String respuesta = sc.nextLine().trim().toLowerCase();

        if (respuesta.equals("s")) {
            System.out.print("¿Cuántos servidores desea lanzar? (Ej: 4, 16, 32, 64): ");
            int numDelegados = sc.nextInt();
            for (int i = 0; i < numDelegados; i++) {
                int puerto = 6000 + i;
                new Thread(new ServidorDelegadoRunnable(puerto, (i % 3) + 1)).start(); // Rota entre 1, 2 y 3
            }
        } else {
            new Thread(new ServidorDelegadoRunnable(6000, 1)).start();
            new Thread(new ServidorDelegadoRunnable(6001, 2)).start();
            new Thread(new ServidorDelegadoRunnable(6002, 3)).start();
        }

        try (ServerSocket servidor = new ServerSocket(5000)) {
            System.out.println("[SERVIDOR PRINCIPAL] Iniciando y escuchando...");
            while (true) {
                Socket cliente = servidor.accept();
                new Thread(() -> {
                    try {
                        manejarCliente(cliente);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
    }

    private static void manejarCliente(Socket cliente) throws Exception {
        DataInputStream in = new DataInputStream(cliente.getInputStream());
        DataOutputStream out = new DataOutputStream(cliente.getOutputStream());
    
        long startFirma = 0, endFirma = 0, startCifrado = 0, endCifrado = 0, startVerif = 0, endVerif = 0;
        long tiempoCifradoAsim = 0;
    
        // === Fase 1: Autenticación ===
        System.out.println("[Fase 1 - Paso 1] Recibiendo saludo HELLO.");
        String saludo = in.readUTF();
        if (!"HELLO".equals(saludo)) {
            out.writeUTF("ERROR");
            return;
        }
    
        System.out.println("[Fase 1 - Paso 2] Generando y enviando reto.");
        byte[] reto = new byte[32];
        new SecureRandom().nextBytes(reto);
        out.writeInt(reto.length);
        out.write(reto);
    
        System.out.println("[Fase 1 - Paso 3] Recibiendo firma del cliente.");
        byte[] firma = new byte[in.readInt()];
        in.readFully(firma);
    
        System.out.println("[Fase 1 - Paso 4] Verificando firma con K_w+ (cliente).");
        startFirma = System.nanoTime();
        boolean valido = ProtocoloSeguridad.validarRSA(reto, firma, "keys/public.key");
        endFirma = System.nanoTime();
        out.writeUTF(valido ? "OK" : "ERROR");
        if (!valido) return;
    
        System.out.println("[Fase 1 - Paso 5] Firma válida, autenticación completada.");
    
        // === Fase 2: Diffie-Hellman ===
        System.out.println("[Fase 2 - Paso 5] Generando parámetros p, g, g^a.");
        ProtocoloSeguridad.DHResultado dh = ProtocoloSeguridad.generarDH();
        out.writeUTF(dh.p.toString());
        out.writeUTF(dh.g.toString());
        out.writeUTF(dh.gx.toString());
        byte[] firmaDH = ProtocoloSeguridad.firmarParametros(dh.p, dh.g, dh.gx, "keys/private.key");
        out.writeInt(firmaDH.length);
        out.write(firmaDH);
    
        System.out.println("[Fase 2 - Paso 6] Esperando g^b del cliente.");
        dh.recibirGy(new BigInteger(in.readUTF()));
    
        System.out.println("[Fase 2 - Paso 7] Derivando claves simétricas.");
        ProtocoloSeguridad.ClavesSesion claves = ProtocoloSeguridad.generarLlavesSesion(dh);
    
        // === Fase 3: Comunicación Segura ===
        String tabla = """
            1 ? Estado vuelo
            2 ? Disponibilidad vuelos
            3 ? Costo de un vuelo
            """;
    
        System.out.println("[Fase 3 - Paso 8] Enviando tabla cifrada + HMAC.");
        startCifrado = System.nanoTime();
        ProtocoloSeguridad.enviarMensajeSeguro(out, tabla.getBytes(), claves);
        endCifrado = System.nanoTime();
    
        // Cifrado asimétrico (solo medición)
        long startAsim = System.nanoTime();
        ProtocoloSeguridad.cifrarConLlavePublica(tabla.getBytes(), "keys/public.key");
        long endAsim = System.nanoTime();
        tiempoCifradoAsim = (endAsim - startAsim) / 1_000_000;
    
        System.out.println("[Fase 3 - Paso 9] Recibiendo ID de servicio cifrado + HMAC.");
        startVerif = System.nanoTime();
        ProtocoloSeguridad.MensajeSeguro ms = ProtocoloSeguridad.recibirMensajeSeguro(in, claves);
        endVerif = System.nanoTime();
        int id = ByteBuffer.wrap(ms.getContenido()).getInt();
    
        System.out.println("[Fase 3 - Paso 10] Enviando IP/puerto cifrado + HMAC.");
        String direccion = TablaServicios.obtenerDireccionPorId(id);
        ProtocoloSeguridad.enviarMensajeSeguro(out, direccion.getBytes(), claves);
    
        // === Medición de tiempos ===
        long tiempoFirma = (endFirma - startFirma) / 1_000_000;
        long tiempoCifrado = (endCifrado - startCifrado) / 1_000_000;
        long tiempoVerif = (endVerif - startVerif) / 1_000_000;
    
        System.out.printf("[TIEMPOS] Firma: %d ms | Cifrado: %d ms | Verificación: %d ms | Cifrado Asim: %d ms%n",
                tiempoFirma, tiempoCifrado, tiempoVerif, tiempoCifradoAsim);
    
        // === Detección de contexto ===
        String escenario = LanzadorConcurrente.getLanzadoractivo() ? "concurrente" : "iterativo";
        String consultas = escenario.equals("iterativo") ? String.valueOf(ClienteIterativo.getConsultas()) : "-";
        String clientes = escenario.equals("concurrente") ? String.valueOf(LanzadorConcurrente.getNumClientes()) : "-";
        String servidores = LanzadorServidoresDelegados.getNumServidores() > 0 ? 
                            String.valueOf(LanzadorServidoresDelegados.getNumServidores()) : "-";
        
        // === Guardar CSV ===
        synchronized (ServidorPrincipal.class) {
            try (FileWriter fw = new FileWriter("docs/tiempos_0.csv", true);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(escenario + "," + consultas + "," + clientes + "," + servidores + "," +
                         tiempoFirma + "," + tiempoCifrado + "," + tiempoVerif + "," + tiempoCifradoAsim + "\n");
            }
        }
    }
}
