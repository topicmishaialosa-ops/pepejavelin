package tech.huihui.client.accounts;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.session.Session;
import tech.huihui.HuihuiClient;
import tech.huihui.utility.crypt.CryptUtility;
import tech.huihui.utility.interfaces.IMinecraft;
import tech.huihui.utility.mixin.accessors.MinecraftClientAccessor;

public final class AccountManager implements IMinecraft {
   private static final File FILE = new File(HuihuiClient.DIRECTORY, "accounts.huihui");
   public static final AccountManager INSTANCE = new AccountManager();
   private final List<Account> accounts = new ArrayList();

   private AccountManager() {
      this.load();
   }

   public List<Account> getAccounts() {
      return this.accounts;
   }

   public Account findByName(String name) {
      for (Account account : this.accounts) {
         if (account.getName().equalsIgnoreCase(name)) {
            return account;
         }
      }
      return null;
   }

   public void add(String name) {
      if (name == null || name.trim().isEmpty() || this.findByName(name) != null) {
         return;
      }
      this.accounts.add(new Account(name.trim()));
      this.save();
   }

   public void remove(Account account) {
      this.accounts.remove(account);
      this.save();
   }

   public void switchTo(Account account) {
      if (account == null) {
         return;
      }
      Session session = new Session(account.getName(), account.getUuid(), "0", Optional.empty(), Optional.empty(), Session.AccountType.MOJANG);
      ((MinecraftClientAccessor) mc).setSession(session);
   }

   public String currentName() {
      Session session = mc.getSession();
      return session == null ? "" : session.getUsername();
   }

   public void load() {
      this.accounts.clear();
      if (!FILE.exists()) {
         return;
      }
      try (BufferedReader reader = new BufferedReader(new FileReader(FILE))) {
         String encryptedBase64 = reader.readLine();
         if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
            return;
         }
         byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);
         byte[] decrypted = CryptUtility.decryptData(encrypted, "config");
         if (decrypted == null) {
            return;
         }
         JsonObject object = JsonParser.parseString(new String(decrypted, StandardCharsets.UTF_8)).getAsJsonObject();
         if (object.has("accounts")) {
            for (JsonElement element : object.getAsJsonArray("accounts")) {
               JsonObject acc = element.getAsJsonObject();
               if (acc.has("name")) {
                  this.accounts.add(new Account(acc.get("name").getAsString()));
               }
            }
         }
      } catch (Exception var7) {
         var7.printStackTrace();
      }
   }

   public void save() {
      try {
         File parent = FILE.getParentFile();
         if (parent != null) {
            parent.mkdirs();
         }
         JsonArray array = new JsonArray();
         for (Account account : this.accounts) {
            JsonObject object = new JsonObject();
            object.addProperty("name", account.getName());
            array.add(object);
         }
         JsonObject root = new JsonObject();
         root.add("accounts", array);
         String json = root.toString();
         byte[] encrypted = CryptUtility.encryptData(json.getBytes(StandardCharsets.UTF_8), "config");
         String content = Base64.getEncoder().encodeToString(encrypted);
         try (FileWriter writer = new FileWriter(FILE)) {
            writer.write(content);
         }
      } catch (Exception var7) {
         var7.printStackTrace();
      }
   }
}
