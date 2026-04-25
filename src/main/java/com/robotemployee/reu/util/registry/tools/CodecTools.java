package com.robotemployee.reu.util.registry.tools;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public class CodecTools {


    public static final <T> StreamCodec<FriendlyByteBuf, T> streamCodecFromNormalCodec(Codec<T> codec) {
        return StreamCodec.of(
                (buf, t) -> {
                    buf.writeNbt(codec.encodeStart(NbtOps.INSTANCE, t).getOrThrow());
                },
                friendlyByteBuf -> {
                    return codec.decode(NbtOps.INSTANCE, friendlyByteBuf.readNbt()).getOrThrow().getFirst();
                });
    }
}
