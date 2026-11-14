package org.blade.mixin.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.blade.feature.impl.visual.CosmeticsFeature;
import org.blade.utils.render.cosmetics.CosmeticWeapons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverMixin {
    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("blade-cosmetics");
    @Unique
    private static final Set<Identifier> REPORTED = ConcurrentHashMap.newKeySet();

    @WrapOperation(method = "appendItemLayers", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;getItemModel(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/item/ItemModel;"))
    private ItemModel blade$cosmeticSword(ItemModelResolver resolver, Identifier model, Operation<ItemModel> original,
                                          @Local(argsOnly = true) ItemStack stack,
                                          @Local(argsOnly = true) ItemDisplayContext context,
                                          @Local(argsOnly = true) ItemOwner owner) {
        CosmeticsFeature feature = CosmeticsFeature.getEnabled();
        if (feature == null || !feature.appliesToSword(stack)) {
            return original.call(resolver, model);
        }
        if (context.firstPerson() && !feature.showFirstPerson()) {
            return original.call(resolver, model);
        }
        if (owner != null && owner != Minecraft.getInstance().player && !feature.renderOthers()) {
            return original.call(resolver, model);
        }
        CosmeticsFeature.Cosmetic sword = feature.selected(CosmeticsFeature.Category.SWORDS);
        if (sword == null) {
            return original.call(resolver, model);
        }
        Identifier cosmetic = CosmeticWeapons.itemModel(sword);
        if (REPORTED.add(cosmetic)) {
            LOGGER.info("Replacing sword model {} with {}", model, cosmetic);
        }
        return original.call(resolver, cosmetic);
    }
}
