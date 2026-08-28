package tech.huihui.client.script;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Хранит последние ошибки загрузки скриптов (включая диагностику javac, ошибки
 * Lua/JSON), чтобы VibeCoding мог отправить их нейросети для исправления.
 */
public class ScriptErrorCapture {

   private static final List<Entry> errors = new ArrayList<>();
   private static final int MAX = 12;

   public static void capture(String name, String lang, Throwable t) {
      if (t == null) return;
      errors.add(new Entry(System.currentTimeMillis(), name, lang, buildMessage(t)));
      while (errors.size() > MAX) errors.remove(0);
   }

   public static Entry getLast() {
      return errors.isEmpty() ? null : errors.get(errors.size() - 1);
   }

   public static List<Entry> all() {
      return new ArrayList<>(errors);
   }

   public static void clear() {
      errors.clear();
   }

   private static String buildMessage(Throwable t) {
      StringBuilder sb = new StringBuilder();
      sb.append(t.getClass().getSimpleName());
      if (t.getMessage() != null && !t.getMessage().isBlank()) {
         sb.append(": ").append(t.getMessage());
      }
      StringWriter sw = new StringWriter();
      t.printStackTrace(new PrintWriter(sw));
      String[] lines = sw.toString().split("\n");
      int lim = Math.min(lines.length, 40);
      sb.append("\n").append(String.join("\n", Arrays.copyOfRange(lines, 0, lim)));
      return sb.toString();
   }

   public static final class Entry {
      public final long time;
      public final String name;
      public final String lang;
      public final String message;

      Entry(long time, String name, String lang, String message) {
         this.time = time;
         this.name = name;
         this.lang = lang;
         this.message = message;
      }

      public String full() {
         return "Скрипт: " + name + " (" + lang + ")\n" + message;
      }
   }
}
