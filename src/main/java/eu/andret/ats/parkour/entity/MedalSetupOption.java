/*
 * Copyright Andret Tools System (c) 2025. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.entity;

import eu.andret.ats.parkour.parkour.ParkourMedal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.ToDoubleFunction;

@Getter
@AllArgsConstructor
public enum MedalSetupOption {
	TIME(false, ParkourMedal::getTime, ParkourMedal::setTime, (d1, d2) -> d1 > d2),
	REWARD(true, ParkourMedal::getReward, ParkourMedal::setReward, (d1, d2) -> d1 < d2);

	final boolean economyRequired;
	@NotNull
	final ToDoubleFunction<ParkourMedal> getterFunction;
	@NotNull
	final BiConsumer<ParkourMedal, Double> setterFunction;
	/**
	 * Informs, if "better" medal should have smalled (time) value or greater (reward) maybe?
	 */
	@NotNull
	final BiPredicate<Double, Double> valuesRelation;

	public void set(final ParkourMedal medalData, final double value) {
		setterFunction.accept(medalData, value);
	}

	public double get(final ParkourMedal medalData) {
		return getterFunction.applyAsDouble(medalData);
	}
}
