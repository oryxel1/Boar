package ac.boar.anticheat.player;

import ac.boar.anticheat.compensated.cache.entity.EntityCache;
import ac.boar.anticheat.compensated.world.CompensatedWorldImpl;
import ac.boar.anticheat.data.UseItemCache;
import ac.boar.anticheat.data.block.BoarBlockState;
import ac.boar.anticheat.data.vanilla.AttributeInstance;
import ac.boar.anticheat.teleport.TeleportUtil;
import ac.boar.anticheat.util.MathUtil;
import ac.boar.anticheat.util.math.Vec3;
import ac.boar.geyser.util.GeyserUtil;
import ac.boar.protocol.mitm.CloudburstReceiveListener;
import lombok.Getter;

import ac.boar.anticheat.check.api.holder.CheckHolder;
import ac.boar.anticheat.compensated.CompensatedInventory;
import ac.boar.anticheat.data.FluidState;
import ac.boar.anticheat.validator.BreakingBlockValidator;
import ac.boar.anticheat.util.math.Box;
import ac.boar.anticheat.util.math.Mutable;
import ac.boar.anticheat.validator.ItemTransactionValidator;
import ac.boar.protocol.mitm.CloudburstSendListener;
import ac.boar.anticheat.player.data.PlayerData;
import ac.boar.anticheat.util.ChatUtil;
import lombok.Setter;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.TrigMath;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.Ability;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.registry.type.BlockMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.BlockTag;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

public final class BoarPlayer extends PlayerData {
    @Getter
    private final GeyserSession session;
    public BedrockServerSession cloudburstDownstream;
    public CloudburstSendListener cloudburstUpstream;
    public CloudburstReceiveListener downstreamPacketHandler;

    public final long joinedTime = System.currentTimeMillis();
    public long runtimeEntityId, javaEntityId;

    @Getter
    private final TeleportUtil teleportUtil = new TeleportUtil(this);

    @Getter
    private final CheckHolder checkHolder = new CheckHolder(this);

    // Lag compensation
    public final CompensatedWorldImpl compensatedWorld = new CompensatedWorldImpl(this);
    public final CompensatedInventory compensatedInventory = new CompensatedInventory(this);

    // Validation
    public final BreakingBlockValidator breakingValidator = new BreakingBlockValidator(this);
    public final ItemTransactionValidator transactionValidator = new ItemTransactionValidator(this);

    @Getter
    private final UseItemCache useItemCache = new UseItemCache(this);

    @Getter
    @Setter
    private boolean debugMode, alertEnabled;

    public BoarPlayer(GeyserSession session) {
        this.session = session;

        BlockMappings mappings = session.getBlockMappings();
        for (int i = 0; i < mappings.getJavaToBedrockBlocks().length; i++) {
            this.bedrockBlockToJava.put(mappings.getJavaToBedrockBlocks()[i].getRuntimeId(), i);
        }

        BEDROCK_AIR = mappings.getBedrockAir().getRuntimeId();
//
        for (GeyserAttributeType type : GeyserAttributeType.values()) {
            final String identifier = type.getBedrockIdentifier();
            if (identifier == null || this.attributes.containsKey(type.getBedrockIdentifier())) {
                continue;
            }

            this.attributes.put(identifier, new AttributeInstance(type.getDefaultValue()));
        }
    }

    public void sendLatencyStack() {
        this.sendLatencyStack(false);
    }

    public void sendLatencyStack(boolean immediate) {
        long id = this.sentStackId.incrementAndGet();
        if (id == -GeyserUtil.MAGIC_FORM_IMAGE_HACK_TIMESTAMP || id == -GeyserUtil.MAGIC_VIRTUAL_INVENTORY_HACK) {
            id = this.sentStackId.incrementAndGet();
        }

        // We have to send negative values since geyser translate positive one.
        final NetworkStackLatencyPacket latencyPacket = new NetworkStackLatencyPacket();
        latencyPacket.setTimestamp(-id);
        latencyPacket.setFromServer(true);

        if (immediate) {
            this.cloudburstDownstream.sendPacketImmediately(latencyPacket);
        } else {
            this.cloudburstDownstream.sendPacket(latencyPacket);
        }

        this.latencyUtil.addLatencyToQueue(id);
    }

    public boolean isAbilityExempted() {
        return this.abilities.contains(Ability.MAY_FLY) || this.flying || this.wasFlying;
    }

    public void kick(String reason) {
        this.session.disconnect(ChatUtil.PREFIX + " " + reason);
    }

