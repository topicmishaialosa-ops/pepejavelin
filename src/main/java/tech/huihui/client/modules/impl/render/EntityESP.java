package tech.huihui.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4d;
import org.joml.Vector4f;
import tech.huihui.HuihuiClient;
import tech.huihui.base.events.impl.render.EventRender2D;
import tech.huihui.base.font.Fonts;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.impl.misc.NameProtect;
import tech.huihui.client.modules.impl.misc.ScoreboardHealth;
import tech.huihui.utility.game.other.ReplaceUtil;
import tech.huihui.utility.game.player.PlayerIntersectionUtil;
import tech.huihui.utility.math.ProjectionUtil;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

@ModuleAnnotation(
   name = "NameTags",
   category = Category.RENDER,
   description = "Показывает информацию о игроке"
)
public final class EntityESP extends Module {
   public static final EntityESP INSTANCE = new EntityESP();
   private static final Text GRAY_OPEN = Text.literal(" [").setStyle(Style.EMPTY.withColor(Formatting.GRAY));
   private static final Text GRAY_CLOSE = Text.literal("]").setStyle(Style.EMPTY.withColor(Formatting.GRAY));
   private static final ColorRGBA BG_DARK = new ColorRGBA(0, 0, 0, 123);
   private static final ColorRGBA BG_FRIEND = new ColorRGBA(0, 166, 0, 123);
   private static final int ENCH_DANGER_RGB = (new ColorRGBA(212, 45, 43, 255)).getRGB();
   private static final EquipmentSlot[] EQUIP_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private final HashMap<Entity, Text> nameCache = new HashMap();
   private final HashMap<Entity, Text> itemNameCache = new HashMap();
   private final HashMap<Entity, TagCache> tagCache = new HashMap();
   private final IdentityHashMap<ItemStack, EnchText[]> enchCache = new IdentityHashMap();
   private final ItemStack[] itemArray = new ItemStack[6];
   private int cacheCleanupTick;

   private static final class TagCache {
      int hpInt;
      Text name;
      Text text;
      float width;
   }

   private static final class EnchText {
      String text;
      float width;
      int color;
   }

   @EventTarget
   private void onRender(EventRender2D e) {
      if (mc.world != null && mc.player != null) {
         if (++this.cacheCleanupTick % 50 == 0) {
            this.nameCache.entrySet().removeIf((entry) -> entry.getKey().isRemoved());
            this.itemNameCache.entrySet().removeIf((entry) -> entry.getKey().isRemoved());
            this.tagCache.entrySet().removeIf((entry) -> entry.getKey().isRemoved());
         }
         if (this.enchCache.size() > 128) {
            this.enchCache.clear();
         }
         float tickDelta = e.getTickDelta();
         this.renderPlayerTags(tickDelta, e);
         this.renderItemTags(tickDelta, e);
      }
   }

   private Text cachedName(Entity entity) {
      if (entity == mc.player && NameProtect.INSTANCE.isEnabled()) {
         return Text.literal(NameProtect.getCustomName());
      }
      Text cached = this.nameCache.get(entity);
      if (cached == null || mc.player.age - entity.age > 5) {
         cached = ReplaceUtil.replaceSymbols(entity.getDisplayName());
         this.nameCache.put(entity, cached);
      }
      return cached;
   }

