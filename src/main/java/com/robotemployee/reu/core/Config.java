package com.robotemployee.reu.core;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.joml.Vector2i;

import java.util.ArrayList;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = RobotEmployeeUtils.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{

    public static final boolean ACCESSORIES_PRESENT = ModList.get().isLoaded("accessories");

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<Double> MIMI_VOLUME = BUILDER
            .comment("Client stuff!!")
            .comment("MIMI volume multiplier. Increase to increase volume, decrease to decrease it.")
            .comment("There is an upper limit to the volume, and it is shallow - only increase it if you've turned down jukeboxes / note blocks for some reason.")
            .comment("Default: 1")
            .define("MIMIVolume", 1d);

    private static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_SCULK_BORDERS = BUILDER
            .comment("Server stuff!!!")
            .comment("Whether to prevent the Sculk Horde from existing outside of a safe area.\nWill also lock them in the Overworld.")
            .define("enableSculkBorders", true);
    private static final ForgeConfigSpec.ConfigValue<String> SCULK_BORDERS = BUILDER
            .comment("Allowed area of the Sculk Horde. Format is \"(x1, z1):(x2, z2)\"")
            .define("sculkBorders", "(-100,-100):(100,100)");

    // a list of strings that are treated as resource locations for items
    /*
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);
     */

    static final ForgeConfigSpec SPEC = BUILDER.build();

    private static double MIMIVolume;

    public static double getMIMIVolume() {
        return MIMIVolume;
    }

    /*public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;*/
    private static final ArrayList<Vector2i> sculkBorders = new ArrayList<>(2);
    public static ArrayList<Vector2i> getSculkBorders() {
        return sculkBorders;
    }
    public static boolean areBordersEnabled;
    //public static Set<Item> items;

    /*private static boolean validateItemName(final Object obj)
    {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(new ResourceLocation(itemName));
    }*/

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {

        MIMIVolume = MIMI_VOLUME.get();

        /*
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();
         */
        // this code. it blows. it sucks. oh my god. and it's mine!
        String[] borderEntries = SCULK_BORDERS.get().split("\\(");
        for (int i=1; i<borderEntries.length;i++) {
            String[] entry = borderEntries[i].split("\\)")[0].split(", *");
            sculkBorders.add(new Vector2i(Integer.parseInt(entry[0]), Integer.parseInt(entry[1])));
        }
        Vector2i lowest = new Vector2i(
                Math.min(sculkBorders.get(0).x, sculkBorders.get(1).x),
                Math.min(sculkBorders.get(0).y, sculkBorders.get(1).y)
        );
        Vector2i highest = new Vector2i(
                Math.max(sculkBorders.get(0).x, sculkBorders.get(1).x),
                Math.max(sculkBorders.get(0).y, sculkBorders.get(1).y)
        );
        sculkBorders.clear();
        sculkBorders.add(lowest);
        sculkBorders.add(highest);

        areBordersEnabled = ENABLE_SCULK_BORDERS.get();
        //LogUtils.getLogger().info("HERE IS WHAT I CAN SEE:::: " + sculkBorders.get(0).toString() + " " + sculkBorders.get(1).toString());



        // convert the list of strings into a set of items
        /*items = ITEM_STRINGS.get().stream()
                .map(itemName -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName)))
                .collect(Collectors.toSet());
         */
    }
}
