package tech.huihui.client.hud.elements.component;

import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import tech.huihui.base.font.Fonts;
import tech.huihui.client.hud.elements.draggable.ClickableHudElement;
import tech.huihui.client.hud.elements.draggable.DraggableHudElement;
import tech.huihui.client.modules.impl.render.Interface;
import tech.huihui.utility.render.display.base.BorderRadius;
import tech.huihui.utility.render.display.base.CustomDrawContext;
import tech.huihui.utility.render.display.base.color.ColorRGBA;
import tech.huihui.utility.render.display.shader.DrawUtil;

public class MusicInfoComponent extends DraggableHudElement implements ClickableHudElement {
   private static final ColorRGBA BG = new ColorRGBA(15, 15, 20, 220);
   private static final ColorRGBA TEXT = new ColorRGBA(255, 255, 255, 255);
   private static final ColorRGBA SUBTEXT = new ColorRGBA(170, 170, 180, 255);
   private static final ColorRGBA BAR_BG = new ColorRGBA(40, 40, 50, 255);
   private static final ColorRGBA BAR_FILL = new ColorRGBA(179, 145, 255, 255);
   private final ExecutorService mediaExecutor = Executors.newSingleThreadExecutor((r) -> {
      Thread thread = new Thread(r, "huihui-mediainfo");
      thread.setDaemon(true);
      return thread;
   });
   private final Identifier artworkId = Identifier.of("huihui", "hud/music_artwork");
   private final float pad = 6.0F;
   private final float coverSize = 56.0F;
   private final float widgetWidth = 170.0F;
   private volatile String title = "";
   private volatile String artist = "";
   private volatile byte[] artworkPng;
   private volatile long position;
   private volatile long duration;
   private volatile boolean playing;
   private volatile boolean artworkRegistered;
   private volatile dev.redstones.mediaplayerinfo.IMediaSession session;
   private long lastPoll;
   private float progressAnimated;
   private float visibility;
   private final float btnSize = 15.0F;
   private final float btnGap = 9.0F;
   private final float closeSize = 10.0F;
   private long lastPosMs;
   private long lastPosAtMs;
   private byte[] lastArtwork;

