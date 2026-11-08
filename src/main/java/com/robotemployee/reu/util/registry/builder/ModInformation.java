package com.robotemployee.reu.util.registry.builder;

import com.robotemployee.reu.util.datagen.DatagenInstance;
import net.neoforged.bus.api.IEventBus;

import java.lang.ref.WeakReference;

public class ModInformation {
    private final String namespace;
    private final WeakReference<IEventBus> modEventBus;
    private final DatagenInstance datagenInstance;

    public ModInformation(String namespace, IEventBus modEventBus, DatagenInstance datagenInstance) {
        this.namespace = namespace;
        this.modEventBus = new WeakReference<>(modEventBus);
        this.datagenInstance = datagenInstance;
    }

    public String getNamespace() {
        return namespace;
    }

    public IEventBus getModEventBus() {
        IEventBus bus = modEventBus.get();
        if (bus == null) throw new IllegalStateException("Attempted to get the mod event bus after it was discarded");
        return bus;
    }

    public DatagenInstance getDatagenInstance() {
        return datagenInstance;
    }
}
