package eu.andret.parkourtracks.track;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The settings of a track an admin changes with {@code /ptracks option}. The field initializers are the defaults; they
 * also fill in fields missing from the tracks file.
 */
public final class TrackOptions {
	public static final int MIN_DIFFICULTY = 1;
	public static final int MAX_DIFFICULTY = 5;

	private boolean sprintForced;
	private boolean hardcore;
	private boolean damageAllowed;
	private boolean boat;
	@NotNull
	private EntityType boatType = EntityType.OAK_BOAT;
	private boolean pauseOnCheckpoints;
	@NotNull
	private SkipMode skipMode = SkipMode.FAIL;
	private boolean enderPearls;
	private double fee;
	private double reward;
	private int difficulty = MIN_DIFFICULTY;
	@Nullable
	private Material icon;
	@Nullable
	private String permission;
	@NotNull
	private AfterFinish afterFinish = AfterFinish.LOBBY;

	public boolean isSprintForced() {
		return sprintForced;
	}

	public void setSprintForced(final boolean sprintForced) {
		this.sprintForced = sprintForced;
	}

	public boolean isHardcore() {
		return hardcore;
	}

	public void setHardcore(final boolean hardcore) {
		this.hardcore = hardcore;
	}

	public boolean isDamageAllowed() {
		return damageAllowed;
	}

	public void setDamageAllowed(final boolean damageAllowed) {
		this.damageAllowed = damageAllowed;
	}

	public boolean isBoat() {
		return boat;
	}

	public void setBoat(final boolean boat) {
		this.boat = boat;
	}

	@NotNull
	public EntityType getBoatType() {
		return boatType;
	}

	public void setBoatType(@NotNull final EntityType boatType) {
		this.boatType = boatType;
	}

	public boolean isPauseOnCheckpoints() {
		return pauseOnCheckpoints;
	}

	public void setPauseOnCheckpoints(final boolean pauseOnCheckpoints) {
		this.pauseOnCheckpoints = pauseOnCheckpoints;
	}

	@NotNull
	public SkipMode getSkipMode() {
		return skipMode;
	}

	public void setSkipMode(@NotNull final SkipMode skipMode) {
		this.skipMode = skipMode;
	}

	public boolean isEnderPearls() {
		return enderPearls;
	}

	public void setEnderPearls(final boolean enderPearls) {
		this.enderPearls = enderPearls;
	}

	public double getFee() {
		return fee;
	}

	public void setFee(final double fee) {
		if (fee < 0) {
			throw new IllegalArgumentException("The fee cannot be negative");
		}
		this.fee = fee;
	}

	public double getReward() {
		return reward;
	}

	public void setReward(final double reward) {
		if (reward < 0) {
			throw new IllegalArgumentException("The reward cannot be negative");
		}
		this.reward = reward;
	}

	public int getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(final int difficulty) {
		if (difficulty < MIN_DIFFICULTY || difficulty > MAX_DIFFICULTY) {
			throw new IllegalArgumentException("The difficulty has to be between " + MIN_DIFFICULTY + " and " + MAX_DIFFICULTY);
		}
		this.difficulty = difficulty;
	}

	@Nullable
	public Material getIcon() {
		return icon;
	}

	public void setIcon(@Nullable final Material icon) {
		this.icon = icon;
	}

	/**
	 * The permission a player needs to join the track, or {@code null} when anyone can.
	 */
	@Nullable
	public String getPermission() {
		return permission;
	}

	public void setPermission(@Nullable final String permission) {
		this.permission = permission;
	}

	@NotNull
	public AfterFinish getAfterFinish() {
		return afterFinish;
	}

	public void setAfterFinish(@NotNull final AfterFinish afterFinish) {
		this.afterFinish = afterFinish;
	}
}