   private void renderPlayerTags(float tickDelta, EventRender2D e) {
      Iterator var3 = mc.world.getPlayers().iterator();

      while(var3.hasNext()) {
         PlayerEntity entity = (PlayerEntity)var3.next();
         if (entity == mc.player && !mc.getEntityRenderDispatcher().camera.isThirdPerson()) {
            continue;
         }
         if (entity.squaredDistanceTo(mc.player) > 9216.0D) {
            continue;
         }
         if (!ProjectionUtil.canSee(entity.getBoundingBox().getCenter())) {
            continue;
         }
         double x = MathHelper.lerp((double)tickDelta, entity.lastRenderX, entity.getX());
         double y = MathHelper.lerp((double)tickDelta, entity.lastRenderY, entity.getY()) + (double)entity.getHeight() + 0.2D;
         double z = MathHelper.lerp((double)tickDelta, entity.lastRenderZ, entity.getZ());
         Vec3d pos = ProjectionUtil.worldSpaceToScreenSpace(new Vec3d(x, y, z));
         if (pos.z <= 0.0D || pos.z >= 1.0D) {
            continue;
         }
         Vector4d position = ProjectionUtil.getVector4D(entity);
         float posY = (float)(position.y - 11.0D);
         float hp = ScoreboardHealth.INSTANCE.isEnabled() && entity != mc.player ? PlayerIntersectionUtil.getHealth(entity) : entity.getHealth();
         int hpInt = (int)hp;
         Text name = this.cachedName(entity);
         TagCache cachedTag = this.tagCache.get(entity);
         Text nameWithHp;
         float textWidth;
         if (cachedTag == null || cachedTag.hpInt != hpInt || cachedTag.name != name) {
            nameWithHp = name.copy().append(EntityESP.GRAY_OPEN).append(Text.literal(String.valueOf(hpInt)).setStyle(Style.EMPTY.withColor(Formatting.RED))).append(EntityESP.GRAY_CLOSE);
            textWidth = Fonts.REGULAR.getWidth(nameWithHp.getString(), 6.5F);
            if (cachedTag == null) {
               cachedTag = new TagCache();
               this.tagCache.put(entity, cachedTag);
            }
            cachedTag.hpInt = hpInt;
            cachedTag.name = name;
            cachedTag.text = nameWithHp;
            cachedTag.width = textWidth;
         } else {
            nameWithHp = cachedTag.text;
            textWidth = cachedTag.width;
         }
         DrawUtil.drawRoundedRect(e.getContext().getMatrices(), (float)(position.x + (position.z - position.x) / 2.0D - (double)(textWidth / 2.0F) - 3.0D), posY - 2.5F, textWidth + 5.0F, 10.0F, BorderRadius.ZERO, HuihuiClient.getInstance().getFriendManager().isFriend(entity.getNameForScoreboard()) ? EntityESP.BG_FRIEND : EntityESP.BG_DARK);
         e.getContext().drawText(Fonts.REGULAR.getFont(6.5F), nameWithHp, (float)(position.x + (position.z - position.x) / 2.0D - (double)(textWidth / 2.0F)), posY, 255.0F);
         int itemCount = 0;
         EquipmentSlot[] var18 = EntityESP.EQUIP_SLOTS;
         int var19 = var18.length;

         for(int var20 = 0; var20 < var19; ++var20) {
            EquipmentSlot slot = var18[var20];
            ItemStack stack = entity.getEquippedStack(slot);
            if (!stack.isEmpty()) {
               this.itemArray[itemCount++] = stack;
            }
         }

         ItemStack mainHand = entity.getMainHandStack();
         if (!mainHand.isEmpty()) {
            this.itemArray[itemCount++] = mainHand;
         }

         ItemStack offHand = entity.getOffHandStack();
         if (!offHand.isEmpty()) {
            this.itemArray[itemCount++] = offHand;
         }
         if (itemCount == 0) {
            continue;
         }

         float iconSize = 16.0F;
         float spacing = 0.0F;
         float totalWidth = (float)itemCount * iconSize + (float)(itemCount - 1) * spacing;
         float startX = (float)(position.x + (position.z - position.x) / 2.0D - (double)(totalWidth / 2.0F) + 7.5D);
         float iconY = posY - 12.0F;
         MatrixStack matrices = e.getContext().getMatrices();

         for(int i = 0; i < itemCount; ++i) {
            ItemStack stack = this.itemArray[i];
            if (stack != null && !stack.isEmpty()) {
               float x2 = startX + (float)i * (iconSize + spacing);
               ItemEnchantmentsComponent enchComp = EnchantmentHelper.getEnchantments(stack);
               float enchantmentY;
               if (!enchComp.isEmpty()) {
                  enchantmentY = iconY - 16.0F;
                  EnchText[] cachedEnch = this.enchCache.get(stack);
                  int enchSize = enchComp.getSize();
                  if (cachedEnch == null || cachedEnch.length != enchSize) {
                     cachedEnch = new EnchText[enchSize];
                     int idx = 0;

                     for(Object2IntMap.Entry<RegistryEntry<Enchantment>> enchEntry : enchComp.getEnchantmentEntries()) {
                        int lvl = enchEntry.getIntValue();
                        if (lvl <= 0) {
                           continue;
                        }
                        EnchText enchText = new EnchText();
                        String fullName = Enchantment.getName(enchEntry.getKey(), lvl).getString();
                        String shortName = fullName.length() > 2 ? fullName.substring(0, 2) : fullName;
                        enchText.text = shortName + lvl;
                        enchText.width = Fonts.REGULAR.getWidth(enchText.text, 6.0F);
                        enchText.color = shortName.equalsIgnoreCase("Sh") && lvl > 5 || shortName.equalsIgnoreCase("Pr") && lvl > 4 ? EntityESP.ENCH_DANGER_RGB : -1;
                        cachedEnch[idx++] = enchText;
                     }
                     this.enchCache.put(stack, cachedEnch);
                  }

                  for(EnchText enchText : cachedEnch) {
                     if (enchText == null) {
                        continue;
                     }
                     e.getContext().drawText(Fonts.REGULAR.getFont(6.0F), enchText.text, x2 - enchText.width / 2.0F, enchantmentY, new ColorRGBA(enchText.color));
                     enchantmentY -= 8.0F;
                  }
               }

               DrawUtil.drawRoundedRect(matrices, x2 - 7.0F, iconY - 7.0F, 14.0F, 14.0F, BorderRadius.all(3.0F), EntityESP.BG_DARK);
               float scale = 0.7F;
               enchantmentY = -18.0F;
               matrices.push();
               matrices.translate(x2 + enchantmentY, iconY + enchantmentY, 0.0F);
               matrices.scale(scale, scale, 1.0F);
               int drawX = (int)(-enchantmentY);
               int drawY = (int)(-enchantmentY);
               e.getContext().drawItem(stack, drawX, drawY);
               e.getContext().drawStackOverlay(mc.textRenderer, stack, drawX, drawY);
               matrices.pop();
            }
         }
      }
   }

