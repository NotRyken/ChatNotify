package notryken.chatnotify.gui.component.listwidget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import notryken.chatnotify.config.Notification;
import notryken.chatnotify.gui.component.widget.RgbChannelSlider;
import notryken.chatnotify.util.ColorUtil;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;

/**
 * {@code ConfigListWidget} containing controls for text color of the
 * specified {@code Notification}.
 */
public class ColorConfigListWidget extends ConfigListWidget {
    public final Notification notif;

    public ColorConfigListWidget(Minecraft minecraft, int width, int height,
                                 int top, int bottom, int itemHeight,
                                 int entryRelX, int entryWidth, int entryHeight,
                                 int scrollWidth, Notification notif) {
        super(minecraft, width, height, top, bottom, itemHeight, 
                entryRelX, entryWidth, entryHeight, scrollWidth);
        this.notif = notif;

        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("Notification Text Color")
                        .setStyle(Style.EMPTY.withColor(this.notif.getColor())),
                null, -1));

        addEntry(new Entry.RgbSliderEntry(entryX, entryWidth, entryHeight, "Red: ", notif::getColorInt,
                (color) -> {
                    notif.setColorInt(ColorUtil.withRed.applyAsInt(notif.getColorInt(), color));
                    refreshColorIndicator();
                },
                ColorUtil.toRed, ColorUtil.fromRed));
        addEntry(new Entry.RgbSliderEntry(entryX, entryWidth, entryHeight, "Green: ", notif::getColorInt,
                (color) -> {
                    notif.setColorInt(ColorUtil.withGreen.applyAsInt(notif.getColorInt(), color));
                    refreshColorIndicator();
                },
                ColorUtil.toGreen, ColorUtil.fromGreen));
        addEntry(new Entry.RgbSliderEntry(entryX, entryWidth, entryHeight, "Blue: ", notif::getColorInt,
                (color) -> {
                    notif.setColorInt(ColorUtil.withBlue.applyAsInt(notif.getColorInt(), color));
                    refreshColorIndicator();
                },
                ColorUtil.toBlue, ColorUtil.fromBlue));

        addEntry(new Entry.ColorSelectionEntry(entryX, entryWidth, notif::setColorInt, this));
    }

    @Override
    public ColorConfigListWidget resize(int width, int height, int top, int bottom, int itemHeight) {
        return new ColorConfigListWidget(minecraft, width, height, top, bottom, itemHeight,
                entryRelX, entryWidth, entryHeight, scrollWidth, notif);
    }

    public void refreshColorIndicator() {
        remove(0);
        addEntryToTop(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("Notification Text Color")
                        .setStyle(Style.EMPTY.withColor(this.notif.getColor())),
                null, -1));
    }

    private abstract static class Entry extends ConfigListWidget.Entry {

        protected static class RgbSliderEntry extends Entry {
            public RgbSliderEntry(int x, int width, int height, @Nullable String message,
                                  Supplier<Integer> source, Consumer<Integer> dest,
                                  IntUnaryOperator toChannel, IntUnaryOperator fromChannel) {
                super();
                elements.add(new RgbChannelSlider(x, 0, width, height, message, null,
                        source, dest, toChannel, fromChannel));
            }
        }

        protected static class ColorSelectionEntry extends Entry {
            int[] colors = new int[] {
                    10027008,
                    16711680,
                    16753920,
                    16761856,
                    16776960,
                    65280,
                    32768,
                    19456,
                    2142890,
                    65535,
                    255,
                    8388736,
                    16711935,
                    16777215,
                    8421504,
                    0};

            public ColorSelectionEntry(int x, int width, Consumer<Integer> dest,
                                       ColorConfigListWidget listWidget) {
                super();

                int buttonWidth = width / colors.length;
                for (int i = 0; i < colors.length; i++) {
                    int color = colors[i];
                    int setX = x + (width / 2) - (buttonWidth * colors.length / 2);
                    elements.add(Button.builder(Component.literal("\u2588")
                                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))), (button) ->
                            {
                                dest.accept(color);
                                listWidget.reload();
                            })
                            .pos(setX + (buttonWidth * i), 0)
                            .size(buttonWidth, buttonWidth)
                            .build());
                }
            }
        }
    }
}