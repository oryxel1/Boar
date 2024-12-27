package ac.boar.util;

public class MathUtil {
    public static float sign(final float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0;
        }

        return value == 0 ? value : value > 0 ? 1 : -1;
    }
}
