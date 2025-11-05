package com.robotemployee.reu.registry;

import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.function.Supplier;

public class ModCreativeModeTabs {

    private static final ArrayList<Supplier<Item>> addedItems = new ArrayList<>();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RobotEmployeeUtils.MODID);

    public static ArrayList<Supplier<Item>> getAddedItems() {
        return addedItems;
    }

    public static void addItem(Supplier<Item> item) {
        addedItems.add(item);
    }
}
