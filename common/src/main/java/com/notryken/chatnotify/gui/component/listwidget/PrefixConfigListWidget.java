package com.notryken.chatnotify.gui.component.listwidget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import com.notryken.chatnotify.ChatNotify;

import java.util.Locale;

/**
 * {@code ConfigListWidget} containing controls for message modifier prefixes.
 */
public class PrefixConfigListWidget extends ConfigListWidget {
    public PrefixConfigListWidget(Minecraft minecraft, int width, int height,
                                  int y, int itemHeight,
                                  int entryRelX, int entryWidth, int entryHeight, 
                                  int scrollWidth) {
        super(minecraft, width, height, y, itemHeight, 
                entryRelX, entryWidth, entryHeight, scrollWidth);

        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("Message Modifier Prefixes \u2139"),
                Tooltip.create(Component.literal("A message prefix is a character or " +
                        "sequence of characters that you type before a message to modify it. " +
                        "For example, '!' or '/shout' may be used on some servers to communicate " +
                        "in global chat. This may be useful for preventing spurious notifications.")), -1));

        int max = ChatNotify.config().prefixes.size();
        for (int i = 0; i < max; i++) {
            addEntry(new Entry.PrefixFieldEntry(entryX, entryWidth, entryHeight, this, i));
        }
        addEntry(new ConfigListWidget.Entry.ActionButtonEntry(entryX, 0, entryWidth, entryHeight,
                Component.literal("+"), null, -1,
                (button) -> {
                    ChatNotify.config().prefixes.add("");
                    reload();
                }));
    }

    @Override
    public PrefixConfigListWidget resize(int width, int height, int y,
                                         int itemHeight, double scrollAmount) {
        PrefixConfigListWidget newListWidget = new PrefixConfigListWidget(
                minecraft, width, height, y, itemHeight,
                entryRelX, entryWidth, entryHeight, scrollWidth);
        newListWidget.setScrollAmount(scrollAmount);
        return newListWidget;
    }

    private abstract static class Entry extends ConfigListWidget.Entry {

        private static class PrefixFieldEntry extends Entry {
            PrefixFieldEntry(int x, int width, int height, PrefixConfigListWidget listWidget, int index) {
                super();

                int spacing = 5;
                int removeButtonWidth = 24;

                EditBox prefixEditBox = new EditBox(
                        Minecraft.getInstance().font, x, 0, width, height,
                        Component.literal("Message Prefix"));
                prefixEditBox.setMaxLength(20);
                prefixEditBox.setValue(ChatNotify.config().prefixes.get(index));
                prefixEditBox.setResponder(
                        (prefix) -> ChatNotify.config().prefixes.set(
                                index, prefix.strip().toLowerCase(Locale.ROOT)));
                elements.add(prefixEditBox);

                elements.add(Button.builder(Component.literal("\u274C"),
                                (button) -> {
                                    ChatNotify.config().prefixes.remove(index);
                                    listWidget.reload();
                                })
                        .pos(x + width + spacing, 0)
                        .size(removeButtonWidth, height)
                        .build());
            }
        }
    }
}