package tech.huihui.utility.mixin.accessors;

import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.UserApiService;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.telemetry.TelemetryManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
   @Mutable
   @Accessor("session")
   void setSession(Session session);

   @Mutable
   @Accessor("sessionService")
   void setSessionService(MinecraftSessionService sessionService);

   @Mutable
   @Accessor("userApiService")
   void setUserApiService(UserApiService userApiService);

   @Mutable
   @Accessor("profileKeys")
   void setProfileKeys(ProfileKeys profileKeys);

   @Mutable
   @Accessor("telemetryManager")
   void setTelemetryManager(TelemetryManager telemetryManager);

   @Mutable
   @Accessor("abuseReportContext")
   void setAbuseReportContext(AbuseReportContext abuseReportContext);
}
