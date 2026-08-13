package tech.huihui;

import java.io.File;
import lombok.Generated;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.base.autobuy.AutoBuyManager;
import tech.huihui.base.comand.CommandManager;
import tech.huihui.base.config.ConfigManager;
import tech.huihui.base.filemanager.impl.FriendManager;
import tech.huihui.base.filemanager.impl.StaffManager;
import tech.huihui.base.macro.MacroManager;
import tech.huihui.base.modules.ModuleManager;
import tech.huihui.base.notify.NotifyManager;
import tech.huihui.base.repository.RCTRepository;
import tech.huihui.base.request.ScriptManager;
import tech.huihui.base.theme.ThemeManager;
import tech.huihui.base.waypoint.WaypointManager;
import tech.huihui.client.screens.clickgui.ClickGuiScreen;
import tech.huihui.client.screens.menu.MenuScreen;
import tech.huihui.client.screens.targethud.TargetHudPresetManager;
import tech.huihui.utility.game.server.ServerHandler;
import tech.huihui.utility.render.display.shader.DrawUtil;

public enum HuihuiClient implements ClientModInitializer {
   INSTANCE;

   public static final String NAME = "Huihui Client";
   public static final String VER = "";
   public static final String TYPE = "DEV";
   private static final String MOD_ID = "huihui";
   public static File DIRECTORY;
   private ModuleManager moduleManager;
   private ThemeManager themeManager;
   private MenuScreen menuScreen;
   private ClickGuiScreen clickGuiScreen;
   private ScriptManager scriptManager;
   private ServerHandler serverHandler;
   private FriendManager friendManager;
   private MacroManager macroManager;
   private StaffManager staffManager;
   private TargetHudPresetManager targetHudPresetManager;
   private AutoBuyManager autoBuyManager;
   private WaypointManager waypointManager;
   private NotifyManager notifyManager;
   private CommandManager commandManager;
   private ConfigManager configManager;
    private RCTRepository rctRepository;
    private boolean initialized = false;

   @Override
   public void onInitializeClient() {
      try {
         init();
      } catch (Exception e) {
         e.printStackTrace();
         throw e;
      }
   }

   @Native
   public void init() {
      if (initialized) {
         return;
      }
      initialized = true;
      
      try {
         DIRECTORY = new File(MinecraftClient.getInstance().runDirectory, "Huihui");
         if (!DIRECTORY.exists()) {
            DIRECTORY.mkdirs();
         }
         
         Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            getInstance().shutdown();
         }));
         
          this.friendManager = new FriendManager();
          this.macroManager = new MacroManager();
          this.staffManager = new StaffManager();
          this.targetHudPresetManager = new TargetHudPresetManager();
         this.notifyManager = new NotifyManager();
         this.serverHandler = new ServerHandler();
         this.rctRepository = new RCTRepository();
         this.themeManager = new ThemeManager();
         this.moduleManager = new ModuleManager();
         this.configManager = new ConfigManager();
         this.autoBuyManager = new AutoBuyManager();
         this.commandManager = new CommandManager();
         this.scriptManager = new ScriptManager();
         this.waypointManager = new WaypointManager();
         this.menuScreen = new MenuScreen();
         this.clickGuiScreen = new ClickGuiScreen();
         DrawUtil.initializeShaders();
      } catch (Exception e) {
         e.printStackTrace();
         throw new RuntimeException("Huihui Client initialization failed", e);
      }
   }

   @Native
   public void shutdown() {
      this.friendManager.save();
      this.staffManager.save();
      this.targetHudPresetManager.save();
      this.configManager.save();
      this.macroManager.save();
   }

   public static Identifier id(String path) {
      return Identifier.of("huihui", path);
   }

   public static HuihuiClient getInstance() {
      return INSTANCE;
   }

   public RCTRepository getRCTRepository() {
      return this.rctRepository;
   }

   @Generated
   public ModuleManager getModuleManager() {
      return this.moduleManager;
   }

   @Generated
   public ThemeManager getThemeManager() {
      return this.themeManager;
   }

   @Generated
   public MenuScreen getMenuScreen() {
      return this.menuScreen;
   }

   @Generated
   public ClickGuiScreen getClickGuiScreen() {
      return this.clickGuiScreen;
   }

   @Generated
   public ScriptManager getScriptManager() {
      return this.scriptManager;
   }

   @Generated
   public ServerHandler getServerHandler() {
      return this.serverHandler;
   }

   @Generated
   public FriendManager getFriendManager() {
      return this.friendManager;
   }

   @Generated
   public MacroManager getMacroManager() {
      return this.macroManager;
   }

   @Generated
   public StaffManager getStaffManager() {
      return this.staffManager;
   }

   @Generated
   public TargetHudPresetManager getTargetHudPresetManager() {
      return this.targetHudPresetManager;
   }

   @Generated
   public AutoBuyManager getAutoBuyManager() {
      return this.autoBuyManager;
   }

   @Generated
   public WaypointManager getWaypointManager() {
      return this.waypointManager;
   }

   @Generated
   public NotifyManager getNotifyManager() {
      return this.notifyManager;
   }

   @Generated
   public CommandManager getCommandManager() {
      return this.commandManager;
   }

   @Generated
   public ConfigManager getConfigManager() {
      return this.configManager;
   }


   private static HuihuiClient[] $values() {
      return new HuihuiClient[]{INSTANCE};
   }
}