    // Prediction related method
    public void tick() {
        this.getActiveEffects().entrySet().removeIf(filter -> {
            filter.getValue().tick();
            return filter.getValue().getDuration() == 0;
        });

        for (final EntityCache cache : this.compensatedWorld.getEntities().values()) {
            if (cache.getPast() != null) {
                cache.getPast().tick();
            }

            if (cache.getCurrent() != null) {
                cache.getCurrent().tick();
            }
        }
    }

    public void postTick() {
        this.getUseItemCache().tick();
        this.glideBoostTicks--;
    }

    public BlockState getInBlockState() {
        if (this.inBlockState == null) {
            this.inBlockState = this.compensatedWorld.getBlockState(this.position.toVector3i(), 0).getState();
        }

        return this.inBlockState;
    }

    public boolean onClimbable() {
        return this.getSession().getTagCache().is(BlockTag.CLIMBABLE, this.getInBlockState().block());
    }

    public float getJumpPower() {
        return PlayerData.JUMP_HEIGHT * this.getBlockJumpFactor() + this.getJumpBoostPower();
    }

    public float getJumpBoostPower() {
        return this.hasEffect(Effect.JUMP_BOOST) ? 0.1F * (this.getActiveEffects().get(Effect.JUMP_BOOST).getAmplifier() + 1.0F) : 0.0F;
    }

    public Vec3 jumpFromGround(Vec3 vec3) {
        float f = this.getJumpPower();
        if (f <= 1.0E-5f) {
            return vec3;
        }
        vec3 = new Vec3(vec3.x, Math.max(f, vec3.y), vec3.z);
        if (this.getFlagTracker().has(EntityFlag.SPRINTING)) {
            float g = this.yaw * MathUtil.DEGREE_TO_RAD;
            vec3 = vec3.add(-TrigMath.sin(g) * 0.2F, 0, TrigMath.cos(g) * 0.2F);
        }

        return vec3;
    }

    public float getBlockJumpFactor() {
        float f = this.compensatedWorld.getBlockState(this.position.toVector3i(), 0).getJumpFactor();
        float g = this.compensatedWorld.getBlockState(this.getBlockPosBelowThatAffectsMyMovement(), 0).getJumpFactor();
        return (double)f == 1.0 ? g : f;
    }

    public Vector3i getBlockPosBelowThatAffectsMyMovement() {
        return this.getOnPos(0.1F);
    }

    public Vector3i getOnPos(final float offset) {
//        if (this.supportingBlockPos.isPresent()) {
//            if (!(offset > 1.0E-5F)) {
//                return this.supportingBlockPos.get();
//            } else {
//                final Vector3i vector3i = this.supportingBlockPos.get();
//                final TagCache cache = this.session.getTagCache();
//                final BlockState lv2 = this.compensatedWorld.getBlockState(vector3i);
//                return offset > 0.5 || !cache.is(BlockTag.FENCES, lv2.block())
//                        && !cache.is(BlockTag.WALLS, lv2.block()) && !cache.is(BlockTag.FENCE_GATES, lv2.block())
//                        ? Vector3i.from(vector3i.getX(), GenericMath.floor(y - offset), vector3i.getZ()) : vector3i;
//            }
//        } else {
//            int i = GenericMath.floor(x);
//            int j = GenericMath.floor(y - offset);
//            int k = GenericMath.floor(z);
//            return Vector3i.from(i, j, k);
//        }

        return this.position.subtract(0, offset, 0).toVector3i();
    }

    public boolean isRegionUnloaded() {
        final Box lv = this.boundingBox.expand(1);
        int i = GenericMath.floor(lv.minX);
        int j = GenericMath.ceil(lv.maxX);
        int k = GenericMath.floor(lv.minZ);
        int l = GenericMath.ceil(lv.maxZ);
        return !this.compensatedWorld.hasChunksAt(i, k, j, l);
    }

    public boolean containsFluid(Box box) {
        int i = GenericMath.floor(box.minX);
        int j = GenericMath.ceil(box.maxX);
        int k = GenericMath.floor(box.minY);
        int l = GenericMath.ceil(box.maxY);
        int m = GenericMath.floor(box.minZ);
        int n = GenericMath.ceil(box.maxZ);
        Mutable lv = new Mutable();

        for (int o = i; o < j; o++) {
            for (int p = k; p < l; p++) {
                for (int q = m; q < n; q++) {
                    lv.set(o, p, q);
                    FluidState lv2 = this.compensatedWorld.getFluidState(lv);
                    if (lv2.fluid() != Fluid.EMPTY) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean doesNotCollide(float offsetX, float offsetY, float offsetZ) {
        return this.doesNotCollide(this.boundingBox.offset(offsetX, offsetY, offsetZ));
    }

    private boolean doesNotCollide(Box box) {
        return this.compensatedWorld.noCollision(box) && !containsFluid(box);
    }
}
