package mekanism.client.render.obj;

import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.IModelTransform;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.model.Material;
import net.minecraft.client.renderer.model.ModelBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.geometry.IModelGeometryPart;
import net.minecraftforge.client.model.geometry.IMultipartModelGeometry;
import net.minecraftforge.client.model.obj.OBJModel;

public class TransmitterModel implements IMultipartModelGeometry<TransmitterModel> {

    private final OBJModel internal;

    public TransmitterModel(OBJModel internalModel) {
        this.internal = internalModel;
    }

    @Override
    public Collection<? extends IModelGeometryPart> getParts() {
        return internal.getParts();
    }

    @Override
    public Optional<? extends IModelGeometryPart> getPart(String name) {
        return internal.getPart(name);
    }

    @Override
    public IBakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, IModelTransform modelTransform,
          ItemOverrideList overrides, ResourceLocation modelLocation) {
        return new TransmitterBakedModel(internal, owner, bakery, spriteGetter, modelTransform, overrides, modelLocation);
    }

    @Override
    public Collection<Material> getTextures(IModelConfiguration owner, Function<ResourceLocation, IUnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
        Set<Material> combined = Sets.newHashSet();
        IModelConfiguration configuration = new OpaqueModelConfiguration(owner);
        for (IModelGeometryPart part : getParts()) {
            combined.addAll(part.getTextures(owner, modelGetter, missingTextureErrors));
            //Add the opaque versions of the textures as well
            combined.addAll(part.getTextures(configuration, modelGetter, missingTextureErrors));
        }
        return combined;
    }
}