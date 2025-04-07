package ac.boar.anticheat.util;

import ac.boar.anticheat.GlobalSetting;
import ac.boar.anticheat.data.PredictionData;
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

    private final Map<Long, PlayerAuthInputPacket> authInputHistory = new ConcurrentSkipListMap<>();
    private final Map<Long, RewindData> rewindHistory = new ConcurrentSkipListMap<>();

    public void rewind(long tick) {
        this.rewind(this.rewindHistory.getOrDefault(tick, new RewindData(player.tick, this.lastKnowValid, player.predictionResult)));
    }

    public void rewind(long tick, PredictionData data) {
        Vec3 position = this.lastKnowValid;
        if (this.rewindHistory.containsKey(tick)) {
            position = this.rewindHistory.get(tick).position();
        } else {
            tick = player.tick;
        }

        this.rewind(new RewindData(tick, position, data));
    }

    public void rewind(final RewindData rewind) {
        if (this.teleportInQueue()) {
            return;
        }

        final PredictionData data = rewind.data();
        final Vec3 position = rewind.position().add(data.after());
        final boolean onGround = data.before().y != data.after().y && data.before().y < 0;

        final long tick = rewind.tick();
        final CorrectPlayerMovePredictionPacket packet = new CorrectPlayerMovePredictionPacket();
        packet.setPosition(position.toVector3f());
        packet.setOnGround(onGround);
        packet.setTick(tick);
        packet.setDelta(data.tickEnd().toVector3f());
        packet.setPredictionType(PredictionType.PLAYER);

        this.addRewindToQueue(tick, position, data.tickEnd(), onGround, true);
        this.player.cloudburstDownstream.sendPacketImmediately(packet);

        if (GlobalSetting.REWIND_INFO_DEBUG) {
            ChatUtil.alert("Attempted to rewind player to tick=" + tick + ", current tick=" + player.tick);
            ChatUtil.alert("Rewind info: tick=" + tick + ", onGround=" + onGround + ", velocity=" + data.after().toVector3f().toString());
        }
    }

    public void addTeleportToQueue(Vec3 Vec3, boolean respawn, boolean immediate) {
        this.player.sendLatencyStack(immediate);

        final TeleportCache teleportCache = new TeleportCache(Vec3, this.player.sentStackId.get());
        teleportCache.setRespawnTeleport(respawn);
        this.teleportQueue.add(teleportCache);

        this.lastKnowValid = Vec3.clone();
    }

    public void addRewindToQueue(final long tick, final Vec3 vec3, final Vec3 velocity, final boolean groundCollision, boolean immediate) {
        this.player.sendLatencyStack(immediate);

        final RewindTeleportCache teleportCache = new RewindTeleportCache(tick, vec3, velocity, groundCollision, this.player.sentStackId.get());
        this.rewindTeleportCaches.add(teleportCache);
    }

    public void setbackTo(final TeleportCache cache) {
        setbackTo(cache.getPosition());
    }

    public void setbackTo(final Vec3 Vec3) {
        if (teleportInQueue()) {
            return;
        }

        // Server won't know about this if we sent it like this, well they don't need to anyway.
        // As long as we handle thing correctly, it won't be a problem
        // If we do not however, server will likely set player back for 'Moved too quickly'
        // Also this (prob) going to prevent respawn tp, if there is one.

        final MovePlayerPacket packet = new MovePlayerPacket();
        packet.setRuntimeEntityId(player.runtimeEntityId);
        packet.setPosition(Vector3f.from(Vec3.x, Vec3.y, Vec3.z));
        packet.setRotation(player.bedrockRotation);
        packet.setOnGround(player.groundCollision);
        packet.setMode(MovePlayerPacket.Mode.TELEPORT);
        packet.setTeleportationCause(MovePlayerPacket.TeleportationCause.BEHAVIOR);
        this.addTeleportToQueue(Vec3,false, true);

        this.player.cloudburstDownstream.sendPacketImmediately(packet);
    }

    public boolean teleportInQueue() {
        return !this.teleportQueue.isEmpty() || !this.rewindTeleportCaches.isEmpty();
    }

    public void cacheKnowValid(long tick, Vec3 lastKnowValid) {
        this.rewindHistory.put(tick, new RewindData(tick, this.lastKnowValid.clone(), player.predictionResult));
        this.lastKnowValid = lastKnowValid;

        final Iterator<Map.Entry<Long, RewindData>> iterator = this.rewindHistory.entrySet().iterator();
        while (iterator.hasNext() && this.rewindHistory.size() > GlobalSetting.REWIND_HISTORY_SIZE_TICKS) {
            iterator.next();
            iterator.remove();
        }

        final Iterator<Map.Entry<Long, PlayerAuthInputPacket>> iterator1 = this.authInputHistory.entrySet().iterator();
        while (iterator1.hasNext() && this.authInputHistory.size() > GlobalSetting.REWIND_HISTORY_SIZE_TICKS) {
            iterator1.next();
            iterator1.remove();
        }

    }
}