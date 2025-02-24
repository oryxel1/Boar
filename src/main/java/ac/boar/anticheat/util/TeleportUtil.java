package ac.boar.anticheat.util;

import ac.boar.anticheat.GlobalSetting;
import ac.boar.anticheat.data.teleport.RewindData;
import ac.boar.anticheat.data.teleport.RewindTeleportCache;
import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.data.teleport.TeleportCache;
import ac.boar.anticheat.util.math.Vec3;
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
    public Vec3 lastKnowValid = Vec3.ZERO;

    private final Map<Long, Vec3> savedKnowValid = new ConcurrentSkipListMap<>();

    public void rewind(long tick, final Vec3 before, final Vec3 after) {
        this.rewind(new RewindData(tick, before, after));
    }

    public void rewind(final RewindData data) {
        if (this.teleportInQueue()) {
            return;
        }

        final Vec3 beforeVelocity = this.savedKnowValid.getOrDefault(data.tick(), this.lastKnowValid);
        final Vector3f position = beforeVelocity.add(data.after()).toVector3f();
        Vec3 endOfTick = data.after().clone();

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
            if (GlobalSetting.REWIND_INFO_DEBUG) {
                ChatUtil.alert("Can't find tick=" + tick);
            }

            tick = Math.max(0, player.tick - 1);
        }

        final boolean onGround = data.before().y != data.after().y && data.before().y < 0;

        final CorrectPlayerMovePredictionPacket packet = new CorrectPlayerMovePredictionPacket();
        packet.setPosition(position);
        packet.setOnGround(onGround);
        packet.setTick(tick);
        packet.setDelta(endOfTick.toVector3f());
        packet.setPredictionType(PredictionType.PLAYER);

        this.addRewindToQueue(tick, beforeVelocity, new Vec3(position), endOfTick, onGround, true);
        this.player.cloudburstSession.sendPacketImmediately(packet);

        if (GlobalSetting.REWIND_INFO_DEBUG) {
            ChatUtil.alert("Attempted to rewind player to tick=" + tick + ", current tick=" + player.tick);
            ChatUtil.alert("Rewind info: tick=" + (tick) + ", onGround=" + onGround + ", velocity=" + data.after().toVector3f().toString());
        }
    }

    public void addTeleportToQueue(Vec3 vec3, boolean respawn, boolean immediate) {
        this.player.sendTransaction(immediate);

        final TeleportCache teleportCache = new TeleportCache(vec3, this.player.lastSentId);
        teleportCache.setRespawnTeleport(respawn);
        this.teleportQueue.add(teleportCache);

        this.lastKnowValid = vec3.clone();
    }

    public void addRewindToQueue(final long tick, final Vec3 last, final Vec3 vec3, final Vec3 eot, final boolean onGround, boolean immediate) {
        this.player.sendTransaction(immediate);

        final RewindTeleportCache teleportCache = new RewindTeleportCache(tick, last, vec3, eot, onGround, this.player.lastSentId);
        this.rewindTeleportCaches.add(teleportCache);

        if (GlobalSetting.REWIND_INFO_DEBUG) {
            final long id = player.lastSentId;
            player.latencyUtil.addTransactionToQueue(player.lastSentId, () -> ChatUtil.alert("Player accepted rewind with transaction id=" + id));
        }
    }

    public void setbackTo(final TeleportCache cache) {
        setbackTo(cache.getPosition());
    }

    public void setbackTo(final Vec3 vec3) {
        if (teleportInQueue()) {
            return;
        }

        // Server won't know about this if we sent it like this, well they don't need to anyway.
        // As long as we handle thing correctly, it won't be a problem
        // If we do not however, server will likely set player back for 'Moved too quickly'
        // Also this (prob) going to prevent respawn tp, if there is one.

        final MovePlayerPacket movePlayerPacket = new MovePlayerPacket();
        movePlayerPacket.setRuntimeEntityId(player.runtimeEntityId);
        movePlayerPacket.setPosition(Vector3f.from(vec3.x, vec3.y, vec3.z));
        movePlayerPacket.setRotation(player.bedrockRotation);
        movePlayerPacket.setOnGround(player.onGround);
        movePlayerPacket.setMode(MovePlayerPacket.Mode.TELEPORT);
        movePlayerPacket.setTeleportationCause(MovePlayerPacket.TeleportationCause.BEHAVIOR);
        this.addTeleportToQueue(vec3,false, true);

        this.player.cloudburstSession.sendPacketImmediately(movePlayerPacket);
    }

    public boolean teleportInQueue() {
        return !this.teleportQueue.isEmpty() || !this.rewindTeleportCaches.isEmpty();
    }

    public void setLastKnowValid(long tick, Vec3 lastKnowValid) {
        this.savedKnowValid.put(tick, lastKnowValid);
        this.lastKnowValid = lastKnowValid;

        final Iterator<Map.Entry<Long, Vec3>> iterator = this.savedKnowValid.entrySet().iterator();
        while (iterator.hasNext() && this.savedKnowValid.size() > GlobalSetting.REWIND_HISTORY_SIZE_TICKS) {
            iterator.next();
            iterator.remove();
        }

        final Iterator<Map.Entry<Long, PlayerAuthInputPacket>> iterator1 = this.player.savedInputMap.entrySet().iterator();
        while (iterator1.hasNext() && this.player.savedInputMap.size() > GlobalSetting.REWIND_HISTORY_SIZE_TICKS) {
            iterator1.next();
            iterator1.remove();
        }

    }
}