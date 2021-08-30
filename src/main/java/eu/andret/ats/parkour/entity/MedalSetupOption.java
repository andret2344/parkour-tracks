/*
 * Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.entity;

import eu.andret.ats.parkour.parkour.ParkourMedalData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.ToDoubleFunction;

@Getter
@AllArgsConstructor
public enum MedalSetupOption {
	TIME(false, ParkourMedalData::getTime, ParkourMedalData::setTime, (d1, d2) -> d1 > d2),
	REWARD(true, ParkourMedalData::getReward, ParkourMedalData::setReward, (d1, d2) -> d1 < d2);

	final boolean economyRequired;
	@NotNull
	final ToDoubleFunction<ParkourMedalData> getterFunction;
	@NotNull
	final BiConsumer<ParkourMedalData, Double> setterFunction;
	/**
	 * Informs, if "better" medal should have smalled (time) value or greater (reward) maybe?
	 */
	@NotNull
	final BiPredicate<Double, Double> valuesRelation;

	public void set(final ParkourMedalData medalData, final double value) {
		setterFunction.accept(medalData, value);
	}

	public double get(final ParkourMedalData medalData) {
		return getterFunction.applyAsDouble(medalData);
	}
}
