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
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

public class PotionEffectTypeAdapter implements JsonSerializer<PotionEffectType>, JsonDeserializer<PotionEffectType> {
	@NotNull
	@Override
	public JsonElement serialize(@NotNull final PotionEffectType effectType, @NotNull final Type typeOfSrc, @NotNull final JsonSerializationContext context) {
		return new JsonPrimitive(effectType.getName());
	}

	@Override
	public PotionEffectType deserialize(@NotNull final JsonElement json, @NotNull final Type typeOfT, @NotNull final JsonDeserializationContext context) throws JsonParseException {
		return PotionEffectType.getByName(json.getAsString());
	}
}
