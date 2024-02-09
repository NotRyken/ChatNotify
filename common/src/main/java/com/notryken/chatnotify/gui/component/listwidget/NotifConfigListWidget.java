package com.notryken.chatnotify.gui.component.listwidget;

import com.notryken.chatnotify.config.Notification;
import com.notryken.chatnotify.config.TriState;
import com.notryken.chatnotify.config.Trigger;
import com.notryken.chatnotify.gui.screen.ConfigScreen;
import com.notryken.chatnotify.util.ColorUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * {@code ConfigListWidget} containing controls for the specified
 * {@code Notification}, including references to sub-screens.
 */
public class NotifConfigListWidget extends ConfigListWidget {
    private final Notification notif;
    private final boolean isUsernameNotif;

    public NotifConfigListWidget(Minecraft minecraft, int width, int height,
                                 int y, int itemHeight,
                                 int entryRelX, int entryWidth, int entryHeight,
                                 int scrollWidth, Notification notif,
                                 boolean isUsernameNotif) {
        super(minecraft, width, height, y, itemHeight, 
                entryRelX, entryWidth, entryHeight, scrollWidth);
        notif.editing = true;
        this.notif = notif;
        this.isUsernameNotif = isUsernameNotif;

        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("Notification Triggers"), null, -1));

        for (int i = 0; i < notif.triggers.size(); i++) {
            addEntry(new Entry.TriggerFieldEntry(entryX, entryWidth, entryHeight,
                    this, notif, notif.triggers.get(i), i));
        }
        addEntry(new ConfigListWidget.Entry.ActionButtonEntry(entryX, 0, entryWidth, entryHeight,
                Component.literal("+"), null, -1,
                (button) -> {
                    notif.triggers.add(new Trigger());
                    reload();
                }));


        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("Notification Options"), null, -1));

        addEntry(new Entry.SoundConfigEntry(entryX, entryWidth, entryHeight, notif, this));
        addEntry(new Entry.ColorConfigEntry(entryX, entryWidth, entryHeight, notif, this));
        addEntry(new Entry.FormatConfigEntry1(entryX, entryWidth, entryHeight, notif));
        addEntry(new Entry.FormatConfigEntry2(entryX, entryWidth, entryHeight, notif));

        addEntry(new ConfigListWidget.Entry.ActionButtonEntry(entryX, 0, entryWidth, entryHeight,
                Component.literal("Advanced Settings"),
                Tooltip.create(Component.literal("Here be Dragons!")), 500,
                (button) -> openAdvancedConfig()));
    }

    @Override
    public NotifConfigListWidget resize(int width, int height, int y,
                                        int itemHeight, double scrollAmount) {
        NotifConfigListWidget newListWidget = new NotifConfigListWidget(
                minecraft, width, height, y, itemHeight,
                entryRelX, entryWidth, entryHeight, scrollWidth, notif, isUsernameNotif);
        newListWidget.setScrollAmount(scrollAmount);
        return newListWidget;
    }

    @Override
    public void onClose() {
        notif.editing = false;
        notif.autoDisable();
    }

    private void openKeyConfig(Trigger trigger) {
        minecraft.setScreen(new ConfigScreen(minecraft.screen,
                Component.translatable("screen.chatnotify.title.key"),
                new KeyConfigListWidget(minecraft, screen.width, screen.height, getY(),
                        itemHeight, entryRelX, entryWidth, entryHeight, scrollWidth, trigger)));
    }

    private void openColorConfig() {
        minecraft.setScreen(new ConfigScreen(minecraft.screen,
                Component.translatable("screen.chatnotify.title.color"),
                new ColorConfigListWidget(minecraft, screen.width, screen.height, getY(), 
                        itemHeight, entryRelX, entryWidth, entryHeight, scrollWidth, notif)));
    }

    private void openSoundConfig() {
        minecraft.setScreen(new ConfigScreen(minecraft.screen,
                Component.translatable("screen.chatnotify.title.sound"),
                new SoundConfigListWidget(minecraft, screen.width, screen.height, getY(), 
                        itemHeight, entryRelX, entryWidth, entryHeight, scrollWidth, notif)));
    }

    private void openAdvancedConfig() {
        minecraft.setScreen(new ConfigScreen(minecraft.screen,
                Component.translatable("screen.chatnotify.title.advanced"),
                new AdvancedConfigListWidget(minecraft, screen.width, screen.height, getY(), 
                        itemHeight, entryRelX, entryWidth, entryHeight, scrollWidth, notif)));
    }

    private abstract static class Entry extends ConfigListWidget.Entry {

        private static class TriggerFieldEntry extends Entry {

            TriggerFieldEntry(int x, int width, int height, NotifConfigListWidget listWidget,
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

                if (listWidget.isUsernameNotif && index <= 1) {
                    triggerEditBox.setEditable(false);
                    triggerEditBox.active = false;
                    triggerEditBox.setTooltip(Tooltip.create(Component.literal(
                            index == 0 ? "Profile name" : "Display name")));
                    triggerEditBox.setTooltipDelay(500);
                    elements.add(triggerEditBox);
                }
                else {
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
                            regexButton.active = false;
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
                                "Translation Key trigger")));
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
                                        notif.triggers.remove(index);
                                        listWidget.reload();
                                    })
                            .pos(x + width + spacing, 0)
                            .size(removeButtonWidth, height)
                            .build());
                }
            }
        }

        private static class SoundConfigEntry extends Entry {
            SoundConfigEntry(int x, int width, int height, Notification notif,
                             NotifConfigListWidget listWidget) {
                super();

                int spacing = 5;
                int statusButtonWidth = 25;
                int mainButtonWidth = width - statusButtonWidth - spacing;

                elements.add(Button.builder(
                                Component.literal("Sound: " + notif.sound.getId()),
                                (button) -> listWidget.openSoundConfig())
                        .pos(x, 0)
                        .size(mainButtonWidth, height)
                        .build());

                elements.add(CycleButton.booleanBuilder(
                        Component.translatable("options.on").withStyle(ChatFormatting.GREEN),
                                Component.translatable("options.off").withStyle(ChatFormatting.RED))
                        .displayOnlyValue()
                        .withInitialValue(notif.sound.isEnabled())
                        .create(x + width - statusButtonWidth, 0, statusButtonWidth, height,
                                Component.empty(),
                                (button, status) -> notif.sound.setEnabled(status)));
            }
        }

        private static class ColorConfigEntry extends Entry {
            ColorConfigEntry(int x, int width, int height, Notification notif, NotifConfigListWidget listWidget) {
                super();

                Font activeFont = Minecraft.getInstance().font;
                int spacing = 5;
                int statusButtonWidth = 25;
                int reloadButtonWidth = 20;
                int colorFieldWidth = activeFont.width("#FFAAFF+++");
                int mainButtonWidth = width - statusButtonWidth - reloadButtonWidth -
                        colorFieldWidth - spacing * 2;

                String mainButtonMessage = "Text Color";
                Button mainButton = Button.builder(Component.literal(mainButtonMessage)
                                        .setStyle(Style.EMPTY.withColor(notif.textStyle.getTextColor())),
                                (button) -> listWidget.openColorConfig())
                        .pos(x, 0)
                        .size(mainButtonWidth, height)
                        .build();
                elements.add(mainButton);

                EditBox colorEditBox = new EditBox(activeFont, x + mainButtonWidth + spacing, 0,
                        colorFieldWidth, height, Component.literal("Hex Color"));
                colorEditBox.setMaxLength(7);
                colorEditBox.setResponder(strColor -> {
                    TextColor color = ColorUtil.parseColor(strColor);
                    if (color != null) {
                        notif.textStyle.color = color.getValue();
                        mainButton.setMessage(Component.literal(mainButtonMessage)
                                .setStyle(Style.EMPTY.withColor(notif.textStyle.getTextColor())));
                    }
                });
                colorEditBox.setValue(notif.textStyle.getTextColor().formatValue());
                elements.add(colorEditBox);

                elements.add(Button.builder(Component.literal("\ud83d\uddd8"),
                                (button) -> listWidget.reload())
                        .tooltip(Tooltip.create(Component.literal("Check Value")))
                        .pos(x + mainButtonWidth + spacing + colorFieldWidth, 0)
                        .size(reloadButtonWidth, height)
                        .build());

                elements.add(CycleButton.booleanBuilder(
                        Component.translatable("options.on").withStyle(ChatFormatting.GREEN),
                                Component.translatable("options.off").withStyle(ChatFormatting.RED))
                        .displayOnlyValue()
                        .withInitialValue(notif.textStyle.doColor)
                        .create(x + width - statusButtonWidth, 0,
                                statusButtonWidth, height, Component.empty(),
                                (button, status) -> notif.textStyle.doColor = status));
            }
        }

        private static class FormatConfigEntry1 extends Entry {
            FormatConfigEntry1(int x, int width, int height, Notification notif) {
                super();

                int spacing = 5;
                int buttonWidth = (width - spacing * 2) / 3;

                CycleButton<TriState.State> boldButton = CycleButton.<TriState.State>builder(
                        (state) -> getMessage(state, ChatFormatting.BOLD))
                        .withValues(TriState.State.values())
                        .withInitialValue(notif.textStyle.bold.getState())
                        .withTooltip(this::getTooltip)
                        .create(x, 0, buttonWidth, height,
                                Component.literal("Bold"),
                                (button, state) -> notif.textStyle.bold.state = state);
                boldButton.setTooltipDelay(500);
                elements.add(boldButton);

                CycleButton<TriState.State> italicButton = CycleButton.<TriState.State>builder(
                        (state) -> getMessage(state, ChatFormatting.ITALIC))
                        .withValues(TriState.State.values())
                        .withInitialValue(notif.textStyle.italic.getState())
                        .withTooltip(this::getTooltip)
                        .create(x + width / 2 - buttonWidth / 2, 0, buttonWidth, height,
                                Component.literal("Italic"),
                                (button, state) -> notif.textStyle.italic.state = state);
                italicButton.setTooltipDelay(500);
                elements.add(italicButton);

                CycleButton<TriState.State> underlineButton = CycleButton.<TriState.State>builder(
                        (state) -> getMessage(state, ChatFormatting.UNDERLINE))
                        .withValues(TriState.State.values())
                        .withInitialValue(notif.textStyle.underlined.getState())
                        .withTooltip(this::getTooltip)
                        .create(x + width - buttonWidth, 0, buttonWidth, height,
                                Component.literal("Underline"),
                                (button, state) -> notif.textStyle.underlined.state = state);
                underlineButton.setTooltipDelay(500);
                elements.add(underlineButton);
            }

            private Component getMessage(TriState.State state, ChatFormatting format) {
                return switch(state) {
                    case OFF -> Component.translatable("options.off").withStyle(ChatFormatting.RED);
                    case ON -> Component.translatable("options.on").withStyle(ChatFormatting.GREEN)
                            .withStyle(format);
                    default -> Component.literal("/").withStyle(ChatFormatting.GRAY);
                };
            }

            private Tooltip getTooltip(TriState.State state) {
                if (state.equals(TriState.State.DISABLED)) return
                        Tooltip.create(Component.literal("Use existing format"));
                return null;
            }
        }

        private static class FormatConfigEntry2 extends Entry {
            FormatConfigEntry2(int x, int width, int height, Notification notif) {
                super();

                int spacing = 6;
                int buttonWidth = (width - spacing) / 2;

                CycleButton<TriState.State> strikethroughButton = CycleButton.<TriState.State>builder(
                        (state) -> getMessage(state, ChatFormatting.STRIKETHROUGH))
                        .withValues(TriState.State.values())
                        .withInitialValue(notif.textStyle.strikethrough.getState())
                        .withTooltip(this::getTooltip)
                        .create(x, 0, buttonWidth, height,
                                Component.literal("Strikethrough"),
                                (button, state) -> notif.textStyle.strikethrough.state = state);
                strikethroughButton.setTooltipDelay(500);
                elements.add(strikethroughButton);

                CycleButton<TriState.State> obfuscateButton = CycleButton.<TriState.State>builder(
                        (state) -> getMessage(state, ChatFormatting.OBFUSCATED))
                        .withValues(TriState.State.values())
                        .withInitialValue(notif.textStyle.obfuscated.getState())
                        .withTooltip(this::getTooltip)
                        .create(x + width - buttonWidth, 0, buttonWidth, height,
                                Component.literal("Obfuscate"),
                                (button, state) -> notif.textStyle.obfuscated.state = state);
                obfuscateButton.setTooltipDelay(500);
                elements.add(obfuscateButton);
            }

            private Component getMessage(TriState.State state, ChatFormatting format) {
                return switch(state) {
                    case OFF -> Component.translatable("options.off").withStyle(ChatFormatting.RED);
                    case ON -> Component.translatable("options.on").withStyle(ChatFormatting.GREEN)
                            .withStyle(format);
                    default -> Component.literal("/").withStyle(ChatFormatting.GRAY);
                };
            }

            private Tooltip getTooltip(TriState.State state) {
                if (state.equals(TriState.State.DISABLED)) return
                        Tooltip.create(Component.literal("Use existing format"));
                return null;
            }
        }
    }
}