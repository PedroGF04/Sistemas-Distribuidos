import java.io.*;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class ClienteEstudiante {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        System.out.println("🚀 INICIANDO CLIENTE ESTUDIANTE");
        System.out.print("👉 Ingresa tu ID o Matrícula (ej. 101): ");
        String idEstudiante = scanner.nextLine();

        try (Socket socket = new Socket("localhost", 5000);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String tiempo = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            System.out.println("[" + tiempo + "] 👋 Solicitando equipo al servidor...");
            
            // 1. Enviar ID al servidor
            out.println(idEstudiante);

            // 2. Esperar asignación (el cliente se queda pausado aquí si el centro de cómputo está lleno)
            String respuesta = in.readLine();
            if (respuesta == null) {
                System.out.println("❌ El servidor cerró la conexión abruptamente.");
                return;
            }

            int equipoAsignado = Integer.parseInt(respuesta);
            
            tiempo = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            System.out.println("[" + tiempo + "] 🪑 ¡Éxito! El Servidor te asignó el equipo #" + equipoAsignado);

            // 3. Simular tiempo de trabajo 
            int tiempoUso = ThreadLocalRandom.current().nextInt(30000, 60000);;
            System.out.println("   🖥️ Trabajando en el equipo #" + equipoAsignado + " por " + (tiempoUso / 1000) + " segundos...");
            Thread.sleep(tiempoUso);

            // 4. Avisar al servidor que terminó
            tiempo = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            System.out.println("[" + tiempo + "] ✨ Trabajo terminado. Liberando equipo y desconectando.");
            out.println("LIBERAR");

        } catch (IOException | InterruptedException e) {
            System.out.println("❌ No se pudo conectar con el servidor. ¿Está encendido?");
        } finally {
            scanner.close();
        }
    }
}