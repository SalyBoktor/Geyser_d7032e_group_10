/*
 * Copyright (c) 2019-2022 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.translator.protocol.java.inventory;

import com.google.common.collect.SortedSetMultimap;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtMapBuilder;
import org.cloudburstmc.nbt.NbtType;
import org.cloudburstmc.protocol.bedrock.data.entity.EntityFlag;
import org.cloudburstmc.protocol.bedrock.data.inventory.ContainerType;
import org.cloudburstmc.protocol.bedrock.packet.UpdateEquipPacket;
import org.geysermc.geyser.api.util.Identifier;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.entity.type.living.animal.horse.CamelEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.ChestedHorseEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.LlamaEntity;
import org.geysermc.geyser.entity.type.living.animal.horse.SkeletonHorseEntity;
import org.geysermc.geyser.entity.type.living.animal.nautilus.NautilusEntity;
import org.geysermc.geyser.inventory.Container;
import org.geysermc.geyser.inventory.InventoryHolder;
import org.geysermc.geyser.item.GeyserCustomMappingData;
import org.geysermc.geyser.registry.type.ItemMapping;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.inventory.InventoryTranslator;
import org.geysermc.geyser.translator.inventory.horse.DonkeyInventoryTranslator;
import org.geysermc.geyser.translator.inventory.horse.MountInventoryTranslator;
import org.geysermc.geyser.translator.inventory.horse.LlamaInventoryTranslator;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.util.InventoryUtils;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.inventory.ClientboundMountScreenOpenPacket;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Translator(packet = ClientboundMountScreenOpenPacket.class)
public class JavaMountScreenOpenTranslator extends PacketTranslator<ClientboundMountScreenOpenPacket> {
    // ISSUE #6535: these are the JAVA identifiers of every item that is legitimate horse armor.
    // We resolve each one through the *current session's* item mappings, so that if a server/extension
    // has registered a custom Bedrock identifier for one of these (overriding the vanilla item),
    // that custom identifier ends up in the Bedrock GUI's acceptedItems instead of being hard-coded.
    private static final String[] VANILLA_HORSE_ARMOR_JAVA_IDENTIFIERS = new String[] {
        "minecraft:leather_horse_armor",
        "minecraft:iron_horse_armor",
        "minecraft:golden_horse_armor",
        "minecraft:diamond_horse_armor",
        "minecraft:copper_horse_armor",
        "minecraft:netherite_horse_armor"
    };

    private static final String[] ACCEPTED_NAUTILUS_ARMORS = new String[] {"minecraft:copper_nautilus_armor", "minecraft:iron_nautilus_armor",
        "minecraft:golden_nautilus_armor", "minecraft:diamond_nautilus_armor", "minecraft:netherite_nautilus_armor"};

    private static final NbtMap SADDLE_SLOT, CARPET_SLOT, NAUTILUS_ARMOR_SLOT;

    static {
        NAUTILUS_ARMOR_SLOT = buildAcceptedArmorSlot(ACCEPTED_NAUTILUS_ARMORS, "minecraft:nautilusarmor");

        NbtMapBuilder carpetBuilder = NbtMap.builder();
        NbtMapBuilder carpetItem = NbtMap.builder()
            .putShort("Aux", Short.MAX_VALUE)
            .putString("Name", "minecraft:carpet");
        List<NbtMap> acceptedCarpet = Collections.singletonList(NbtMap.builder().putCompound("slotItem", carpetItem.build()).build());
        carpetBuilder.putList("acceptedItems", NbtType.COMPOUND, acceptedCarpet);
        carpetBuilder.putCompound("item", carpetItem.build());
        carpetBuilder.putInt("slotNumber", 1);
        CARPET_SLOT = carpetBuilder.build();

        NbtMapBuilder saddleBuilder = NbtMap.builder();
        NbtMapBuilder acceptedSaddle = NbtMap.builder()
            .putShort("Aux", Short.MAX_VALUE)
            .putString("Name", "minecraft:saddle");
        List<NbtMap> acceptedItem = Collections.singletonList(NbtMap.builder().putCompound("slotItem", acceptedSaddle.build()).build());
        saddleBuilder.putList("acceptedItems", NbtType.COMPOUND, acceptedItem);
        saddleBuilder.putCompound("item", acceptedSaddle.build());
        saddleBuilder.putInt("slotNumber", 0);
        SADDLE_SLOT = saddleBuilder.build();
    }

    private static NbtMap buildAcceptedArmorSlot(String[] accepted, String name) {
        NbtMapBuilder armorBuilder = NbtMap.builder();
        List<NbtMap> acceptedArmors = new ArrayList<>(accepted.length);

        for (String identifier : accepted) {
            NbtMapBuilder acceptedItemBuilder = NbtMap.builder()
                .putShort("Aux", Short.MAX_VALUE)
                .putString("Name", identifier);
            acceptedArmors.add(NbtMap.builder().putCompound("slotItem", acceptedItemBuilder.build()).build());
        }

        armorBuilder.putList("acceptedItems", NbtType.COMPOUND, acceptedArmors);
        NbtMapBuilder armorItem = NbtMap.builder()
            .putShort("Aux", Short.MAX_VALUE)
            .putString("Name", name);
        armorBuilder.putCompound("item", armorItem.build());
        armorBuilder.putInt("slotNumber", 1);
        return armorBuilder.build();
    }

    /**
     * ISSUE #6535: builds the horse armor slot for the current session, including any custom
     * Bedrock identifiers registered against a vanilla horse armor Java item — both a full
     * item override (mapping's own bedrockIdentifier) and any additional variants registered
     * via item-model/predicate matching (mapping's customItemDefinitions).
     */
    private static NbtMap buildHorseArmorSlot(GeyserSession session) {
        Set<String> acceptedBedrockIdentifiers = new LinkedHashSet<>();

        for (String javaIdentifier : VANILLA_HORSE_ARMOR_JAVA_IDENTIFIERS) {
            ItemMapping mapping = session.getItemMappings().getMapping(javaIdentifier);
            if (mapping == null) {
                continue;
            }

            if (mapping.getBedrockIdentifier() != null) {
                acceptedBedrockIdentifiers.add(mapping.getBedrockIdentifier());
            }

            SortedSetMultimap<Key, GeyserCustomMappingData> customDefinitions = mapping.getCustomItemDefinitions();
            if (customDefinitions != null) {
                for (GeyserCustomMappingData customMapping : customDefinitions.values()) {
                    Identifier bedrockIdentifier = customMapping.definition().bedrockIdentifier();
                    if (bedrockIdentifier != null) {
                        acceptedBedrockIdentifiers.add(bedrockIdentifier.toString());
                    }
                }
            }
        }

        return buildAcceptedArmorSlot(acceptedBedrockIdentifiers.toArray(new String[0]), "minecraft:horsearmoriron");
    }

    @Override
    public void translate(GeyserSession session, ClientboundMountScreenOpenPacket packet) {
        Entity entity = session.getEntityCache().getEntityByJavaId(packet.getEntityId());
        if (entity == null) {
            return;
        }

        UpdateEquipPacket updateEquipPacket = new UpdateEquipPacket();
        updateEquipPacket.setWindowId((short) packet.getContainerId());
        updateEquipPacket.setWindowType((short) ContainerType.HORSE.getId());
        updateEquipPacket.setUniqueEntityId(entity.geyserId());

        NbtMapBuilder builder = NbtMap.builder();
        List<NbtMap> slots = new ArrayList<>();

        int slotCount = 2;

        InventoryTranslator<Container> inventoryTranslator;
        switch (entity) {
            case LlamaEntity llamaEntity -> {
                if (entity.getFlag(EntityFlag.CHESTED)) {
                    slotCount += llamaEntity.getStrength() * 3;
                }
                inventoryTranslator = new LlamaInventoryTranslator(slotCount);
                slots.add(CARPET_SLOT);
            }
            case ChestedHorseEntity ignored -> {
                if (entity.getFlag(EntityFlag.CHESTED)) {
                    slotCount += 15;
                }
                inventoryTranslator = new DonkeyInventoryTranslator(slotCount);
                slots.add(SADDLE_SLOT);
            }
            case CamelEntity ignored -> {
                if (entity.getFlag(EntityFlag.CHESTED)) {
                    slotCount += 15;
                }
                inventoryTranslator = new DonkeyInventoryTranslator(slotCount);
                slots.add(SADDLE_SLOT);
            }
            default -> {
                inventoryTranslator = new MountInventoryTranslator(slotCount);
                slots.add(SADDLE_SLOT);
                if (entity instanceof NautilusEntity) {
                    slots.add(NAUTILUS_ARMOR_SLOT);
                } else if (!(entity instanceof SkeletonHorseEntity)) {
                    slots.add(buildHorseArmorSlot(session));
                }
            }
        }

        builder.putList("slots", NbtType.COMPOUND, slots);
        updateEquipPacket.setTag(builder.build());
        session.sendUpstreamPacket(updateEquipPacket);

        Container container = new Container(session, entity.getNametag(), packet.getContainerId(), slotCount, null);
        InventoryUtils.openInventory(new InventoryHolder<>(session, container, inventoryTranslator));
    }
}