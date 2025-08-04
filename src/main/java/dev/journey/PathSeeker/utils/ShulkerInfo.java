package dev.journey.PathSeeker.utils;

import dev.journey.PathSeeker.modules.render.InventoryInfoModule;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ShulkerInfo(String name, Type type, int color, int slot, List<ItemStack> stacks) {

    public static ShulkerInfo create(ItemStack stack, int slot) {
        if (!(stack.getItem() instanceof BlockItem) || !(((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock block))
            return null;
        List<ItemStack> items = DefaultedList.ofSize(27, ItemStack.EMPTY);
        Type type = Type.COMPACT;

        if (!Modules.get().get(InventoryInfoModule.class).compact.get()) type = Type.FULL;

        // Try the new data component system first, fallback to NBT if needed
        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
        if (container != null) {
            // Use the new data component system
            items.clear();
            int index = 0;
            for (ItemStack item : container.iterateNonEmpty()) {
                if (index < 27) {
                    items.set(index, item);
                    index++;
                }
            }
        } else {
            // Fallback to NBT system for backward compatibility
            NbtCompound nbt = stack.getComponents().getOrDefault(DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.DEFAULT).copyNbt();

            if (nbt.contains("BlockEntityTag", 10)) {
                nbt = nbt.getCompound("BlockEntityTag");
            }

            if (nbt.contains("Items", 9)) {
                Item unstackable = null;
                NbtList nbt2 = nbt.getList("Items", 10);
                RegistryWrapper.WrapperLookup wrapperLookup = MinecraftClient.getInstance().world.getRegistryManager();
                for (int i = 0; i < nbt2.size(); i++) {
                    int slot2 = nbt2.getCompound(i).contains("Slot", 99) ? nbt2.getCompound(i).getByte("Slot") : i;
                    ItemStack item = ItemStack.fromNbt(wrapperLookup, nbt2.getCompound(i)).orElse(ItemStack.EMPTY);
                    items.set(slot2, item);
                    if (item.getMaxCount() == 1) {
                        if (unstackable != null && !item.getItem().equals(unstackable)) type = Type.FULL;
                        unstackable = item.getItem();
                    }
                }
            }
        }

        if (type == Type.COMPACT) {
            Map<Item, Integer> map = new HashMap<>();
            for (ItemStack item : items) {
                if (item.isEmpty()) continue;
                map.compute(item.getItem(), (k, v) -> {
                    if (v == null) return item.getCount();
                    return v + item.getCount();
                });
            }
            items.clear();
            int k = 0;
            for (Map.Entry<Item, Integer> entry : map.entrySet()) {
                items.set(k, new ItemStack(entry.getKey(), entry.getValue()));
                k++;
            }
        }

        int color = -1;
        if (block.getColor() != null) {
            // Use the new color system in 1.21.1
            net.minecraft.util.DyeColor dyeColor = block.getColor();
            int colorValue = dyeColor.getMapColor().color;
            color = colorValue;
        }

        return new ShulkerInfo(stack.getName().getString(), type, color, slot, items);
    }

}
