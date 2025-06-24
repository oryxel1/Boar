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
    private final Map<Long, StackLatencyData> map = new ConcurrentHashMap<>();

    public void addLatencyToQueue(long id) {
        this.sentStackLatency.add(id);
    }

    public void addTaskToQueue(long id, Runnable runnable) {
        if (id <= player.receivedStackId.get()) {
            runnable.run();
            return;
        }

        synchronized (this) {
            if (!this.map.containsKey(id)) {
                this.map.put(id, new StackLatencyData(id));
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
                this.map.remove(l).tasks.forEach(Runnable::run);
            }

            return l <= id;
        });

        player.receivedStackId.set(id);
        return true;
    }

    @RequiredArgsConstructor
    private static class StackLatencyData {
        private final long stackId;
        private final List<Runnable> tasks = new ArrayList<>();
    }
}