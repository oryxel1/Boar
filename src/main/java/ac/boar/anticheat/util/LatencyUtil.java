package ac.boar.anticheat.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import ac.boar.anticheat.player.data.PlayerData;

import java.util.Iterator;
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

        if (!this.map.containsKey(id)) {
            List<Runnable> list = new CopyOnWriteArrayList<>();
            list.add(runnable);
            this.map.put(id, list);
            return;
        }

        this.map.get(id).add(runnable);
    }

    public boolean confirmTransaction(long id) {
        if (!this.sentTransactions.contains(id) || id <= player.lastReceivedId) {
            return false;
        }

        for (Long l : this.sentTransactions) {
            if (l > id) {
                break;
            }

            this.sentTransactions.remove(l);
        }

        Iterator<Map.Entry<Long, List<Runnable>>> iterator = this.map.entrySet().iterator();

        Map.Entry<Long, List<Runnable>> entry;
        while (iterator.hasNext() && (entry = iterator.next()) != null && entry.getKey() <= id) {
            entry.getValue().forEach(Runnable::run);
            iterator.remove();
        }

        player.lastReceivedId = id;
        player.lastResponseTime = System.currentTimeMillis();
        return true;
    }
}