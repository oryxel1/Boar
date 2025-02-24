package ac.boar.anticheat.player;

import ac.boar.anticheat.check.api.holder.CheckHolder;
import ac.boar.anticheat.collision.Collision;
import ac.boar.anticheat.compensated.CompensatedInventory;
import ac.boar.anticheat.compensated.CompensatedWorld;
import ac.boar.anticheat.data.FluidState;
import ac.boar.anticheat.data.StatusEffect;
import ac.boar.anticheat.validator.BreakingBlockValidator;
import ac.boar.anticheat.util.BlockUtil;
import ac.boar.anticheat.util.TeleportUtil;
import ac.boar.anticheat.util.math.Box;
import ac.boar.anticheat.util.math.Mutable;
import ac.boar.anticheat.validator.ItemTransactionValidator;
import ac.boar.protocol.network.CloudburstSendListener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import ac.boar.anticheat.player.data.PlayerData;
import ac.boar.anticheat.util.ChatUtil;
import ac.boar.util.GeyserUtil;
import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.definitions.BlockDefinition;
import org.cloudburstmc.protocol.bedrock.packet.NetworkStackLatencyPacket;
import org.geysermc.geyser.entity.attribute.GeyserAttributeType;
import org.geysermc.geyser.level.block.Blocks;
import org.geysermc.geyser.level.block.Fluid;
import org.geysermc.geyser.level.block.type.BlockState;
import org.geysermc.geyser.registry.type.GeyserBedrockBlock;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.session.cache.TagCache;
import org.geysermc.geyser.session.cache.tags.BlockTag;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;

import java.util.*;

@RequiredArgsConstructor
public final class BoarPlayer extends PlayerData {
    @Getter
    private final GeyserSession session;
    public BedrockServerSession cloudburstSession;
    public CloudburstSendListener geyserUpstream;
    public ClientSession mcplSession;

    public final long joinedTime = System.currentTimeMillis();
    public long runtimeEntityId, javaEntityId;

    public final TeleportUtil teleportUtil = new TeleportUtil(this);

    public final CheckHolder checkHolder = new CheckHolder(this);

    // Lag compensation
    public final CompensatedWorld compensatedWorld = new CompensatedWorld(this);
    public final CompensatedInventory compensatedInventory = new CompensatedInventory(this);

    // Validation
    public final BreakingBlockValidator breakingValidator = new BreakingBlockValidator(this);
    public final ItemTransactionValidator transactionValidator = new ItemTransactionValidator(this);

    // Mappings
    public final Map<BlockDefinition, Integer> bedrockToJavaBlocks = new HashMap<>();

    public void loadBlockMappings() {
        final GeyserBedrockBlock[] javaToBedrockBlocks = this.session.getBlockMappings().getJavaToBedrockBlocks();
        for (int i = 0; i < javaToBedrockBlocks.length; i++) {
            this.bedrockToJavaBlocks.put(javaToBedrockBlocks[i], i);
        }
    }

    public void sendTransaction() {
        sendTransaction(false);
    }

    public void sendTransaction(boolean immediate) {
        lastSentId++;
        if (lastSentId == GeyserUtil.MAGIC_FORM_IMAGE_HACK_TIMESTAMP) {
            lastSentId++;
        }

        // We have to send negative values since geyser translate positive one.
        final NetworkStackLatencyPacket latencyPacket = new NetworkStackLatencyPacket();
        latencyPacket.setTimestamp(-lastSentId);
        latencyPacket.setFromServer(true);

        if (immediate) {
            this.cloudburstSession.sendPacketImmediately(latencyPacket);
        } else {
            this.cloudburstSession.sendPacket(latencyPacket);
        }

        this.latencyUtil.getSentTransactions().add(lastSentId);
    }

    public void disconnect(String reason) {
        this.session.disconnect(ChatUtil.PREFIX + " " + reason);
    }

    // Mappings related
    public int bedrockToJavaBlockId(final BlockDefinition definition) {
        return this.bedrockToJavaBlocks.getOrDefault(definition, -1);
    }

