package com.robotemployee.reu.registry.help.datagen;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import com.robotemployee.reu.registry.ModItems;
import com.robotemployee.reu.registry.help.builder.ItemBuilder;
import net.minecraft.ResourceLocationException;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ForgeAdvancementProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = RobotEmployeeUtils.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Datagen {

    public static final Logger LOGGER = LogUtils.getLogger();

    static {
        //TagKey<Item> back = TagKey.create(Registries.ITEM, new ResourceLocation("curios", "back"));
        //addTagToTag(() -> AllTags.AllItemTags.PRESSURIZED_AIR_SOURCES.tag, () -> back);
    }

    @SubscribeEvent
    public static void run(GatherDataEvent event) {
        LOGGER.info("Received data generation event");
        FluidDatagen.run(event);

        DataGenerator gen = event.getGenerator();

        gen.addProvider(event.includeClient(), new ModItemModelProvider(gen.getPackOutput(), event.getExistingFileHelper()));
        ModTagsProvider.attach(event);

        gen.addProvider(event.includeServer(), ModLootTableProvider.create(gen.getPackOutput()));

        gen.addProvider(event.includeServer(), new ModAdvancementProvider(gen.getPackOutput(), event.getLookupProvider(), event.getExistingFileHelper()));
    }


    public static boolean doesResourceAlreadyExist(ResourceLocation loc) {
        return Files.exists(Paths.get("src", "main", "resources", "assets", loc.getNamespace(), loc.getPath()));
    }

    public static void simpleSingleLootTable(ResourceLocation loc, Supplier<ItemStack> supplier, LootContextParamSet params) {
        ModLootTableProvider.queueRequest(loc, params, () -> LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(supplier.get().getItem())))
        );
    }

    public static void basicItemModel(ResourceLocation loc) {
        ModItemModelProvider.queueRequest((provider) -> {
            provider.basicItem(loc);
        });
    }

    public static void basicItemModel(Item item) {
        basicItemModel(() -> item);
    }

    public static void basicItemModel(Supplier<Item> supplier) {
        ModItemModelProvider.queueRequest((provider) -> {
            LOGGER.info("Creating basic item model for " + supplier.get().getDescriptionId());
            provider.basicItem(supplier.get());
        });
    }

    public static void basicItem(Item item) {
        basicItemModel(item);
    }

    public static void basicItem(Supplier<Item> supplier) {
        basicItemModel(supplier);
    }

    public static void addTagToItem(Supplier<Item> itemSupplier, Supplier<TagKey<Item>> tagSupplier) {
        ModTagsProvider.queueRequest(Registries.ITEM, (provider -> {
            ResourceKey<Item> resourceKey = ForgeRegistries.ITEMS.getResourceKey(itemSupplier.get()).orElse(null);
            provider.tag(tagSupplier.get()).add(resourceKey);
        }));
    }

    public static void addTagToTag(Supplier<TagKey<Item>> addition, Supplier<TagKey<Item>> reciever) {
        ModTagsProvider.queueRequest(Registries.ITEM, (provider -> {
            provider.tag(reciever.get()).addTag(addition.get());
        }));
    }


    // Textures

    public static void saveTextureAs(BufferedImage image, ResourceLocation loc) throws IOException {
        Path outputPath = Paths.get("src", "generated", "resources", "assets", loc.getNamespace(), loc.getPath());
        ImageIO.write(image, "PNG", outputPath.toFile());
    }

    public static ResourceLocation getItemTextureLocation(Item item) {
        ResourceLocation loc = ForgeRegistries.ITEMS.getKey(item);
        if (loc == null) throw new ResourceLocationException("Could not find the texture associated with an item");
        return (item instanceof BlockItem) ?
                new ResourceLocation("minecraft" + "block/" + loc.getPath())
                : loc;
    }

    public static Path resourceLocationToPath(ResourceLocation loc) {
        return Paths.get("src", "main", "resources", "assets", loc.getNamespace(), loc.getPath());
    }

    public static BufferedImage getTexture(ResourceLocation loc) throws IOException {
        return ImageIO.read(resourceLocationToPath(loc).toFile());
    }

    public static BufferedImage tintTexture(BufferedImage image, int tint) {
        BufferedImage newborn = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = newborn.createGraphics();

        graphics.drawImage(image, 0, 0, null);
        graphics.setColor(new Color(tint));
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();

        return newborn;

        //Path newTexturePath = Paths.get("src", "generated", "resources", "assets", oldTexture.getNamespace(), oldTexture.getPath() + "_" + newName);

        //ImageIO.write(newborn, "PNG", newTexturePath.toFile());
    }

    public static class ModItemModelProvider extends ItemModelProvider {

        public static ArrayList<Consumer<ModItemModelProvider>> requests = new ArrayList<>();

        public static void queueRequest(Consumer<ModItemModelProvider> request) {
            requests.add(request);
        }

        Logger LOGGER = LogUtils.getLogger();

        public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
            super(output, RobotEmployeeUtils.MODID, existingFileHelper);
        }

        @Override
        protected void registerModels() {
            LOGGER.info("Generating models... Amount of requests: " + requests.size());
            for (Consumer<ModItemModelProvider> request : requests) request.accept(this);
            LOGGER.info("Finished generating models!");
        }

        /** for use in {@link ItemBuilder} */
        // isn't that pretty
        public static Consumer<RegistryObject<Item>> spawnEgg() {
            return (newborn) -> Datagen.ModItemModelProvider.queueRequest(provider -> {
                provider.getBuilder(newborn.getId().getPath())
                        .parent(provider.getExistingFile(new ResourceLocation("item/template_spawn_egg")))
                        .texture("layer0", "minecraft:item/spawn_egg");
            });
        }
    }

    public static class ModTagsProvider {

        static HashMap<ResourceKey<? extends Registry<?>>, ArrayList<Consumer<TagsProviderImpl<?>>>> requests = new HashMap<>();

        public static void attach(GatherDataEvent event) {
            DataGenerator gen = event.getGenerator();
            PackOutput output = gen.getPackOutput();
            ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
            CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

            LOGGER.info("Attaching tag datagen");

            gen.addProvider(event.includeServer(), new TagsProviderImpl<>(output, Registries.ITEM, lookupProvider, existingFileHelper));
        }

        public static <T> void queueRequest(ResourceKey<? extends Registry<T>> key, Consumer<TagsProviderImpl<T>> request) {
            if (!requests.containsKey(key)) requests.put(key, new ArrayList<>());
            ArrayList<Consumer<TagsProviderImpl<?>>> specificRequests = requests.get(key);
            // don't look
            LOGGER.info("Queuing tag datagen request of type " + key.location());
            specificRequests.add((Consumer<TagsProviderImpl<?>>) (Consumer<?>) request);
        }


        private static class TagsProviderImpl<T> extends TagsProvider<T> {

            protected TagsProviderImpl(PackOutput output, ResourceKey<? extends Registry<T>> key, CompletableFuture<HolderLookup.Provider> future, @Nullable ExistingFileHelper existingFileHelper) {
                super(output, key, future, RobotEmployeeUtils.MODID, existingFileHelper);
            }

            @Override
            protected void addTags(@NotNull HolderLookup.Provider provider) {
                ArrayList<Consumer<TagsProviderImpl<?>>> requests = ModTagsProvider.requests.get(this.registryKey);
                LOGGER.info("Amount of " + this.registryKey + " tag requests: " + requests.size());
                for (Consumer<TagsProviderImpl<?>> request : requests) {
                    request.accept(this);
                }
            }

            @Override
            public TagAppender<T> tag(TagKey<T> key) {
                return super.tag(key);
            }
        }
    }

    // i can guarantee you this code sucks completely i have barely any understanding of what's going on and honestly i just got my wisdom teeth removed i don't care anymore

    public static class ModLootTableProvider extends LootTableProvider {

        //public static final HashSet<SubProviderEntry> requests = new HashSet<>();

        protected static final HashSet<ResourceLocation> locs = new HashSet<>();

        // these are basically just entries but using a HashMap so that there can be 1 provider per LootContextParamSet
        // in create() it actually just iterates through the map and creates a LootTableSubProviderEntry whatever thingy for each pair
        protected static final HashMap<LootContextParamSet, LootTableSubProvider> requests = new HashMap<>();



        /*public static void queueRequest(ResourceLocation loc, LootContextParamSet params, Consumer<LootTable.Builder> consumer) {
            queueRequest(loc, new SubProviderEntry(() -> new EveryLootTableSubProvider(loc, consumer), params));
        }*/

        public static void queueRequest(ResourceLocation loc, LootContextParamSet params, Supplier<LootTable.Builder> builder) {
            if (locs.contains(loc)) {
                LOGGER.error("attempted to queue loot tables with duplicate resource locations");
                return;
            }

            locs.add(loc);


            if (!requests.containsKey(params)) requests.put(params, new EveryLootTableSubProvider());

            LootTableSubProvider provider = requests.get(params);

            if (provider instanceof EveryLootTableSubProvider every) {
                every.queueRequest(loc, builder);
            }
        }

        public static ModLootTableProvider create(PackOutput output) {
            ArrayList<SubProviderEntry> entries = new ArrayList<>();

            for (HashMap.Entry<LootContextParamSet, LootTableSubProvider> entry : requests.entrySet()) {
                entries.add(new SubProviderEntry(entry::getValue, entry.getKey()));
            }

            return new ModLootTableProvider(output, entries);
        }


        protected ModLootTableProvider(PackOutput output, ArrayList<SubProviderEntry> entries) {
            super(output, locs, entries);

        }



        public static class EveryLootTableSubProvider implements LootTableSubProvider {

            public final HashMap<ResourceLocation, Supplier<LootTable.Builder>> requests = new HashMap<>();

            public EveryLootTableSubProvider() {}

            public void queueRequest(ResourceLocation loc, Supplier<LootTable.Builder> builder) {
                requests.put(loc, builder);
            }

            @Override
            public void generate(@NotNull BiConsumer<ResourceLocation, LootTable.Builder> consumer) {
                for (HashMap.Entry<ResourceLocation, Supplier<LootTable.Builder>> entry : requests.entrySet()) {
                    consumer.accept(entry.getKey(), entry.getValue().get());
                }
            }
        }
    }

    public static class ModAdvancementProvider extends ForgeAdvancementProvider {

        public static Advancement ROOT;
        public static ResourceLocation ROOT_LOCATION = new ResourceLocation(RobotEmployeeUtils.MODID, "root");

        public static final ArrayList<Consumer<Consumer<Advancement>>> requests = new ArrayList<>();

        protected static final HashMap<ResourceLocation, Advancement> advancements = new HashMap<>();

        // Here's how you use this.
        // Call queueRequest() with a lambda where you create an advancement
        // and use the save() method on the provided consumer.
        // Then call record() on the advancement so that it can be used
        // as a parent for another advancement. This is necessary because
        // minecraft is silly

        public static void queueRequest(Consumer<Consumer<Advancement>> request) {
            //LOGGER.info("Queueing request for advancement");
            requests.add(request);
        }

        public static void record(Advancement advancement) {
            advancements.put(advancement.getId(), advancement);
        }

        public static ResourceLocation simpleAdvancement(String id, Supplier<Item> icon, Component title, Component desc, @Nullable ResourceLocation parent) {
            ResourceLocation loc = new ResourceLocation(RobotEmployeeUtils.MODID, id);
            Datagen.ModAdvancementProvider.queueRequest((consumer) -> {

                //LOGGER.info("Root is " + ROOT + " and rootloc is " + ROOT_LOCATION);

                record(Datagen.ModAdvancementProvider.createNormalAdvancement(id, icon, title, desc, parent)
                        .addCriterion("award_through_code", new ImpossibleTrigger.TriggerInstance())
                        .save(consumer, loc.toString()));
            });
            return loc;
        }

        public static ResourceLocation simpleItemObtainedAdvancement(String id, Supplier<Item> supplier, Component desc) {
            return simpleItemObtainedAdvancement(id, supplier, desc, null);
        }

        public static ResourceLocation simpleItemObtainedAdvancement(String id, Supplier<Item> supplier, Component desc, @Nullable ResourceLocation parent) {
            ResourceLocation loc = new ResourceLocation(RobotEmployeeUtils.MODID, id);

            Datagen.ModAdvancementProvider.queueRequest((consumer) -> {
                Item item = supplier.get();

                Component title = Component.translatable(item.getDescriptionId() + ".desc");

                //LOGGER.info("Root is " + ROOT + " and rootloc is " + ROOT_LOCATION);

                record(Datagen.ModAdvancementProvider.createNormalAdvancement(id, supplier, title, desc, parent)
                        .addCriterion("has_item", InventoryChangeTrigger.TriggerInstance.hasItems(supplier.get()))
                        .save(consumer, loc.toString()));

            });

            LOGGER.info("Queued simple item obtainment advancement with id " + id);
            return loc;
        }


        public static Advancement.Builder createNormalAdvancement(String id, Supplier<Item> icon, Component title, Component desc, @Nullable ResourceLocation parent) {
            //ResourceLocation loc = new ResourceLocation(RobotEmployeeUtils.MODID, id);
            if (parent == null) parent = ROOT_LOCATION;

            return Advancement.Builder.advancement()
                    .display(
                            icon.get(),
                            title,
                            desc,
                            null,
                            FrameType.TASK,
                            true,
                            true,
                            false
                    ).parent(advancements.get(parent));
        }


        /**
         * Constructs an advancement provider using the generators to write the
         * advancements to a file.
         *
         * @param output             the target directory of the data generator
         * @param registries         a future of a lookup for registries and their objects
         * @param existingFileHelper a helper used to find whether a file exists
         */
        public ModAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper existingFileHelper) {
            super(output, registries, existingFileHelper, List.of(new ModAdvancementsProcessor()));
        }

        protected static class ModAdvancementsProcessor implements AdvancementGenerator {

            @Override
            public void generate(HolderLookup.@NotNull Provider registries, @NotNull Consumer<Advancement> saver, @NotNull ExistingFileHelper existingFileHelper) {
                LOGGER.info("Generating advancements... There are " + requests.size() + " requests");
                ROOT = Advancement.Builder.advancement()
                        .display(
                                ModItems.ONE_DAY_BLINDING_STEW.get(),
                                Component.literal("H.D.S.C."),
                                Component.literal("Acronym! Acronym! Acronym! acronuym"),
                                new ResourceLocation(RobotEmployeeUtils.MODID, "textures/misc/irkwall.png"),
                                FrameType.TASK,
                                false,
                                false,
                                false
                        )
                        .addCriterion("joined_game", PlayerTrigger.TriggerInstance.located(LocationPredicate.ANY))
                        .save(saver, ROOT_LOCATION.toString());
                record(ROOT);


                for (Consumer<Consumer<Advancement>> request : requests) request.accept(saver);
            }
        }
    }

}