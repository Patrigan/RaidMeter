package com.buuz135.raidmeter.command;

import com.buuz135.raidmeter.meter.RaidMeterObject;
import com.buuz135.raidmeter.storage.RaidMeterWorldSavedData;
import com.buuz135.raidmeter.util.MeterPosition;
import com.buuz135.raidmeter.util.MeterRenderType;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.EntitySelector;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.util.text.StringTextComponent;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

public class RaidMeterCommandHandler {


    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(
                Commands.literal("raidmeter")
                        .then(getAdd())
                        .then(getRemove())
                        .then(getModify())
                        .then(getInfo())
        );
    }

    public static LiteralArgumentBuilder<CommandSource> getAdd() {
        return Commands.literal("add")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("display_name", StringArgumentType.string())
                                .then(Commands.argument("max_amount", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                        .then(Commands.argument("current_amount", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                                .then(Commands.argument("position", StringArgumentType.word()).suggests((context, builder) -> ISuggestionProvider.suggest(Arrays.stream(MeterPosition.values()).map(Enum::name).collect(Collectors.toList()), builder))
                                                        .then(Commands.argument("type", StringArgumentType.word()).suggests((context, builder) -> ISuggestionProvider.suggest(Arrays.stream(MeterRenderType.values()).map(Enum::name).collect(Collectors.toList()), builder))
                                                                .executes(RaidMeterCommandHandler::add)
                                                        )
                                                )
                                        )
                                )
                        )
                );
    }

    public static LiteralArgumentBuilder<CommandSource> getRemove() {
        return Commands.literal("remove")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> ISuggestionProvider.suggest(RaidMeterWorldSavedData.getInstance(context.getSource().getWorld()).map(raidMeterWorldSavedData -> raidMeterWorldSavedData.getMeters().keySet()).orElse(Collections.EMPTY_SET), builder))
                        .executes(RaidMeterCommandHandler::remove)
                );
    }

    public static LiteralArgumentBuilder<CommandSource> getModify() {
        return Commands.literal("modify")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> ISuggestionProvider.suggest(RaidMeterWorldSavedData.getInstance(context.getSource().getWorld()).map(raidMeterWorldSavedData -> raidMeterWorldSavedData.getMeters().keySet()).orElse(Collections.EMPTY_SET), builder))
                        .then(Commands.literal("max_amount").then(Commands.argument("max_amount", IntegerArgumentType.integer(0, Integer.MAX_VALUE)).executes(context -> modify(context, ModifyType.MAX_AMOUNT))))
                        .then(Commands.literal("current_amount").then(Commands.argument("amount", IntegerArgumentType.integer(0, Integer.MAX_VALUE)).executes(context -> modify(context, ModifyType.CURRENT_AMOUNT))))
                        .then(Commands.literal("add").then(Commands.argument("amount", IntegerArgumentType.integer(Integer.MIN_VALUE, Integer.MAX_VALUE)).executes(context -> modify(context, ModifyType.ADD))))
                        .then(Commands.literal("set").then(Commands.argument("amount", IntegerArgumentType.integer(Integer.MIN_VALUE, Integer.MAX_VALUE)).executes(context -> modify(context, ModifyType.ADD))))
                        .then(Commands.literal("position").then(Commands.argument("position", StringArgumentType.word()).suggests((context, builder) -> ISuggestionProvider.suggest(Arrays.stream(MeterPosition.values()).map(Enum::name).collect(Collectors.toList()), builder)).executes(context -> modify(context, ModifyType.POSITION))))
                        .then(Commands.literal("type").then(Commands.argument("type", StringArgumentType.word()).suggests((context, builder) -> ISuggestionProvider.suggest(Arrays.stream(MeterRenderType.values()).map(Enum::name).collect(Collectors.toList()), builder)).executes(context -> modify(context, ModifyType.TYPE))))
                        .then(Commands.literal("name").then(Commands.argument("name", StringArgumentType.string()).executes(context -> modify(context, ModifyType.NAME))))
                        .then(Commands.literal("color").then(Commands.argument("color", StringArgumentType.string()).suggests((context, builder) -> ISuggestionProvider.suggest(Arrays.stream(DyeColor.values()).map(Enum::name).collect(Collectors.toList()), builder)).executes(context -> modify(context, ModifyType.COLOR))))
                        .then(Commands.literal("display_add").then(Commands.argument("player", EntityArgument.players()).executes(context -> modify(context, ModifyType.DISPLAY_ADD))))
                        .then(Commands.literal("display_remove").then(Commands.argument("player", EntityArgument.players()).executes(context -> modify(context, ModifyType.DISPLAY_REMOVE))))
                );
    }

    public static LiteralArgumentBuilder<CommandSource> getInfo() {
        return Commands.literal("info")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> ISuggestionProvider.suggest(RaidMeterWorldSavedData.getInstance(context.getSource().getWorld()).map(raidMeterWorldSavedData -> raidMeterWorldSavedData.getMeters().keySet()).orElse(Collections.EMPTY_SET), builder))
                        .then(Commands.literal("max_amount").executes(context -> info(context, ModifyType.MAX_AMOUNT)))
                        .then(Commands.literal("current_amount").executes(context -> info(context, ModifyType.CURRENT_AMOUNT)))
                        .then(Commands.literal("position").executes(context -> info(context, ModifyType.POSITION)))
                        .then(Commands.literal("type").executes(context -> info(context, ModifyType.TYPE)))
                        .then(Commands.literal("name").executes(context -> info(context, ModifyType.NAME)))
                        .then(Commands.literal("color").executes(context -> info(context, ModifyType.COLOR)))
                );
    }

    private static int remove(CommandContext<CommandSource> context) {
        RaidMeterWorldSavedData data = RaidMeterWorldSavedData.getInstance(context.getSource().getWorld()).orElse(null);
        if (data != null) {
            data.getMeters().remove(context.getArgument("id", String.class));
            data.markDirty(context.getSource().getWorld());
            return 1;
        }
        return 0;
    }

    private static int modify(CommandContext<CommandSource> context, ModifyType type) throws CommandSyntaxException {
        RaidMeterWorldSavedData data = RaidMeterWorldSavedData.getInstance(context.getSource().getWorld()).orElse(null);
        if (data != null) {
            RaidMeterObject meterObject = data.getMeters().get(context.getArgument("id", String.class));
            if (meterObject != null) {
                if (type == ModifyType.MAX_AMOUNT) {
                    meterObject.setMaxProgress(context.getArgument("max_amount", Integer.class));
                }
                if (type == ModifyType.CURRENT_AMOUNT) {
                    meterObject.setCurrentProgress(context.getArgument("amount", Integer.class));
                }
                if (type == ModifyType.ADD) {
                    meterObject.add(context.getArgument("amount", Integer.class));
                }
                if (type == ModifyType.SET) {
                    meterObject.setCurrentProgress(context.getArgument("amount", Integer.class));
                }
                if (type == ModifyType.POSITION) {
                    meterObject.setMeterPosition(MeterPosition.valueOf(context.getArgument("position", String.class)));
                }
                if (type == ModifyType.TYPE) {
                    meterObject.setMeterRenderType(MeterRenderType.valueOf(context.getArgument("type", String.class)));
                }
                if (type == ModifyType.NAME) {
                    meterObject.setName(context.getArgument("name", String.class));
                }
                if (type == ModifyType.COLOR) {
                    meterObject.setColor(DyeColor.valueOf(context.getArgument("color", String.class)).getTextColor());
                }
                if (type == ModifyType.DISPLAY_ADD) {
                    for (ServerPlayerEntity player : context.getArgument("player", EntitySelector.class).selectPlayers(context.getSource())) {
                        String uuid = player.getUniqueID().toString();
                        if (!meterObject.getVisibleToPlayers().contains(uuid)) {
                            meterObject.getVisibleToPlayers().add(uuid);
                        }
                    }
                }
                if (type == ModifyType.DISPLAY_REMOVE) {
                    for (ServerPlayerEntity player : context.getArgument("player", EntitySelector.class).selectPlayers(context.getSource())) {
                        String uuid = player.getUniqueID().toString();
                        meterObject.getVisibleToPlayers().remove(uuid);
                    }
                }
                data.markDirty(context.getSource().getWorld());
            }
            return 1;
        }
        return 0;
    }

    private static int info(CommandContext<CommandSource> context, ModifyType type) throws CommandSyntaxException {
        RaidMeterWorldSavedData data = RaidMeterWorldSavedData.getInstance(context.getSource().getWorld()).orElse(null);
        if (data != null) {
            RaidMeterObject meterObject = data.getMeters().get(context.getArgument("id", String.class));
            if (meterObject != null) {
                if (type == ModifyType.MAX_AMOUNT) {
                    context.getSource().asPlayer().sendStatusMessage(new StringTextComponent("Max amount: " + meterObject.getMaxProgress()), false);
                }
                if (type == ModifyType.CURRENT_AMOUNT) {
                    context.getSource().asPlayer().sendStatusMessage(new StringTextComponent("Current amount: " + meterObject.getCurrentProgress()), false);
                }
                if (type == ModifyType.POSITION) {
                    context.getSource().asPlayer().sendStatusMessage(new StringTextComponent("Position: " + meterObject.getMeterPosition().name()), false);
                }
                if (type == ModifyType.TYPE) {
                    context.getSource().asPlayer().sendStatusMessage(new StringTextComponent("Type: " + meterObject.getMeterRenderType().name()), false);
                }
                if (type == ModifyType.NAME) {
                    context.getSource().asPlayer().sendStatusMessage(new StringTextComponent("Name: " + meterObject.getName()), false);
                }
                if (type == ModifyType.COLOR) {
                    context.getSource().asPlayer().sendStatusMessage(new StringTextComponent("Color: " + meterObject.getColor()), false);
                }
                data.markDirty(context.getSource().getWorld());
            }
            return 1;
        }
        return 0;
    }

    private static int add(CommandContext<CommandSource> context) {
        RaidMeterWorldSavedData data = RaidMeterWorldSavedData.getInstance(context.getSource().getWorld()).orElse(null);
        if (data != null) {
            RaidMeterObject object = new RaidMeterObject(
                    context.getArgument("id", String.class),
                    context.getArgument("display_name", String.class),
                    context.getArgument("max_amount", Integer.class),
                    context.getArgument("current_amount", Integer.class),
                    MeterPosition.valueOf(context.getArgument("position", String.class)),
                    MeterRenderType.valueOf(context.getArgument("type", String.class))
            );
            try {
                object.getVisibleToPlayers().add(context.getSource().asPlayer().getUniqueID().toString());
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
            }
            data.getMeters().put(context.getArgument("id", String.class), object);
            data.markDirty(context.getSource().getWorld());
            return 1;
        }
        return 0;
    }

    public static enum ModifyType {
        MAX_AMOUNT,
        CURRENT_AMOUNT,
        ADD,
        POSITION,
        TYPE,
        NAME,
        COLOR,
        SET,
        DISPLAY_ADD,
        DISPLAY_REMOVE;
    }

}