    // Prediction related method
    public void tick() {
        this.movementSpeed = this.attributes.get(GeyserAttributeType.MOVEMENT_SPEED.getBedrockIdentifier()).getValue();

        final List<Effect> shouldBeRemoved = new ArrayList<>();
        for (Map.Entry<Effect, StatusEffect> entry : this.statusEffects.entrySet()) {
            entry.getValue().tick();
            if (entry.getValue().getDuration() <= 0) {
                shouldBeRemoved.add(entry.getKey());
            }
        }

        shouldBeRemoved.forEach(this.statusEffects::remove);
    }

    public float getVelocityMultiplier() {
        final BlockState lv = this.compensatedWorld.getBlockState(this.position.toVector3i());
        final float f = BlockUtil.getVelocityMultiplier(lv);
        if (!lv.is(Blocks.WATER) && !lv.is(Blocks.BUBBLE_COLUMN)) {
            return f == 1.0 ? BlockUtil.getVelocityMultiplier(this.compensatedWorld.getBlockState(this.getVelocityAffectingPos())) : f;
        } else {
            return f;
        }
    }

    public boolean isClimbing(boolean old) {
        final TagCache cache = this.getSession().getTagCache();

        Vector3i lv = Vector3i.from(old ? this.prevPosition.x : this.position.x, old ? this.prevPosition.y : this.position.y,
                old ? this.prevPosition.z : this.position.z);
        BlockState lv2 = this.compensatedWorld.getBlockState(lv);
        if (cache.is(BlockTag.CLIMBABLE, lv2.block())) {
            this.climbingPos = Optional.of(lv);
            return true;
        } else {
            return false;
        }
    }

    public float getJumpVelocity() {
        return PlayerData.JUMP_HEIGHT * this.getJumpVelocityMultiplier() + this.getJumpBoostVelocityModifier();
    }

    public float getJumpBoostVelocityModifier() {
        return this.hasStatusEffect(Effect.JUMP_BOOST) ? 0.1F * (this.statusEffects.get(Effect.JUMP_BOOST).getAmplifier() + 1.0F) : 0.0F;
    }

    public float getJumpVelocityMultiplier() {
        float f = BlockUtil.getJumpVelocityMultiplier(this.compensatedWorld.getBlockState(this.prevPosition.toVector3i()));
        float g = BlockUtil.getJumpVelocityMultiplier(this.compensatedWorld.getBlockState(this.getVelocityAffectingPos()));
        return (double)f == 1.0 ? g : f;
    }

    public Vector3i getVelocityAffectingPos() {
        return this.getPosWithYOffset(true, 0.1F);
    }

    public Vector3i getPosWithYOffset(final boolean old, final float offset) {
        float x = old ? this.prevPosition.x : this.position.x;
        float y = old ? this.prevPosition.y : this.position.y;
        float z = old ? this.prevPosition.z : this.position.z;

        if (this.supportingBlockPos.isPresent()) {
            if (!(offset > 1.0E-5F)) {
                return this.supportingBlockPos.get();
            } else {
                final Vector3i vector3i = this.supportingBlockPos.get();
                final TagCache cache = this.session.getTagCache();
                final BlockState lv2 = this.compensatedWorld.getBlockState(vector3i);
                return offset > 0.5 || !cache.is(BlockTag.FENCES, lv2.block())
                        && !cache.is(BlockTag.WALLS, lv2.block()) && !cache.is(BlockTag.FENCE_GATES, lv2.block())
                        ? Vector3i.from(vector3i.getX(), GenericMath.floor(y - offset), vector3i.getZ()) : vector3i;
            }
        } else {
            int i = GenericMath.floor(x);
            int j = GenericMath.floor(y - offset);
            int k = GenericMath.floor(z);
            return Vector3i.from(i, j, k);
        }
    }

    public boolean isRegionUnloaded() {
        final Box lv = this.boundingBox.expand(1);
        int i = GenericMath.floor(lv.minX);
        int j = GenericMath.ceil(lv.maxX);
        int k = GenericMath.floor(lv.minZ);
        int l = GenericMath.ceil(lv.maxZ);
        return !this.compensatedWorld.isRegionLoaded(i, k, j, l);
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
        return Collision.isSpaceEmpty(this, box) && !containsFluid(box);
    }
}
