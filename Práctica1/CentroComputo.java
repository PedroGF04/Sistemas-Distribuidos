import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Semaphore;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CentroComputo {
    private static final int TOTAL_EQUIPOS = 5;
    private static final Semaphore equipos = new Semaphore(TOTAL_EQUIPOS, true);
    private static final AtomicInteger estudiantesEnEspera = new AtomicInteger(0);
    private static final AtomicInteger estudiantesAtendidos = new AtomicInteger(0);
    private static final AtomicInteger numeroEquipo = new AtomicInteger(1);

    public static void main(String[] args) throws InterruptedException {
        System.out.println("🏪 CENTRO DE COMPUTO 'Trailblazer' - Simulación de Concurrencia");
        System.out.println("📅 " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        System.out.println("🪑 Equiupos disponibles: " + TOTAL_EQUIPOS);
        System.out.println("=".repeat(60));

        List<Thread> listaEstudiantes = new ArrayList<>();

        // Crear múltiples clientes llegando en diferentes momentos
        for (int i = 1; i <= 12; i++) {
            Thread estudiante = new Thread(new Estudiante(i));
            listaEstudiantes.add(estudiante);
            estudiante.start();

            // Simular llegadas aleatorias
            Thread.sleep(ThreadLocalRandom.current().nextInt(200, 500));
        }

        // Hilo para mostrar estadísticas periódicas
        Thread monitor = new Thread(new MonitorCentro());
        monitor.setDaemon(true);
        monitor.start();

        // Esperar a que todos los estudiantes terminen
        for (Thread t : listaEstudiantes) {
            t.join();
        }

        System.out.println("\n🔒 El Centro de Cómputo ha cerrado. Todas las sesiones finalizaron.");
    }

    static class Estudiante implements Runnable {
        private final int idEstudiante;
        private int equipoAsignado;

        public Estudiante(int id) {
            this.idEstudiante = id;
        }

        @Override
        public void run() {
            try {
                llegadaAlCentro();
                esperarEquipo();
                trabajar();
                salirDelCentro();
            } catch (InterruptedException e) {
                System.out.println("⚠️ Estudiante " + idEstudiante + " se fue por la interrupción");
                Thread.currentThread().interrupt();
            }
        }

        private void llegadaAlCentro() {
            String tiempo = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            System.out.println("[" + tiempo + "] 👋 Estudiante " + idEstudiante + " llega al centro");

            if (equipos.availablePermits() == 0) {
                estudiantesEnEspera.incrementAndGet();
                System.out.println("   ⏳ Estudiante " + idEstudiante + " espera equipo disponible");
            }
        }

        private void esperarEquipo() throws InterruptedException {
            equipos.acquire();

            if (estudiantesEnEspera.get() > 0) {
                estudiantesEnEspera.decrementAndGet();
            }

            equipoAsignado = numeroEquipo.getAndIncrement();
            if (equipoAsignado > TOTAL_EQUIPOS) {
                equipoAsignado = ((equipoAsignado - 1) % TOTAL_EQUIPOS) + 1;
            }

            String tiempo = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            System.out.println("[" + tiempo + "] 🪑 Estudiante " + idEstudiante +
                    " usa el equipo #" + equipoAsignado);
        }

        private void trabajar() throws InterruptedException {
            int tiempoUso = ThreadLocalRandom.current().nextInt(1500, 4000);

            System.out.println("   🖥 Estudiante " + idEstudiante + " trabajando... (" +
                    (tiempoUso / 1000.0) + "s)");

            Thread.sleep(tiempoUso);
        }

        private void salirDelCentro() {
            String tiempo = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            System.out.println("[" + tiempo + "] ✨ Estudiante " + idEstudiante +
                    " libera el Equipo #" + equipoAsignado);

            equipos.release();

            int totalAtendidos = estudiantesAtendidos.incrementAndGet();
            System.out.println("   📊 Total de estudiantes atendidos: " + totalAtendidos);
        }
    }

    static class MonitorCentro implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    Thread.sleep(2000);

                    String tiempo = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    System.out.println("\n📈 [" + tiempo + "] ESTADO ACTUAL:");
                    System.out.println("   🪑 Equipos en uso: " +
                            (TOTAL_EQUIPOS - equipos.availablePermits()) + "/" + TOTAL_EQUIPOS);
                    System.out.println("   ⏳ Estudiantes en espera: " + estudiantesEnEspera.get());
                    System.out.println("   ✅ Clientes atendidos: " + estudiantesAtendidos.get());
                    System.out.println();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}