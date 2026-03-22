import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ClienteEstudianteRMI implements Runnable {
    private static final String HOST = "localhost";
    
    // LISTA DE SERVIDORES DISPONIBLES
    private static final int[] PUERTOS_DISPONIBLES = {5001, 5002, 5003}; 
    
    private final String idEstudiante;

    public static AtomicInteger exitosos = new AtomicInteger(0);
    public static AtomicInteger fallidos = new AtomicInteger(0);

    public ClienteEstudianteRMI(String id) {
        this.idEstudiante = id;
    }

    @Override
    public void run() {
        // El cliente elige un servidor al azar de la lista
        int indiceAleatorio = ThreadLocalRandom.current().nextInt(PUERTOS_DISPONIBLES.length);
        int puertoDestino = PUERTOS_DISPONIBLES[indiceAleatorio];

        try {
            // 1. Nos conectamos al registro RMI del puerto elegido
            Registry registry = LocateRegistry.getRegistry(HOST, puertoDestino);
            
            // 2. Buscamos el objeto remoto
            IServidorLaboratorio nodo = (IServidorLaboratorio) registry.lookup("NodoLaboratorio");
            
            System.out.println("🧭 " + idEstudiante + " -> Decidió ir al Servidor en el puerto " + puertoDestino);
            System.out.println("⏳ " + idEstudiante + " -> Solicitando PC y haciendo fila...");
            
            // 3. Llamada remota: Ocupar equipo
            int miPC = nodo.ocuparEquipo(idEstudiante);
            System.out.println("✅ " + idEstudiante + " -> ¡Éxito! Equipo #PC-" + miPC + " en el puerto " + puertoDestino);
            
            // Simular trabajo
            int tiempoUso = ThreadLocalRandom.current().nextInt(5000, 10000);
            Thread.sleep(tiempoUso);
            
            // 4. Llamada remota: Liberar equipo
            System.out.println("✨ " + idEstudiante + " terminó. Liberando PC-" + miPC + " del puerto " + puertoDestino + "...");
            nodo.liberarEquipo(miPC, idEstudiante);
            
            exitosos.incrementAndGet();

        } catch (Exception e) {
            System.out.println("❌ " + idEstudiante + " -> El Servidor " + puertoDestino + " está lleno, falló o está apagado. Error: " + e.getMessage());
            fallidos.incrementAndGet();
        }
    }

    public static void main(String[] args) {
        int totalClientes = 30; // Mandaremos 30 estudiantes a distribuirse entre los servidores
        ExecutorService executor = Executors.newFixedThreadPool(totalClientes);
        String prefijo = String.valueOf(System.currentTimeMillis()).substring(10);

        System.out.println("🚀 LANZANDO " + totalClientes + " ESTUDIANTES A MULTIPLES SERVIDORES RMI...");
        System.out.println("==========================================================");

        for (int i = 0; i < totalClientes; i++) {
            executor.execute(new ClienteEstudianteRMI("Est-" + prefijo + "-" + i));
            try { Thread.sleep(100); } catch (InterruptedException e) {}
        }
        
        executor.shutdown(); 

        try {
            if (executor.awaitTermination(5, TimeUnit.MINUTES)) {
                System.out.println("\n📊 === REPORTE FINAL DE LA SIMULACIÓN ===");
                System.out.println("✅ Atendidos con éxito: " + exitosos.get());
                System.out.println("🚫 Fallidos: " + fallidos.get());
                System.out.println("=========================================");
            }
        } catch (InterruptedException e) {
            System.out.println("Simulación interrumpida.");
        }
    }
}