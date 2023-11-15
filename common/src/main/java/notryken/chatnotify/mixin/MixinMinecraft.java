package notryken.chatnotify.mixin;

import notryken.chatnotify.Constants;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static notryken.chatnotify.ChatNotify.saveConfig;

@Mixin(Minecraft.class)
public class MixinMinecraft {
    
    @Inject(at = @At("TAIL"), method = "<init>")
    private void init(CallbackInfo info) {
        
        Constants.LOG.info("This line is printed by an example mod common mixin!");
        Constants.LOG.info("MC Version: {}", Minecraft.getInstance().getVersionType());
    }

    /**
     * Save config on close.
     */
    @Inject(at = @At("HEAD"), method = "close")
    private void close(CallbackInfo ci) {
        saveConfig();
    }
}