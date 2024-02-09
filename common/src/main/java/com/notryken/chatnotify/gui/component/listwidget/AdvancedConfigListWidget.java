package com.notryken.chatnotify.gui.component.listwidget;

import com.notryken.chatnotify.config.Notification;
import com.notryken.chatnotify.config.Trigger;
import com.notryken.chatnotify.gui.screen.ConfigScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import com.notryken.chatnotify.ChatNotify;

/**
 * {@code ConfigListWidget} containing controls for advanced settings of the
 * specified {@code Notification}, including regex toggle, exclusion triggers,
 * automatic response messages and reset buttons.
 */
public class AdvancedConfigListWidget extends ConfigListWidget {
    private final Notification notif;

    public AdvancedConfigListWidget(Minecraft minecraft, int width, int height, int y, int itemHeight,
                                    int entryRelX, int entryWidth, int entryHeight,
                                    int scrollWidth, Notification notif) {
        super(minecraft, width, height, y, itemHeight,
                entryRelX, entryWidth, entryHeight, scrollWidth);
        this.notif = notif;

        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("WARNING").withStyle(ChatFormatting.RED),
                null, -1));

        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("These settings allow you to break ChatNotify and crash " +
                        "Minecraft. Use with caution."), null, -1));

        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("Notification Trigger Regex"), null, -1));
        addEntry(new Entry.RegexToggleButton(entryX, entryWidth, entryHeight, notif, this));

        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("Notification Exclusion Triggers"), null, -1));
        addEntry(new Entry.ExclusionToggleButton(entryX, entryWidth, entryHeight, notif, this));

        if (notif.exclusionEnabled) {
            for (int i = 0; i < this.notif.exclusionTriggers.size(); i ++) {
                addEntry(new Entry.ExclusionTriggerField(entryX, entryWidth, entryHeight,
                        this, notif, notif.exclusionTriggers.get(i), i));
            }
            addEntry(new ConfigListWidget.Entry.ActionButtonEntry(entryX, 0, entryWidth, entryHeight,
                    Component.literal("+"), null, -1,
                    (button) -> {
                        notif.exclusionTriggers.add(new Trigger());
                        reload();
                    }));
        }

        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("Auto Response Messages"), null, -1));
        addEntry(new Entry.ResponseToggleButton(entryX, entryWidth, entryHeight, notif, this));

        if (notif.responseEnabled) {
            for (int i = 0; i < notif.responseMessages.size(); i ++) {
                addEntry(new Entry.ResponseMessageField(entryX, entryWidth, entryHeight, notif, this, i));
            }
            addEntry(new ConfigListWidget.Entry.ActionButtonEntry(entryX, 0, entryWidth, entryHeight,
                    Component.literal("+"), null, -1,
                    (button) -> {
                        notif.responseMessages.add("");
                        reload();
                    }));
        }

        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("Broken Everything?"), null, -1));

        addEntry(new ConfigListWidget.Entry.ActionButtonEntry(entryX, 0, entryWidth, entryHeight,
                Component.literal("Reset Advanced Options"), Tooltip.create(
                        Component.literal("Resets all advanced settings for THIS notification.")),
                -1,
                (button) -> {
                    notif.resetAdvanced();
                    reload();
                }));

        addEntry(new ConfigListWidget.Entry.ActionButtonEntry(entryX, 0, entryWidth, entryHeight,
                Component.literal("Reset All Advanced Options"), Tooltip.create(
                Component.literal("Resets all advanced settings for ALL notifications.")),
                -1,
                (button) -> minecraft.setScreen(new ConfirmScreen(
                        (value) -> {
                            if (value) {
                                for (Notification notif2 : ChatNotify.config().getNotifs()) {
                                    notif2.resetAdvanced();
                                }
                                minecraft.setScreen(null);
                            }
                            else {
                                reload();
                            }},
                        Component.literal("Advanced Options Reset"),
                        Component.literal("Are you sure you want to reset all advanced settings " +
                                "for ALL notifications?")))));

        addEntry(new ConfigListWidget.Entry.ActionButtonEntry(entryX, 0, entryWidth, entryHeight,
                Component.literal("Nuclear Reset"), Tooltip.create(
                Component.literal("Deletes all ChatNotify notifications and resets all settings.")),
                -1,
                (button) -> minecraft.setScreen(new ConfirmScreen(
                        (value) -> {
                            if (value) {
                                ChatNotify.restoreDefaultConfig();
                                minecraft.setScreen(null);
                            }
                            else {
                                reload();
                            }},
                        Component.literal("Nuclear Reset"),
                        Component.literal("Are you sure you want to delete all ChatNotify " +
                                "notifications and reset all settings?")))));
    }

    @Override
    public AdvancedConfigListWidget resize(int width, int height, int y,
                                           int itemHeight, double scrollAmount) {
        AdvancedConfigListWidget newListWidget = new AdvancedConfigListWidget(
                minecraft, width, height, y, itemHeight,
                entryRelX, entryWidth, entryHeight, scrollWidth, notif);
        newListWidget.setScrollAmount(scrollAmount);
        return newListWidget;
    }

    private void openKeyConfig(Trigger trigger) {
        minecraft.setScreen(new ConfigScreen(minecraft.screen,
                Component.translatable("screen.chatnotify.title.key"),
                new KeyConfigListWidget(minecraft, screen.width, screen.height, getY(),
                        itemHeight, entryRelX, entryWidth, entryHeight, scrollWidth, trigger)));
    }

    private abstract static class Entry extends ConfigListWidget.Entry {

        private static class RegexToggleButton extends Entry {
            RegexToggleButton(int x, int width, int height, Notification notif,
                              AdvancedConfigListWidget listWidget) {
                super();
                elements.add(CycleButton.booleanBuilder(
                        Component.translatable("Allowed").withStyle(ChatFormatting.GREEN),
                                Component.translatable("Disabled").withStyle(ChatFormatting.RED))
                        .withInitialValue(notif.allowRegex)
                        .withTooltip(
                                (status) -> {
                                    if (status) {
                                        return Tooltip.create(Component.literal("Use the .* " +
                                                "button next to each trigger to enable/disable regex."));
                                    }
                                    else {
                                        return Tooltip.create(Component.literal("Regex is disabled " +
                                                "for all triggers."));
                                    }
                                })
                        .create(x, 0, width, height, Component.literal("Regex"),
                                (button, status) -> {
                                    notif.allowRegex = status;
                                    listWidget.reload();
                                }));
            }
        }

        private static class ExclusionToggleButton extends Entry {
            ExclusionToggleButton(int x, int width, int height, Notification notif,
                              AdvancedConfigListWidget listWidget) {
                super();
                elements.add(CycleButton.booleanBuilder(
                        Component.translatable("options.on").withStyle(ChatFormatting.GREEN),
                                Component.translatable("options.off").withStyle(ChatFormatting.RED))
                        .withInitialValue(notif.exclusionEnabled)
                        .withTooltip((status) -> Tooltip.create(Component.nullToEmpty(
                                "If an exclusion trigger is detected in a message, " +
                                "it will prevent this notification from activating.")))
                        .create(x, 0, width, height, Component.literal("Exclusion Triggers"),
                                (button, status) -> {
                                    notif.exclusionEnabled = status;
                                    listWidget.reload();
                                }));
            }
        }

        private static class ExclusionTriggerField extends Entry {

            ExclusionTriggerField(int x, int width, int height, AdvancedConfigListWidget listWidget,
                                  Notification notif, Trigger trigger, int index) {
                super();

                int spacing = 5;
                int regexButtonWidth = 15;
                int keyButtonWidth = notif.allowRegex ? 15 : 20;
                int removeButtonWidth = 20;

                EditBox triggerEditBox = new EditBox(Minecraft.getInstance().font,
                        x, 0, width, height, Component.literal("Notification Trigger"));
                triggerEditBox.setMaxLength(120);
                triggerEditBox.setValue(trigger.string);
                triggerEditBox.setResponder((string) -> trigger.string = string.strip());

                Button keyButton;
                if (notif.allowRegex) {
                    Button regexButton;
                    if (trigger.isKey()) {
                        regexButton = Button.builder(Component.literal(".*")
                                                .withStyle(ChatFormatting.GRAY),
                                        (button) -> {})
                                .pos(x - spacing - regexButtonWidth - regexButtonWidth, 0)
                                .size(regexButtonWidth, height)
                                .build();
                        regexButton.setTooltip(Tooltip.create(Component.literal(
                                "Regex Disabled [Key trigger]")));
                        regexButton.setTooltipDelay(500);
                    }
                    else if (trigger.isRegex) {
                        regexButton = Button.builder(Component.literal(".*")
                                                .withStyle(ChatFormatting.GREEN),
                                        (button) -> {
                                            trigger.isRegex = false;
                                            listWidget.reload();
                                        })
                                .pos(x - spacing - regexButtonWidth - regexButtonWidth, 0)
                                .size(regexButtonWidth, height)
                                .build();
                        regexButton.setTooltip(Tooltip.create(Component.literal(
                                "Regex Enabled")));
                        regexButton.setTooltipDelay(500);
                    }
                    else {
                        regexButton = Button.builder(Component.literal(".*")
                                                .withStyle(ChatFormatting.RED),
                                        (button) -> {
                                            trigger.isRegex = true;
                                            listWidget.reload();
                                        })
                                .pos(x - spacing - regexButtonWidth - regexButtonWidth, 0)
                                .size(regexButtonWidth, height)
                                .build();
                        regexButton.setTooltip(Tooltip.create(Component.literal(
                                "Regex Disabled")));
                        regexButton.setTooltipDelay(500);
                    }
                    elements.add(regexButton);
                }
                if (trigger.isKey()) {
                    keyButton = Button.builder(Component.literal("\uD83D\uDD11")
                                            .withStyle(ChatFormatting.GREEN),
                                    (button) -> {
                                        if (Screen.hasShiftDown()) {
                                            trigger.setIsKey(false);
                                            listWidget.reload();
                                        } else {
                                            listWidget.openKeyConfig(trigger);
                                        }})
                            .pos(x - spacing - keyButtonWidth, 0)
                            .size(keyButtonWidth, height)
                            .build();
                    keyButton.setTooltip(Tooltip.create(Component.literal(
                            "Translation key trigger")));
                    keyButton.setTooltipDelay(500);
                }
                else {
                    keyButton = Button.builder(Component.literal("\uD83D\uDD11")
                                            .withStyle(ChatFormatting.RED),
                                    (button) -> {
                                        if (Screen.hasShiftDown()) {
                                            trigger.setIsKey(true);
                                            listWidget.reload();
                                        } else {
                                            listWidget.openKeyConfig(trigger);
                                        }})
                            .pos(x - spacing - keyButtonWidth, 0)
                            .size(keyButtonWidth, height)
                            .build();
                    keyButton.setTooltip(Tooltip.create(Component.literal(
                            "Normal trigger")));
                    keyButton.setTooltipDelay(500);
                }
                elements.add(keyButton);
                elements.add(triggerEditBox);
                elements.add(Button.builder(Component.literal("\u274C"),
                                (button) -> {
                                    notif.exclusionTriggers.remove(index);
                                    listWidget.reload();
                                })
                        .pos(x + width + spacing, 0)
                        .size(removeButtonWidth, height)
                        .build());
            }
        }

        private static class ResponseToggleButton extends Entry {
            ResponseToggleButton(int x, int width, int height, Notification notif,
                                 AdvancedConfigListWidget listWidget)
            {
                super();
                elements.add(CycleButton.booleanBuilder(
                        Component.translatable("options.on").withStyle(ChatFormatting.GREEN),
                                Component.translatable("options.off").withStyle(ChatFormatting.RED))
                        .withInitialValue(notif.responseEnabled)
                        .withTooltip((status) -> Tooltip.create(Component.nullToEmpty(
                                "Chat messages or commands to be sent by the client " +
                                        "immediately when this notification is activated.")))
                        .create(x, 0, width, height, Component.literal("Response Messages"),
                                (button, status) -> {
                                    notif.responseEnabled = status;
                                    listWidget.reload();
                                }));
            }
        }

        private static class ResponseMessageField extends Entry {

            ResponseMessageField(int x, int width, int height, Notification notif,
                                 AdvancedConfigListWidget listWidget, int index) {
                super();

                int spacing = 5;
                int moveButtonWidth = 12;
                int removeButtonWidth = 20;

                elements.add(Button.builder(Component.literal("\u2191"),
                                (button) -> {
                                    if (Screen.hasShiftDown()) {
                                        if (index > 0) {
                                            notif.responseMessages.add(0, notif.responseMessages.get(index));
                                            notif.responseMessages.remove(index + 1);
                                            listWidget.reload();
                                        }
                                    } else {
                                        if (index > 0) {
                                            String temp = notif.responseMessages.get(index);
                                            notif.responseMessages.set(index, notif.responseMessages.get(index - 1));
                                            notif.responseMessages.set(index - 1, temp);
                                            listWidget.reload();
                                        }
                                    }})
                        .pos(x - 2 * moveButtonWidth - spacing, 0)
                        .size(moveButtonWidth, height)
                        .build());

                elements.add(Button.builder(Component.literal("\u2193"),
                                (button) -> {
                                    if (Screen.hasShiftDown()) {
                                        if (index < notif.responseMessages.size() - 1) {
                                            notif.responseMessages.add(notif.responseMessages.get(index));
                                            notif.responseMessages.remove(index);
                                            listWidget.reload();
                                        }
                                    } else {
                                        if (index < notif.responseMessages.size() - 1) {
                                            String temp = notif.responseMessages.get(index);
                                            notif.responseMessages.set(index, notif.responseMessages.get(index + 1));
                                            notif.responseMessages.set(index + 1, temp);
                                            listWidget.reload();
                                        }
                                    }})
                        .pos(x - moveButtonWidth - spacing, 0)
                        .size(moveButtonWidth, height)
                        .build());

                EditBox messageEditBox = new EditBox(Minecraft.getInstance().font,
                        x, 0, width, height, Component.literal("Response Message"));
                messageEditBox.setMaxLength(120);
                messageEditBox.setValue(notif.responseMessages.get(index));
                messageEditBox.setResponder((message) ->
                        notif.responseMessages.set(index, message.strip()));
                elements.add(messageEditBox);

                elements.add(Button.builder(Component.literal("\u274C"),
                                (button) -> {
                                    notif.responseMessages.remove(index);
                                    listWidget.reload();
                                })
                        .pos(x + width + spacing, 0)
                        .size(removeButtonWidth, height)
                        .build());
            }
        }
    }
}