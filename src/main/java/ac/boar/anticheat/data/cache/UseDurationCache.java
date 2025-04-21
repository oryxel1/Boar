package ac.boar.anticheat.data.cache;

import org.geysermc.geyser.item.Items;

import java.util.HashMap;
import java.util.Map;

public class UseDurationCache {
    private static final Map<Integer, Integer> USE_DURATIONS = new HashMap<>();

    public static void init() {
        USE_DURATIONS.put(Items.BOW.javaId(), 72000);
        USE_DURATIONS.put(Items.CROSSBOW.javaId(), 72000);
        USE_DURATIONS.put(Items.TRIDENT.javaId(), 72000);
        USE_DURATIONS.put(Items.BRUSH.javaId(), 200);
        USE_DURATIONS.put(Items.BUNDLE.javaId(), 200);
        USE_DURATIONS.put(Items.ENDER_EYE.javaId(), 0);
        USE_DURATIONS.put(Items.SPYGLASS.javaId(), 1200);

        // Auto generated, well you could get this from ItemData component too but eh.
        USE_DURATIONS.put(Items.APPLE.javaId(), 32);
        USE_DURATIONS.put(Items.MUSHROOM_STEW.javaId(), 32);
        USE_DURATIONS.put(Items.BREAD.javaId(), 32);
        USE_DURATIONS.put(Items.PORKCHOP.javaId(), 32);
        USE_DURATIONS.put(Items.COOKED_PORKCHOP.javaId(), 32);
        USE_DURATIONS.put(Items.GOLDEN_APPLE.javaId(), 32);
        USE_DURATIONS.put(Items.ENCHANTED_GOLDEN_APPLE.javaId(), 32);
        USE_DURATIONS.put(Items.MILK_BUCKET.javaId(), 32);
        USE_DURATIONS.put(Items.COD.javaId(), 32);
        USE_DURATIONS.put(Items.SALMON.javaId(), 32);
        USE_DURATIONS.put(Items.TROPICAL_FISH.javaId(), 32);
        USE_DURATIONS.put(Items.PUFFERFISH.javaId(), 32);
        USE_DURATIONS.put(Items.COOKED_COD.javaId(), 32);
        USE_DURATIONS.put(Items.COOKED_SALMON.javaId(), 32);
        USE_DURATIONS.put(Items.COOKIE.javaId(), 32);
        USE_DURATIONS.put(Items.MELON_SLICE.javaId(), 32);
        USE_DURATIONS.put(Items.DRIED_KELP.javaId(), 16);
        USE_DURATIONS.put(Items.BEEF.javaId(), 32);
        USE_DURATIONS.put(Items.COOKED_BEEF.javaId(), 32);
        USE_DURATIONS.put(Items.CHICKEN.javaId(), 32);
        USE_DURATIONS.put(Items.COOKED_CHICKEN.javaId(), 32);
        USE_DURATIONS.put(Items.ROTTEN_FLESH.javaId(), 32);
        USE_DURATIONS.put(Items.POTION.javaId(), 32);
        USE_DURATIONS.put(Items.SPIDER_EYE.javaId(), 32);
        USE_DURATIONS.put(Items.CARROT.javaId(), 32);
        USE_DURATIONS.put(Items.POTATO.javaId(), 32);
        USE_DURATIONS.put(Items.BAKED_POTATO.javaId(), 32);
        USE_DURATIONS.put(Items.POISONOUS_POTATO.javaId(), 32);
        USE_DURATIONS.put(Items.GOLDEN_CARROT.javaId(), 32);
        USE_DURATIONS.put(Items.PUMPKIN_PIE.javaId(), 32);
        USE_DURATIONS.put(Items.RABBIT.javaId(), 32);
        USE_DURATIONS.put(Items.COOKED_RABBIT.javaId(), 32);
        USE_DURATIONS.put(Items.RABBIT_STEW.javaId(), 32);
        USE_DURATIONS.put(Items.MUTTON.javaId(), 32);
        USE_DURATIONS.put(Items.COOKED_MUTTON.javaId(), 32);
        USE_DURATIONS.put(Items.CHORUS_FRUIT.javaId(), 32);
        USE_DURATIONS.put(Items.BEETROOT.javaId(), 32);
        USE_DURATIONS.put(Items.BEETROOT_SOUP.javaId(), 32);
        USE_DURATIONS.put(Items.SUSPICIOUS_STEW.javaId(), 32);
        USE_DURATIONS.put(Items.SWEET_BERRIES.javaId(), 32);
        USE_DURATIONS.put(Items.GLOW_BERRIES.javaId(), 32);
        USE_DURATIONS.put(Items.HONEY_BOTTLE.javaId(), 40);
        USE_DURATIONS.put(Items.OMINOUS_BOTTLE.javaId(), 32);
    }

    public static int getUseDuration(int javaId) {
        return USE_DURATIONS.getOrDefault(javaId, -1);
    }
}
