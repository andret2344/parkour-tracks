/*
 * Copyright Andret (c) 2019. Copying and modifying allowed only keeping git link reference.
 */
package eu.andret.ats.parkour;

import org.json.JSONObject;

public interface JSONSerializable {
	JSONObject toJSON();

	void fromJSON(JSONObject object);
}
