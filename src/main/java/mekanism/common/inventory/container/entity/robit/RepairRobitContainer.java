package mekanism.common.inventory.container.entity.robit;

import javax.annotation.Nonnull;
import mekanism.common.entity.EntityRobit;
import mekanism.common.inventory.container.ISecurityContainer;
import mekanism.common.inventory.container.entity.IEntityContainer;
import mekanism.common.lib.security.ISecurityObject;
import mekanism.common.registries.MekanismContainerTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.RepairContainer;

public class RepairRobitContainer extends RepairContainer implements IEntityContainer<EntityRobit>, ISecurityContainer {

    private final EntityRobit entity;

    public RepairRobitContainer(int id, PlayerInventory inv, EntityRobit robit) {
        super(id, inv, robit.getWorldPosCallable());
        this.entity = robit;
        entity.open(inv.player);
    }

    @Override
    public boolean stillValid(@Nonnull PlayerEntity player) {
        return entity.isAlive();
    }

    @Nonnull
    @Override
    public EntityRobit getEntity() {
        return entity;
    }

    @Nonnull
    @Override
    public ContainerType<?> getType() {
        return MekanismContainerTypes.REPAIR_ROBIT.getContainerType();
    }

    @Override
    public void removed(@Nonnull PlayerEntity player) {
        super.removed(player);
        entity.close(player);
    }

    @Override
    public ISecurityObject getSecurityObject() {
        return entity;
    }
}