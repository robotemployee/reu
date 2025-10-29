package com.robotemployee.reu.foliant;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
                Commands.literal("foliant")
                        .then(Commands.literal("meow")
                                .executes(context -> {
                                    //NOTE this mentions ballsack
                                    context.getSource().sendSuccess(() -> Component.literal("You're super epic and I believe in you :thumbsup: :ballsack:"), false);
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(Commands.literal("startRaid")
                                .requires(source -> source.hasPermission(2))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                    .executes(context -> {
                                        CommandSourceStack source = context.getSource();
                                        FoliantRaidLevelManager manager = FoliantRaidServerManager.getLevelManager(context.getSource().getLevel());
                                        BlockPos epicenter = BlockPosArgument.getBlockPos(context, "pos");
                                        if (!manager.canCreateRaidAt(epicenter)) {
                                            context.getSource().sendSuccess(() -> Component.literal("Raid already exists at " + epicenter), false);
                                            return Command.SINGLE_SUCCESS;
                                        }
                                        manager.startRaid(epicenter);
                                        context.getSource().sendSuccess(() -> Component.literal("Created raid at " + epicenter), false);
                                        return Command.SINGLE_SUCCESS;
                                    })
                                )
                        )
                        .then(Commands.literal("stopDimensionRaids")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    FoliantRaidLevelManager manager = FoliantRaidServerManager.getLevelManager(context.getSource().getLevel());
                                    context.getSource().sendSuccess(() -> Component.literal("Stopped all raids in this dimension"), false);
                                    manager.stopAll();
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(Commands.literal("stopAllServerRaids")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    context.getSource().sendSuccess(() -> Component.literal("Stopped all raids in the server"), false);
                                    FoliantRaidServerManager.stopAll();
                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(Commands.literal("ctl")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .then(Commands.literal("stop")
                                                .requires(source -> source.hasPermission(2))
                                                        .executes(context -> {
                                                            CommandSourceStack stack = context.getSource();
                                                            FoliantRaidLevelManager manager = FoliantRaidServerManager.getLevelManager(stack.getLevel());
                                                            BlockPos blockPos = BlockPosArgument.getBlockPos(context, "pos");
                                                            FoliantRaid raid = manager.getRaidNearby(blockPos);

                                                            if (raid == null) {
                                                                stack.sendFailure(Component.literal("No raid found nearby"));
                                                                return Command.SINGLE_SUCCESS;
                                                            }

                                                            stack.sendSuccess(() -> Component.literal("Stopped raid at " + blockPos), false);

                                                            manager.stopRaid(raid);

                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                        )
                                        .then(Commands.literal("power")
                                                .then(Commands.literal("add")
                                                        .requires(source -> source.hasPermission(2))
                                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                                .executes(context -> {
                                                                    CommandSourceStack stack = context.getSource();
                                                                    FoliantRaidLevelManager manager = FoliantRaidServerManager.getLevelManager(stack.getLevel());
                                                                    BlockPos blockPos = BlockPosArgument.getBlockPos(context, "pos");
                                                                    FoliantRaid raid = manager.getRaidNearby(blockPos);

                                                                    if (raid == null) {
                                                                        stack.sendFailure(Component.literal("No raid found nearby"));
                                                                        return Command.SINGLE_SUCCESS;
                                                                    }

                                                                    int amount = IntegerArgumentType.getInteger(context, "amount");

                                                                    raid.addPower(amount);
                                                                    stack.sendSuccess(() -> Component.literal(String.format("Added %s power (now %s total)", amount, raid.getPower())), false);

                                                                    return Command.SINGLE_SUCCESS;
                                                                })
                                                        )
                                                )
                                                .then(Commands.literal("subtract")
                                                        .requires(source -> source.hasPermission(2))
                                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                                .executes(context -> {
                                                                    CommandSourceStack stack = context.getSource();
                                                                    FoliantRaidLevelManager manager = FoliantRaidServerManager.getLevelManager(stack.getLevel());
                                                                    BlockPos blockPos = BlockPosArgument.getBlockPos(context, "pos");
                                                                    FoliantRaid raid = manager.getRaidNearby(blockPos);

                                                                    if (raid == null) {
                                                                        stack.sendFailure(Component.literal("No raid found nearby"));
                                                                        return Command.SINGLE_SUCCESS;
                                                                    }

                                                                    int amount = IntegerArgumentType.getInteger(context, "amount");

                                                                    raid.subtractPower(amount);
                                                                    stack.sendSuccess(() -> Component.literal(String.format("Subtracted %s power (now %s total)", amount, raid.getPower())), false);

                                                                    return Command.SINGLE_SUCCESS;
                                                                })
                                                        )
                                                )
                                                .then(Commands.literal("set")
                                                        .requires(source -> source.hasPermission(2))
                                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                                .executes(context -> {
                                                                    CommandSourceStack stack = context.getSource();
                                                                    FoliantRaidLevelManager manager = FoliantRaidServerManager.getLevelManager(stack.getLevel());
                                                                    BlockPos blockPos = BlockPosArgument.getBlockPos(context, "pos");
                                                                    FoliantRaid raid = manager.getRaidNearby(blockPos);

                                                                    if (raid == null) {
                                                                        stack.sendFailure(Component.literal("No raid found nearby"));
                                                                        return Command.SINGLE_SUCCESS;
                                                                    }

                                                                    int amount = IntegerArgumentType.getInteger(context, "amount");

                                                                    raid.setPower(amount);
                                                                    stack.sendSuccess(() -> Component.literal(String.format("Power set to %s", amount)), false);

                                                                    return Command.SINGLE_SUCCESS;
                                                                })
                                                        )
                                                )
                                                .then(Commands.literal("query")
                                                        .requires(source -> source.hasPermission(2))
                                                        .executes(context -> {
                                                            CommandSourceStack stack = context.getSource();
                                                            FoliantRaidLevelManager manager = FoliantRaidServerManager.getLevelManager(stack.getLevel());
                                                            BlockPos blockPos = BlockPosArgument.getBlockPos(context, "pos");
                                                            FoliantRaid raid = manager.getRaidNearby(blockPos);

                                                            if (raid == null) {
                                                                stack.sendFailure(Component.literal("No raid found nearby"));
                                                                return Command.SINGLE_SUCCESS;
                                                            }

                                                            stack.sendSuccess(() -> Component.literal(raid.getPower() + " power"), false);

                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                                )
                                        )
                                        .then(Commands.literal("entities")
                                                .then(Commands.literal("list")
                                                        .executes(context -> {
                                                            CommandSourceStack stack = context.getSource();
                                                            FoliantRaidLevelManager manager = FoliantRaidServerManager.getLevelManager(stack.getLevel());
                                                            BlockPos blockPos = BlockPosArgument.getBlockPos(context, "pos");
                                                            FoliantRaid raid = manager.getRaidNearby(blockPos);

                                                            if (raid == null) {
                                                                stack.sendFailure(Component.literal("No raid found nearby"));
                                                                return Command.SINGLE_SUCCESS;
                                                            }

                                                            StringBuilder output = new StringBuilder();
                                                            int total = raid.getPopulation();
                                                            output.append(total).append("x total population\n");
                                                            for (FoliantRaid.EnemyType type : FoliantRaid.EnemyType.values()) {
                                                                // i don't like the yllow line :(
                                                                output.append(type).append(" ... ").append(raid.getPopulation(type))
                                                                        .append("x, overpop:").append(type.isOverpopulated(raid)).append("\n");
                                                            }

                                                            stack.sendSuccess(() -> Component.literal(output.toString()), false);

                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                                )
                                        )
                                        .then(Commands.literal("spawners")
                                                .then(Commands.literal("create")
                                                        .requires(source -> source.hasPermission(2))
                                                        .executes(context -> {
                                                            CommandSourceStack stack = context.getSource();
                                                            FoliantRaidLevelManager manager = FoliantRaidServerManager.getLevelManager(stack.getLevel());
                                                            BlockPos blockPos = BlockPosArgument.getBlockPos(context, "pos");
                                                            FoliantRaid raid = manager.getRaidNearby(blockPos);

                                                            if (raid == null) {
                                                                stack.sendFailure(Component.literal("No raid found nearby"));
                                                                return Command.SINGLE_SUCCESS;
                                                            }

                                                            if (raid.isSpawnerCenteredAt(blockPos)) {
                                                                stack.sendFailure(Component.literal("Spawner already centered on " + blockPos));
                                                                return Command.SINGLE_SUCCESS;
                                                            }

                                                            stack.sendSuccess(() -> Component.literal("Created spawner at " + blockPos), false);

                                                            raid.createSpawner(blockPos);

                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                                )
                                                .then(Commands.literal("clearNearest")
                                                        .requires(source -> source.hasPermission(2))
                                                        .executes(context -> {
                                                            CommandSourceStack stack = context.getSource();
                                                            FoliantRaidLevelManager manager = FoliantRaidServerManager.getLevelManager(stack.getLevel());
                                                            BlockPos blockPos = BlockPosArgument.getBlockPos(context, "pos");
                                                            FoliantRaid raid = manager.getRaidNearby(blockPos);

                                                            if (raid == null) {
                                                                stack.sendFailure(Component.literal("No raid found nearby"));
                                                                return Command.SINGLE_SUCCESS;
                                                            }

                                                            BlockPos spawner = raid.getSpawnerNear(blockPos);

                                                            if (spawner == null) {
                                                                stack.sendFailure(Component.literal("No spawner found near " + blockPos));
                                                                return Command.SINGLE_SUCCESS;
                                                            }

                                                            stack.sendSuccess(() -> Component.literal("Removed spawner centered on " + spawner), false);

                                                            raid.removeSpawner(spawner);

                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                                )
                                                .then(Commands.literal("clearAll")
                                                        .requires(source -> source.hasPermission(2))
                                                        .executes(context -> {
                                                            CommandSourceStack stack = context.getSource();
                                                            FoliantRaidLevelManager manager = FoliantRaidServerManager.getLevelManager(stack.getLevel());
                                                            BlockPos blockPos = BlockPosArgument.getBlockPos(context, "pos");
                                                            FoliantRaid raid = manager.getRaidNearby(blockPos);

                                                            if (raid == null) {
                                                                stack.sendFailure(Component.literal("No raid found nearby"));
                                                                return Command.SINGLE_SUCCESS;
                                                            }

                                                            stack.sendSuccess(() -> Component.literal("Removed all spawners"), false);

                                                            raid.removeAllSpawners();

                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                                )
                                                .then(Commands.literal("list")
                                                        .requires(source -> source.hasPermission(2))
                                                        .executes(context -> {
                                                            CommandSourceStack stack = context.getSource();
                                                            FoliantRaidLevelManager manager = FoliantRaidServerManager.getLevelManager(stack.getLevel());
                                                            BlockPos blockPos = BlockPosArgument.getBlockPos(context, "pos");
                                                            FoliantRaid raid = manager.getRaidNearby(blockPos);

                                                            if (raid == null) {
                                                                stack.sendFailure(Component.literal("No raid found nearby"));
                                                                return Command.SINGLE_SUCCESS;
                                                            }

                                                            StringBuilder stringBuilder = new StringBuilder("Spawners: \n ");
                                                            raid.getSpawners().forEach(spawner -> {
                                                                stringBuilder.append("[").append(spawner.toShortString()).append("], ");
                                                            });

                                                            stack.sendSuccess(() -> Component.literal(stringBuilder.toString()), false);

                                                            return Command.SINGLE_SUCCESS;
                                                        })
                                                )
                                        )
                                )
                        )

        );
    }
}
