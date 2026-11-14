package org.blade.utils.render;

import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import org.blade.context.MinecraftContext;
import org.blade.feature.FeatureManager;
import org.blade.feature.impl.visual.CapeFeature;
import org.blade.feature.impl.visual.CosmeticsFeature;

import java.util.UUID;

public final class ClientCape {

    private ClientCape() {
    }

    public static boolean shouldForceCape(UUID playerUuid) {
        if (MinecraftContext.mc.player == null) {
            return false;
        }

        if (!MinecraftContext.mc.player.getUUID().equals(playerUuid)) {
            return false;
        }

        CosmeticsFeature cosmetics = getCosmetics();
        if (cosmetics != null && cosmetics.capeFile() != null) {
            return true;
        }

        CapeFeature feature = getFeature();
        return feature != null && feature.isEnabled();
    }

    public static PlayerSkin apply(PlayerSkin skin) {
        String fileName = null;
        CosmeticsFeature cosmetics = getCosmetics();
        if (cosmetics != null) {
            fileName = cosmetics.capeFile();
        }
        if (fileName == null) {
            CapeFeature feature = getFeature();
            fileName = (feature != null) ? feature.currentStyle().getFileName() : "glass.png";
        }

        Identifier dynamicTextureId = Identifier.fromNamespaceAndPath("blade", "textures/cape/" + fileName);

        ClientAsset.Texture dynamicTexture = new ClientAsset.Texture() {
            @Override
            public Identifier id() {
                return dynamicTextureId;
            }

            @Override
            public Identifier texturePath() {
                return dynamicTextureId;
            }
        };

        return new PlayerSkin(skin.body(), dynamicTexture, skin.elytra(), skin.model(), skin.secure());
    }

    private static CapeFeature getFeature() {
        return FeatureManager.INSTANCE.getFeature(CapeFeature.class);
    }

    private static CosmeticsFeature getCosmetics() {
        return FeatureManager.INSTANCE.getEnabled(CosmeticsFeature.class);
    }
}