   public MusicInfoComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
      super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
      this.width = this.widgetWidth;
      this.height = this.computeHeight();
   }

   private float computeHeight() {
      return this.pad * 5.0F + this.coverSize + 3.0F + 1.0F + 5.0F + this.btnSize;
   }

   @Override
   public void tick() {
      if (mc.player == null || mc.player.age % 5 != 0) {
         return;
      }
      long now = System.currentTimeMillis();
      if (now - this.lastPoll < 250L) {
         return;
      }
      this.lastPoll = now;
      this.mediaExecutor.execute(this::pollMedia);
   }

   private void pollMedia() {
      try {
         var sessions = dev.redstones.mediaplayerinfo.MediaPlayerInfo.Instance.getMediaSessions();
         if (sessions == null || sessions.isEmpty()) {
            return;
         }
         var session = sessions.stream()
            .filter((s) -> s != null && s.getMedia() != null)
            .max(Comparator.comparing((s) -> {
               try {
                  return s.getMedia().getPlaying();
               } catch (Exception ignored) {
                  return false;
               }
            }))
            .orElse(null);
         if (session == null) {
            return;
         }
         var info = session.getMedia();
         if (info.getTitle().isEmpty() && info.getArtist().isEmpty() && !info.getPlaying()) {
            return;
         }
         byte[] png = info.getArtworkPng();
         if (png != null && png.length > 0 && png != this.artworkPng) {
            this.artworkPng = png;
            try {
               java.nio.file.Files.write(java.nio.file.Path.of(System.getProperty("user.home"), ".minecraft", "Huihui", "artwork.bin"), png);
            } catch (Exception ignored) {
            }
            mc.execute(this::registerArtwork);
         }
         this.title = info.getTitle();
         this.artist = info.getArtist();
         this.duration = info.getDuration() * 1000L;
         this.playing = info.getPlaying();
         this.session = session;
         long posSec = info.getPosition();
         long now = System.currentTimeMillis();
         if (this.playing) {
            if (posSec > 0L) {
               this.lastPosMs = posSec * 1000L;
               this.lastPosAtMs = now;
            } else if (this.lastPosAtMs == 0L) {
               this.lastPosAtMs = now;
            }
            this.position = this.lastPosMs + (now - this.lastPosAtMs);
            if (this.duration > 0L && this.position > this.duration) {
               this.position = this.duration;
            }
         } else {
            if (this.lastPosAtMs > 0L) {
               this.position = this.lastPosMs + (now - this.lastPosAtMs);
            } else {
               this.position = posSec * 1000L;
            }
            this.lastPosAtMs = 0L;
         }
      } catch (Exception ignored) {
      }
   }

   private void registerArtwork() {
      byte[] png = this.artworkPng;
      if (png == null || png.length == 0) {
         this.artworkRegistered = false;
         return;
      }
      if (java.util.Arrays.equals(png, this.lastArtwork)) {
         return;
      }
      this.lastArtwork = png;
      try {
         NativeImage image = NativeImage.read(new java.io.ByteArrayInputStream(png));
         this.registerImage(image);
         return;
      } catch (Exception ignored) {
      }
      try {
         java.awt.image.BufferedImage buffered = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
         if (buffered == null) {
            this.artworkRegistered = false;
            return;
         }
         NativeImage image = new NativeImage(NativeImage.Format.RGBA, buffered.getWidth(), buffered.getHeight(), false);
         for (int y = 0; y < buffered.getHeight(); ++y) {
            for (int x = 0; x < buffered.getWidth(); ++x) {
               image.setColorArgb(x, y, buffered.getRGB(x, y));
            }
         }
         this.registerImage(image);
      } catch (Exception ignored) {
         this.artworkRegistered = false;
      }
   }

   private void registerImage(NativeImage image) {
      try {
         mc.getTextureManager().registerTexture(this.artworkId, new NativeImageBackedTexture(image));
         this.artworkRegistered = true;
      } catch (Exception ignored) {
         this.artworkRegistered = false;
      }
   }

   @Override
   public void render(CustomDrawContext ctx) {
      this.visibility = 1.0F;

      float x = this.x;
      float y = this.y;
      float coverX = x + this.pad;
      float coverY = y + this.pad;
      float textX = coverX + this.coverSize + 8.0F;
      float textMax = x + this.widgetWidth - this.pad - textX;

      DrawUtil.drawBlur(ctx.getMatrices(), x, y, this.widgetWidth, this.height, 10.0F, BorderRadius.all(6.0F), BG.mulAlpha(this.visibility));
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x, y, this.widgetWidth, this.height, BorderRadius.all(6.0F), BG.mulAlpha(this.visibility));

      if (this.artworkRegistered && this.artworkPng != null && this.artworkPng.length > 0) {
         ctx.drawTexture(this.artworkId, coverX, coverY, this.coverSize, this.coverSize, new ColorRGBA(255, 255, 255, 255.0F * this.visibility));
      } else {
         DrawUtil.drawRoundedRect(ctx.getMatrices(), coverX, coverY, this.coverSize, this.coverSize, BorderRadius.all(4.0F), new ColorRGBA(25, 25, 32, 200.0F * this.visibility));
      }

      float closeX = x + this.widgetWidth - this.pad - this.closeSize;
      float closeY = y + this.pad;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), closeX, closeY, this.closeSize, this.closeSize, BorderRadius.all(this.closeSize / 2.0F), new ColorRGBA(40, 40, 50, 255.0F * this.visibility));
      ctx.drawText(Fonts.ICONS2.getFont(6.0F), "\uf057", closeX + this.closeSize / 2.0F - Fonts.ICONS2.getWidth("\uf057", 6.0F) / 2.0F, closeY + this.closeSize / 2.0F - Fonts.ICONS2.getFont(6.0F).height() / 2.0F, new ColorRGBA(200, 200, 210, 255.0F * this.visibility));

      float titleSize = 7.0F;
      float artistSize = 6.5F;
      String title = this.truncate(this.title.isEmpty() ? "—" : this.title, titleSize, textMax);
      String artist = this.truncate(this.artist.isEmpty() ? "—" : this.artist, artistSize, textMax);
      ctx.drawText(Fonts.REGULAR.getFont(titleSize), title, textX, coverY + 2.0F, TEXT.mulAlpha(this.visibility));
      if (this.playing) {
         float dotX = textX + Fonts.REGULAR.getWidth(title, titleSize) + 5.0F;
         DrawUtil.drawRoundedRect(ctx.getMatrices(), dotX, coverY + 4.0F, 3.0F, 3.0F, BorderRadius.all(1.5F), new ColorRGBA(80, 220, 120, 255.0F * this.visibility));
      }
      ctx.drawText(Fonts.REGULAR.getFont(artistSize), artist, textX, coverY + 2.0F + titleSize + 2.0F, SUBTEXT.mulAlpha(this.visibility));

      float barY = coverY + this.coverSize + this.pad;
      float barH = 3.0F;
      float barW = this.widgetWidth - this.pad * 2.0F;
      float target = this.duration > 0 ? Math.min(1.0F, (float) this.position / (float) this.duration) : 0.0F;
      this.progressAnimated += (target - this.progressAnimated) * 0.22F;
      DrawUtil.drawRoundedRect(ctx.getMatrices(), x + this.pad, barY, barW, barH, BorderRadius.all(1.5F), BAR_BG.mulAlpha(this.visibility));
      if (this.progressAnimated > 0.001F) {
         DrawUtil.drawRoundedRect(ctx.getMatrices(), x + this.pad, barY, barW * Math.min(1.0F, this.progressAnimated), barH, BorderRadius.all(1.5F), BAR_FILL.mulAlpha(this.visibility));
      }

      String time = this.formatTime(this.position) + " / " + this.formatTime(this.duration);
      float timeW = Fonts.REGULAR.getWidth(time, 5.0F);
      ctx.drawText(Fonts.REGULAR.getFont(5.0F), time, x + this.widgetWidth - this.pad - timeW, barY + barH + 1.0F, SUBTEXT.mulAlpha(this.visibility));

      float rowW = this.btnSize * 3.0F + this.btnGap * 2.0F;
      float rowX = x + (this.widgetWidth - rowW) / 2.0F;
      float rowY = barY + barH + 1.0F + 5.0F + this.pad;
      this.drawButton(ctx, "<", rowX, rowY);
      this.drawButton(ctx, this.playing ? "||" : ">", rowX + this.btnSize + this.btnGap, rowY);
      this.drawButton(ctx, ">", rowX + (this.btnSize + this.btnGap) * 2.0F, rowY);

      this.width = this.widgetWidth;
      this.height = this.computeHeight();
   }

   private void drawButton(CustomDrawContext ctx, String symbol, float bx, float by) {
      DrawUtil.drawRoundedRect(ctx.getMatrices(), bx, by, this.btnSize, this.btnSize, BorderRadius.all(this.btnSize / 2.0F), new ColorRGBA(40, 40, 50, 255.0F * this.visibility));
      float symbolSize = 6.0F;
      float w = Fonts.REGULAR.getWidth(symbol, symbolSize);
      float h = Fonts.REGULAR.getFont(symbolSize).height();
      ctx.drawText(Fonts.REGULAR.getFont(symbolSize), symbol, bx + (this.btnSize - w) / 2.0F, by + (this.btnSize - h) / 2.0F - 0.5F, BAR_FILL.mulAlpha(this.visibility));
   }

   @Override
   public boolean onMouseClick(float mouseX, float mouseY) {
      if (this.visibility < 0.02F) {
         return false;
      }
      float x = this.x;
      float y = this.y;

      float closeX = x + this.widgetWidth - this.pad - this.closeSize;
      float closeY = y + this.pad;
      if (mouseX >= closeX && mouseX <= closeX + this.closeSize && mouseY >= closeY && mouseY <= closeY + this.closeSize) {
         int index = Interface.INSTANCE.getElements().indexOf(this);
         var settings = Interface.INSTANCE.getElementsSetting().getBooleanSettings();
         if (index >= 0 && index < settings.size()) {
            settings.get(index).setEnabled(false);
         }
         return true;
      }

      if (this.session == null) {
         return false;
      }
      float barY = y + this.pad + this.coverSize + this.pad;
      float rowW = this.btnSize * 3.0F + this.btnGap * 2.0F;
      float rowX = x + (this.widgetWidth - rowW) / 2.0F;
      float rowY = barY + 3.0F + 1.0F + 5.0F + this.pad;
      if (mouseX >= rowX && mouseX <= rowX + this.btnSize && mouseY >= rowY && mouseY <= rowY + this.btnSize) {
         this.invokeSession(dev.redstones.mediaplayerinfo.IMediaSession::previous);
         return true;
      }
      float playX = rowX + this.btnSize + this.btnGap;
      if (mouseX >= playX && mouseX <= playX + this.btnSize && mouseY >= rowY && mouseY <= rowY + this.btnSize) {
         this.invokeSession((s) -> {
            if (this.playing) {
               s.pause();
            } else {
               s.play();
            }
         });
         return true;
      }
      float nextX = playX + this.btnSize + this.btnGap;
      if (mouseX >= nextX && mouseX <= nextX + this.btnSize && mouseY >= rowY && mouseY <= rowY + this.btnSize) {
         this.invokeSession(dev.redstones.mediaplayerinfo.IMediaSession::next);
         return true;
      }
      return false;
   }

   private void invokeSession(java.util.function.Consumer<dev.redstones.mediaplayerinfo.IMediaSession> action) {
      dev.redstones.mediaplayerinfo.IMediaSession s = this.session;
      if (s == null) {
         return;
      }
      this.mediaExecutor.execute(() -> {
         try {
            action.accept(s);
         } catch (Exception ignored) {
         }
      });
   }

   private String truncate(String text, float size, float maxWidth) {
      if (Fonts.REGULAR.getWidth(text, size) <= maxWidth) {
         return text;
      }
      for (int i = text.length() - 1; i > 0; --i) {
         if (Fonts.REGULAR.getWidth(text.substring(0, i) + "…", size) <= maxWidth) {
            return text.substring(0, i) + "…";
         }
      }
      return "";
   }

   private String formatTime(long millis) {
      long seconds = Math.max(0L, millis) / 1000L;
      return (seconds / 60L) + ":" + String.format("%02d", seconds % 60L);
   }
}
