/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.tasks.counter;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class ParkourCountdown implements Runnable {
	private final int begin;
	@Nullable
	private final Runnable opening;
	@Nullable
	private final Consumer<Integer> step;
	@Nullable
	private final Runnable closing;
	private int iterator;

	public ParkourCountdown(final int begin, @Nullable final Runnable opening, @Nullable final Consumer<Integer> step, @Nullable final Runnable closing) {
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
			if (opening != null) {
				opening.run();
			}
			return;
		}
		if (iterator == 0) {
			if (closing != null) {
				closing.run();
			}
			return;
		}
		if (step != null) {
			step.accept(iterator);
		}
	}
}
