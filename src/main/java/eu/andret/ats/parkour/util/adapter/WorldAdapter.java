/*
 * Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.util.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import eu.andret.ats.parkour.ParkourPlugin;
import lombok.AllArgsConstructor;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

@AllArgsConstructor
public class WorldAdapter implements JsonSerializer<World>, JsonDeserializer<World> {
	@NotNull
	private final ParkourPlugin plugin;

	@Override
	public JsonElement serialize(final World world, final Type typeOfSrc, final JsonSerializationContext context) {
		return new JsonPrimitive(world.getName());
	}

	@Override
	public World deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
		return plugin.getServer().getWorld(json.getAsString());
	}
}
