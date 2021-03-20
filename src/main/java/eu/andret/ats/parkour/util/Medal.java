/*
 * Copyright Andret (c) 2019-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The Enum with medals.
 */
@AllArgsConstructor
@Getter
public enum Medal {
	/**
	 * The best medal.
	 */
	PLATINUM(100),
	/**
	 * Second one.
	 */
	GOLD(80),
	/**
	 * Third...
	 */
	SILVER(50),
	/**
	 * Worst one.
	 */
	BRONZE(20),
	/**
	 * No medal.
	 */
	NONE(0);

	/**
	 * The price for getting exact medal.
	 */
	private final int price;
}
