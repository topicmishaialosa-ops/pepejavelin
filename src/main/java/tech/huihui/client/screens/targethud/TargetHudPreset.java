package tech.huihui.client.screens.targethud;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TargetHudPreset {
   private String name = "Таргетхуд";
   private String type = "Крупный";
   private float x = 4.0F;
   private float y = 4.0F;
   private float barThickness = 1.0F;
   private float headSize = 1.0F;
   private float headYaw;
   private float headPitch;
   private boolean headAutoRotate;
   private String displayMode = "Проценты и HP";
   private boolean customColors;
   private int barColor = -8384257;
   private int bgColor = -1610612736;
   private int borderColor = -7106445;
   private int textColor = -1;
}
