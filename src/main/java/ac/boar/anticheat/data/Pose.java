package ac.boar.anticheat.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum Pose {
	STANDING(0),
	GLIDING(1),
	SLEEPING(2),
	SWIMMING(3),
	SPIN_ATTACK(4),
	CROUCHING(5),
	DYING(7);

	private final int index;
}