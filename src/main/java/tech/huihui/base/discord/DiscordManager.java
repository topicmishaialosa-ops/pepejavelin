package tech.huihui.base.discord;

import java.io.IOException;
import lombok.Generated;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.IPCUser;
import net.minecraft.util.Identifier;
import ru.nexusguard.protection.annotations.Native;
import tech.huihui.HuihuiClient;
import tech.huihui.base.discord.utils.HuihuiClientRichPresence;
import tech.huihui.utility.render.display.BufferUtil;

public class DiscordManager {
   private static final long APP_ID = 1375824160957403177L;
   private final DiscordManager.DiscordDaemonThread discordDaemonThread = new DiscordManager.DiscordDaemonThread();
   private boolean running = true;
   private DiscordManager.DiscordInfo info = new DiscordManager.DiscordInfo("Unknown", "", "");
   private Identifier avatarId;

   public DiscordManager() {
      this.initRPC();
   }

   @Native
   private void initRPC() {
      try {
         DiscordIPC.setOnError((code, message) -> {
            System.err.println("[HuihuiClient] Discord RPC error " + code + ": " + message);
            DiscordManager.this.stopRPC();
         });
         boolean connected = DiscordIPC.start(APP_ID, () -> {
            IPCUser user = DiscordIPC.getUser();
            if (user != null) {
               HuihuiClient.getInstance().getDiscordManager().setInfo(new DiscordManager.DiscordInfo(user.username, "https://cdn.discordapp.com/avatars/" + user.id + "/" + user.avatar + ".png", user.id));
            }
            HuihuiClientRichPresence richPresence = new HuihuiClientRichPresence();
            richPresence.setStart(System.currentTimeMillis() / 1000L);
            richPresence.setDetails("USER » vorkis");
            richPresence.setState("UID » 1");
            richPresence.setLargeImage("logo", "");
            richPresence.addButton("Buy Client", "https://javelinclient.fun");
            richPresence.addButton("Discord", "https://discord.gg/hYgEF3gYzX");
            DiscordIPC.setActivity(richPresence);
         });
         if (!connected) {
            this.running = false;
            return;
         }
         this.discordDaemonThread.start();
      } catch (Exception var2) {
         this.running = false;
      }

   }

   @Native
   public void stopRPC() {
      try {
         DiscordIPC.stop();
      } catch (Exception var2) {
      }

      this.running = false;
   }

   @Native
   public void load() throws IOException {
      if (this.avatarId == null && !this.info.avatarUrl.isEmpty()) {
         this.avatarId = BufferUtil.registerDynamicTexture("avatar-", BufferUtil.getHeadFromURL(this.info.avatarUrl));
      }

   }

   @Generated
   public void setRunning(boolean running) {
      this.running = running;
   }

   @Generated
   public void setInfo(DiscordManager.DiscordInfo info) {
      this.info = info;
   }

   @Generated
   public void setAvatarId(Identifier avatarId) {
      this.avatarId = avatarId;
   }

   @Generated
   public DiscordManager.DiscordDaemonThread getDiscordDaemonThread() {
      return this.discordDaemonThread;
   }

   @Generated
   public boolean isRunning() {
      return this.running;
   }

   @Generated
   public DiscordManager.DiscordInfo getInfo() {
      return this.info;
   }

   @Generated
   public Identifier getAvatarId() {
      return this.avatarId;
   }

   private class DiscordDaemonThread extends Thread {
      @Native
      public void run() {
         this.setName("Discord-RPC");

         try {
            while(HuihuiClient.getInstance().getDiscordManager().isRunning()) {
               DiscordManager.this.load();
               Thread.sleep(15000L);
            }
         } catch (Exception var2) {
            DiscordManager.this.stopRPC();
         }

         super.run();
      }
   }

   public static record DiscordInfo(String userName, String avatarUrl, String userId) {
      public DiscordInfo(String userName, String avatarUrl, String userId) {
         this.userName = userName;
         this.avatarUrl = avatarUrl;
         this.userId = userId;
      }

      public String userName() {
         return this.userName;
      }

      public String avatarUrl() {
         return this.avatarUrl;
      }

      public String userId() {
         return this.userId;
      }
   }
}
