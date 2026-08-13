package tech.huihui.base.lang;

public final class Lang {

   private Lang() {
   }

   public static String t(String lang, String ru, String en, String zh) {
      if ("English".equals(lang)) {
         return en;
      }
      if ("中文".equals(lang)) {
         return zh;
      }
      return ru;
   }
}