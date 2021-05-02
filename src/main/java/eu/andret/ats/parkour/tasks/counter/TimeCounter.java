/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.counter;

import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class TimeCounter implements Runnable {
	@Nullable
	private final BooleanSupplier reset;
	@Nullable
	private final Consumer<Integer> step;
	@Nullable
	private final BooleanSupplier condition;
	private int counter = 0;

	public TimeCounter(@Nullable final BooleanSupplier reset, @Nullable final Consumer<Integer> step, @Nullable final BooleanSupplier condition) {
		this.reset = reset;
		this.step = step;
		this.condition = condition;
	}

	@Override
	public void run() {
		if (reset != null && reset.getAsBoolean()) {
			counter = 0;
		}
		if (condition != null && condition.getAsBoolean() && step != null) {
			step.accept(counter++);
		}
	}
}
