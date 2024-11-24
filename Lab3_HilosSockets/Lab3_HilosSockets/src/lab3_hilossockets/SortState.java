package lab3_hilossockets;

import java.io.Serializable;
import java.util.List;
/**
 *
 * @author lymich
 */
public class SortState implements Serializable {
    public List<Long> vector; // El vector a ordenar
    public int left;          // Índice inicial para el ordenamiento
    public int right;         // Índice final para el ordenamiento
    public int progress;
    
    public SortState(List<Long> vector, int left, int right, int progress) {
        this.vector = vector;
        this.left = left;
        this.right = right;
        this.progress = progress;
    }
}
