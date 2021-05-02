package eu.andret.ats.parkour.sample;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum BalanceInteraction {
	ADD,
	SUB,
	SET;

	@NotNull
	static List<String> stringValues() {
		return Arrays.stream(values())
				.map(Enum::name)
				.collect(Collectors.toList());
	}
}
