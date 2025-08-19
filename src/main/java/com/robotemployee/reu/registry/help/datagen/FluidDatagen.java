package com.robotemployee.reu.registry.help.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Supplier;

public class FluidDatagen {

    static DataGenerator generator;

    private static final ArrayList<Runnable> clientRequests = new ArrayList<>();

    private static final ArrayList<Runnable> serverRequests = new ArrayList<>();


    public static void generateBucket(Supplier<Item> supplier) throws IOException {
        //generateTintedBucketTexture(builder);
        Datagen.basicItem(supplier);
    }

    public static void generateBottle(Supplier<Item> supplier) throws IOException {
        Datagen.ModItemModelProvider.queueRequest((provider) -> {
            ResourceLocation loc = ForgeRegistries.ITEMS.getKey(supplier.get());
            provider.getBuilder(loc.toString())
                    .parent(new ModelFile.UncheckedModelFile("item/generated"))
                    .texture("layer0", new ResourceLocation("minecraft", "item/potion"))
                    .texture("layer1", new ResourceLocation("minecraft", "item/potion_overlay"));
        });

    }

    /*
    public static void generateTintedBucketTexture(FluidBuilder builder) throws IOException {
        String name = builder.getName();
        int tint = builder.getTint();

        ResourceLocation loc = new ResourceLocation(RobotEmployeeUtils.MODID, String.format("textures/item/%s_bucket", name));
        if (Datagen.doesResourceAlreadyExist(loc)) throw new IllegalStateException("Attempting to create a generated resource that already exists manually");

        BufferedImage bucketTexture = Datagen.getTexture(Datagen.getItemTextureLocation(Items.BUCKET));
        BufferedImage tinted = Datagen.tintTexture(bucketTexture, tint);
        // rare me using string.format
        Datagen.saveTextureAs(tinted, loc);
    }

    public static void generateTintedBottleTexture(FluidBuilder builder) throws IOException {
        String name = builder.getName();
        int tint = builder.getTint();

        ResourceLocation loc = new ResourceLocation(RobotEmployeeUtils.MODID, String.format("textures/item/%s_bottle", name));
        if (Datagen.doesResourceAlreadyExist(loc)) throw new IllegalStateException("Attempting to create a generated resource that already exists manually");

        BufferedImage bucketTexture = Datagen.getTexture(Datagen.getItemTextureLocation(Items.GLASS_BOTTLE));
        BufferedImage tinted = Datagen.tintTexture(bucketTexture, tint);
        // rare me using string.format
        Datagen.saveTextureAs(tinted, loc);
    }

     */

    public static void run(GatherDataEvent event) {
        generator = event.getGenerator();
        ArrayList<Runnable> requests = new ArrayList<Runnable>();

        if (event.includeClient()) requests.addAll(clientRequests);
        if (event.includeServer()) requests.addAll(serverRequests);

        for (Runnable request : requests) {
            try {
                request.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void addClientRequest(Runnable request) {
        clientRequests.add(request);
    }

    public static void addServerRequest(Runnable request) {
        serverRequests.add(request);
    }


}
