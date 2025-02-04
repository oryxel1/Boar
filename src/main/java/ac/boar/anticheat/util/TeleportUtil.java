package ac.boar.anticheat.util;

import ac.boar.anticheat.RewindSetting;
import ac.boar.anticheat.data.teleport.RewindData;
import ac.boar.anticheat.data.teleport.RewindTeleportCache;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.data.teleport.TeleportCache;
import ac.boar.anticheat.util.math.Box;
import ac.boar.anticheat.util.math.Vec3f;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.protocol.bedrock.data.PredictionType;
import org.cloudburstmc.protocol.bedrock.packet.CorrectPlayerMovePredictionPacket;
import org.cloudburstmc.protocol.bedrock.packet.MovePlayerPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;

import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;

@RequiredArgsConstructor
@Getter
public final class TeleportUtil {
    private final BoarPlayer player;
    private final Queue<TeleportCache> teleportQueue = new ConcurrentLinkedQueue<>();
    private final Queue<RewindTeleportCache> rewindTeleportCaches = new ConcurrentLinkedQueue<>();
    public Vec3f lastKnowValid = Vec3f.ZERO;
    public RewindData prevRewind, prevRewindTeleport;

    private final Map<Long, Vec3f> savedKnowValid = new ConcurrentSkipListMap<>();

    public void rewind(long tick, final Vec3f before, final Vec3f after) {
        this.rewind(new RewindData(tick, before, after));
    }

    public void rewind(final RewindData data) {
        if (this.teleportInQueue()) {
            return;
        }

        final Vec3f beforeVelocity = this.savedKnowValid.getOrDefault(data.tick(), this.lastKnowValid).add(0, Box.MAX_TOLERANCE_ERROR, 0);
        final Vector3f position = beforeVelocity.add(data.after()).toVector3f();
        Vec3f endOfTick = data.after().clone();

        if (data.before().y != data.after().y) {
            endOfTick.y = 0;
        }

        if (data.before().x != data.after().x) {
            endOfTick.x = 0;
        }

        if (data.before().z != data.after().z) {
            endOfTick.z = 0;
        }

        if (player.engine != null) {
            endOfTick = player.engine.applyEndOfTick(endOfTick);
        }

        long tick = data.tick();
        if (!this.savedKnowValid.containsKey(tick)) {
            if (RewindSetting.REWIND_INFO_DEBUG) {
                ChatUtil.alert("Can't find tick=" + tick);
            }

            tick = Math.max(0, player.tick - 1);
        }

        final boolean onGround = data.before().y != data.after().y && data.before().y < 0;

        final CorrectPlayerMovePredictionPacket packet = new CorrectPlayerMovePredictionPacket();
        packet.setPosition(position);
        packet.setOnGround(onGround);
        packet.setTick(tick + 1);
        packet.setDelta(endOfTick.toVector3f());
        packet.setPredictionType(PredictionType.PLAYER);

        this.addRewindToQueue(tick + 1, beforeVelocity, new Vec3f(position), endOfTick, onGround, true);
        this.player.cloudburstSession.sendPacketImmediately(packet);

        if (RewindSetting.REWIND_INFO_DEBUG) {
            ChatUtil.alert("Attempted to rewind player to tick=" + tick + ", current tick=" + player.tick);
            ChatUtil.alert("Rewind info: tick=" + (tick + 1) + ", onGround=" + onGround + ", velocity=" + data.after().toVector3f().toString());
        }

        this.prevRewind = data;
    }

    public void addTeleportToQueue(RewindData data, Vec3f vec3f, boolean immediate) {
        this.player.sendTransaction(immediate);

        final TeleportCache teleportCache = new TeleportCache(vec3f, this.player.lastSentId);
        teleportCache.setData(data);
        this.teleportQueue.add(teleportCache);

        this.lastKnowValid = vec3f.clone();
    }

    public void addRewindToQueue(final long tick, final Vec3f last, final Vec3f vec3f, final Vec3f eot, final boolean onGround, boolean immediate) {
        this.player.sendTransaction(immediate);

        final RewindTeleportCache teleportCache = new RewindTeleportCache(tick, last, vec3f, eot, onGround, this.player.lastSentId);
        this.rewindTeleportCaches.add(teleportCache);

        if (RewindSetting.REWIND_INFO_DEBUG) {
            final long id = player.lastSentId;
            player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> ChatUtil.alert("Player accepted rewind with transaction id=" + id));
        }
    }

    public void setbackTo(final TeleportCache cache) {
        setbackTo(cache.getData(), cache.getPosition());
    }

    public void setbackTo(final RewindData data, final Vec3f vec3f) {
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
        this.addTeleportToQueue(data, vec3f, true);

        this.player.cloudburstSession.sendPacketImmediately(movePlayerPacket);
    }

    public boolean teleportInQueue() {
        return !this.teleportQueue.isEmpty() || !this.rewindTeleportCaches.isEmpty();
    }

    public void setLastKnowValid(long tick, Vec3f lastKnowValid) {
        this.savedKnowValid.put(tick, lastKnowValid);
        this.lastKnowValid = lastKnowValid;

        // ChatUtil.alert("Saved " + tick + " tick position!");

        final Iterator<Map.Entry<Long, Vec3f>> iterator = this.savedKnowValid.entrySet().iterator();
        while (iterator.hasNext() && this.savedKnowValid.size() > RewindSetting.REWIND_HISTORY_SIZE_TICKS) {
            Map.Entry<Long, Vec3f> entry = iterator.next();
            iterator.remove();

            // ChatUtil.alert("Removed entry: " + entry.getKey());
        }

        final Iterator<Map.Entry<Long, PlayerAuthInputPacket>> iterator1 = this.player.savedInputMap.entrySet().iterator();
        while (iterator1.hasNext() && this.player.savedInputMap.size() > RewindSetting.REWIND_HISTORY_SIZE_TICKS) {
            iterator1.next();
            iterator1.remove();
        }

    }
}