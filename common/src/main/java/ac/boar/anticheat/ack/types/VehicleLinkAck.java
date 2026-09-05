package ac.boar.anticheat.ack.types;

import ac.boar.anticheat.ack.Acknowledgment;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityLinkData;

public record VehicleLinkAck(EntityLinkData linkData) implements Acknowledgment {
}
