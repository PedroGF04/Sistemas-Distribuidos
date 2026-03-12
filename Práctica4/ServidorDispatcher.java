import java.io.*;
import java.net.*;

public class ServidorDispatcher {
    private static final int PUERTO_PUBLICO = 5000;
    // Estos son los puertos de los Servidores Gráficos reales (Nodos Trabajadores)
    private static final int[] NODOS_TRABAJADORES = {6001, 6002, 6003}; 
    private static int indiceTurno = 0; // Para el algoritmo Round-Robin

    public static void main(String[] args) {
        System.out.println("DISPATCHER (MAESTRO) INICIADO EN PUERTO " + PUERTO_PUBLICO);
        System.out.println("Balanceando carga entre los nodos: 6001, 6002, 6003...");
        System.out.println("==========================================================");

        try (ServerSocket serverSocket = new ServerSocket(PUERTO_PUBLICO)) {
            while (true) {
                Socket cliente = serverSocket.accept();
                new Thread(new ManejadorAsignacion(cliente)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ManejadorAsignacion implements Runnable {
        private Socket socket;

        public ManejadorAsignacion(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                String peticion = in.readLine();
                
                if (peticion != null && peticion.startsWith("SOLICITAR_NODO")) {

                    String[] partes = peticion.split(":");
                    String idEstudiante = (partes.length > 1) ? partes[1] : "Desconocido";

                    int puertoAsignado;
                    
                    // SECCIÓN CRÍTICA: Asignamos el turno de forma segura
                    synchronized (ServidorDispatcher.class) {
                        puertoAsignado = NODOS_TRABAJADORES[indiceTurno];
                        // Avanzamos al siguiente puerto. Si llegamos al final, volvemos a empezar (Round-Robin)
                        indiceTurno = (indiceTurno + 1) % NODOS_TRABAJADORES.length; 
                    }
                    
                    System.out.println("👉 Redirigiendo estudiante [" + idEstudiante + "] al Nodo: " + puertoAsignado);
                    out.println(puertoAsignado); // Le enviamos el puerto al cliente
                }
            } catch (IOException e) {
                System.out.println("⚠️ Error despachando cliente.");
            }
        }
    }
}