package com.robotemployee.reu.core.registry_help.datagen;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
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
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = RobotEmployeeUtils.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Datagen {

    public static final Logger LOGGER = LogUtils.getLogger();

    public static boolean doesResourceAlreadyExist(ResourceLocation loc) {
        return Files.exists(Paths.get("src", "main", "resources", "assets", loc.getNamespace(), loc.getPath()));
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

    @SubscribeEvent
    public static void run(GatherDataEvent event) {
        LOGGER.info("Received data generation event");
        FluidDatagen.run(event);

        DataGenerator gen = event.getGenerator();

        gen.addProvider(event.includeClient(), new ModItemModelProvider(gen.getPackOutput(), event.getExistingFileHelper()));
        ModTagsProvider.attach(event);
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
            specificRequests.add((Consumer<TagsProviderImpl<?>>)(Consumer<?>)request);
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

        /*private static class ItemTagProvider extends TagsProvider<Item> {

            protected ItemTagProvider(PackOutput output, ResourceKey<? extends Registry<Item>> resourceKey, CompletableFuture<HolderLookup.Provider> future, String modId, @Nullable ExistingFileHelper existingFileHelper) {
                super(output, resourceKey, future, modId, existingFileHelper);
            }

            @Override
            protected void addTags(HolderLookup.Provider provider) {

            }
        }*/
    }

/*
    public static class ModTextureProvider implements DataProvider {

        public final Map<ResourceLocation, Texture> generatedTextures = new HashMap<>();

        public ModTextureProvider(PackOutput output, String folder, Function<ResourceLocation, T> factory, ) {
            Preconditions.checkNotNull(output);
            this.output = output;
            Preconditions.checkNotNull(folder);
            this.folder = folder;
            Preconditions.checkNotNull(factory);
            this.factory = factory;
            Preconditions.checkNotNull(existingFileHelper);
            this.existingFileHelper = existingFileHelper;
        }

        @Override @NotNull
        public CompletableFuture<?> run(CachedOutput cache) {
            return generateAll(cache);
        }

        @Override @NotNull
        public String getName() {
            return "Texture Provider";
        }

        protected CompletableFuture<?> generateAll(CachedOutput cache) {
            CompletableFuture<?>[] futures = new CompletableFuture<?>[this.generatedTextures.size()];
            int i = 0;

            for (Texture texture : this.generatedTextures.values()) {
                Path target = getPath(texture);
                futures[i++] = DataProvider.saveStable(cache, texture.toJson(), target);
            }

            return CompletableFuture.allOf(futures);
        }

        protected Path getPath(Texture texture) {
            ResourceLocation loc = texture.getLocation();
            return this.output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(loc.getNamespace()).resolve("models").resolve(loc.getPath() + ".json");
        }

        public static class Texture {
            ResourceLocation loc;
            BufferedImage image;
            public Texture(ResourceLocation loc) {
                this.loc = loc;
            }

            public ResourceLocation getLocation() { return loc; }
            public BufferedImage get() { return image; }
        }
    }
    */

}
