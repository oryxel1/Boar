package ac.boar.anticheat.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import ac.boar.anticheat.player.data.PlayerData;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@RequiredArgsConstructor
public final class LatencyUtil {
    private final PlayerData player;
    @Getter
    private final List<Long> sentTransactions = new CopyOnWriteArrayList<>();
    private final Map<Long, List<Runnable>> map = new ConcurrentHashMap<>();

    public void addTransactionToQueue(long id, Runnable runnable) {
        if (id <= player.lastReceivedId) {
            runnable.run();
            return;
        }

        synchronized (this) {
            if (!this.map.containsKey(id)) {
                List<Runnable> list = new CopyOnWriteArrayList<>();
                list.add(runnable);
                this.map.put(id, list);
            } else {
                this.map.get(id).add(runnable);
            }
        }
    }

    public boolean confirmTransaction(long id) {
        if (!this.sentTransactions.contains(id) || id <= player.lastReceivedId) {
            return false;
        }

        for (final Long l : this.sentTransactions) {
            if (l > id) {
                break;
            }

            if (this.map.containsKey(id)) {
                this.map.remove(id).forEach(Runnable::run);
            }
            this.sentTransactions.remove(l);
        }

        player.lastReceivedId = id;
        player.lastResponseTime = System.currentTimeMillis();
        return true;
    }
}