package mekanism.common.tile.base;

import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import io.netty.buffer.ByteBuf;
import javax.annotation.Nonnull;
import mekanism.api.TileNetworkList;
import mekanism.common.base.IBlockProvider;
import mekanism.common.base.IEnergyWrapper;
import mekanism.common.block.interfaces.IBlockElectric;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.CapabilityWrapperManager;
import mekanism.common.config.MekanismConfig;
import mekanism.common.integration.MekanismHooks;
import mekanism.common.integration.forgeenergy.ForgeEnergyIntegration;
import mekanism.common.integration.ic2.IC2Integration;
import mekanism.common.tile.interfaces.ITileElectric;
import mekanism.common.util.CapabilityUtils;
import mekanism.common.util.MekanismUtils;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Optional.Method;

public abstract class TileEntityElectric extends TileEntityMekanism implements ITileElectric {

    /**
     * How much energy is stored in this block.
     */
    private double electricityStored;

    //TODO
    private IBlockElectric electricBlock;

    /**
     * Actual maximum energy storage, including upgrades
     */
    private double maxEnergy;

    private final double BASE_ENERGY_PER_TICK;

    private double energyPerTick;

    private boolean ic2Registered = false;
    private CapabilityWrapperManager<IEnergyWrapper, ForgeEnergyIntegration> forgeEnergyManager = new CapabilityWrapperManager<>(IEnergyWrapper.class, ForgeEnergyIntegration.class);

    public TileEntityElectric(IBlockProvider blockProvider) {
        super(blockProvider);
        electricBlock = (IBlockElectric) blockProvider.getBlock();
        setMaxEnergy(electricBlock.getStorage());
        BASE_ENERGY_PER_TICK = energyPerTick = electricBlock.getUsage();
    }

