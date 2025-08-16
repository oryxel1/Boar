package ac.boar.anticheat.util;

import ac.boar.anticheat.player.BoarPlayer;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RequiredArgsConstructor
public final class LatencyUtil {
    private final BoarPlayer player;
    private final List<Long> sentStackLatency = new CopyOnWriteArrayList<>();
    private final Map<Long, Long> idToSentTime = new ConcurrentHashMap<>();
    private final Map<Long, List<Runnable>> idToTasks = new ConcurrentHashMap<>();

    private long prevReceivedSentTime = -1;
    public long getLastSentTime() {
        return this.prevReceivedSentTime;
    }

    public boolean hasId(long id) {
        return this.sentStackLatency.contains(id);
    }

    public void addLatencyToQueue(long id) {
        this.sentStackLatency.add(id);
        this.idToSentTime.put(id, System.currentTimeMillis());
    }

    public void addTaskToQueue(long id, Runnable runnable) {
        if (id <= player.receivedStackId.get()) {
            runnable.run();
            return;
        }

        synchronized (this) {
            if (!this.idToTasks.containsKey(id)) {
                this.idToTasks.put(id, new ArrayList<>());
            }

            this.idToTasks.get(id).add(runnable);
        }
    }

    public void confirmByTime(long time) {
        if (time < this.prevReceivedSentTime) {
            return;
        }

        final List<Long> removeIds = new ArrayList<>();

        long lastId = -1;
        for (long next : this.sentStackLatency) {
            final Long sentTime = this.idToSentTime.get(next);
            if (sentTime > time) {
                break;
            }
            this.idToSentTime.remove(next);
            this.prevReceivedSentTime = Math.max(this.prevReceivedSentTime, sentTime);

            final List<Runnable> tasks = this.idToTasks.remove(next);
            if (tasks != null) {
                tasks.forEach(Runnable::run);
            }

            removeIds.add(next);
            lastId = next;
        }

        this.sentStackLatency.removeAll(removeIds);

        if (lastId == -1 || lastId < player.receivedStackId.get()) {
            return;
        }

        player.receivedStackId.set(lastId);
    }

    public boolean confirmStackId(long id) {
        if (!hasId(id) || id <= player.receivedStackId.get()) {
            return false;
        }

        final List<Long> removeIds = new ArrayList<>();

        for (long next : this.sentStackLatency) {
            if (next > id) {
                break;
            }

            final List<Runnable> tasks = this.idToTasks.remove(next);
            if (tasks != null) {
                tasks.forEach(Runnable::run);
            }

            final Long sentTime = this.idToSentTime.remove(next);
            if (sentTime != null) {
                this.prevReceivedSentTime = Math.max(this.prevReceivedSentTime, sentTime);
            }

            removeIds.add(next);
        }

        this.sentStackLatency.removeAll(removeIds);

        player.receivedStackId.set(id);
        return true;
    }
}