package ac.boar.anticheat.ack.types;

import ac.boar.anticheat.ack.Acknowledgment;

public record AddEntityAck(long runtimeEntityId) implements Acknowledgment {
}
