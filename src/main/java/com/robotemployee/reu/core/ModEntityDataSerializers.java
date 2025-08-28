package com.robotemployee.reu.core;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ModEntityDataSerializers {

    public static final DeferredRegister<EntityDataSerializer<?>> SERIALIZERS = DeferredRegister.create(ForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, RobotEmployeeUtils.MODID);
    public static final RegistryObject<EntityDataSerializer<List<Integer>>> INTEGER_LIST = SERIALIZERS.register("integer_list", () -> new EntityDataSerializer<>() {
        @Override
        public void write(FriendlyByteBuf buffer, List<Integer> list) {
            buffer.writeInt(list.size());
            for (Integer id : list) {
                buffer.writeInt(id);
            }
        }

        @Override
        @NotNull
        public List<Integer> read(FriendlyByteBuf buffer) {
            int size = buffer.readInt();
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                list.add(buffer.readInt());
            }
            return list;
        }

        @Override
        @NotNull
        public List<Integer> copy(@NotNull List<Integer> list) {
            return new ArrayList<>(list);
        }
    });
}
