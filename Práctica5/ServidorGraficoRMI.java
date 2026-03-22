import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class ServidorGraficoRMI extends JFrame implements IServidorLaboratorio {
    private final int PUERTO;
    private final int TOTAL_EQUIPOS = 5;
    private final Semaphore semaforo = new Semaphore(TOTAL_EQUIPOS, true);
    private final boolean[] equiposOcupados = new boolean[TOTAL_EQUIPOS];
    private final String archivoReporte;
    
    // Para medir el tiempo en RMI (Asocia PC -> Tiempo de inicio)
    private final Map<Integer, Long> tiemposInicio = new ConcurrentHashMap<>();
    
    private JLabel[] etiquetasEquipos = new JLabel[TOTAL_EQUIPOS];
    private JLabel lblEspera, lblAtendidos;
    private int esperaCount = 0, atendidosCount = 0;

    public ServidorGraficoRMI(int puerto) {
        this.PUERTO = puerto;
        this.archivoReporte = "Reporte_Trailblazer_RMI_" + puerto + ".csv";
        prepararArchivoReporte();
        configurarVentana();
        iniciarServidorRMI(); 
    }

    private void iniciarServidorRMI() {
        try {
            // Exportamos este objeto para que pueda recibir llamadas remotas
            IServidorLaboratorio stub = (IServidorLaboratorio) UnicastRemoteObject.exportObject(this, 0);
            
            // Creamos el registro RMI en el puerto especificado
            Registry registry = LocateRegistry.createRegistry(PUERTO);
            
            // Registramos nuestro objeto con un nombre para que el cliente lo encuentre
            registry.rebind("NodoLaboratorio", stub);
            
            System.out.println("✅ Servidor RMI listo y escuchando en el puerto " + PUERTO);
        } catch (Exception e) {
            System.err.println("❌ Error al iniciar RMI: " + e.getMessage());
        }
    }

    // --- MÉTODOS REMOTOS (RMI) ---

    @Override
    public int ocuparEquipo(String idEstudiante) throws RemoteException {
        try {
            synchronized(this) { esperaCount++; }
            actualizarUI(-1, null, false);

            semaforo.acquire(); // Se bloquea aquí si no hay PCs

            int miPC = -1;
            synchronized(this) {
                esperaCount--;
                for(int i = 0; i < TOTAL_EQUIPOS; i++) {
                    if(!equiposOcupados[i]) { 
                        equiposOcupados[i] = true; 
                        miPC = i + 1; 
                        break; 
                    }
                }
            }

            // Iniciamos el cronómetro interno para esta PC
            tiemposInicio.put(miPC, System.currentTimeMillis());
            
            actualizarUI(miPC, idEstudiante, true);
            return miPC;

        } catch (InterruptedException e) {
            throw new RemoteException("Error de concurrencia en el servidor", e);
        }
    }

    @Override
    public void liberarEquipo(int pc, String idEstudiante) throws RemoteException {
        long finCrono = System.currentTimeMillis();
        long inicioCrono = tiemposInicio.getOrDefault(pc, finCrono);
        long tiempoTotal = finCrono - inicioCrono;

        guardarEnReporte(idEstudiante, pc, tiempoTotal);

        synchronized(this) {
            equiposOcupados[pc - 1] = false;
            atendidosCount++;
        }
        
        tiemposInicio.remove(pc);
        actualizarUI(pc, null, false);
        semaforo.release();
    }

    private void prepararArchivoReporte() {
        File archivo = new File(archivoReporte);
        if (!archivo.exists()) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(archivoReporte, true))) {
                pw.println("Fecha_Hora,ID_Estudiante,Equipo_Asignado,Tiempo_Uso_Segundos");
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private synchronized void guardarEnReporte(String id, int equipo, long tiempoUsoMs) {
        String fechaHora = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        long segundos = tiempoUsoMs / 1000;
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivoReporte, true))) {
            pw.println(fechaHora + "," + id + ",PC-" + equipo + "," + segundos);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void configurarVentana() {
        setTitle("🖥️ Centro 'Trailblazer' (RMI) - Puerto: " + PUERTO);
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

    public static void main(String[] args) {
        int p = (args.length > 0) ? Integer.parseInt(args[0]) : 5000;
        new ServidorGraficoRMI(p);
    }
}