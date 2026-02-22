import java.io.*;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ClientesEstudiante {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("🚀 INICIANDO SIMULADOR DE ESTUDIANTES...");
        List<Thread> hilos = new ArrayList<>();

        for (int i = 1; i <= 12; i++) {
            Thread estudiante = new Thread(new EstudianteCliente(i));
            hilos.add(estudiante);
            estudiante.start();
            Thread.sleep(ThreadLocalRandom.current().nextInt(100, 300));
        }

        for (Thread t : hilos) {
            t.join();
        }
        System.out.println("🏁 Todos los estudiantes han terminado.");
    }

    static class EstudianteCliente implements Runnable {
        private final int idEstudiante;

        public EstudianteCliente(int id) {
            this.idEstudiante = id;
        }

        @Override
        public void run() {
            try (Socket socket = new Socket("localhost", 5000);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                String tiempo = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                
                System.out.println("[" + tiempo + "] 👋 Estudiante " + idEstudiante + " solicita equipo al servidor desde el puerto local " + socket.getLocalPort() + "...");
                
                // Enviar ID
                out.println(idEstudiante);

                // Esperar asignación de equipo (
                String respuesta = in.readLine();
                int equipoAsignado = Integer.parseInt(respuesta);
                
                tiempo = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                System.out.println("[" + tiempo + "] 🪑 Servidor asignó el equipo #" + equipoAsignado + " al Estudiante " + idEstudiante);

                
                int tiempoUso = ThreadLocalRandom.current().nextInt(1500, 4000);
                System.out.println("   🖥 Estudiante " + idEstudiante + " trabajando en equipo #" + equipoAsignado + "... (" + (tiempoUso / 1000.0) + "s)");
                Thread.sleep(tiempoUso);

                
                tiempo = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                System.out.println("[" + tiempo + "] ✨ Estudiante " + idEstudiante + " libera el Equipo #" + equipoAsignado);
                out.println("LIBERAR");

            } catch (IOException | InterruptedException e) {
                System.out.println("❌ Error de conexión en el estudiante " + idEstudiante);
            }
        }
    }
}