   private void renderItemTags(float tickDelta, EventRender2D e) {
      Vec3d playerPos = mc.player.getPos();
      Box searchBox = new Box(playerPos.x - 64.0D, playerPos.y - 64.0D, playerPos.z - 64.0D, playerPos.x + 64.0D, playerPos.y + 64.0D, playerPos.z + 64.0D);
      Iterator var3 = mc.world.getEntitiesByType(TypeFilter.instanceOf(ItemEntity.class), searchBox, (itemEntity) -> true).iterator();

      while(var3.hasNext()) {
         Entity entity = (Entity)var3.next();
         if (entity instanceof ItemEntity) {
            ItemEntity itemEntity = (ItemEntity)entity;
            if (itemEntity.squaredDistanceTo(mc.player) <= 4096.0D && ProjectionUtil.canSee(itemEntity.getBoundingBox().getCenter())) {
               double x = MathHelper.lerp((double)tickDelta, entity.lastRenderX, entity.getX());
               double y = MathHelper.lerp((double)tickDelta, entity.lastRenderY, entity.getY()) + (double)entity.getHeight() + 0.1D;
               double z = MathHelper.lerp((double)tickDelta, entity.lastRenderZ, entity.getZ());
               Vec3d pos = ProjectionUtil.worldSpaceToScreenSpace(new Vec3d(x, y, z));
               if (!(pos.z <= 0.0D) && !(pos.z >= 1.0D)) {
                  Vector4d position = ProjectionUtil.getVector4D(entity);
                  float posY = (float)(position.y - 11.0D);
                  ItemStack stack = itemEntity.getStack();
                  if (!stack.isEmpty()) {
                     int rarityOrdinal = stack.getRarity().ordinal();
                     Formatting var10000;
                     switch(rarityOrdinal) {
                     case 1:
                        var10000 = Formatting.YELLOW;
                        break;
                     case 2:
                        var10000 = Formatting.AQUA;
                        break;
                     case 3:
                        var10000 = Formatting.LIGHT_PURPLE;
                        break;
                     default:
                        var10000 = Formatting.WHITE;
                     }

                     Formatting rarityColor = var10000;
                     Text nameText = this.cachedItemName(itemEntity, rarityColor);
                     Text countComponent = stack.getCount() > 1 ? Text.literal(" х" + stack.getCount()).setStyle(Style.EMPTY.withColor(Formatting.GRAY)) : Text.empty();
                     Text textComponent = ((Text)nameText).copy().append(countComponent);
                     float textWidth = Fonts.REGULAR.getFont(6.5F).width((Text)textComponent);
                     DrawUtil.drawRoundedRect(e.getContext().getMatrices(), (float)(position.x + (position.z - position.x) / 2.0D - (double)(textWidth / 2.0F) - 3.0D), (float)(position.y - 13.5D), textWidth + 4.0F, 10.0F, BorderRadius.ZERO, EntityESP.BG_DARK);
                     e.getContext().drawText(Fonts.REGULAR.getFont(6.5F), textComponent, (float)(position.x + (position.z - position.x) / 2.0D - (double)(textWidth / 2.0F)), (float)position.y - 11.0F, 255.0F);
                  }
               }
            }
         }
      }

   }

   private Text cachedItemName(ItemEntity itemEntity, Formatting rarityColor) {
      Text cached = this.itemNameCache.get(itemEntity);
      if (cached == null || mc.player.age - itemEntity.age > 20) {
         ItemStack stack = itemEntity.getStack();
         String itemName = stack.getName().getString();
         Text nameText = Text.literal(itemName).setStyle(Style.EMPTY.withColor(rarityColor));
         if (!stack.getName().getSiblings().isEmpty()) {
            nameText = stack.getName();
         }
         cached = nameText;
         this.itemNameCache.put(itemEntity, cached);
      }
      return cached;
   }

