package ac.boar.anticheat.compensated.cache;

import org.geysermc.mcprotocollib.protocol.data.game.chunk.DataPalette;


public record ChunkCache(DataPalette[] sections, long transactionId) {
}