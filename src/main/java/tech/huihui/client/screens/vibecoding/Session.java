package tech.huihui.client.screens.vibecoding;

import java.util.ArrayList;
import java.util.List;

public final class Session {
   private static int nextId = 1;

   public final int id;
   public final String name;
   public final long createdAt;
   public final List<Msg> history = new ArrayList<>();
   public String lastPrompt = "";
   public int retries = 0;

   public Session(String name) {
      this.id = nextId++;
      this.name = name;
      this.createdAt = System.currentTimeMillis();
   }

   public record Msg(boolean user, String text) {
   }

   public void addMessage(boolean user, String text) {
      history.add(new Msg(user, text));
   }

   public String getShortLabel() {
      if (history.size() > 1) {
         for (Msg m : history) {
            if (m.user()) {
               String t = m.text();
               return t.length() > 22 ? t.substring(0, 22) + "..." : t;
            }
         }
      }
      return name;
   }
}
