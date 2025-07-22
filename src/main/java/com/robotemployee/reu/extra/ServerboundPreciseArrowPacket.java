package com.robotemployee.reu.extra;

import com.mojang.logging.LogUtils;
import com.robotemployee.reu.core.RobotEmployeeUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;

import java.util.Date;
import java.util.UUID;
import java.util.function.Supplier;

public class ServerboundPreciseArrowPacket {

    Logger LOGGER = LogUtils.getLogger();
    final UUID arrow;
    final UUID player;
    final long tick;
    final float x;
    final float y;
    final float z;
    final Date date;

    public ServerboundPreciseArrowPacket(UUID arrow, UUID player, long firedAt, Vec3 position, Date date) {
        this.arrow = arrow;
        this.player = player;
        this.tick = firedAt;
        this.x = (float)position.x;
        this.y = (float)position.y;
        this.z = (float)position.z;
        this.date = date;
    }

    public ServerboundPreciseArrowPacket(FriendlyByteBuf buf) {
        this.arrow = buf.readUUID();
        this.player = buf.readUUID();
        this.tick = buf.readLong();
        this.x = buf.readFloat();
        this.y = buf.readFloat();
        this.z = buf.readFloat();
        this.date = buf.readDate();
    }

    public void save(FriendlyByteBuf buf) {
        buf.writeUUID(arrow);
        buf.writeUUID(player);
        buf.writeLong(tick);
        buf.writeFloat(x);
        buf.writeFloat(y);
        buf.writeFloat(z);
        buf.writeDate(date);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();

        context.enqueueWork(() -> {
            //ServerPlayer sender = context.getSender();
            //if (sender == null || sender.getUUID() != this.player) return;
            MusicDiscObtainment.PRECISE_ARROWS.put(this.arrow, new MusicDiscObtainment.ArrowShotStatistics(this.tick, new Vec3(x, y, z), date));
            //LOGGER.info("Epic arrow packet recieved by server!!! Amount of epic arrows: " + MusicDiscObtainment.PRECISE_ARROWS.size());
        });
        context.setPacketHandled(true);
    }



    static final String PROTOCOL_VERSION = "reu_precisearrow";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(RobotEmployeeUtils.MODID, "main_channel"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    // evil magic number. if i ever add another packet, i'll have to change this.
    static final int ID = 1;
    public static void register() {
        INSTANCE.messageBuilder(ServerboundPreciseArrowPacket.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ServerboundPreciseArrowPacket::save)
                .decoder(ServerboundPreciseArrowPacket::new)
                .consumerMainThread(ServerboundPreciseArrowPacket::handle)
                .add();
    }
}
