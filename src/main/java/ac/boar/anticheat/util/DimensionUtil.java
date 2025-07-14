package ac.boar.anticheat.util;

import org.geysermc.geyser.level.BedrockDimension;

public class DimensionUtil {
    public static BedrockDimension dimensionFromId(int id) {
        return id == BedrockDimension.OVERWORLD_ID ? BedrockDimension.OVERWORLD : id == BedrockDimension.DEFAULT_NETHER_ID ? BedrockDimension.THE_NETHER : BedrockDimension.THE_END;
    }
}
