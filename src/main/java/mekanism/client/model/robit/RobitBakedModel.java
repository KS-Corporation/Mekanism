package mekanism.client.model.robit;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.api.robit.RobitSkin;
import mekanism.client.RobitSpriteUploader;
import mekanism.client.model.MekanismModelCache;
import mekanism.client.model.baked.ExtensionBakedModel;
import mekanism.client.render.lib.QuadTransformation;
import mekanism.client.render.lib.QuadTransformation.TextureFilteredTransformation;
import mekanism.client.render.lib.QuadUtils;
import mekanism.common.entity.EntityRobit;
import mekanism.common.item.ItemRobit;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemOverride;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;

public class RobitBakedModel extends ExtensionBakedModel<ResourceLocation> {

    private final RobitItemOverrideList overrideList;

    public RobitBakedModel(IBakedModel original) {
        super(original);
        this.overrideList = new RobitItemOverrideList(super.getOverrides());
    }

    @Nonnull
    @Override
    public ItemOverrideList getOverrides() {
        return overrideList;
    }

    @Override
    public List<BakedQuad> createQuads(QuadsKey<ResourceLocation> key) {
        List<BakedQuad> quads = key.getQuads();
        if (RobitSpriteUploader.UPLOADER != null) {
            ResourceLocation selectedTexture = key.getData();
            //Only replace missing textures (which should in general be #robit in the actual json without a mapping to it)
            //TODO: This technically doesn't behave quite right for textures that are not replaced given the sprites on the
            // model likely are on a different atlas than the robit textures, so the render type will be wrong
            QuadTransformation transformation = QuadTransformation.texture(RobitSpriteUploader.UPLOADER.getSprite(selectedTexture));
            transformation = TextureFilteredTransformation.of(transformation, rl -> rl.getPath().equals("missingno"));
            quads = QuadUtils.transformBakedQuads(quads, transformation);
        }
        return quads;
    }

    @Nullable
    @Override
    public QuadsKey<ResourceLocation> createKey(QuadsKey<ResourceLocation> key, IModelData data) {
        ResourceLocation skinTexture = data.getData(EntityRobit.SKIN_TEXTURE_PROPERTY);
        if (skinTexture == null) {
            return null;
        }
        return key.data(skinTexture, skinTexture.hashCode(), ResourceLocation::equals);
    }

    @Override
    protected RobitBakedModel wrapModel(IBakedModel model) {
        return new RobitBakedModel(model);
    }

    private static class RobitItemOverrideList extends ItemOverrideList {

        private final ItemOverrideList original;

        RobitItemOverrideList(ItemOverrideList original) {
            this.original = original;
        }

        @Nullable
        @Override
        public IBakedModel resolve(@Nonnull IBakedModel model, @Nonnull ItemStack stack, @Nullable ClientWorld world, @Nullable LivingEntity entity) {
            if (!stack.isEmpty() && stack.getItem() instanceof ItemRobit) {
                RobitSkin skin = ((ItemRobit) stack.getItem()).getRobitSkin(stack).getSkin();
                if (skin.getCustomModel() != null) {
                    //If the skin has a custom model look it up and if it isn't the model we are currently resolving for
                    // (to avoid stack overflow and recursion), then lookup the overrides of that model
                    IBakedModel customModel = MekanismModelCache.INSTANCE.getRobitSkin(skin);
                    if (customModel != null && customModel != model) {
                        return customModel.getOverrides().resolve(customModel, stack, world, entity);
                    }
                }
                List<ResourceLocation> textures = skin.getTextures();
                if (!textures.isEmpty()) {
                    //Assuming the skin actually has textures (it should), grab the first texture as the model data
                    ModelDataMap modelData = new ModelDataMap.Builder().withInitial(EntityRobit.SKIN_TEXTURE_PROPERTY, textures.get(0)).build();
                    // perform any overrides the original may have (most likely it doesn't have any)
                    // and then wrap the baked model so that it makes use of the model data
                    IBakedModel resolved = original.resolve(model, stack, world, entity);
                    if (resolved == null) {
                        resolved = model;
                    }
                    return new RobitModelDataBakedModel(resolved, modelData);
                }
            }
            return original.resolve(model, stack, world, entity);
        }

        @Nonnull
        @Override
        public ImmutableList<ItemOverride> getOverrides() {
            return original.getOverrides();
        }
    }
}