package com.robotemployee.reu.foliant;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RobotEmployeeUtils.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Foliant {


    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event){
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("foliant").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("startRaid")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                    .executes(context -> {
                                        CommandSourceStack source = context.getSource();
                                        FoliantRaidLevelManager manager = FoliantRaidServerManager.getLevelManager(context.getSource().getLevel());
                                        BlockPos epicenter = BlockPosArgument.getBlockPos(context, "pos");
                                        if (!manager.canCreateRaid(epicenter)) {
                                            context.getSource().sendSuccess(() -> Component.literal("Raid already exists at " + epicenter), false);
                                            return Command.SINGLE_SUCCESS;
                                        }
                                        manager.createRaidOrNull(epicenter);
                                        context.getSource().sendSuccess(() -> Component.literal("Created raid at " + epicenter), false);
                                        return Command.SINGLE_SUCCESS;
                                    })
                                )
                        )
                        .then(Commands.literal("stopLevelRaids")
                                .executes(context -> {
                                    FoliantRaidLevelManager manager = FoliantRaidServerManager.getLevelManager(context.getSource().getLevel());
                                    context.getSource().sendSuccess(() -> Component.literal("Stopped all raids in the level"), false);
                                    manager.stopAll();
                                    return Command.SINGLE_SUCCESS;
                                })
                        )

        );
    }
}
