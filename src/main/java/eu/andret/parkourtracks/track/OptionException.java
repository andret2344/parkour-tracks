package eu.andret.parkourtracks.track;

import org.jetbrains.annotations.NotNull;

/**
 * Thrown when a value cannot be set to a track option. The command turns the problem into a message.
 */
public final class OptionException extends RuntimeException {
	public enum Problem {
		/**
		 * Not {@code true} or {@code false}; the detail is empty.
		 */
		NOT_A_BOOLEAN,
		/**
		 * Not a number; the detail is empty.
		 */
		NOT_A_NUMBER,
		/**
		 * A number outside the allowed range; the detail is the range.
		 */
		OUT_OF_RANGE,
		/**
		 * None of the allowed values; the detail lists them.
		 */
		NOT_A_CHOICE,
		/**
		 * Not a valid permission node; the detail is empty.
		 */
		NOT_A_PERMISSION,
		/**
		 * Not allowed together with another option of the track; the detail is that option.
		 */
		CONFLICT
	}

	@NotNull
	private final Problem problem;
	@NotNull
	private final String detail;

	public OptionException(@NotNull final Problem problem, @NotNull final String detail) {
		super(problem + ": " + detail);
		this.problem = problem;
		this.detail = detail;
	}

	@NotNull
	public Problem getProblem() {
		return problem;
	}

	@NotNull
	public String getDetail() {
		return detail;
	}
}
