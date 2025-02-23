/*
 * Copyright Andret Tools System (c) 2025. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.region;

import com.sk89q.worldedit.regions.CuboidRegion;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

@Value
@NonFinal
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Checkpoint extends BasicRegion {
	@NotNull
	Location location;

	public Checkpoint(@NotNull final CuboidRegion cuboidRegion, @NotNull final Location location) {
		super(cuboidRegion);
		this.location = location;
	}
}
