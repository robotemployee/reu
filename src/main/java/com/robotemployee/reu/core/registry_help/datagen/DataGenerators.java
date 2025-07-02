package com.robotemployee.reu.core.registry_help.datagen;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod.EventBusSubscriber(modid = RobotEmployeeUtils.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGenerators {

    static Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void gatherData(final GatherDataEvent event) {
        System.out.println("gagagsadgaga");
        LOGGER.info("Received data generation event in reu");
        Datagen.run(event);
    }
}
