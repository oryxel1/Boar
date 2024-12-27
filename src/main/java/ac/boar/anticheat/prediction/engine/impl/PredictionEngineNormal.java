package ac.boar.anticheat.prediction.engine.impl;

import ac.boar.anticheat.player.BoarPlayer;
import ac.boar.anticheat.prediction.engine.base.PredictionEngine;
import ac.boar.anticheat.util.math.Vec3f;

public class PredictionEngineNormal extends PredictionEngine {
    public PredictionEngineNormal(final BoarPlayer player) {
        super(player);
    }

    @Override
    protected Vec3f travel(Vec3f vec3f) {
        return null;
    }

    @Override
    protected Vec3f applyEndOfTick(Vec3f vec3f) {
        return null;
    }

    @Override
    protected Vec3f jump(boolean sprinting, Vec3f vec3f) {
        return null;
    }

    @Override
    protected boolean shouldJump() {
        return false;
    }
}
