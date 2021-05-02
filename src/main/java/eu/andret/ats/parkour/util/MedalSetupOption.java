package eu.andret.ats.parkour.util;

import eu.andret.ats.parkour.parkour.ParkourMedalData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.ToDoubleFunction;

@AllArgsConstructor
public enum MedalSetupOption {
	TIME(false, ParkourMedalData::getTime, ParkourMedalData::setTime, (d1, d2) -> d1 > d2),
	REWARD(true, ParkourMedalData::getReward, ParkourMedalData::setReward, (d1, d2) -> d1 < d2);

	@Getter
	final boolean economyRequired;
	@NotNull
	final ToDoubleFunction<ParkourMedalData> getterFunction;
	@NotNull
	final BiConsumer<ParkourMedalData, Double> setterFunction;
	/**
	 * If "better" medal should have smalled (time) value or greater (reward) maybe?
	 */
	@Getter
	@NotNull
	final BiPredicate<Double, Double> valuesRelation;

	public void set(final ParkourMedalData medalData, final double value) {
		setterFunction.accept(medalData, value);
	}

	public double get(final ParkourMedalData medalData) {
		return getterFunction.applyAsDouble(medalData);
	}
}
