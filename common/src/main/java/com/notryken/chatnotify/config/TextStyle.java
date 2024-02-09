package com.notryken.chatnotify.config;

import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.Optional;

public class TextStyle {
    public static final int DEFAULT_COLOR = 16761856;

    public boolean doColor;
    public int color;
    public TriState bold;
    public TriState italic;
    public TriState underlined;
    public TriState strikethrough;
    public TriState obfuscated;

    public TextStyle() {
        this.doColor = true;
        this.color = DEFAULT_COLOR;
        this.bold = new TriState();
        this.italic = new TriState();
        this.underlined = new TriState();
        this.strikethrough = new TriState();
        this.obfuscated = new TriState();
    }

    public TextStyle(boolean doColor, int color, TriState bold, TriState italic,
                     TriState underlined, TriState strikethrough, TriState obfuscated) {
        this.doColor = doColor;
        this.color = color;
        this.bold = bold;
        this.italic = italic;
        this.underlined = underlined;
        this.strikethrough = strikethrough;
        this.obfuscated = obfuscated;
    }

    public TextColor getTextColor() {
        return TextColor.fromRgb(color);
    }

    public boolean isEnabled() {
        return doColor ||
                bold.isEnabled() ||
                italic.isEnabled() ||
                underlined.isEnabled() ||
                strikethrough.isEnabled() ||
                obfuscated.isEnabled();
    }

    public Style getStyle() {
        return Style.create(
                Optional.ofNullable(doColor ? getTextColor() : null),
                Optional.ofNullable(bold.isEnabled() ? bold.isOn() : null),
                Optional.ofNullable(italic.isEnabled() ? italic.isOn() : null),
                Optional.ofNullable(underlined.isEnabled() ? underlined.isOn() : null),
                Optional.ofNullable(strikethrough.isEnabled() ? strikethrough.isOn() : null),
                Optional.ofNullable(obfuscated.isEnabled() ? obfuscated.isOn() : null),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }
}
