package tech.huihui.client.accounts;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class Account {
   private final String name;
   private final UUID uuid;
   private boolean selected;
   private boolean favorited;

   public Account(String name) {
      this.name = name;
      this.uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
   }

   public Account(String name, boolean selected, boolean favorited) {
      this.name = name;
      this.uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
      this.selected = selected;
      this.favorited = favorited;
   }

   public String getName() {
      return this.name;
   }

   public UUID getUuid() {
      return this.uuid;
   }

   public void setSelected(boolean selected) {
      this.selected = selected;
   }

   public boolean isSelected() {
      return this.selected;
   }

   public void setFavorited(boolean favorited) {
      this.favorited = favorited;
   }

   public boolean isFavorited() {
      return this.favorited;
   }
}