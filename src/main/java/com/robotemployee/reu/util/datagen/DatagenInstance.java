package com.robotemployee.reu.util.datagen;

import com.mojang.logging.LogUtils;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ForgeAdvancementProvider;
import net.minecraftforge.common.data.SoundDefinition;
import net.minecraftforge.common.data.SoundDefinitionsProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;


public class DatagenInstance {

    public static final Logger LOGGER = LogUtils.getLogger();

    public final String MODID;

    public final ModItemModelProviderManager modItemModelProviderManager = new ModItemModelProviderManager();
    public final ModLootTableProviderManager modLootTableProviderManager = new ModLootTableProviderManager();
    public final ModSoundProviderManager modSoundProviderManager = new ModSoundProviderManager();
    public final ModTagsProviderManager modTagsProviderManager = new ModTagsProviderManager();
    public final ModAdvancementProviderManager modAdvancementProviderManager = new ModAdvancementProviderManager();
    public DatagenInstance(String modid) {
        this.MODID = modid;
    }



    // you need to call this during the GatherDataEvent for your mod
    public void run(GatherDataEvent event) {
        LOGGER.debug("Received data generation event");

        DataGenerator gen = event.getGenerator();

        gen.addProvider(event.includeClient(), modItemModelProviderManager.run(gen.getPackOutput(), event.getExistingFileHelper(), MODID));
        gen.addProvider(event.includeClient(), modSoundProviderManager.run(gen.getPackOutput(), event.getExistingFileHelper(), MODID));
        modTagsProviderManager.attach(event, MODID);

        gen.addProvider(event.includeServer(), modLootTableProviderManager.run(gen.getPackOutput()));

        gen.addProvider(event.includeServer(), modAdvancementProviderManager.run(gen.getPackOutput(), event.getLookupProvider(), event.getExistingFileHelper()));
    }


    public boolean doesResourceAlreadyExist(ResourceLocation loc) {
        return Files.exists(Paths.get("src", "main", "resources", "assets", loc.getNamespace(), loc.getPath()));
    }

