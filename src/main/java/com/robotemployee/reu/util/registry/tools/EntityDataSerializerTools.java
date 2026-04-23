package com.robotemployee.reu.util.registry.tools;

import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class EntityDataSerializerTools {

    public static final DeferredRegister<EntityDataSerializer<?>> SERIALIZERS = DeferredRegister.create(NeoForgeRegistries.Keys.ENTITY_DATA_SERIALIZERS, RobotEmployeeUtils.MODID);
    public static final Supplier<EntityDataSerializer<List<Integer>>> INTEGER_LIST = SERIALIZERS.register("integer_list", () -> new EntityDataSerializer<List<Integer>>() {
        public void write(RegistryFriendlyByteBuf buffer, List<Integer> list) {
            buffer.writeInt(list.size());
            for (Integer id : list) {
                buffer.writeInt(id);
            }
        }

        @NotNull
        public List<Integer> read(RegistryFriendlyByteBuf buffer) {
            int size = buffer.readInt();
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < size; i++) {
                list.add(buffer.readInt());
            }
            return list;
        }

        @Override
        @NotNull
        public StreamCodec<? super RegistryFriendlyByteBuf, List<Integer>> codec() {
            return StreamCodec.of(this::write, this::read);
        }

        @Override
        @NotNull
        public List<Integer> copy(@NotNull List<Integer> list) {
            return new ArrayList<>(list);
        }
    });
}
