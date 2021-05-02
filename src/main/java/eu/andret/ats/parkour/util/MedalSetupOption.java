package eu.andret.ats.parkour.util;

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

	final boolean financialProviderRequired;
	@NotNull
	final ToDoubleFunction<ParkourMedalData> getterFunction;
	@NotNull
	final BiConsumer<ParkourMedalData, Double> setterFunction;
	@NotNull
	final BiPredicate<Double, Double> relation;
}
