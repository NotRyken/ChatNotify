package com.notryken.chatnotify;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import com.notryken.chatnotify.gui.screen.GlobalConfigScreen;

@Mod(ChatNotify.MOD_ID)
public class ChatNotifyForge {
    public ChatNotifyForge() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> new GlobalConfigScreen(parent))
                );

        ChatNotify.init();
    }
}