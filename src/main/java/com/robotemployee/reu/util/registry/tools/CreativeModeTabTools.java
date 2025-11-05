package com.robotemployee.reu.util.registry.tools;

import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.function.Supplier;

public class CreativeModeTabTools {

    private static final ArrayList<Supplier<Item>> addedItems = new ArrayList<>();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RobotEmployeeUtils.MODID);

    public static ArrayList<Supplier<Item>> getAddedItems() {
        return addedItems;
    }

    public static void addItem(Supplier<Item> item) {
        addedItems.add(item);
    }
}
