import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Semaphore;

public class CentroComputoServidor {
    private static final int TOTAL_EQUIPOS = 5;
    private static final int PUERTO = 5000;
    
    // Variables de estado
    private static int estudiantesEnEspera = 0;
    private static int estudiantesAtendidos = 0;
    private static boolean[] equiposOcupados = new boolean[TOTAL_EQUIPOS];

    // HERRAMIENTAS DE CONCURRENCIA
    private static final Semaphore semaforoEquipos = new Semaphore(TOTAL_EQUIPOS, true);
    private static final Object lockEstado = new Object();

    public static void main(String[] args) {
        System.out.println("🏪 SERVIDOR DEL CENTRO DE COMPUTO 'Trailblazer' INICIADO");
        System.out.println("📡 Escuchando en el puerto: " + PUERTO);
        System.out.println("==========================================================");

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            while (true) {
                Socket clienteSocket = serverSocket.accept();
                
                // Obtener información de conexión del cliente
                String ipCliente = clienteSocket.getInetAddress().getHostAddress();
                int puertoCliente = clienteSocket.getPort();
                
                // Sincronizamos la impresión para que no se mezcle con otros mensajes
                synchronized (System.out) {
                    System.out.println("\n🔌 [NUEVA CONEXION] Cliente conectado desde IP: " + ipCliente + " | Puerto: " + puertoCliente);
                }

                new ManejadorCliente(clienteSocket, ipCliente, puertoCliente).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void imprimirEstado(String evento) {
        String tiempo = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        synchronized (System.out) {
            System.out.println("📊 [" + tiempo + "] " + evento);
            System.out.println("   🪑 Equipos disponibles: " + semaforoEquipos.availablePermits());
            System.out.println("   ⏳ Estudiantes en espera: " + estudiantesEnEspera);
            System.out.println("   ✅ Clientes atendidos: " + estudiantesAtendidos);
            System.out.println("-".repeat(60));
        }
    }

    static class ManejadorCliente extends Thread {
        private final Socket socket;
        private final String ip;
        private final int puerto;

        public ManejadorCliente(Socket socket, String ip, int puerto) {
            this.socket = socket;
            this.ip = ip;
            this.puerto = puerto;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                String idEstudiante = in.readLine(); 
                String infoCliente = "Estudiante " + idEstudiante + " (" + ip + ":" + puerto + ")";
                
                // --- LLEGADA A LA FILA ---
                synchronized (lockEstado) {
                    estudiantesEnEspera++;
                    imprimirEstado("⚠️ El " + infoCliente + " llegó y se unió a la fila.");
                }

                // --- ESPERA ---
                semaforoEquipos.acquire(); 

                // --- SECCIÓN CRÍTICA: ASIGNACIÓN DE EQUIPO ---
                int miEquipo = -1;
                synchronized (lockEstado) {
                    estudiantesEnEspera--; 
                    
                    for (int i = 0; i < TOTAL_EQUIPOS; i++) {
                        if (!equiposOcupados[i]) {
                            equiposOcupados[i] = true;
                            miEquipo = i + 1;
                            break;
                        }
                    }
                    imprimirEstado("▶️ El " + infoCliente + " ocupó el equipo #" + miEquipo);
                }

                out.println(miEquipo);

                // --- ESPERAR A QUE EL CLIENTE TERMINE ---
                in.readLine(); 

                // --- SECCIÓN CRÍTICA: LIBERACIÓN DE EQUIPO ---
                synchronized (lockEstado) {
                    equiposOcupados[miEquipo - 1] = false; 
                    estudiantesAtendidos++; 
                    imprimirEstado("⏹️ El " + infoCliente + " liberó el equipo #" + miEquipo);
                }

                semaforoEquipos.release();

            } catch (Exception e) {
                System.out.println("❌ Error con un cliente (" + ip + ":" + puerto + "): " + e.getMessage());
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }
}