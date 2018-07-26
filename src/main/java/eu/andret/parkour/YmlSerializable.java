package eu.andret.parkour;

import java.util.Map;

public interface YmlSerializable {
    Map<String, Object> toYmlStructure();

    void fromYmlStructure(Map<String, Object> structure);
}