    public void simpleSingleLootTable(ResourceLocation loc, Supplier<ItemStack> supplier, LootContextParamSet params) {
        modLootTableProviderManager.queueRequest(loc, params, () -> LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(supplier.get().getItem())))
        );
    }

    public void basicItemModel(ResourceLocation loc) {
        modItemModelProviderManager.queueRequest((provider) -> {
            provider.basicItem(loc);
        });
    }

    public void basicItemModel(Item item) {
        basicItemModel(() -> item);
    }

    public void basicItemModel(Supplier<Item> supplier) {
        modItemModelProviderManager.queueRequest((provider) -> {
            LOGGER.info("Creating basic item model for " + supplier.get().getDescriptionId());
            provider.basicItem(supplier.get());
        });
    }

    public void basicItem(Item item) {
        basicItemModel(item);
    }

    public void basicItem(Supplier<Item> supplier) {
        basicItemModel(supplier);
    }

    public void generateBucket(Supplier<Item> supplier) {
        //generateTintedBucketTexture(builder);
        basicItem(supplier);
    }

    public void generateBottle(Supplier<Item> supplier) {
        modItemModelProviderManager.queueRequest((provider) -> {
            ResourceLocation loc = ForgeRegistries.ITEMS.getKey(supplier.get());
            provider.getBuilder(loc.toString())
                    .parent(new ModelFile.UncheckedModelFile("item/generated"))
                    .texture("layer0", new ResourceLocation("minecraft", "item/potion"))
                    .texture("layer1", new ResourceLocation("minecraft", "item/potion_overlay"));
        });
    }

    public Consumer<RegistryObject<Item>> spawnEgg() {
        return (newborn) -> modItemModelProviderManager.queueRequest(provider -> {
            provider.getBuilder(newborn.getId().getPath())
                    .parent(provider.getExistingFile(new ResourceLocation("item/template_spawn_egg")))
                    .texture("layer0", "minecraft:item/spawn_egg");
        });
    }

    public void addTagToItem(Supplier<Item> itemSupplier, Supplier<TagKey<Item>> tagSupplier) {
        modTagsProviderManager.queueRequest(Registries.ITEM, (provider -> {
            ResourceKey<Item> resourceKey = ForgeRegistries.ITEMS.getResourceKey(itemSupplier.get()).orElse(null);
            provider.tag(tagSupplier.get()).add(resourceKey);
        }));
    }

    public void addTagToTag(Supplier<TagKey<Item>> addition, Supplier<TagKey<Item>> reciever) {
        modTagsProviderManager.queueRequest(Registries.ITEM, (provider -> {
            provider.tag(reciever.get()).addTag(addition.get());
        }));
    }

    public static class ModItemModelProviderManager {
        public ArrayList<Consumer<ModItemModelProvider>> requests = new ArrayList<>();

        public void queueRequest(Consumer<ModItemModelProvider> request) {
            requests.add(request);
        }

        // TODO fix this
        //  need everything to be instance-based instead of static. this is a library mod now, need it to be instantiated for each mod i have that uses this.
        //  this class in specific can probably just use composition instead of inheritance
        //  ModTagsProvider is more or less already what we want
        //  all of these can just be composition tbh. whole thing needed a bit of a refactoring anyway
        //  and once you're done with that, do something about the awful ItemBuilder.Manager workflow. it should be the other way around, where ItemBuilder is the Manager and it creates individual ItemBuilderInstances. Fix it.

        public ModItemModelProvider run(PackOutput output, ExistingFileHelper existingFileHelper, String modid) {
            ModItemModelProvider provider = new ModItemModelProvider(output, existingFileHelper, modid, requests);
            provider.registerModels();
            return provider;
        }

        protected static class ModItemModelProvider extends ItemModelProvider {
            public final ArrayList<Consumer<ModItemModelProvider>> requests;
            public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper, String modid, ArrayList<Consumer<ModItemModelProvider>> requests) {
                super(output, modid, existingFileHelper);
                this.requests = requests;
            }

            @Override
            protected void registerModels() {
                LOGGER.debug("Generating models... Amount of requests: " + requests.size());
                for (Consumer<ModItemModelProvider> request : requests) request.accept(this);
                LOGGER.debug("Finished generating models!");
            }
        }

    }

    public static class ModTagsProviderManager {

        HashMap<ResourceKey<? extends Registry<?>>, ArrayList<Consumer<TagsProviderImpl<?>>>> requests = new HashMap<>();

        public void attach(GatherDataEvent event, String modid) {
            DataGenerator gen = event.getGenerator();
            PackOutput output = gen.getPackOutput();
            ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
            CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

            LOGGER.debug("Attaching tag datagen");

            gen.addProvider(event.includeServer(), new TagsProviderImpl<>(output, Registries.ITEM, lookupProvider, existingFileHelper, modid, requests));
        }

        public <T> void queueRequest(ResourceKey<? extends Registry<T>> key, Consumer<TagsProviderImpl<T>> request) {
            if (!requests.containsKey(key)) requests.put(key, new ArrayList<>());
            ArrayList<Consumer<TagsProviderImpl<?>>> specificRequests = requests.get(key);
            // don't look
            LOGGER.debug("Queuing tag datagen request of type " + key.location());
            specificRequests.add((Consumer<TagsProviderImpl<?>>) (Consumer<?>) request);
        }


        private static class TagsProviderImpl<T> extends TagsProvider<T> {

            public final HashMap<ResourceKey<? extends Registry<?>>, ArrayList<Consumer<TagsProviderImpl<?>>>> allRequests;

            protected TagsProviderImpl(PackOutput output, ResourceKey<? extends Registry<T>> key, CompletableFuture<HolderLookup.Provider> future, @Nullable ExistingFileHelper existingFileHelper, String modid, HashMap<ResourceKey<? extends Registry<?>>, ArrayList<Consumer<TagsProviderImpl<?>>>> allRequests) {
                super(output, key, future, modid, existingFileHelper);
                this.allRequests = allRequests;
            }

            @Override
            protected void addTags(@NotNull HolderLookup.Provider provider) {
                ArrayList<Consumer<TagsProviderImpl<?>>> requests = allRequests.computeIfAbsent(registryKey, k -> new ArrayList<>());
                LOGGER.debug("Amount of " + this.registryKey + " tag requests: " + requests.size());
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

    public static class ModLootTableProviderManager {

        //public static final HashSet<SubProviderEntry> requests = new HashSet<>();

        protected final HashSet<ResourceLocation> locs = new HashSet<>();

        // these are basically just entries but using a HashMap so that there can be 1 provider per LootContextParamSet
        // in create() it actually just iterates through the map and creates a LootTableSubProviderEntry whatever thingy for each pair
        protected final HashMap<LootContextParamSet, LootTableSubProvider> requests = new HashMap<>();



        /*public static void queueRequest(ResourceLocation loc, LootContextParamSet params, Consumer<LootTable.Builder> consumer) {
            queueRequest(loc, new SubProviderEntry(() -> new EveryLootTableSubProvider(loc, consumer), params));
        }*/

        public void queueRequest(ResourceLocation loc, LootContextParamSet params, Supplier<LootTable.Builder> builder) {
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

        public ModLootTableProvider run(PackOutput output) {
            return ModLootTableProvider.create(output, requests, locs);
        }

        public static class ModLootTableProvider extends LootTableProvider {
            // parent class stores this yes, but it's private and im really really fucking lazy right now and i don't wanna do an access modifier so
            // shut up
            protected ModLootTableProvider(PackOutput output, HashSet<ResourceLocation> locs, ArrayList<SubProviderEntry> entries) {
                super(output, locs, entries);
            }

            public static ModLootTableProvider create(PackOutput output, HashMap<LootContextParamSet, LootTableSubProvider> requests, HashSet<ResourceLocation> locs) {
                ArrayList<SubProviderEntry> entries = new ArrayList<>();

                for (HashMap.Entry<LootContextParamSet, LootTableSubProvider> entry : requests.entrySet()) {
                    entries.add(new SubProviderEntry(entry::getValue, entry.getKey()));
                }

                return new ModLootTableProvider(output, locs, entries);
            }
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

    public static class ModAdvancementProviderManager {

        public Advancement ROOT;
        public ResourceLocation ROOT_LOCATION;

        public final ArrayList<Consumer<Consumer<Advancement>>> requests = new ArrayList<>();

        protected final HashMap<ResourceLocation, Advancement> advancements = new HashMap<>();

        // Here's how you use this.
        // Call queueRequest() with a lambda where you create an advancement
        // and use the save() method on the provided consumer.
        // Then call record() on the advancement so that it can be used
        // as a parent for another advancement. This is necessary because
        // minecraft is silly

        public void queueRequest(Consumer<Consumer<Advancement>> request) {
            //LOGGER.info("Queueing request for advancement");
            requests.add(request);
        }

        public ModAdvancementProvider run(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper existingFileHelper) {
            ModAdvancementProvider provider = new ModAdvancementProvider(output, registries, existingFileHelper, requests);
            return provider;
        }

        public void record(Advancement advancement) {
            advancements.put(advancement.getId(), advancement);
        }

        public ResourceLocation simpleAdvancement(ResourceLocation id, Supplier<Item> icon, Component title, Component desc, @Nullable ResourceLocation parent) {
            queueRequest((consumer) -> {

                //LOGGER.info("Root is " + ROOT + " and rootloc is " + ROOT_LOCATION);

                record(createNormalAdvancement(icon, title, desc, parent)
                        .addCriterion("award_through_code", new ImpossibleTrigger.TriggerInstance())
                        .save(consumer, id.toString()));
            });
            return id;
        }

        public ResourceLocation simpleItemObtainedAdvancement(ResourceLocation loc, Supplier<Item> icon, Component desc) {
            return simpleItemObtainedAdvancement(loc, icon, desc, null);
        }

        public ResourceLocation simpleItemObtainedAdvancement(ResourceLocation loc, Supplier<Item> icon, Component desc, @Nullable ResourceLocation parent) {

            queueRequest((consumer) -> {
                Item item = icon.get();

                Component title = Component.translatable(item.getDescriptionId() + ".desc");

                //LOGGER.info("Root is " + ROOT + " and rootloc is " + ROOT_LOCATION);

                record(createNormalAdvancement(icon, title, desc, parent)
                        .addCriterion("has_item", InventoryChangeTrigger.TriggerInstance.hasItems(icon.get()))
                        .save(consumer, loc.toString()));

            });

            LOGGER.debug("Queued simple item obtainment advancement with id " + loc.getPath());
            return loc;
        }


        public Advancement.Builder createNormalAdvancement(Supplier<Item> icon, Component title, Component desc, @Nullable ResourceLocation parent) {
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

        public ResourceLocation root(ResourceLocation id, Supplier<Item> icon, Component title, Component desc, ResourceLocation texture) {
            ROOT_LOCATION = id;
            queueRequest((consumer) -> {
                ROOT = Advancement.Builder.advancement()
                        .display(
                                icon.get(),
                                title,
                                desc,
                                texture,
                                FrameType.TASK,
                                false,
                                false,
                                false
                        )
                        .addCriterion("joined_game", PlayerTrigger.TriggerInstance.located(LocationPredicate.ANY))
                        .save(consumer, id.toString());
                record(ROOT);
            });
            return id;
        }

        public static class ModAdvancementProvider extends ForgeAdvancementProvider {
            public ModAdvancementProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper existingFileHelper, ArrayList<Consumer<Consumer<Advancement>>> requests) {
                super(output, registries, existingFileHelper, List.of(new ModAdvancementsProcessor(requests)));
            }

            protected static class ModAdvancementsProcessor implements AdvancementGenerator {

                final ArrayList<Consumer<Consumer<Advancement>>> requests;
                public ModAdvancementsProcessor(ArrayList<Consumer<Consumer<Advancement>>> requests) {
                    this.requests = requests;
                }

                @Override
                public void generate(HolderLookup.@NotNull Provider registries, @NotNull Consumer<Advancement> saver, @NotNull ExistingFileHelper existingFileHelper) {
                    LOGGER.debug("Generating advancements... There are " + requests.size() + " requests");
                /*
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
                 */


                    for (Consumer<Consumer<Advancement>> request : requests) request.accept(saver);
                }
            }
        }
    }

    public static class ModSoundProviderManager {

        private final ArrayList<Consumer<ModSoundProvider>> requests = new ArrayList<>();

        protected void queueRequest(Consumer<ModSoundProvider> request) {
            requests.add(request);
        }

        public void register(Supplier<SoundEvent> soundEvent, SoundDefinition definition) {
            queueRequest(provider -> {
                provider.add(soundEvent, definition);
            });
        }

        public ModSoundProvider run(PackOutput output, ExistingFileHelper helper, String modid) {
            ModSoundProvider modSoundProvider = new ModSoundProvider(output, helper, modid, requests);
            modSoundProvider.registerSounds();
            return modSoundProvider;
        }

        public static class ModSoundProvider extends SoundDefinitionsProvider {

            final ArrayList<Consumer<ModSoundProvider>> requests;
            protected ModSoundProvider(PackOutput output, ExistingFileHelper helper, String modid, ArrayList<Consumer<ModSoundProvider>> requests) {
                super(output, modid, helper);
                this.requests = requests;
            }

            @Override
            public void registerSounds() {
                requests.forEach(request -> request.accept(this));
            }

            @Override
            public void add(Supplier<SoundEvent> soundEvent, SoundDefinition definition) {
                super.add(soundEvent, definition);
            }
        }
    }

}