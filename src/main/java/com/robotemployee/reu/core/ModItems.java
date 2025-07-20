package com.robotemployee.reu.core;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.registry_help.builder.ItemBuilder;
import com.robotemployee.reu.item.FryingPanItem;
import com.robotemployee.reu.item.ReconstructorItem;
import com.robotemployee.reu.item.SculkReconstructorItem;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.RecordItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

import java.util.function.Supplier;

public class ModItems {

    // note that this is not the only place items are registered.
    // for example, items are registered by BlockBuilder in order to automatically make block items
    // they are also registered by FluidBuilder if a bucket and bottle for the fluid are required
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RobotEmployeeUtils.MODID);
    //public static final RegistryObject<Item> RECONSTRUCTOR = ITEMS.register("reconstructor", () -> new ReconstructorItem(new Item.Properties().rarity(Rarity.RARE)));
    //public static final RegistryObject<Item> SCULK_RECONSTRUCTOR = ITEMS.register("sculk_reconstructor", () -> new SculkReconstructorItem(new Item.Properties().rarity(Rarity.EPIC)));
    //public static final RegistryObject<Item> INJECTOR = ITEMS.register("injector", () -> new InjectorItem(new Item.Properties()));


    static Logger LOGGER = LogUtils.getLogger();

    public static final RegistryObject<Item> RECONSTRUCTOR = new ItemBuilder()
            .withName("reconstructor")
            .withSupplier(() -> new ReconstructorItem(
                    new Item.Properties()
                            .rarity(Rarity.RARE)
                    ))
            .noDatagen()
            .build();


    public static final RegistryObject<Item> SCULK_RECONSTRUCTOR = new ItemBuilder()
            .withName("sculk_reconstructor")
            .withSupplier(() -> new SculkReconstructorItem(
                    new Item.Properties()
                            .rarity(Rarity.EPIC)
                    ))
            .noDatagen()
            .build();


    public static final RegistryObject<Item> BLINDING_STEW = new ItemBuilder()
            .withName("one_day_blinding_stew")
            .withSupplier(() -> new Item(
                    new Item.Properties()
                            .food((new FoodProperties.Builder())
                                    .alwaysEat()
                                    .effect(() -> new MobEffectInstance(MobEffects.BLINDNESS, 24000), 1f)
                                    .nutrition(4)
                                    .build()
                            )))
            .build();

    public static final RegistryObject<Item> FRYING_PAN = new ItemBuilder()
            .withName("frying_pan")
            .withSupplier(() -> new FryingPanItem(
                    new Item.Properties()
                            .stacksTo(1)
                            .defaultDurability(10)
            ))
            .noDatagen()
            .build();

    // Obtainable
    public static final RegistryObject<Item> MUSIC_DISC_BIRDBRAIN = createDiscItem("birdbrain", ModSounds.BIRDBRAIN_DISC, 5140); // 5140
    // Not obtainable
    public static final RegistryObject<Item> MUSIC_DISC_HATRED = createDiscItem("hatred_jackulation", ModSounds.HATRED_JACKULATION_DISC, 8400);
    // Not obtainable
    public static final RegistryObject<Item> MUSIC_DISC_CALIFORNIA = createDiscItem("california_girls", ModSounds.CALIFORNIA_GIRLS_DISC, 4720);
    // Not obtainable
    public static final RegistryObject<Item> MUSIC_DISC_TRIPLE_BAKA = createDiscItem("triple_baka", ModSounds.TRIPLE_BAKA_DISC, 4780);
    // Not obtainable
    public static final RegistryObject<Item> MUSIC_DISC_CLAIRO = createDiscItem("clairo", ModSounds.CLAIRO_DISC, 4000);
    // Not obtainable
    public static final RegistryObject<Item> MUSIC_DISC_GIANT_ROBOTS = createDiscItem("giant_robots", ModSounds.GIANT_ROBOTS_DISC, 4540);
    // Not obtainable
    public static final RegistryObject<Item> MUSIC_DISC_MEMORIES = createDiscItem("armored_core_20th", ModSounds.MEMORIES_DISC, 6600);
    // Not obtainable
    public static final RegistryObject<Item> MUSIC_DISC_SO_BE_IT = createDiscItem("so_be_it", ModSounds.SO_BE_IT_DISC, 4060);
    // Not obtainable
    public static final RegistryObject<Item> MUSIC_DISC_HEART_OF_GLASS = createDiscItem("heart_of_glass", ModSounds.HEART_OF_GLASS_DISC, 4680);

    // note that the resulting item id will have "music_disc_" appended to the start of the itemId input
    public static RegistryObject<Item> createDiscItem(String itemId, Supplier<SoundEvent> sound, int ticks) {
        String finalItemId = "music_disc_" + itemId;

        return new ItemBuilder()
                .withName(finalItemId)
                .withSupplier(() -> {
                        assert sound.get() != null;
                        LOGGER.info(String.format("Registering new music disc... id=%s sound=%s duration=%s", finalItemId, sound.get().getLocation(), ticks));
                        return new RecordItem(
                                1,
                                sound,
                                new Item.Properties()
                                        .rarity(Rarity.RARE)
                                        .stacksTo(1)
                                ,
                                ticks
                        );
                })
                .addTag(() -> ItemTags.MUSIC_DISCS)
                .build();

    }
}
