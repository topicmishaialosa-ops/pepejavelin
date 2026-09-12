package tech.huihui.base.bots;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class InstanceBot {
   public enum State {
      OFFLINE,
      LAUNCHING,
      RUNNING,
      DEAD
   }

   private final String name;
   private final UUID uuid;
   private String server;
   private volatile State state = State.OFFLINE;
   private volatile boolean alive;
   private volatile long pid;

   public InstanceBot(String name, String server) {
      this.name = name == null ? "" : name.trim();
      this.uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + this.name).getBytes(StandardCharsets.UTF_8));
      this.server = server == null ? "" : server.trim();
   }

   public String getName() {
      return this.name;
   }

   public UUID getUuid() {
      return this.uuid;
   }

   public String getServer() {
      return this.server;
   }

   public void setServer(String server) {
      this.server = server == null ? "" : server.trim();
   }

   public State getState() {
      return this.state;
   }

   public void setState(State state) {
      this.state = state;
   }

   public boolean isAlive() {
      return this.alive;
   }

   public void setAlive(boolean alive) {
      this.alive = alive;
   }

   public long getPid() {
      return this.pid;
   }

   public void setPid(long pid) {
      this.pid = pid;
   }
}