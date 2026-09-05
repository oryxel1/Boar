package ac.boar.mappings.entity;

import ac.boar.anticheat.util.Reference;
import ac.boar.anticheat.util.ReferencePopulator;

public final class EntityTypes {
    static final ReferencePopulator<EntityType> POPULATOR = new ReferencePopulator<>("entity_type");

    public static final Reference<EntityType> BIRCH_BOAT = create("birch_boat");
    public static final Reference<EntityType> PLAYER = create("player");

    public static final Reference<EntityType> HORSE = create("horse");
    public static final Reference<EntityType> SKELETON_HORSE = create("skeleton_horse");
    public static final Reference<EntityType> ZOMBIE_HORSE = create("zombie_horse");

    private static Reference<EntityType> create(String key) {
        return POPULATOR.defer("minecraft:" + key);
    }
}
