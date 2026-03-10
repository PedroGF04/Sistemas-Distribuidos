import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Semaphore;

public class ServidorGrafico extends JFrame {
    private final int PUERTO;
    private final int TOTAL_EQUIPOS = 5;
    private final Semaphore semaforo = new Semaphore(TOTAL_EQUIPOS, true);
    private final boolean[] equiposOcupados = new boolean[TOTAL_EQUIPOS];
    private final String archivoReporte;
    
    // Componentes de la Interfaz
    private JLabel[] etiquetasEquipos = new JLabel[TOTAL_EQUIPOS];
    private JLabel lblEspera, lblAtendidos;
    private int esperaCount = 0, atendidosCount = 0;

    public ServidorGrafico(int puerto) {
        this.PUERTO = puerto;
        this.archivoReporte = "Reporte_Trailblazer_Puerto_" + puerto + ".csv";
        prepararArchivoReporte();
        configurarVentana();
        new Thread(this::iniciarServidor).start(); 
    }

    private void prepararArchivoReporte() {
        File archivo = new File(archivoReporte);
        if (!archivo.exists()) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(archivoReporte, true))) {
                // Escribimos los encabezados del CSV
                pw.println("Fecha_Hora,ID_Estudiante,Equipo_Asignado,Tiempo_Uso_Segundos");
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    // 🔒 SECCIÓN CRÍTICA: Solo un hilo puede escribir en el archivo a la vez
    private synchronized void guardarEnReporte(String id, int equipo, long tiempoUsoMs) {
        String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        long segundos = tiempoUsoMs / 1000;
        
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivoReporte, true))) {
            pw.println(fechaHora + "," + id + ",PC-" + equipo + "," + segundos);
        } catch (IOException e) {
            System.err.println("Error al guardar reporte: " + e.getMessage());
        }
    }

    private void configurarVentana() {
        setTitle("🖥️ Centro 'Trailblazer' - Puerto: " + PUERTO);
        setSize(500, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel panelEquipos = new JPanel(new GridLayout(1, 5, 10, 10));
        panelEquipos.setBorder(BorderFactory.createTitledBorder("Estado de los Equipos"));
        for (int i = 0; i < TOTAL_EQUIPOS; i++) {
            etiquetasEquipos[i] = new JLabel("PC " + (i + 1), SwingConstants.CENTER);
            etiquetasEquipos[i].setOpaque(true);
            etiquetasEquipos[i].setBackground(Color.GREEN);
            etiquetasEquipos[i].setBorder(BorderFactory.createLineBorder(Color.BLACK));
            panelEquipos.add(etiquetasEquipos[i]);
        }

        JPanel panelStats = new JPanel(new GridLayout(2, 1));
        lblEspera = new JLabel("⏳ Estudiantes en fila: 0", SwingConstants.CENTER);
        lblAtendidos = new JLabel("✅ Total atendidos: 0", SwingConstants.CENTER);
        panelStats.add(lblEspera);
        panelStats.add(lblAtendidos);

        add(panelEquipos, BorderLayout.CENTER);
        add(panelStats, BorderLayout.SOUTH);
        setVisible(true);
    }

    private void actualizarUI(int equipo, String id, boolean ocupado) {
        SwingUtilities.invokeLater(() -> {
            if (equipo > 0) {
                etiquetasEquipos[equipo - 1].setBackground(ocupado ? Color.RED : Color.GREEN);
                etiquetasEquipos[equipo - 1].setText(ocupado ? "<html><center>PC " + equipo + "<br>" + id + "</center></html>" : "PC " + equipo);
            }
            lblEspera.setText("⏳ Estudiantes en fila: " + esperaCount);
            lblAtendidos.setText("✅ Total atendidos: " + atendidosCount);
        });
    }

    private void iniciarServidor() {
        try (ServerSocket server = new ServerSocket(PUERTO)) {
            while (true) {
                Socket cliente = server.accept();
                new Thread(new Manejador(cliente)).start();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    class Manejador implements Runnable {
        private Socket s;
        public Manejador(Socket s) { this.s = s; }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                 PrintWriter out = new PrintWriter(s.getOutputStream(), true)) {
                
                String id = in.readLine();
                synchronized(ServidorGrafico.this) { esperaCount++; }
                actualizarUI(-1, null, false);

                semaforo.acquire(); 

                int miPC = -1;
                synchronized(ServidorGrafico.this) {
                    esperaCount--;
                    for(int i=0; i<TOTAL_EQUIPOS; i++) {
                        if(!equiposOcupados[i]) { equiposOcupados[i]=true; miPC=i+1; break; }
                    }
                }

                actualizarUI(miPC, id, true);
                out.println(miPC); 

                // ⏱️ INICIA EL CRONÓMETRO DE USO
                long inicioCrono = System.currentTimeMillis();

                in.readLine(); // Espera a que el cliente envíe "LIBERAR"

                // ⏱️ DETIENE EL CRONÓMETRO
                long finCrono = System.currentTimeMillis();
                long tiempoTotal = finCrono - inicioCrono;

                // Guardar en el archivo de forma segura
                guardarEnReporte(id, miPC, tiempoTotal);

                synchronized(ServidorGrafico.this) {
                    equiposOcupados[miPC-1] = false;
                    atendidosCount++;
                }
                
                actualizarUI(miPC, null, false);
                semaforo.release();

            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public static void main(String[] args) {
        int p = (args.length > 0) ? Integer.parseInt(args[0]) : 5000;
        new ServidorGrafico(p);
    }
}