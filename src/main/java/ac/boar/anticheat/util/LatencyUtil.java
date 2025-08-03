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

    public void addLatencyToQueue(long id) {
        this.sentStackLatency.add(id);
        this.idToSentTime.put(id, System.nanoTime());
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

    public boolean confirmStackId(long id) {
        if (!this.sentStackLatency.contains(id) || id <= player.receivedStackId.get()) {
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