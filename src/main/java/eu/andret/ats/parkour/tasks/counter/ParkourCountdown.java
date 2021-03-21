/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.counter;

import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ParkourCountdown implements Runnable {
	private final int begin;
	private final Runnable opening;
	private final Consumer<Integer> step;
	private final Runnable closing;
	private int iterator;

	public ParkourCountdown(final int begin, @NotNull final Runnable opening, @NotNull final Consumer<Integer> step, @NotNull final Runnable closing) {
		this.begin = begin;
		iterator = begin + 1;
		this.opening = opening;
		this.step = step;
		this.closing = closing;
	}

	@Override
	public void run() {
		iterator--;
		if (iterator == begin) {
			opening.run();
			return;
		}
		if (iterator == 0) {
			closing.run();
			return;
		}
		step.accept(iterator);
	}
}
