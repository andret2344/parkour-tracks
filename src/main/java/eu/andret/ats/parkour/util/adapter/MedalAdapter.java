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
import eu.andret.ats.parkour.parkour.ParkourMedal;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;

@AllArgsConstructor
public class MedalAdapter implements JsonSerializer<ParkourMedal>, JsonDeserializer<ParkourMedal> {
	@NotNull
	private final ParkourPlugin plugin;

	@Override
	public JsonElement serialize(final ParkourMedal src, final Type typeOfSrc, final JsonSerializationContext context) {
		return new JsonPrimitive(src.getName());
	}

	@Override
	public ParkourMedal deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context) throws JsonParseException {
		return plugin.getMedals().stream()
				.filter(medal -> medal.getName().equals(json.getAsString()))
				.findAny()
				.orElse(null);
	}
}
