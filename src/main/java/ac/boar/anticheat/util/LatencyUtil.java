package ac.boar.anticheat.util;

import ac.boar.anticheat.check.api.Check;
import ac.boar.anticheat.check.api.impl.PingBasedCheck;
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
    private final Map<Long, Time> idToSentTime = new ConcurrentHashMap<>();
    private final Map<Long, List<Runnable>> idToTasks = new ConcurrentHashMap<>();

    private Time prevReceivedSentTime = new Time(-1, -1);
    public Time getLastSentTime() {
        return this.prevReceivedSentTime;
    }

    public boolean hasId(long id) {
        return this.sentStackLatency.contains(id);
    }

    public void addLatencyToQueue(long id) {
        this.sentStackLatency.add(id);
        this.idToSentTime.put(id, new Time(System.currentTimeMillis(), System.nanoTime()));
        onLatencySend();
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
        if (time < this.prevReceivedSentTime.ms()) {
            return;
        }

        final List<Long> removeIds = new ArrayList<>();

        long lastId = -1;
        for (long next : this.sentStackLatency) {
            final Time sentTime = this.idToSentTime.get(next);
            if (sentTime.ms() > time) {
                break;
            }
            this.idToSentTime.remove(next);
            if (sentTime.ms() > this.prevReceivedSentTime.ms()) {
                this.prevReceivedSentTime = sentTime;
            }

            final List<Runnable> tasks = this.idToTasks.remove(next);
            if (tasks != null) {
                tasks.forEach(Runnable::run);
            }

            onLatencyAccepted(next, sentTime);

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

            final Time sentTime = this.idToSentTime.remove(next);
            if (sentTime != null) {
                if (sentTime.ms() > this.prevReceivedSentTime.ms()) {
                    this.prevReceivedSentTime = sentTime;
                }

                onLatencyAccepted(next, sentTime);
            }

            removeIds.add(next);
        }

        this.sentStackLatency.removeAll(removeIds);

        player.receivedStackId.set(id);
        return true;
    }

    private void onLatencySend() {
        for (final Check check : this.player.getCheckHolder().values()) {
            if (!(check instanceof PingBasedCheck pingBasedCheck)) {
                continue;
            }

            pingBasedCheck.onLatencySend(player.sentStackId.get());
        }
    }

    private void onLatencyAccepted(long id, Time time) {
        for (final Check check : this.player.getCheckHolder().values()) {
            if (!(check instanceof PingBasedCheck pingBasedCheck)) {
                continue;
            }

            pingBasedCheck.onLatencyAccepted(id, time);
        }
    }

    public record Time(long ms, long ns) {}
}
