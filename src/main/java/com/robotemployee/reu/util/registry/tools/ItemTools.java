package com.robotemployee.reu.util.registry.tools;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.util.registry.builder.ItemBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class ItemTools {

    // note that this is not the only place items are registered.
    // for example, items are registered by BlockBuilder in order to automatically make block items
    // they are also registered by FluidBuilder if a bucket and bottle for the fluid are required

    static Logger LOGGER = LogUtils.getLogger();

    public static Supplier<Item> createSimpleItem(ItemBuilder.Manager manager, String id) {
        return manager.createBuilder()
                .withName(id)
                .withSupplier(() -> new Item(
                        new Item.Properties()
                ))
                .build();
    }

    public static Supplier<Item> createSimpleFoodItem(ItemBuilder.Manager manager, String id, FoodProperties properties) {
        return manager.createBuilder()
                .withName(id)
                .withSupplier(() -> new Item(
                        new Item.Properties().food(properties)
                ))
                .build();
    }

    // note that the resulting item id will have "music_disc_" appended to the start of the itemId input
    public static Supplier<Item> createDiscItem(ItemBuilder.Manager manager, String itemId, Supplier<SoundEvent> sound, int ticks) {
        String finalItemId = "music_disc_" + itemId;
        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(manager.register.getNamespace(), finalItemId);

        ResourceKey<JukeboxSong> jukeboxSong = manager.datagenInstance.modJukeboxSongProviderManager.registerJukeboxSong(loc, new JukeboxSong(
                Holder.direct(sound.get()),
                Component.translatable("item." + manager.register.getNamespace() + "." + finalItemId + ".desc"),
                (float) ticks / 20,
                7
        ));

        return manager.createBuilder()
                .withName(finalItemId)
                .withSupplier(() -> {
                        assert sound.get() != null;
                        LOGGER.info(String.format("Registering new music disc... id=%s sound=%s duration=%s", finalItemId, sound.get().getLocation(), ticks));
                        return new Item(
                                new Item.Properties()
                                        .rarity(Rarity.RARE)
                                        .stacksTo(1)
                                        .fireResistant()
                                        .jukeboxPlayable(jukeboxSong)
                        );
                })
                .addTag(() -> Tags.Items.MUSIC_DISCS)
                .build();

    }
}
