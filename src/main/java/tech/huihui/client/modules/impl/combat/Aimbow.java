package tech.huihui.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import java.util.Iterator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import tech.huihui.HuihuiClient;
import tech.huihui.base.events.impl.other.EventGameUpdate;
import tech.huihui.client.modules.api.Category;
import tech.huihui.client.modules.api.Module;
import tech.huihui.client.modules.api.ModuleAnnotation;
import tech.huihui.client.modules.api.setting.impl.BooleanSetting;
import tech.huihui.client.modules.api.setting.impl.ModeSetting;
import tech.huihui.client.modules.api.setting.impl.NumberSetting;
import tech.huihui.utility.game.player.rotation.RotationUtil;
import tech.huihui.utility.math.Timer;

@ModuleAnnotation(
   name = "Aimbow",
   category = Category.COMBAT,
   description = "Доводит лук, трезубец и арбалет до цели без видимой ротации"
)
public final class Aimbow extends Module {
   public static final Aimbow INSTANCE = new Aimbow();
   public ModeSetting weapon = new ModeSetting("Оружие", new String[]{"Все", "Лук", "Трезубец", "Арбалет"});
   public NumberSetting range = new NumberSetting("Дистанция", 60.0F, 10.0F, 120.0F, 1.0F);
   public BooleanSetting gravity = new BooleanSetting("Учёт гравитации", true);
   private final Timer timer = new Timer();
   private PlayerEntity target;

   private Aimbow() {
   }

   @EventTarget
   private void onGameUpdate(EventGameUpdate event) {
      if (!this.isEnabled() || mc.world == null || mc.player == null || !mc.player.isAlive()) {
         this.target = null;
         return;
      }

      ItemStack stack = mc.player.getMainHandStack();
      if (!this.isWeaponMatch(stack)) {
         this.target = null;
         return;
      }

      if (!this.isActive(stack)) {
         this.target = null;
         return;
      }

      if (!this.timer.finished(50L)) {
         return;
      }
      this.timer.reset();

      this.target = this.findTarget();
      if (this.target == null) {
         return;
      }

      double speed = this.projectileSpeed(stack);
      Vec3d start = mc.player.getEyePos();
      Vec3d aimPos = this.target.getBoundingBox().getCenter();
      aimPos = aimPos.add(0.0D, (double)(-this.target.getHeight() * 0.15F), 0.0D);

      float yaw = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(aimPos.z - start.z, aimPos.x - start.x)) - 90.0D);
      float pitch = this.calculatePitch(start, aimPos, speed);

      mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, mc.player.isOnGround(), false));
   }

   private boolean isWeaponMatch(ItemStack stack) {
      boolean bow = stack.getItem() instanceof BowItem;
      boolean trident = stack.getItem() instanceof TridentItem;
      boolean crossbow = stack.getItem() instanceof CrossbowItem;
      if (this.weapon.is("Лук")) {
         return bow;
      } else if (this.weapon.is("Трезубец")) {
         return trident;
      } else if (this.weapon.is("Арбалет")) {
         return crossbow;
      } else {
         return bow || trident || crossbow;
      }
   }

   private boolean isActive(ItemStack stack) {
      if (stack.getItem() instanceof BowItem || stack.getItem() instanceof TridentItem) {
         return mc.player.isUsingItem();
      }
      return mc.player.isUsingItem() || CrossbowItem.isCharged(stack);
   }

   private double projectileSpeed(ItemStack stack) {
      if (stack.getItem() instanceof BowItem) {
         float power = MathHelper.clamp((float)mc.player.getItemUseTime() / 20.0F, 0.0F, 1.0F);
         return 3.0D * (double)power;
      } else if (stack.getItem() instanceof TridentItem) {
         return 2.5D;
      } else {
         return 3.15D;
      }
   }

   private float calculatePitch(Vec3d start, Vec3d targetPos, double speed) {
      double dx = targetPos.x - start.x;
      double dy = targetPos.y - start.y;
      double dz = targetPos.z - start.z;
      double horizontal = Math.hypot(dx, dz);
      if (horizontal < 1.0E-4D) {
         return 0.0F;
      }
      double pitch = Math.toDegrees(Math.atan2(-dy, horizontal));
      if (this.gravity.isEnabled() && speed > 0.05D) {
         for (int i = 0; i < 3; ++i) {
            double cos = Math.cos(Math.toRadians(pitch));
            double t = horizontal / Math.max(speed * cos, 0.05D);
            double drop = 10.0D * t * t;
            pitch = Math.toDegrees(Math.atan2(-(dy + drop), horizontal));
         }
      }
      return (float)MathHelper.wrapDegrees(pitch);
   }

   private PlayerEntity findTarget() {
      PlayerEntity best = null;
      double bestDist = Double.MAX_VALUE;
      Vec3d eye = mc.player.getEyePos();
      Iterator<Entity> var4 = mc.world.getEntities().iterator();

      while(var4.hasNext()) {
         Entity entity = (Entity)var4.next();
         if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)entity;
            if (player != mc.player && player.isAlive() && player.getHealth() > 0.0F && !player.isSpectator() && !player.isInvisible() && !HuihuiClient.getInstance().getFriendManager().isFriend(player.getName().getString()) && !AntiBot.INSTANCE.isBot(player)) {
               double dist = player.getEyePos().squaredDistanceTo(eye);
               if (dist <= (double)(this.range.getCurrent() * this.range.getCurrent()) && dist < bestDist) {
                  best = player;
                  bestDist = dist;
               }
            }
         }
      }

      return best;
   }

   public PlayerEntity getTarget() {
      return this.isEnabled() ? this.target : null;
   }
}
