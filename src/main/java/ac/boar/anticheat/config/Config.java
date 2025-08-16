package ac.boar.anticheat.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// https://github.com/Mojang/bedrock-protocol-docs/blob/main/additional_docs/AntiCheatServer.properties
public record Config(@JsonProperty("player-rewind-history-size-ticks") int rewindHistory,
                     @JsonProperty("player-position-acceptance-threshold") float acceptanceThreshold,
                     @JsonProperty("max-tolerance-compensated-reach") float toleranceReach,
                     @JsonProperty("force-reach-java-parity") String reachJavaParityMode,
                     @JsonProperty("differ-till-alert") float alertThreshold,
                     @JsonProperty("disabled-checks") List<String> disabledChecks,
                     @JsonProperty("ignore-ghost-block") boolean ignoreGhostBlock,
                     @JsonProperty("max-acknowledgement-time") long maxAcknowledgementTime,
                     @JsonProperty("debug-mode") boolean debugMode) {
}
