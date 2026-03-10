import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ClienteEstudiante implements Runnable {
    private static final String HOST = "localhost";
    // Solo conocemos al Maestro
    private static final int PUERTO_DISPATCHER = 5000; 
    private final String idEstudiante;

    // 📊 CONTADORES GLOBALES
    public static AtomicInteger exitosos = new AtomicInteger(0);
    public static AtomicInteger fallidos = new AtomicInteger(0);

    public ClienteEstudiante(String id) {
        this.idEstudiante = id;
    }

    @Override
    public void run() {
        int puertoDestino = -1;

        // --- PASO 1: SOLICITAR NODO AL DISPATCHER (MAESTRO) ---
        try (Socket socketDisp = new Socket(HOST, PUERTO_DISPATCHER);
             PrintWriter outDisp = new PrintWriter(socketDisp.getOutputStream(), true);
             BufferedReader inDisp = new BufferedReader(new InputStreamReader(socketDisp.getInputStream()))) {
            
            outDisp.println("SOLICITAR_NODO:" + idEstudiante);
            String respuesta = inDisp.readLine();
            
            if (respuesta != null) {
                puertoDestino = Integer.parseInt(respuesta);
                System.out.println("🧭 " + idEstudiante + ": El Maestro me redirige al puerto " + puertoDestino);
            }
        } catch (IOException e) {
            System.out.println("❌ " + idEstudiante + ": El Maestro (Dispatcher) está apagado o inalcanzable.");
            fallidos.incrementAndGet();
            return; // Si el Maestro cae, el cliente no puede hacer nada
        }

        // --- PASO 2: CONECTARSE AL SERVIDOR ASIGNADO Y ESPERAR TURNO ---
        if (puertoDestino != -1) {
            try (Socket socketNodo = new Socket(HOST, puertoDestino)) {
                
                // ❌ SE ELIMINÓ EL TIMEOUT. Ahora el hilo esperará bloqueado hasta ser atendido.

                PrintWriter out = new PrintWriter(socketNodo.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socketNodo.getInputStream()));
                
                System.out.println("⏳ " + idEstudiante + " -> Llegó al Nodo " + puertoDestino + " y está haciendo fila...");
                out.println(idEstudiante);
                
                // Aquí el estudiante se queda "congelado" pacientemente si el semáforo del servidor está lleno
                String respuesta = in.readLine(); 
                
                if (respuesta != null) {
                    System.out.println("✅ " + idEstudiante + " -> ¡Éxito! Equipo #" + respuesta + " en Nodo " + puertoDestino);
                    
                    // Simular trabajo (5 a 10 segundos)
                    int tiempoUso = ThreadLocalRandom.current().nextInt(5000, 10000);
                    Thread.sleep(tiempoUso);
                    
                    System.out.println("✨ " + idEstudiante + " terminó. Liberando equipo...");
                    out.println("LIBERAR");
                    exitosos.incrementAndGet(); 
                }
            } catch (IOException | InterruptedException e) {
                System.out.println("❌ " + idEstudiante + " -> El Nodo " + puertoDestino + " falló o rechazó la conexión.");
                fallidos.incrementAndGet();
            }
        }
    }

    public static void main(String[] args) {
        int totalClientes = 30; // Disparamos 30 estudiantes
        ExecutorService executor = Executors.newFixedThreadPool(totalClientes);
        String prefijo = String.valueOf(System.currentTimeMillis()).substring(10);

        System.out.println("🚀 LANZANDO " + totalClientes + " ESTUDIANTES AL DISPATCHER...");
        System.out.println("==========================================================");

        for (int i = 0; i < totalClientes; i++) {
            executor.execute(new ClienteEstudiante("Est-" + prefijo + "-" + i));
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        }
        
        executor.shutdown(); 

        // 🛑 REPORTE FINAL
        try {
            // Aumentamos el awaitTermination a 5 minutos para que dé tiempo de que pasen los 30 alumnos
            if (executor.awaitTermination(5, TimeUnit.MINUTES)) {
                System.out.println("\n📊 === REPORTE FINAL DE LA SIMULACIÓN ===");
                System.out.println("Total de estudiantes enviados: " + totalClientes);
                System.out.println("✅ Atendidos con éxito: " + exitosos.get());
                System.out.println("🚫 Fallidos (Por error de red o nodos caídos): " + fallidos.get());
                System.out.println("=========================================");
            } else {
                System.out.println("⏳ La simulación tardó demasiado y se omitió el reporte final.");
            }
        } catch (InterruptedException e) {
            System.out.println("Simulación interrumpida.");
        }
    }
}