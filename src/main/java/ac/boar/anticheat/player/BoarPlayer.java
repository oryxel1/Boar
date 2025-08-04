package ac.boar.anticheat.player;

import ac.boar.anticheat.Boar;
import ac.boar.anticheat.collision.util.CuboidBlockIterator;
import ac.boar.anticheat.compensated.cache.entity.EntityCache;
import ac.boar.anticheat.compensated.world.CompensatedWorldImpl;
import ac.boar.anticheat.data.ItemUseTracker;
import ac.boar.anticheat.data.vanilla.AttributeInstance;
import ac.boar.anticheat.teleport.TeleportUtil;
import ac.boar.anticheat.util.LatencyUtil;
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
import lombok.Setter;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.TrigMath;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.Ability;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket;
import org.geysermc.geyser.entity.EntityDefinitions;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.inventory.item.BedrockEnchantment;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.registry.type.BlockMappings;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.tags.BlockTag;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

import java.util.concurrent.atomic.AtomicLong;

public final class BoarPlayer extends PlayerData {
    @Getter
    private final GeyserSession session;
    @Getter
    @Setter
    private BedrockServerSession cloudburstDownstream;
    public CloudburstSendListener cloudburstUpstream;
    public CloudburstReceiveListener downstreamPacketHandler;

    public long runtimeEntityId, javaEntityId;

    @Getter
    private final TeleportUtil teleportUtil = new TeleportUtil(this);

    @Getter
    private final CheckHolder checkHolder = new CheckHolder(this);

    @Getter
    private final LatencyUtil latencyUtil = new LatencyUtil(this);
    public final AtomicLong receivedStackId = new AtomicLong(-1), sentStackId = new AtomicLong(-1);

    // Lag compensation
    public final CompensatedWorldImpl compensatedWorld = new CompensatedWorldImpl(this);
    public final CompensatedInventory compensatedInventory = new CompensatedInventory(this);

    // Validation
    public final BreakingBlockValidator breakingValidator = new BreakingBlockValidator(this);
    public final ItemTransactionValidator transactionValidator = new ItemTransactionValidator(this);

    @Getter
    private final ItemUseTracker itemUseTracker = new ItemUseTracker(this);

    @Getter
    @Setter
    private boolean debugMode;

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

        this.latencyUtil.addLatencyToQueue(id);

        if (immediate) {
            this.getSession().sendUpstreamPacketImmediately(latencyPacket);
        } else {
            this.getSession().sendUpstreamPacket(latencyPacket);
        }
    }

    public boolean isMovementExempted() {
        try { // Ye, well whatever.
            if (this.session.hasPermission("boar.exempt")) {
                return true;
            }
        } catch (Exception ignored) {}

        return this.abilities.contains(Ability.MAY_FLY) || this.getFlagTracker().isFlying() || this.getFlagTracker().isWasFlying();
    }

    public void kick(String reason) {
        this.session.disconnect(Boar.getInstance().getAlertManager().getPrefix(getSession()) + " " + reason);
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

        this.getItemUseTracker().preTick();
    }

    public void postTick() {
        this.glideBoostTicks--;
        this.getItemUseTracker().postTick();
    }

    public float getYOffset() {
        if (this.vehicleData != null) {
            final EntityCache cache = this.compensatedWorld.getEntity(this.vehicleData.vehicleRuntimeId);
            if (cache != null) {
                final String identifier = cache.getDefinition().identifier();

                if (identifier.equals("minecraft:boat") || identifier.equals("minecraft:chest_boat")) {
                    return EntityDefinitions.BIRCH_BOAT.offset(); // It's all the same anyway, I just like birch :)
                }
            }

            return 0;
        }

        return EntityDefinitions.PLAYER.offset();
    }

    public float getFrictionInfluencedSpeed(float slipperiness) {
        if (this.onGround) {
            float speed = this.getSpeed() * (0.21600002F / (slipperiness * slipperiness * slipperiness));
            if (!CompensatedInventory.getEnchantments(this.compensatedInventory.armorContainer.get(3).getData()).containsKey(BedrockEnchantment.SOUL_SPEED) && this.soulSandBelow) {
                speed *= 0.55F; // not accurate, but well I can just give extra offset if player movement is slower than the predicted one.
            }

            return speed;
        }

        return this.getFlagTracker().has(EntityFlag.SPRINTING) ? 0.026F : 0.02F;
    }

    public BlockState getInBlockState() {
        if (this.inBlockState == null) {
            this.inBlockState = this.compensatedWorld.getBlockState(this.position.toVector3i(), 0).getState();
        }

        return this.inBlockState;
    }

    public boolean onClimbable() {
        return this.getSession().getTagCache().is(BlockTag.CLIMBABLE, this.getInBlockState().block()) && !this.getInBlockState().is(Blocks.SCAFFOLDING);
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
        return f == 1.0 ? g : f;
    }

    public Vector3i getBlockPosBelowThatAffectsMyMovement() {
        // This is correct, not getOnPos, try moving on the edge of the slime block on JE/BE and you will see the difference.
        return position.down(0.1F).toVector3i();
    }

    public Vector3i getOnPos(final float offset) {
        Vector3i blockPos = null;
        float d = Float.MAX_VALUE;

        final CuboidBlockIterator iterator = CuboidBlockIterator.iterator(boundingBox);
        while (iterator.step()) {
            int x = iterator.getX(), y = iterator.getY(), z = iterator.getZ();
            Vector3i blockPos2 = Vector3i.from(x, y, z);
            if (compensatedWorld.getBlockState(x, y, z, 0).findCollision(this, Vector3i.from(x, y, z), boundingBox.expand(1.0E-3F), true).isEmpty()) {
                continue;
            }

            float e = new Vec3(blockPos2).distToCenterSqr(this.position);

            if (e < d || e == d && (blockPos == null || new Vec3(blockPos).compareTo(blockPos2) < 0)) {
                blockPos = blockPos2;
                d = e;
            }
        }

        if (blockPos != null) {
            return Vector3i.from(blockPos.getX(), GenericMath.floor(this.position.y - offset), blockPos.getZ());
        }

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
