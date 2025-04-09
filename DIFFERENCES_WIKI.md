
# Bedrock - Java differences.
So I can keep track of differences between Java - Bedrock in general and also so other developer that works on Projects related to Java-Bedrock can have an easier time :)

- Most of the information here is rather a combination of reverse enginerring BDS (1.21.10) and a lot of debugging, and also from [bedrock-protocol-docs](https://github.com/Mojang/bedrock-protocol-docs) and other that I found when working on Boar.

## Attribute
### Packet
- On Java Edition, the attribute packet consist of 2 thing, baseValue and attribute modifiers, when the client received this, it will well clears the modidifer, update the base value, and add new modifiers into the attribute.

- On Bedrock Edition, the attribute packet consist of minium, maximum, defaultMinimum, defaultMaximum, baseValue, **value**, attribute modifiers. Minium, maximum, and baseValue is pretty self explanatory, meanwhile the **value** is the calculated value (after applying attribute modifiers to the base value or could be any value, depends on the server), and attribute modifiers act the same as on Java. Bedrock client (likely) handle this in this order: clearModifier, baseValue, min/max, value, newAttributes, I'm not certain where newAttributes is placed but likely after clear or after value.

### Class
- On Java Edition, everytime an attribute is added/removed or when baseValue is updated, a value called "**dirty**" will be set to true, and this will make sures that the value will be updated when you called the value.
```java
private void addModifier(AttributeModifier attributeModifier) {
    AttributeModifier attributeModifier2 = this.modifierById.putIfAbsent(attributeModifier.id(), attributeModifier);
    if (attributeModifier2 != null) {
        throw new IllegalArgumentException("Modifier is already applied on this attribute!");
    }
    this.getModifiers(attributeModifier.operation()).put(attributeModifier.id(), attributeModifier);
    this.setDirty();
}

protected void setDirty() {
    this.dirty = true;
    this.onDirty.accept(this);
}

public double getValue() {
    if (this.dirty) {
        this.cachedValue = this.calculateValue();
        this.dirty = false;
    }
    return this.cachedValue;
}
```

- Howerver, on Bedrock there is a few *slight* differences. First of all the "**dirty**" value will only be updated to TRUE **IF THE baseValue IS UPDATED**, in other case like updating min, max, clearing attribute, adding/removing attribute, the value going to be **updated right away**. Looking at the example below *(This is only a recreation of what I see in BDS code)*

```java
public void setBaseValue(float baseValue) {
    if (this.baseValue == baseValue) {
        return;
    }

    this.baseValue = value;
    this.setDirty();
}

private void addModifier(AttributeModifierData modifier) {
    final AttributeModifierData lv = this.modifiers.putIfAbsent(modifier.getId(), modifier);
    if (lv == null) {
        this.update();
    }
}

protected void update() {
    this.value = this.computeValue();
}

public float getValue() {
    if (this.dirty) {
        this.value = this.computeValue();
        this.dirty = false;
    }

    return this.value;
}
```

#### Note for Geyser (and why it is so important).
### READ THIS, IT'S IMPORTANT AND IT WILL BE THE REASON WHY 90% OF ANTICHEATS WILL FALSE FLAG FOR THIS.

- As listed above, the differences in packet and class, you might notice that the Bedrock packet can change the value directly and how Bedrock updated the value instantly. The problem is Geyser **DO NOT** translate attribute modifiers *(don't blame them they have a good reason for doing so)*, so when player *START* sprinting, the server send back attribute and the only thing Geyser translate is the calculated **value** and nothing else, baseValue, min, max, and the rest remains the same. The client received this, cleared the modifiers, update to the new value (which is this case is 0.13) and no new modifiers to add! Now the real problem is when the player *STOP* sprinting, player will attempt to remove the sprinting modifier attribute, but the problem is, *there is none to begin which* since it is already cleared from before, therefore nothing to update, the attribute will remains the same value 0.13, therefore the player keep on sprinting even though they already stopped and only will stop when the server send back the value 0.1.
#### FAQ (For the sections above).
##### Why don't Geyser translate the attribute modifier?
- They simply can't, UUID, values, and differences in each possible attribute modifier that CAN BE CHANGE between update is not worth it to translate, if they fucked anything up, the attribute value is wrong all the time which is the bad thing.
##### Ok, how IS the *Class* section is any relevant to the *Note for Geyser* section.
- Well if you try to handle it normally with the Java version of the class. It will set dirty to true, the value doesn't get updated instantly, but because of the attribute modifiers is now empty, even tho the value is set to 0.13, the next time you call *getValue()* it will try to update, poof now your value is back to 0.1 which is wrong.

## Block Friction, Jump Factor
### getBlockPosBelowThatAffectsMyMovement()
- On Java Edition, this value try to get the block that 0.5 blocks below you meanwhile on Bedrock it's 0.1
#### getOnPos()
- Seems be grabbing the from player pos - offset directly instead of using *supportingBlockPos*.
### Block Friction
- Only that honey block now have the same block friction as slime (0.8)
### Jump Factor
- Only honey block have this behaviour, jump factor will now be 0.6 instead of 0.5.

## Bouncy Block
- Only bed block act differently, you can look at the differences instantly here.
```java
// Java Edition.
private void bounceUp(Entity entity) {
    Vec3 vec3 = entity.getDeltaMovement();
    if (vec3.y < 0.0) {
        double d = entity instanceof LivingEntity ? 1.0 : 0.8;
        entity.setDeltaMovement(vec3.x, -vec3.y * (double)0.66f * d, vec3.z);
    }
}
```

```java
// Bedrock Edition (from Boar code).
if (cache.is(BlockTag.BEDS, state.block()) && player.velocity.y < 0.0 && !player.sneaking) {
    final float d = living ? 1.0F : 0.8F;
    player.velocity.y = -player.velocity.y * 0.75F * d;
    if (player.velocity.y > 0.75) {
        player.velocity.y = 0.75F;
    }

    return;
}
```
## Stepping on a block.
### Honey block.
- On Java Edition, stepping on a honey block will get handle by *speedFactor* multiplier, while on Bedrock it will be handle by the *stepOn* method you normally see on Java.
- On Bedrock Edition, the stepping on thing will act the same as slime block.

```java
double d = Math.abs(entity.getDeltaMovement().y);
if (d < 0.1 && !entity.isSteppingCarefully()) {
    double e = 0.4 + d * 0.2;
    entity.setDeltaMovement(entity.getDeltaMovement().multiply(e, 1.0, e));
}
```
### Slime block
- No idea yet, it acting weird as hell, looking at BDS code doesn't help, one thing I know that it will also affect y motion instead of just XZ.

## Speed Factor
- Doesn't seems to exist at all on Bedrock.

## Block Collision
- Look at [BedrockCollision](https://github.com/Oryxel/Boar/blob/new-engine/src/main/java/ac/boar/anticheat/collision/BedrockCollision.java) class in Boar anticheat (incomplete).

## World Layer
- This behaviour only exist on Bedrock edition, a block could have multiple *layer*.

## Climbing ladder
- On Java Edition, when you're climbing a ladder, a value called 0.2 will be set to your y motion after player done their movement for that tick and will be affect by tick end (0.2 - 0.08) * 0.98 resulted in climbing only 0.1176 blocks per tick.
```java
// Java Edition
private Vec3 handleRelativeFrictionAndCalculateMovement(Vec3 vec3, float f) {
    this.moveRelative(this.getFrictionInfluencedSpeed(f), vec3);
    this.setDeltaMovement(this.handleOnClimbable(this.getDeltaMovement()));
    this.move(MoverType.SELF, this.getDeltaMovement()); // Move player to the calculated position.
    Vec3 vec32 = this.getDeltaMovement();

    // Finally handle climbing.
    if ((this.horizontalCollision || this.jumping) && (this.onClimbable() || this.wasInPowderSnow && PowderSnowBlock.canEntityWalkOnPowderSnow(this))) {
        vec32 = new Vec3(vec32.x, 0.2, vec32.z);
    }
    return vec32;
}

private void travelInAir(Vec3 vec3) {
    BlockPos blockPos = this.getBlockPosBelowThatAffectsMyMovement();
    float f = this.onGround() ? this.level().getBlockState(blockPos).getBlock().getFriction() : 1.0f;
    float g = f * 0.91f;
    Vec3 vec32 = this.handleRelativeFrictionAndCalculateMovement(vec3, f);
    double d = vec32.y;

    // Tick End act the same, climbing won't affect this.
    MobEffectInstance mobEffectInstance = this.getEffect(MobEffects.LEVITATION);
    d = mobEffectInstance != null ? (d += (0.05 * (double)(mobEffectInstance.getAmplifier() + 1) - vec32.y) * 0.2) : (!this.level().isClientSide || this.level().hasChunkAt(blockPos) ? (d -= this.getEffectiveGravity()) : (this.getY() > (double)this.level().getMinY() ? -0.1 : 0.0));
    if (this.shouldDiscardFriction()) {
        this.setDeltaMovement(vec32.x, d, vec32.z);
    } else {
        float h = this instanceof FlyingAnimal ? g : 0.98f;
        this.setDeltaMovement(vec32.x * (double)g, d * (double)h, vec32.z * (double)g);
    }
}
```

- However on Bedrock,Climbing part in *handleRelativeFrictionAndCalculateMovement* will be the move to before move method, climbing will also affect tick end if player is having horizontal collision.
```java
// Bedrock Edition (Recreation of BDS code)
private Vec3 handleRelativeFrictionAndCalculateMovement(Vec3 vec3, float f) {
    this.moveRelative(this.getFrictionInfluencedSpeed(f), vec3);
    this.setDeltaMovement(this.handleOnClimbable(this.getDeltaMovement()));
    Vec3 vec32 = this.getDeltaMovement();
    if ((this.horizontalCollision || this.jumping) && (this.onClimbable() || this.wasInPowderSnow && PowderSnowBlock.canEntityWalkOnPowderSnow(this))) {
        vec32 = new Vec3(vec32.x, 0.2, vec32.z);
    }
    this.move(MoverType.SELF, vec32); // Move player to the calculated position.
    return vec32;
}

public void finalizeMovement() {
    boolean climbable = false;
    // If player is climbing with horizontal collision, this will be run!
    if (player.horizontalCollision && (player.onClimbable() || player.getInBlockState().is(Blocks.POWDER_SNOW) && PowderSnowBlock.canEntityWalkOnPowderSnow(player))) {
        player.velocity.y = 0.2F;
        climbable = true;
    }

    // If player match the condition, no tick end is applied.
    final StatusEffect effect = player.getEffect(Effect.LEVITATION);
    if (!climbable) {
        if (effect != null) {
                player.velocity.y += (0.05f * (effect.getAmplifier() + 1) - player.velocity.y) * 0.2f;
        } else if (player.compensatedWorld.isChunkLoaded((int) player.position.x, (int) player.position.z)) {
            player.velocity.y -= player.getEffectiveGravity();
        } else {
            // Seems to be 0 all the times, not -0.1 depends on your y, or well I don't know?
            player.velocity.y = 0;
        }

        player.velocity.y *= 0.98F;
    }

    float g = this.prevSlipperiness * 0.91F;
    player.velocity = player.velocity.multiply(g, 1, g);
}
```

## Stuck Speed Multiplier (Cobwebs, Sweet Berry, ....)
- On Java Edition, the client loop through a list of blocks that near the player, and then update the stuck multiplier, which means if the order for example is in.
```
Sweet Berry
Cobweb
Sweet Berry
Power Snow <- Ta da, the client use the latest one!
```
- On Bedrock Edition howerver, it will choose the speed multiplier that going slow down player the most
```
Sweet Berry
Cobweb <- Client now use this!
Sweet Berry
Power Snow
```
- You can take a deeper look inside [Boar code](https://github.com/Oryxel/Boar/blob/8fc9e55743690c5382d25530f17a2832d3d547dd/src/main/java/ac/boar/anticheat/data/BoarBlockState.java#L49) to see how it works.
```java
// Crap code don't mind it.
final boolean xLargerThanThreshold = Math.abs(player.stuckSpeedMultiplier.x) >= 1.0E-7;
final boolean yLargerThanThreshold = Math.abs(player.stuckSpeedMultiplier.y) >= 1.0E-7;
final boolean zLargerThanThreshold = Math.abs(player.stuckSpeedMultiplier.z) >= 1.0E-7;
if (xLargerThanThreshold || yLargerThanThreshold || zLargerThanThreshold) {
    player.stuckSpeedMultiplier.x = Math.min(player.stuckSpeedMultiplier.x, movementMultiplier.x);
    player.stuckSpeedMultiplier.y = Math.min(player.stuckSpeedMultiplier.y, movementMultiplier.y);
    player.stuckSpeedMultiplier.z = Math.min(player.stuckSpeedMultiplier.z, movementMultiplier.z);
} else {
    player.stuckSpeedMultiplier = movementMultiplier;
}
```

## Sprinting in water
- On Java Edition, sprinting makes player move faster in water but this will required player to be swimming which means fully submerged.
- On Bedrock Edition, player can send sprinting while being in water without swimming but won't affect their movement until they're actually sprinting.

## Input
- On Bedrock Edition, the client can entirely control how much they move (in range of -1 to 1) and is not limited like Java Edition, however this is only the case with analog move vector (joystick thingy).
- The input will also be normalized **only if** by normalizing the input the player won't gain any advantage before/after the input.
- Input multiplier for using item is different from Java, you can debug yourself since I forgot lol.
- You can take a look at how Boar handle Input [here](https://github.com/Oryxel/Boar/blob/new-engine/src/main/java/ac/boar/anticheat/util/InputUtil.java)

## Minimal motion
- On Java Edtion, if entity motion is smaller than 0.003 or 0.005 depends on the version the motion going to set to 0.
- On Bedrock Edition, this value seems to be around the range 1.0E-8 -> 1.0E-9 before the value is set to 0.