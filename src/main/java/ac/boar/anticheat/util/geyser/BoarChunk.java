package ac.boar.anticheat.util.geyser;

import org.geysermc.mcprotocollib.protocol.data.game.level.block.BlockEntityInfo;

import java.util.List;

public record BoarChunk(BoarChunkSection[] sections, List<BlockEntityInfo> blockEntities) {
}
