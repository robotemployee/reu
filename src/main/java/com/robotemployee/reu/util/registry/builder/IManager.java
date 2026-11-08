package com.robotemployee.reu.util.registry.builder;

import net.neoforged.bus.api.IEventBus;

public interface IManager {
    public void register(IEventBus modEventBus);
}
