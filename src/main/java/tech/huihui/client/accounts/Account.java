package tech.huihui.client.accounts;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class Account {
   private final String name;
   private final UUID uuid;

   public Account(String name) {
      this.name = name;
      this.uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
   }

   public String getName() {
      return this.name;
   }

   public UUID getUuid() {
      return this.uuid;
   }
}
