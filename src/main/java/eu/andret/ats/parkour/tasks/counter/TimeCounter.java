/*
 *  Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.counter;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class TimeCounter implements Runnable {
	private final BooleanSupplier reset;
	private final Consumer<Integer> step;
	private final BooleanSupplier condition;
	private int counter = 0;

	public TimeCounter(final BooleanSupplier reset, final Consumer<Integer> step, final BooleanSupplier condition) {
		this.reset = reset;
		this.step = step;
		this.condition = condition;
	}

	@Override
	public void run() {
		if (reset.getAsBoolean()) {
			counter = 0;
		}
		if (condition.getAsBoolean()) {
			step.accept(counter++);
		}
	}
}
