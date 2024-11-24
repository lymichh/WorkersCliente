package lab3_hilossockets;

/**
 *
 * @author lymich
 */

import java.util.List;

public class Utils {
   public static boolean isSorted(List<Long> vector) {
        for (int i = 0; i < vector.size() - 1; i++) {
            if (vector.get(i) > vector.get(i + 1)) {
                return false;
            }
        }
        return true;
    }
}