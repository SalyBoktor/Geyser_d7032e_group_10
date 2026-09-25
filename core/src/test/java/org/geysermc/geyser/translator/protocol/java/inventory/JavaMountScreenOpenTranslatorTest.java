package org.geysermc.geyser.translator.protocol.java.inventory;

import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaMountScreenOpenTranslatorTest {

        @Test
        public void includesCustomHorseArmorInAcceptedItems() {
                NbtMap slot = JavaMountScreenOpenTranslator.buildAcceptedArmorSlot(
                                new String[] {
                                                "minecraft:diamond_horse_armor",
                                                "example:custom_horse_armor"
                                },
                                "minecraft:horsearmoriron");

                List<NbtMap> acceptedItems = slot.getList("acceptedItems", NbtType.COMPOUND);

                List<String> acceptedIdentifiers = acceptedItems.stream()
                                .map(item -> item.getCompound("slotItem").getString("Name"))
                                .toList();

                assertTrue(
                                acceptedIdentifiers.contains("minecraft:diamond_horse_armor"));

                assertTrue(
                                acceptedIdentifiers.contains("example:custom_horse_armor"));

                assertEquals(2, acceptedIdentifiers.size());
        }
}