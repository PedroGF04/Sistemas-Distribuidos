import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IServidorLaboratorio extends Remote {
    // El cliente llama a esto. Se quedará bloqueado (en espera) si no hay equipos.
    int ocuparEquipo(String idEstudiante) throws RemoteException;
    
    // El cliente llama a esto cuando termina para liberar la PC.
    void liberarEquipo(int pc, String idEstudiante) throws RemoteException;
}