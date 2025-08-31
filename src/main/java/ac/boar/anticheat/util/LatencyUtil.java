package ac.boar.anticheat.util;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.check.api.Check;
import ac.boar.anticheat.check.api.impl.PingBasedCheck;
import ac.boar.anticheat.player.BoarPlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@RequiredArgsConstructor
public final class LatencyUtil {
    private final BoarPlayer player;
    private final Queue<Long> sentQueue = new ConcurrentLinkedQueue<>();
    private final Map<Long, Time> idToSentTime = new ConcurrentHashMap<>();
    private final Map<Long, List<Runnable>> idToTasks = new ConcurrentHashMap<>();
    @Getter
    // Give the player a bit of a leniency for their first latency.
    private long lastRespondTime = System.currentTimeMillis() + Boar.getConfig().maxLatencyWait();

    private Time prevReceivedSentTime = new Time(-1, -1);

    public Time getLastSentTime() {
        return this.prevReceivedSentTime;
    }

    public boolean hasId(long id) {
        return this.sentQueue.contains(id);
    }

    public void addLatencyToQueue(long id) {
        this.sentQueue.add(id);
        this.idToSentTime.put(id, new Time(System.currentTimeMillis(), System.nanoTime()));
        onLatencySend();
    }

    public void addTaskToQueue(long id, Runnable runnable) {
        if (id <= player.receivedStackId.get()) {
            runnable.run();
            return;
        }

        this.idToTasks.computeIfAbsent(id, k -> new ArrayList<>()).add(runnable);
    }

    public void confirmByTime(long time) {
        if (time < this.prevReceivedSentTime.ms()) {
            return;
        }

        long lastId = -1;
        while (true) {
            Long next = this.sentQueue.peek();
            if (next == null) {
                break;
            }

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

            this.sentQueue.poll();
            lastId = next;
        }

        if (lastId == -1 || lastId < player.receivedStackId.get()) {
            return;
        }

        if (System.currentTimeMillis() - this.prevReceivedSentTime.ms() >= Boar.getConfig().maxLatencyWait()) {
            player.kick("Timed out.");
            return;
        }

        this.lastRespondTime = System.currentTimeMillis();
        player.receivedStackId.set(lastId);
    }

    public boolean confirmStackId(long id) {
        if (!hasId(id) || id <= player.receivedStackId.get()) {
            return false;
        }

        while (true) {
            Long next = this.sentQueue.peek();
            if (next == null || next > id) {
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

            this.sentQueue.poll();
        }

        if (System.currentTimeMillis() - this.prevReceivedSentTime.ms() >= Boar.getConfig().maxLatencyWait()) {
            player.kick("Timed out.");
            return true;
        }

//        System.out.println("Receive: " + (System.currentTimeMillis() - this.prevReceivedSentTime.ms()));

        this.lastRespondTime = System.currentTimeMillis();
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

    public record Time(long ms, long ns) {
    }
}
