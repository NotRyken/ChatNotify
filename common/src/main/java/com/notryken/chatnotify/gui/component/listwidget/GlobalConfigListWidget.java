package com.notryken.chatnotify.gui.component.listwidget;

import com.notryken.chatnotify.ChatNotify;
import com.notryken.chatnotify.config.Notification;
import com.notryken.chatnotify.config.Trigger;
import com.notryken.chatnotify.gui.screen.ConfigScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundSource;

import java.util.List;

/**
 * {@code ConfigListWidget} containing global ChatNotify controls and a list
 * of buttons referencing {@code Notification} instances.
 */
public class GlobalConfigListWidget extends ConfigListWidget {
    public GlobalConfigListWidget(Minecraft minecraft, int width, int height, int y, int itemHeight,
                                  int entryRelX, int entryWidth, int entryHeight, 
                                  int scrollWidth) {
        super(minecraft, width, height, y, itemHeight,
                entryRelX, entryWidth, entryHeight, scrollWidth);

        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("Global Options"), null, -1));

        addEntry(new Entry.MixinAndKeyDebugEntry(entryX, entryWidth, entryHeight));
        addEntry(new Entry.OwnMsgToggleEntry(entryX, entryWidth, entryHeight));
        addEntry(new Entry.SoundSourceEntry(entryX, entryWidth, entryHeight));
        addEntry(new Entry.PrefixConfigEntry(entryX, entryWidth, entryHeight, this));

        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("Notifications \u2139"),
                Tooltip.create(Component.literal("Incoming messages will activate the first " +
                        "enabled notification with a matching trigger.")), -1));

        int max = ChatNotify.config().getNotifs().size();
        for (int i = 0; i < max; i++) {
            addEntry(new Entry.NotifConfigEntry(entryX, entryWidth, entryHeight, this, i));
        }
        addEntry(new ConfigListWidget.Entry.ActionButtonEntry(entryX, 0, entryWidth, entryHeight,
                Component.literal("+"), null, -1,
                (button) -> addNotification()));
    }

    @Override
    public GlobalConfigListWidget resize(int width, int height, int y, int itemHeight, double scrollAmount) {
        GlobalConfigListWidget newListWidget = new GlobalConfigListWidget(minecraft, width, height, y, itemHeight,
                entryRelX, entryWidth, entryHeight, scrollWidth);
        newListWidget.setScrollAmount(scrollAmount);
        return newListWidget;
    }

    private void moveNotifUp(int index) {
        ChatNotify.config().increasePriority(index);
        reload();
    }

    private void moveNotifUpFull(int index) {
        ChatNotify.config().toMaxPriority(index);
        reload();
    }

    private void moveNotifDown(int index) {
        ChatNotify.config().decreasePriority(index);
        reload();
    }

    private void moveNotifDownFull(int index) {
        ChatNotify.config().toMinPriority(index);
        reload();
    }

    private void addNotification() {
        ChatNotify.config().addNotif();
        openNotificationConfig(ChatNotify.config().getNotifs().size()-1);
    }

    private void removeNotification(int index) {
        if (ChatNotify.config().removeNotif(index)) {
            reload();
        }
    }

    private void openPrefixConfig() {
        minecraft.setScreen(new ConfigScreen(minecraft.screen,
                Component.translatable("screen.chatnotify.title.prefix"),
                new PrefixConfigListWidget(minecraft, screen.width, screen.height, getY(),
                        itemHeight, entryRelX, entryWidth, entryHeight, scrollWidth)));
    }

    private void openNotificationConfig(int index) {
        minecraft.setScreen(new ConfigScreen(minecraft.screen,
                Component.translatable("screen.chatnotify.title.notif"),
                new NotifConfigListWidget(minecraft, screen.width, screen.height, getY(),
                        itemHeight, entryRelX, entryWidth, entryHeight, scrollWidth,
                        ChatNotify.config().getNotifs().get(index), index == 0)));
    }

    public static class Entry extends ConfigListWidget.Entry {

        private static class MixinAndKeyDebugEntry extends Entry {
            MixinAndKeyDebugEntry(int x, int width, int height) {
                super();

                int spacing = 4;
                int buttonWidth = (width - spacing) / 2;

                elements.add(CycleButton.booleanBuilder(
                                Component.translatable("options.on").withStyle(ChatFormatting.GREEN),
                                Component.translatable("options.off").withStyle(ChatFormatting.RED))
                        .withInitialValue(ChatNotify.config().mixinEarly)
                        .withTooltip((status) -> Tooltip.create(Component.nullToEmpty(
                                "If ChatNotify is not detecting incoming messages, try changing this.")))
                        .create(x, 0, buttonWidth, height, Component.literal("Early Mixin"),
                                (button, status) -> ChatNotify.config().mixinEarly = status));

                elements.add(CycleButton.booleanBuilder(
                                Component.translatable("options.on").withStyle(ChatFormatting.GREEN),
                                Component.translatable("options.off").withStyle(ChatFormatting.RED))
                        .withInitialValue(ChatNotify.config().debugShowKey)
                        .withTooltip((value) -> Tooltip.create(Component.literal(("Turn ON to show " +
                                "translation key info of new messages when you hover over them in chat." +
                                "\nTurn OFF if not in use."))))
                        .create(x + buttonWidth + spacing, 0, buttonWidth, height, Component.literal("Debug Keys"),
                                (button, status) -> ChatNotify.config().debugShowKey = status));
            }
        }

        private static class OwnMsgToggleEntry extends Entry {
            OwnMsgToggleEntry(int x, int width, int height) {
                super();
                elements.add(CycleButton.booleanBuilder(
                        Component.translatable("options.on").withStyle(ChatFormatting.GREEN),
                                Component.translatable("options.off").withStyle(ChatFormatting.RED))
                        .withInitialValue(ChatNotify.config().checkOwnMessages)
                        .withTooltip((status) -> Tooltip.create(Component.nullToEmpty(
                                "Turn OFF to prevent your own messages activating notifications.")))
                        .create(x, 0, width, height, Component.literal("Check Own Messages"),
                                (button, status) -> ChatNotify.config().checkOwnMessages = status));
            }
        }

        private static class SoundSourceEntry extends Entry {
            SoundSourceEntry(int x, int width, int height) {
                super();
                elements.add(CycleButton.<SoundSource>builder(source -> Component.translatable(
                        "soundCategory." + source.getName()))
                        .withValues(SoundSource.values())
                        .withInitialValue(ChatNotify.config().soundSource)
                        .withTooltip((status) -> Tooltip.create(Component.nullToEmpty(
                                "Notification sound volume control category.")))
                        .create(x, 0, width, height, Component.literal("Sound Volume Type"),
                                (button, status) -> ChatNotify.config().soundSource = status));
            }
        }

        private static class PrefixConfigEntry extends Entry {
            PrefixConfigEntry(int x, int width, int height, GlobalConfigListWidget listWidget) {
                super();
                elements.add(Button.builder(getMessage(),
                                (button) -> listWidget.openPrefixConfig())
                        .pos(x, 0)
                        .size(width, height)
                        .build());
            }

            private MutableComponent getMessage() {
                StringBuilder messageBuilder = new StringBuilder("Prefixes: ");
                List<String> prefixes = ChatNotify.config().prefixes;
                if (prefixes.isEmpty()) {
                    messageBuilder.append("[None]");
                }
                else {
                    messageBuilder.append(prefixes.get(0));
                    for (int i = 1; i < prefixes.size(); i++) {
                        messageBuilder.append(", ");
                        messageBuilder.append(prefixes.get(i));
                    }
                }
                return Component.literal(messageBuilder.toString());
            }
        }

        private static class NotifConfigEntry extends Entry {
            private final int mainButtonWidth;

            NotifConfigEntry(int x, int width, int height, GlobalConfigListWidget listWidget, int index) {
                super();

                int spacing = 5;
                int statusButtonWidth = 25;
                mainButtonWidth = width - statusButtonWidth - spacing;
                int moveButtonWidth = 12;
                int removeButtonWidth = 24;
                Notification notif = ChatNotify.config().getNotifs().get(index);

                elements.add(Button.builder(getMessage(notif),
                                (button) -> listWidget.openNotificationConfig(index))
                        .pos(x, 0)
                        .size(mainButtonWidth, height)
                        .build());

                elements.add(CycleButton.booleanBuilder(
                        Component.translatable("options.on").withStyle(ChatFormatting.GREEN),
                                Component.translatable("options.off").withStyle(ChatFormatting.RED))
                        .displayOnlyValue()
                        .withInitialValue(notif.isEnabled())
                        .create(x + mainButtonWidth + spacing, 0, statusButtonWidth, height,
                                Component.empty(),
                                (button, status) -> notif.setEnabled(status)));

                if (index > 0) {
                    elements.add(Button.builder(Component.literal("\u2191"),
                                    (button) -> {
                                        if (Screen.hasShiftDown()) {
                                            listWidget.moveNotifUpFull(index);
                                        } else {
                                            listWidget.moveNotifUp(index);
                                        }})
                            .pos(x - 2 * moveButtonWidth - spacing, 0)
                            .size(moveButtonWidth, height)
                            .build());

                    elements.add(Button.builder(Component.literal("\u2193"),
                                    (button) -> {
                                        if (Screen.hasShiftDown()) {
                                            listWidget.moveNotifDownFull(index);
                                        } else {
                                            listWidget.moveNotifDown(index);
                                        }})
                            .pos(x - moveButtonWidth - spacing, 0)
                            .size(moveButtonWidth, height)
                            .build());

                    elements.add(Button.builder(Component.literal("\u274C"),
                                    (button) -> listWidget.removeNotification(index))
                            .pos(x + width + spacing, 0)
                            .size(removeButtonWidth, height)
                            .build());
                }
            }

            private MutableComponent getMessage(Notification notif) {
                MutableComponent message;
                int maxWidth = (int)(mainButtonWidth * 0.8);
                Font font = Minecraft.getInstance().font;

                if (notif.triggers.isEmpty() || notif.triggers.get(0).string.isBlank()) {
                    message = Component.literal("> Click to Configure <");
                }
                else {
                    StringBuilder messageBuilder = new StringBuilder();

                    for (int i = 0; i < notif.triggers.size(); i++) {
                        Trigger trigger = notif.triggers.get(i);
                        String triggerStr;
                        if (trigger.isKey()) {
                            triggerStr = "[Key] " + (trigger.string.equals(".") ? "Any Message" : trigger.string);
                        }
                        else {
                            triggerStr = trigger.string;
                        }

                        if (i == 0) {
                            messageBuilder.append(triggerStr);
                        }
                        else if (font.width(messageBuilder.toString()) + font.width(triggerStr) <= maxWidth) {
                            messageBuilder.append(", ");
                            messageBuilder.append(triggerStr);
                        }
                        else {
                            messageBuilder.append(" [+");
                            messageBuilder.append(notif.triggers.size() - i);
                            messageBuilder.append("]");
                            break;
                        }
                    }

                    message = Component.literal(messageBuilder.toString())
                            .setStyle(notif.textStyle.getStyle());
                }
                return message;
            }
        }
    }
}