    @Method(modid = MekanismHooks.IC2_MOD_ID)
    public void register() {
        if (!world.isRemote && !ic2Registered) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
            ic2Registered = true;
        }
    }

    @Method(modid = MekanismHooks.IC2_MOD_ID)
    public void deregister() {
        if (!world.isRemote && ic2Registered) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
            ic2Registered = false;
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (MekanismUtils.useIC2()) {
            register();
        }
    }

    @Override
    public void onUpdate() {
		/*if(MekanismUtils.useIC2()) {
			register();
		}*/
    }

    @Override
    public boolean canOutputEnergy(EnumFacing side) {
        return false;
    }

    @Override
    public boolean canReceiveEnergy(EnumFacing side) {
        return true;
    }

    @Override
    public double getMaxOutput() {
        return 0;
    }

    @Override
    public double getEnergy() {
        return electricityStored;
    }

    @Override
    public void setEnergy(double energy) {
        electricityStored = Math.max(Math.min(energy, getMaxEnergy()), 0);
        MekanismUtils.saveChunk(this);
    }

    @Override
    public double getMaxEnergy() {
        return maxEnergy;
    }

    @Override
    public void handlePacketData(ByteBuf dataStream) {
        super.handlePacketData(dataStream);
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            setEnergy(dataStream.readDouble());
        }
    }

    @Override
    public TileNetworkList getNetworkedData(TileNetworkList data) {
        super.getNetworkedData(data);
        data.add(getEnergy());
        return data;
    }

    @Override
    public void onChunkUnload() {
        if (MekanismUtils.useIC2()) {
            deregister();
        }
        super.onChunkUnload();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (MekanismUtils.useIC2()) {
            deregister();
        }
    }

    @Override
    public void validate() {
        boolean wasInvalid = this.tileEntityInvalid;//workaround for pending tile entity invalidate/revalidate cycle
        super.validate();
        if (wasInvalid && MekanismUtils.useIC2()) {//re-register if we got invalidated
            register();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound nbtTags) {
        super.readFromNBT(nbtTags);
        electricityStored = nbtTags.getDouble("electricityStored");
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbtTags) {
        super.writeToNBT(nbtTags);
        nbtTags.setDouble("electricityStored", getEnergy());
        return nbtTags;
    }

    /**
     * Gets the scaled energy level for the GUI.
     *
     * @param i - multiplier
     *
     * @return scaled energy
     */
    public int getScaledEnergyLevel(int i) {
        return (int) (getEnergy() * i / getMaxEnergy());
    }

    @Override
    @Method(modid = MekanismHooks.IC2_MOD_ID)
    public int addEnergy(int amount) {
        if (!MekanismConfig.current().general.blacklistIC2.val()) {
            setEnergy(getEnergy() + IC2Integration.fromEU(amount));
            return IC2Integration.toEUAsInt(getEnergy());
        }
        return 0;
    }

    @Override
    @Method(modid = MekanismHooks.IC2_MOD_ID)
    public double getDemandedEnergy() {
        return !MekanismConfig.current().general.blacklistIC2.val() ? IC2Integration.toEU(getNeededEnergy()) : 0;
    }

    @Override
    @Method(modid = MekanismHooks.IC2_MOD_ID)
    public double injectEnergy(EnumFacing pushDirection, double amount, double voltage) {
        // nb: the facing param contains the side relative to the pushing block
        TileEntity tile = MekanismUtils.getTileEntity(world, getPos().offset(pushDirection.getOpposite()));
        if (MekanismConfig.current().general.blacklistIC2.val() || CapabilityUtils.hasCapability(tile, Capabilities.GRID_TRANSMITTER_CAPABILITY, pushDirection)) {
            return amount;
        }
        return amount - IC2Integration.toEU(acceptEnergy(pushDirection.getOpposite(), IC2Integration.fromEU(amount), false));
    }

    @Override
    @Method(modid = MekanismHooks.IC2_MOD_ID)
    public void drawEnergy(double amount) {
        setEnergy(Math.max(getEnergy() - IC2Integration.fromEU(amount), 0));
    }

    @Override
    public double acceptEnergy(EnumFacing side, double amount, boolean simulate) {
        double toUse = Math.min(getNeededEnergy(), amount);
        if (toUse < 0.0001 || (side != null && !canReceiveEnergy(side))) {
            return 0;
        }
        if (!simulate) {
            setEnergy(getEnergy() + toUse);
        }
        return toUse;
    }

    @Override
    public double pullEnergy(EnumFacing side, double amount, boolean simulate) {
        double toGive = Math.min(getEnergy(), amount);
        if (toGive < 0.0001 || (side != null && !canOutputEnergy(side))) {
            return 0;
        }
        if (!simulate) {
            setEnergy(getEnergy() - toGive);
        }
        return toGive;
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return false;
        }
        return isStrictEnergy(capability) || capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, side);
    }

    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, EnumFacing side) {
        if (isCapabilityDisabled(capability, side)) {
            return null;
        } else if (isStrictEnergy(capability)) {
            return (T) this;
        } else if (capability == CapabilityEnergy.ENERGY) {
            return CapabilityEnergy.ENERGY.cast(getForgeEnergyWrapper(side));
        }
        return super.getCapability(capability, side);
    }

    protected boolean isStrictEnergy(@Nonnull Capability capability) {
        return capability == Capabilities.ENERGY_STORAGE_CAPABILITY || capability == Capabilities.ENERGY_ACCEPTOR_CAPABILITY || capability == Capabilities.ENERGY_OUTPUTTER_CAPABILITY;
    }

    protected ForgeEnergyIntegration getForgeEnergyWrapper(EnumFacing side) {
        return forgeEnergyManager.getWrapper(this, side);
    }

    @Override
    public boolean isCapabilityDisabled(@Nonnull Capability<?> capability, EnumFacing side) {
        if (isStrictEnergy(capability) || capability == CapabilityEnergy.ENERGY) {
            return side != null && !canReceiveEnergy(side) && !canOutputEnergy(side);
        }
        return super.isCapabilityDisabled(capability, side);
    }

    protected void setMaxEnergy(double maxEnergy) {
        this.maxEnergy = maxEnergy;
    }

    public double getBaseUsage() {
        return electricBlock.getUsage();
    }

    public double getBaseStorage() {
        return electricBlock.getStorage();
    }

    public double getBaseEnergyPerTick() {
        return BASE_ENERGY_PER_TICK;
    }

    public double getNeededEnergy() {
        return getMaxEnergy() - getEnergy();
    }

    public double getEnergyPerTick() {
        return energyPerTick;
    }

    public void setEnergyPerTick(double energyPerTick) {
        this.energyPerTick = energyPerTick;
    }
}