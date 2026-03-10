import java.io.*;
import java.net.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Semaphore;

public class ServidorComputo {
    private final int totalEquipos;
    private final int puerto;
    private int estudiantesEnEspera = 0;
    private int estudiantesAtendidos = 0;
    private final boolean[] equiposOcupados;
    private final Semaphore semaforoEquipos;
    private final Object lockEstado = new Object();

    public ServidorComputo(int puerto, int equipos) {
        this.puerto = puerto;
        this.totalEquipos = equipos;
        this.equiposOcupados = new boolean[equipos];
        this.semaforoEquipos = new Semaphore(equipos, true);
    }

    public void iniciar() {
        System.out.println("🏠 SERVIDOR INICIADO EN PUERTO: " + puerto + " | Capacidad: " + totalEquipos);
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            while (true) {
                Socket clienteSocket = serverSocket.accept();
                new Manejador(clienteSocket).start();
            }
        } catch (IOException e) {
            System.err.println("Error en servidor: " + e.getMessage());
        }
    }

    private void imprimirEstado(String evento) {
        String tiempo = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        synchronized (System.out) {
            System.out.println("📊 [" + tiempo + "][Puerto " + puerto + "] " + evento);
            System.out.println("   🪑 Libres: " + semaforoEquipos.availablePermits() + " | ⏳ Espera: " + estudiantesEnEspera);
            System.out.println("-".repeat(40));
        }
    }

    class Manejador extends Thread {
        private final Socket socket;

        public Manejador(Socket socket) { this.socket = socket; }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                
                String idEstudiante = in.readLine();
                
                synchronized (lockEstado) { estudiantesEnEspera++; }
                imprimirEstado("Estudiante " + idEstudiante + " entró en fila.");

                semaforoEquipos.acquire(); // Control de concurrencia

                int miEquipo = -1;
                synchronized (lockEstado) {
                    estudiantesEnEspera--;
                    for (int i = 0; i < totalEquipos; i++) {
                        if (!equiposOcupados[i]) {
                            equiposOcupados[i] = true;
                            miEquipo = i + 1;
                            break;
                        }
                    }
                }
                
                imprimirEstado("Equipo #" + miEquipo + " asignado a " + idEstudiante);
                out.println(miEquipo); // Notifica al cliente

                in.readLine(); // Espera señal de liberación "LIBERAR"

                synchronized (lockEstado) {
                    equiposOcupados[miEquipo - 1] = false;
                    estudiantesAtendidos++;
                    imprimirEstado("Equipo #" + miEquipo + " liberado.");
                }
                semaforoEquipos.release();

            } catch (Exception e) {
                System.out.println("⚠️ Conexión perdida.");
            }
        }
    }

    public static void main(String[] args) {
        // Ejemplo: puedes pasar el puerto por consola
        int p = (args.length > 0) ? Integer.parseInt(args[0]) : 5000;
        new ServidorComputo(p, 5).iniciar();
    }
}