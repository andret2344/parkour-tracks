/*
 * Copyright Andret (c) 2018-2021. Copying and modifying allowed only keeping git link reference.
 */

package eu.andret.ats.parkour.util.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public class LocationAdapter implements JsonSerializer<Location>, JsonDeserializer<Location> {
	@NotNull
	@Override
	public JsonElement serialize(@NotNull final Location src, @NotNull final Type typeOfSrc, @NotNull final JsonSerializationContext context) {
		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("world", src.getWorld().getName());
		jsonObject.addProperty("x", src.getX());
		jsonObject.addProperty("y", src.getY());
		jsonObject.addProperty("z", src.getZ());
		jsonObject.addProperty("yaw", src.getYaw());
		jsonObject.addProperty("pitch", src.getPitch());
		return jsonObject;
	}

	@NotNull
	@Override
	public Location deserialize(@NotNull final JsonElement json, @NotNull final Type typeOfT, @NotNull final JsonDeserializationContext context) throws JsonParseException {
		final JsonObject jsonObject = json.getAsJsonObject();
		final World world = Bukkit.getServer().getWorld(jsonObject.get("world").getAsString());
		final float x = jsonObject.get("x").getAsFloat();
		final float y = jsonObject.get("y").getAsFloat();
		final float z = jsonObject.get("z").getAsFloat();
		final float yaw = jsonObject.get("yaw").getAsFloat();
		final float pitch = jsonObject.get("pitch").getAsFloat();
		return new Location(world, x, y, z, yaw, pitch);
	}
}
