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
   private boolean showSecondBar;
   private boolean matchBarThickness = true;
   private float secondBarThickness = 1.0F;
   private float headSize = 1.0F;
   private float headYaw;
   private float headPitch;
   private boolean headAutoRotate;
   private String displayMode = "Проценты и HP";
   private boolean customColors;
   private int barColor = -8384257;
   private int barColorSecond = -10197901;
   private int bgColor = -1610612736;
   private int borderColor = -7106445;
   private int textColor = -1;
   private float radius = 5.0F;
   private float borderThickness = 1.0F;
   private float backgroundAlpha = 120.0F;
    private float animationSpeed = 1.0F;
    private boolean showArmor = true;
    private boolean showPing = true;
    private boolean showEyes = true;
    private float eyeSize = 1.0F;
    private int eyeColor = -1;
    private int pupilColor = -16777216;
    private String bgImage = "";
    private String headImage = "";
}
