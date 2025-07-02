package com.robotemployee.reu.core.registry_help.datagen;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.ResourceLocationException;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Datagen {

    public static final Logger LOGGER = LogUtils.getLogger();

    private static final ArrayList<Consumer<ModItemModelProvider>> modelRequests = new ArrayList<>();



    public static ArrayList<Consumer<ModItemModelProvider>> getModelRequests() {
        return modelRequests;
    }

    public static void queueModelRequest(Consumer<ModItemModelProvider> request) {
        LOGGER.info("Queueing model request");
        modelRequests.add(request);
    }

    public static boolean doesResourceAlreadyExist(ResourceLocation loc) {
        return Files.exists(Paths.get("src", "main", "resources", "assets", loc.getNamespace(), loc.getPath()));
    }

    public static void basicItemModel(ResourceLocation loc) {
        queueModelRequest((provider) -> {
            provider.basicItem(loc);
        });
    }

    public static void basicItemModel(Item item) {
        queueModelRequest((provider) -> {
            provider.basicItem(item);
        });
    }

    public static void basicItemModel(Supplier<Item> supplier) {
        queueModelRequest((provider) -> {
            provider.basicItem(supplier.get());
        });
    }

    public static void basicItem(Item item) {
        basicItemModel(item);
    }

    public static void basicItem(Supplier<Item> supplier) {
        basicItemModel(supplier);
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

    public static void run(GatherDataEvent event) {
        FluidDatagen.run(event);

        LOGGER.info("Main datagen running");
        DataGenerator gen = event.getGenerator();

        gen.addProvider(event.includeClient(), new ModItemModelProvider(gen.getPackOutput(), event.getExistingFileHelper()));
    }


    public static class ModItemModelProvider extends ItemModelProvider {

        Logger LOGGER = LogUtils.getLogger();

        public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
            super(output, RobotEmployeeUtils.MODID, existingFileHelper);
        }

        @Override
        protected void registerModels() {
            LOGGER.info("Generating models... Amount of requests: " + Datagen.modelRequests.size());
            for (Consumer<ModItemModelProvider> request : Datagen.getModelRequests()) request.accept(this);
            LOGGER.info("Finished generating models!");
        }
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
