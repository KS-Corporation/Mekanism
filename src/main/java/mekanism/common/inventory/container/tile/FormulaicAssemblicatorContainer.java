package mekanism.common.inventory.container.tile;

import mekanism.common.registries.MekanismContainerTypes;
import mekanism.common.tile.machine.TileEntityFormulaicAssemblicator;
import net.minecraft.entity.player.PlayerInventory;

//Note: This class needs to exist for use in FormulaicRecipeTransferInfo
public class FormulaicAssemblicatorContainer extends MekanismTileContainer<TileEntityFormulaicAssemblicator> {

    public FormulaicAssemblicatorContainer(int id, PlayerInventory inv, TileEntityFormulaicAssemblicator tile) {
        super(MekanismContainerTypes.FORMULAIC_ASSEMBLICATOR, id, inv, tile);
    }

    @Override
    protected int getInventoryYOffset() {
        return 148;
    }
}