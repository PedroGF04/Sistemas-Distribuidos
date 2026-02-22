import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class CentroComputoServidor {
    private static final int TOTAL_EQUIPOS = 5;
    private static final int PUERTO = 5000;
    
    private static int equiposDisponibles = TOTAL_EQUIPOS;
    private static int estudiantesEnEspera = 0;
    private static int estudiantesAtendidos = 0;
    
    private static boolean[] equiposOcupados = new boolean[TOTAL_EQUIPOS];

    public static void main(String[] args) {
        System.out.println("🏪 SERVIDOR DEL CENTRO DE COMPUTO 'Trailblazer' INICIADO");
        System.out.println("📡 Escuchando en el puerto: " + PUERTO);
        System.out.println("==========================================================");

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            while (true) {
                Socket clienteSocket = serverSocket.accept();
                
                String ipCliente = clienteSocket.getInetAddress().getHostAddress();
                int puertoCliente = clienteSocket.getPort();
                System.out.println("\n🔌 [CONEXION ENTRANTE] Desde IP: " + ipCliente + " | Puerto: " + puertoCliente);

                new ManejadorCliente(clienteSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static synchronized void imprimirEstado(String evento) {
        String tiempo = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("📊 [" + tiempo + "] " + evento);
        System.out.println("   🪑 Equipos disponibles: " + equiposDisponibles);
        System.out.println("   ⏳ Estudiantes en espera: " + estudiantesEnEspera);
        System.out.println("   ✅ Clientes atendidos: " + estudiantesAtendidos);
        System.out.println("-".repeat(60));
    }

    static class ManejadorCliente extends Thread {
        private final Socket socket;

        public ManejadorCliente(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                String idEstudiante = in.readLine(); 
                System.out.println("👤 [IDENTIFICACION] Puerto " + socket.getPort() + " es el Estudiante " + idEstudiante);
                
                // LLEGADA Y ESPERA
                if (equiposDisponibles <= 0) {
                    estudiantesEnEspera++; 
                    imprimirEstado("⚠️ El Estudiante " + idEstudiante + " entró a la fila de espera.");
                }

                while (equiposDisponibles <= 0) {
                    Thread.sleep(50); 
                }

                if (estudiantesEnEspera > 0) {
                    estudiantesEnEspera--; 
                }

                // SECCIÓN CRÍTICA
                Thread.sleep(ThreadLocalRandom.current().nextInt(10, 150)); 
                
                equiposDisponibles--; 
                
                int miEquipo = 0; 
                
                for (int i = 0; i < TOTAL_EQUIPOS; i++) {
                    if (!equiposOcupados[i]) { 
                        Thread.sleep(ThreadLocalRandom.current().nextInt(10, 50)); 
                        equiposOcupados[i] = true; 
                        miEquipo = i + 1; 
                        break; 
                    }
                }

                if (miEquipo == 0) {
                    imprimirEstado("💥 ERROR: El Estudiante " + idEstudiante + " entró pero no había equipos físicos disponibles.");
                } else {
                    imprimirEstado("▶️ El Estudiante " + idEstudiante + " ocupó el equipo #" + miEquipo);
                }
                
                out.println(miEquipo); 

                // ESPERAR A QUE EL CLIENTE TERMINE
                in.readLine(); 

                if (miEquipo > 0) {
                    equiposOcupados[miEquipo - 1] = false; 
                }
                
                equiposDisponibles++;
                estudiantesAtendidos++; 

                imprimirEstado("⏹️ El Estudiante " + idEstudiante + " liberó el equipo #" + miEquipo);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}