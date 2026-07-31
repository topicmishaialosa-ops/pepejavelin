package tech.huihui.base.discord.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.discordipc.RichPresence;

public class HuihuiClientRichPresence extends RichPresence {
   private final List<RPCButton> buttons = new ArrayList<>();

   public void addButton(String label, String url) {
      if (this.buttons.size() < 2) {
         this.buttons.add(new RPCButton(label, url));
      }
   }

   public JsonObject toJson() {
      JsonObject object = super.toJson();
      if (!this.buttons.isEmpty()) {
         JsonArray array = new JsonArray();
         for (RPCButton button : this.buttons) {
            JsonObject buttonObject = new JsonObject();
            buttonObject.addProperty("label", button.label);
            buttonObject.addProperty("url", button.url);
            array.add(buttonObject);
         }
         object.add("buttons", array);
      }
      return object;
   }

   private record RPCButton(String label, String url) {
      public RPCButton(String label, String url) {
         this.label = label;
         this.url = url;
      }

      public String label() {
         return this.label;
      }

      public String url() {
         return this.url;
      }
   }
}