   public static void drawBox(double x, double y, double width, double height, double size, int color, BufferBuilder bufferbuilder) {
      drawRectBuilding(x + size, y, width - size, y + size, color, bufferbuilder);
      drawRectBuilding(x, y, x + size, height, color, bufferbuilder);
      drawRectBuilding(width - size, y, width, height, color, bufferbuilder);
      drawRectBuilding(x + size, height - size, width - size, height, color, bufferbuilder);
   }

   public static void drawBoxTest(double x, double y, double width, double height, double size, Vector4f colors, BufferBuilder bufferbuilder) {
      drawMCHorizontalBuilding(x + size, y, width - size, y + size, (int)colors.x(), (int)colors.y(), bufferbuilder);
      drawMCVerticalBuilding(width - size, y + size, width, height - size, (int)colors.y(), (int)colors.z(), bufferbuilder);
      drawMCHorizontalBuilding(x + size, height - size, width - size, height, (int)colors.w(), (int)colors.z(), bufferbuilder);
      drawMCVerticalBuilding(x, y + size, x + size, height - size, (int)colors.x(), (int)colors.w(), bufferbuilder);
   }

   public static void drawRectBuilding(double left, double top, double right, double bottom, int color, BufferBuilder bufferbuilder) {
      double j;
      if (left < right) {
         j = left;
         left = right;
         right = j;
      }

      if (top < bottom) {
         j = top;
         top = bottom;
         bottom = j;
      }

      float f3 = (float)(color >> 24 & 255) / 255.0F;
      float f = (float)(color >> 16 & 255) / 255.0F;
      float f1 = (float)(color >> 8 & 255) / 255.0F;
      float f2 = (float)(color & 255) / 255.0F;
      bufferbuilder.vertex((float)left, (float)bottom, 0.0F).color(f, f1, f2, f3);
      bufferbuilder.vertex((float)right, (float)bottom, 0.0F).color(f, f1, f2, f3);
      bufferbuilder.vertex((float)right, (float)top, 0.0F).color(f, f1, f2, f3);
      bufferbuilder.vertex((float)left, (float)top, 0.0F).color(f, f1, f2, f3);
   }

   public static void drawMCHorizontalBuilding(double x1, double y1, double x2, double y2, int start, int end, BufferBuilder bufferbuilder) {
      float a1 = (float)(start >> 24 & 255) / 255.0F;
      float r1 = (float)(start >> 16 & 255) / 255.0F;
      float g1 = (float)(start >> 8 & 255) / 255.0F;
      float b1 = (float)(start & 255) / 255.0F;
      float a2 = (float)(end >> 24 & 255) / 255.0F;
      float r2 = (float)(end >> 16 & 255) / 255.0F;
      float g2 = (float)(end >> 8 & 255) / 255.0F;
      float b2 = (float)(end & 255) / 255.0F;
      bufferbuilder.vertex((float)x1, (float)y2, 0.0F).color(r1, g1, b1, a1);
      bufferbuilder.vertex((float)x2, (float)y2, 0.0F).color(r2, g2, b2, a2);
      bufferbuilder.vertex((float)x2, (float)y1, 0.0F).color(r2, g2, b2, a2);
      bufferbuilder.vertex((float)x1, (float)y1, 0.0F).color(r1, g1, b1, a1);
   }

   public static void drawMCVerticalBuilding(double x1, double y1, double x2, double y2, int start, int end, BufferBuilder bufferbuilder) {
      float a1 = (float)(start >> 24 & 255) / 255.0F;
      float r1 = (float)(start >> 16 & 255) / 255.0F;
      float g1 = (float)(start >> 8 & 255) / 255.0F;
      float b1 = (float)(start & 255) / 255.0F;
      float a2 = (float)(end >> 24 & 255) / 255.0F;
      float r2 = (float)(end >> 16 & 255) / 255.0F;
      float g2 = (float)(end >> 8 & 255) / 255.0F;
      float b2 = (float)(end & 255) / 255.0F;
      bufferbuilder.vertex((float)x1, (float)y2, 0.0F).color(r2, g2, b2, a2);
      bufferbuilder.vertex((float)x2, (float)y2, 0.0F).color(r2, g2, b2, a2);
      bufferbuilder.vertex((float)x2, (float)y1, 0.0F).color(r1, g1, b1, a1);
      bufferbuilder.vertex((float)x1, (float)y1, 0.0F).color(r1, g1, b1, a1);
   }
}
