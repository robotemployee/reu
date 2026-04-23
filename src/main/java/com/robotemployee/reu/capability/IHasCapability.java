package com.robotemployee.reu.capability;

import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.apache.http.annotation.Contract;

import java.util.List;
import java.util.function.Supplier;

/**
 * if you have an Item, Block, BlockEntity, whatever, that inherits from this, their Builders will call this method in the capability registry event.
 * this is so that the capability registry code can be in the class like it was before.
 */

public interface IHasCapability<T> {
    public void onRegisteringCapabilities(RegisterCapabilitiesEvent event);
}
