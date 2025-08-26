package com.robotemployee.reu.banana.entity.ai;

import com.robotemployee.reu.mixin.base.MoveControlAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;

import java.util.Map;

/**
 * <p>This class allows you to have multiple "movement modes" in a single MoveControl that you can switch between for different behavior.</p>
 * <p><b>T</b> is the type used for the keys of the map in which movements are stored.</p>
 * <p>Speaking of which, the movements are stored in a Map. I did it this way to prevent the use of magic numbers and encourage enums</p>
 * <p><b>Note: </b>When the movement key is changed, it copies the wanted position, speed modifier, and strafe from the old movement to the new one.</p>
 * */
public class MultiMoveControl<T> extends MoveControl {
    private final Map<T, MoveControl> movements;
    private T movementKey;

    public MultiMoveControl(Mob mob, T initialMovementKey, Map<T, MoveControl> movements) {
        super(mob);
        this.movements = movements;
        if (!movements.containsKey(initialMovementKey)) throw new IllegalStateException(String.format("Attempted to change movement mode using a key that doesn't exist: %s applied to %s", initialMovementKey, movements));
        movementKey = initialMovementKey;
    }

    public void setMovement(T newMovementKey, boolean keepWantedLocation) {
        MoveControl oldMovement = getCurrentMovement();
        MoveControl newMovement = getMovement(newMovementKey);
        ((MoveControlAccessor)oldMovement).setOperation(Operation.WAIT);

        // we have to do this no matter what -
        // let's say we're swapping from a controller that controls Yya to one that doesn't
        // Yya will be set to some value and *never reset or changed* by the horizontal-only controller.
        // Because of that, we must assume that controllers will not set their own movement commands for any given axis
        // So we will allow them to populate their own... after we reset the commands
        mob.setZza(0);
        mob.setYya(0);
        mob.setXxa(0);

        if (keepWantedLocation) {
            double wantedX = oldMovement.getWantedX();
            double wantedY = oldMovement.getWantedY();
            double wantedZ = oldMovement.getWantedZ();
            double speedmod = oldMovement.getSpeedModifier();

            newMovement.setWantedPosition(wantedX, wantedY, wantedZ, speedmod);
        }
        // we can't get the operation from the oldMovement
        // and i don't know how to create an accessor in a mixin which gives you an object of a type that isn't public (MoveControl.Operation)
        // soooo we just won't copy the strafe
        //if (operation == Operation.STRAFE) getCurrentMovement().strafe(strafeForwards, strafeRight);

        this.movementKey = newMovementKey;
    }

    public MoveControl getCurrentMovement() {
        return getMovement(movementKey);
    }

    public MoveControl getMovement(T movementKey) {
        return movements.get(movementKey);
    }

    public T getKey() {
        return movementKey;
    }

    @Override
    public void tick() {
        getCurrentMovement().tick();
    }

    @Override
    public void setWantedPosition(double x, double y, double z, double speedmod) {
        getCurrentMovement().setWantedPosition(x, y, z ,speedmod);
    }

    @Override
    public void strafe(float strafeForwards, float strafeRight) {
        getCurrentMovement().strafe(strafeForwards, strafeRight);
    }

    @Override
    public boolean hasWanted() {
        return getCurrentMovement().hasWanted();
    }

    @Override
    public double getSpeedModifier() {
        return getCurrentMovement().getSpeedModifier();
    }

    @Override
    public double getWantedX() {
        return getCurrentMovement().getWantedX();
    }

    @Override
    public double getWantedY() {
        return getCurrentMovement().getWantedY();
    }

    @Override
    public double getWantedZ() {
        return getCurrentMovement().getWantedZ();
    }
}
