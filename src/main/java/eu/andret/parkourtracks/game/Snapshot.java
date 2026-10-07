package eu.andret.parkourtracks.game;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * What a player had when entering a game: inventory (armor and off hand included), experience, effects, health,
 * food and flight. Kept as YAML text in the player's PDC, saved with the player's own data, so a crash never loses
 * it.
 */
public final class Snapshot {
	private static final String INVENTORY = "inventory";
	private static final String LEVEL = "level";
	private static final String EXP = "exp";
	private static final String EFFECTS = "effects";
	private static final String HEALTH = "health";
	private static final String FOOD = "food";
	private static final String SATURATION = "saturation";
	private static final String ALLOW_FLIGHT = "allow-flight";
	private static final String FLYING = "flying";

	@NotNull
	private final YamlConfiguration data;

	private Snapshot(@NotNull final YamlConfiguration data) {
		this.data = data;
	}

	@NotNull
	public static Snapshot of(@NotNull final Player player) {
		final YamlConfiguration data = new YamlConfiguration();
		// Empty slots are null, which List.of refuses
		data.set(INVENTORY, new ArrayList<>(Arrays.asList(player.getInventory().getContents())));
		data.set(LEVEL, player.getLevel());
		data.set(EXP, (double) player.getExp());
		data.set(EFFECTS, new ArrayList<>(player.getActivePotionEffects()));
		data.set(HEALTH, player.getHealth());
		data.set(FOOD, player.getFoodLevel());
		data.set(SATURATION, (double) player.getSaturation());
		data.set(ALLOW_FLIGHT, player.getAllowFlight());
		data.set(FLYING, player.isFlying());
		return new Snapshot(data);
	}

	/**
	 * @throws IllegalArgumentException when the text is not a snapshot
	 */
	@NotNull
	public static Snapshot parse(@NotNull final String text) {
		final YamlConfiguration data = new YamlConfiguration();
		try {
			data.loadFromString(text);
		} catch (final InvalidConfigurationException ex) {
			throw new IllegalArgumentException("Not a snapshot: " + ex.getMessage(), ex);
		}
		return new Snapshot(data);
	}

	@NotNull
	@Override
	public String toString() {
		return data.saveToString();
	}

	/**
	 * Gives the player back everything of the snapshot, replacing what they have now.
	 */
	public void restore(@NotNull final Player player) {
		final List<?> items = data.getList(INVENTORY, List.of());
		final ItemStack[] contents = new ItemStack[player.getInventory().getSize()];
		for (int i = 0; i < Math.min(items.size(), contents.length); i++) {
			contents[i] = items.get(i) instanceof final ItemStack item ? item : null;
		}
		player.getInventory().setContents(contents);
		player.setLevel(data.getInt(LEVEL));
		player.setExp((float) data.getDouble(EXP));
		player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
		data.getList(EFFECTS, List.of())
				.stream()
				.filter(PotionEffect.class::isInstance)
				.map(PotionEffect.class::cast)
				.forEach(player::addPotionEffect);
		player.setHealth(Math.min(data.getDouble(HEALTH, getMaxHealth(player)), getMaxHealth(player)));
		player.setFoodLevel(data.getInt(FOOD, 20));
		player.setSaturation((float) data.getDouble(SATURATION, 5));
		player.setAllowFlight(data.getBoolean(ALLOW_FLIGHT));
		player.setFlying(data.getBoolean(ALLOW_FLIGHT) && data.getBoolean(FLYING));
	}

	public static double getMaxHealth(@NotNull final Player player) {
		final AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
		return attribute == null ? 20 : attribute.getValue();
	}
}
