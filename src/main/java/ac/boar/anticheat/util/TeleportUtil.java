package ac.boar.anticheat.util;

import ac.boar.anticheat.RewindSetting;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.util.math.Vec3f;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.PredictionType;
import org.cloudburstmc.protocol.bedrock.packet.CorrectPlayerMovePredictionPacket;
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@RequiredArgsConstructor
@Getter
public final class TeleportUtil {
    private final BoarPlayer player;
    private final Queue<TeleportCache> teleportQueue = new ConcurrentLinkedQueue<>();
    public Vec3f lastKnowValid = Vec3f.ZERO;

    public void rewind(final Vec3f velocity) {
        final CorrectPlayerMovePredictionPacket packet = new CorrectPlayerMovePredictionPacket();
        packet.setPosition(this.lastKnowValid.add(velocity).toVector3f());
        packet.setOnGround(player.onGround);
        packet.setTick(player.tick);
        packet.setDelta(player.engine.applyEndOfTick(velocity).toVector3f());
        packet.setPredictionType(PredictionType.PLAYER);

        ChatUtil.alert("Attempted to rewind player on tick=" + player.tick);

        this.addRewindToQueue(this.lastKnowValid.add(velocity), true);
        this.player.cloudburstSession.sendPacketImmediately(packet);
    }

    public void addTeleportToQueue(Vec3f vec3f, boolean immediate) {
        this.player.sendTransaction(immediate);

        final TeleportCache teleportCache = new TeleportCache(vec3f, this.player.lastSentId);
        this.teleportQueue.add(teleportCache);

        this.lastKnowValid = new Vec3f(vec3f.toVector3f());
    }

    public void addRewindToQueue(final Vec3f vec3f, boolean immediate) {
        this.addTeleportToQueue(vec3f, immediate);

        if (RewindSetting.REWIND_INFO_DEBUG) {
            final long id = player.lastSentId;
            player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> ChatUtil.alert("Player accepted rewind with transaction id=" + id));
        }
    }

    public void setbackTo(Vec3f vec3f) {
        if (teleportInQueue()) {
            return;
        }

        // Server won't know about this if we sent it like this, well they don't need to anyway.
        // As long as we handle thing correctly, it won't be a problem
        // If we do not however, server will likely set player back for 'Moved too quickly'
        // Also this (prob) going to prevent respawn tp, if there is one.

        final MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId(player.runtimeEntityId);
        movePlayerPacket.setPosition(Vector3f.from(vec3f.x, vec3f.y, vec3f.z));
        movePlayerPacket.setRotation(player.bedrockRotation);
        movePlayerPacket.setOnGround(player.onGround);
        movePlayerPacket.setMode(MovePlayerPacket.Mode.TELEPORT);
        movePlayerPacket.setTeleportationCause(MovePlayerPacket.TeleportationCause.BEHAVIOR);
        this.addTeleportToQueue(vec3f, true);

        this.player.cloudburstSession.sendPacketImmediately(movePlayerPacket);
    }

    public boolean teleportInQueue() {
        return !this.teleportQueue.isEmpty();
    }

    @RequiredArgsConstructor
    @Getter
    @Setter
    public static class TeleportCache {
        private final Vec3f position;
        private final long transactionId;
    }
}