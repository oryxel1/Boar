package ac.boar.anticheat.util;

import ac.boar.anticheat.player.BoarPlayer;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
public final class LatencyUtil {
    private final BoarPlayer player;
    private final List<Long> sentStackLatency = new CopyOnWriteArrayList<>();
    private final Map<Long, StackLatencyData> map = new ConcurrentHashMap<>();

    private final AtomicLong prevReceivedSentTime = new AtomicLong(-1);
    public long getLastSentTime() {
        return this.prevReceivedSentTime.get();
    }

    public void addLatencyToQueue(long id) {
        this.sentStackLatency.add(id);
        this.map.put(id, new StackLatencyData(System.nanoTime()));
    }

    public void addTaskToQueue(long id, Runnable runnable) {
        if (id <= player.receivedStackId.get()) {
            runnable.run();
            return;
        }

        synchronized (this) {
            if (!this.map.containsKey(id)) {
                this.map.put(id, new StackLatencyData(System.nanoTime())); // Shouldn't happen but just in case.
            }

            this.map.get(id).tasks.add(runnable);
        }
    }

    public boolean confirmStackId(long id) {
        if (!this.sentStackLatency.contains(id) || id <= player.receivedStackId.get()) {
            return false;
        }

        this.sentStackLatency.removeIf(l -> {
            if (this.map.containsKey(l) && l <= id) {
                final StackLatencyData latencyData = this.map.remove(l);
                latencyData.tasks.forEach(Runnable::run);

                this.prevReceivedSentTime.set(Math.max(this.prevReceivedSentTime.get(), latencyData.sentTime));
            }

            return l <= id;
        });

        player.receivedStackId.set(id);
        return true;
    }

    @RequiredArgsConstructor
    private static class StackLatencyData {
        private final long sentTime;
        private final List<Runnable> tasks = new ArrayList<>();
    }
}