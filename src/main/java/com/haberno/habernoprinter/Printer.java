package com.haberno.habernoprinter;

import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.schematic.placement.SchematicPlacement;
import fi.dy.masa.litematica.selection.Box;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import fi.dy.masa.malilib.util.InfoUtils;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.*;


public class Printer {
    public final ClientPlayerEntity player;
    public final ActionHandler actionHandler;
    private int tickCounter = 0;
    private int currentLayerIndex = 0;
    private ArrayList<Map.Entry<BlockPos, String>> statesToPlace;
    private final Map<BlockState, String> blockStateCache = new LinkedHashMap<>();

    public Printer(MinecraftClient client, ClientPlayerEntity player) {
        this.player = player;
        this.actionHandler = new ActionHandler(client, player);
    }

    public boolean onGameTick() {
        WorldSchematic worldSchematic = SchematicWorldHandler.getSchematicWorld();
        SchematicPlacement litematicSchematic = DataManager.getSchematicPlacementManager().getSelectedSchematicPlacement();

        if (worldSchematic == null || litematicSchematic == null) return false;
        if (!LitematicaMixinMod.PRINT_MODE.getBooleanValue() && !LitematicaMixinMod.PRINT.getKeybind().isPressed()) return false;

        tickCounter++;
        if (tickCounter < LitematicaMixinMod.PRINTING_INTERVAL.getIntegerValue()) return false;
        tickCounter = 0;

        if (statesToPlace == null || statesToPlace.isEmpty()) {
            Map<BlockPos, String> stateDifferences = generateStatesToPlace(worldSchematic, litematicSchematic);
            statesToPlace = new ArrayList<>(stateDifferences.entrySet());
        }

        int startIndex = currentLayerIndex * LitematicaMixinMod.BLOCKS_PER_TICK.getIntegerValue();
        int endIndex = Math.min(startIndex + LitematicaMixinMod.BLOCKS_PER_TICK.getIntegerValue(), statesToPlace.size());

        StringBuilder commandBuilder = new StringBuilder();
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<BlockPos, String> entry = statesToPlace.get(i);
            BlockPos pos = entry.getKey();
            String state = entry.getValue();

            commandBuilder.setLength(0);
            commandBuilder.append("setblock ")
                    .append(pos.getX()).append(" ")
                    .append(pos.getY()).append(" ")
                    .append(pos.getZ()).append(" ")
                    .append(state);

            player.networkHandler.sendChatCommand(commandBuilder.toString());
        }

        currentLayerIndex++;
        if (currentLayerIndex * LitematicaMixinMod.BLOCKS_PER_TICK.getIntegerValue() >= statesToPlace.size()) {
            currentLayerIndex = 0;
            statesToPlace = null;
            LitematicaMixinMod.PRINT_MODE.setBooleanValue(false);
            InfoUtils.printActionbarMessage("Printer Finished");
        }
        return true;
    }

    private Map<BlockPos, String> generateStatesToPlace(WorldSchematic worldSchematic, SchematicPlacement loadedSchematic) {
        Map<BlockPos, String> stateDifferences = new LinkedHashMap<>();
        Box schemBox = loadedSchematic.getEclosingBox();

        if (schemBox == null) {
            loadedSchematic.toggleRenderEnclosingBox();
            loadedSchematic.toggleRenderEnclosingBox();
            schemBox = loadedSchematic.getEclosingBox();
        }
        BlockPos pos1 = schemBox.getPos1();
        BlockPos pos2 = schemBox.getPos2();

        for (int y = pos1.getY(); y <= pos2.getY(); y++) {
            for (int z = pos1.getZ(); z <= pos2.getZ(); z++) {
                for (int x = pos1.getX(); x <= pos2.getX(); x++) {
                    BlockPos currentPos = new BlockPos(x, y, z);
                    BlockState targetState = worldSchematic.getBlockState(currentPos);
                    if (targetState.isAir()) continue;
                    BlockState currentState = player.getWorld().getBlockState(currentPos);

                    if (!targetState.equals(currentState) && !targetState.isAir()) {
                        String stateString = blockStateCache.computeIfAbsent(targetState, state -> {
                            String str = state.toString();
                            int endIndex = str.indexOf('}');
                            str = str.substring(6, endIndex) + str.substring(endIndex + 1);
                            return str;
                        });
                        stateDifferences.put(currentPos, stateString);
                    }
                }
            }
        }
        return stateDifferences;
    }
}