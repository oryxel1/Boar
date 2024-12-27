package ac.boar.anticheat.compensated.cache;

import org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette;

public record BoarChunk(DataPalette[] sections, long id) {
}