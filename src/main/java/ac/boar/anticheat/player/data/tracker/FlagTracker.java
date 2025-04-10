package ac.boar.anticheat.player.data.tracker;

import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;

import java.util.EnumSet;
import java.util.Set;

public final class FlagTracker {
    private final Set<EntityFlag> flags = EnumSet.noneOf(EntityFlag.class);

    public void clear() {
        this.flags.clear();
    }

    public void set(final Set<EntityFlag> flags) {
        this.clear();
        this.flags.addAll(flags);
    }

    public void set(final EntityFlag flag, boolean value) {
        this.flags.remove(flag);
        if (value) {
            this.flags.add(flag);
        }
    }

    public boolean has(final EntityFlag flag) {
        return flags.contains(flag);
    }

    public EnumSet<EntityFlag> cloneFlags() {
        final EnumSet<EntityFlag> flags = EnumSet.noneOf(EntityFlag.class);
        flags.addAll(this.flags);

        return flags;
    }
}
