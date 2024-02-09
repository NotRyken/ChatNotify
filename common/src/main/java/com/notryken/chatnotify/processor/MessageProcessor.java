package com.notryken.chatnotify.processor;

import com.mojang.datafixers.util.Pair;
import com.notryken.chatnotify.ChatNotify;
import com.notryken.chatnotify.config.Notification;
import com.notryken.chatnotify.config.TextStyle;
import com.notryken.chatnotify.config.Trigger;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.notryken.chatnotify.ChatNotify.config;
import static com.notryken.chatnotify.ChatNotify.recentMessages;

/*
 * Message processing algorithm, starting at processMessage().
 */
public class MessageProcessor {

    /**
     * Initiates the message processing algorithm.
     * @param msg The original message.
     * @return A modified copy of {@code msg}, or the original if no modifying
     * was required.
     */
    public static Component processMessage(Component msg) {
        if (config().debugShowKey) {
            msg = addKeyInfo(msg);
        }

        String msgStr = msg.getString();
        if (msgStr.isBlank()) return msg; // Ignore blank messages
        String checkedMsgStr = checkOwner(msgStr);
        Component modifiedMsg = null;

        if (checkedMsgStr != null) {
            modifiedMsg = tryNotify(msg.copy(), msgStr, checkedMsgStr);
        }

        return (modifiedMsg == null ? msg : modifiedMsg);
    }

    public static Component addKeyInfo(Component msg) {
        Style newStyle;
        // Create new Hover and Click events
        if (msg.getContents() instanceof TranslatableContents tc) {
            newStyle = Style.EMPTY
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Key: " + tc.getKey())
                                    .append(Component.literal("\n[Click to Copy]")
                                            .withStyle(ChatFormatting.GOLD))))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD,
                            tc.getKey()));
        }
        else {
            newStyle = Style.EMPTY
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            Component.literal("Message is not translatable")
                                    .withStyle(ChatFormatting.GRAY)));
        }
        // Overwrite existing events
        return overwriteStyle(newStyle, msg.copy());
    }

    public static MutableComponent overwriteStyle(Style style, MutableComponent msg) {
        msg.setStyle(style.applyTo(msg.getStyle()));
        msg.getSiblings().replaceAll((sibling) -> overwriteStyle(style, sibling.copy()));
        return msg;
    }

    /**
     * Determines whether {@code strMsg} was sent by the user and modifies it if
     * necessary to prevent unwanted notifications.
     * <p>
     * {@code strMsg} is identified as sent by the user if it contains a stored
     * message (or command) sent by the user, and has a prefix that is both not
     * contained in the stored message, and contains (according to
     * {@code msgContainsStr()}) a trigger of the username {@code Notification}.
     * <p>
     * If {@code strMsg} is positively identified, it is set to {@code null} if
     * the configuration {@code ignoreOwnMessages} is true, else the part of the
     * prefix that matched a trigger is removed.
     * <p>
     * <b>Note:</b> This approach is imperfect and may fail if, for example,
     * two messages are sent, the first contains the second, and the return of
     * the second message arrives first.
     * @param msgStr the message {@code String} to process.
     * @return the processed version of {@code strMsg}.
     */
    private static @Nullable String checkOwner(String msgStr) {
        // Stored messages are always converted to lowercase, convert to match.
        String msgStrLow = msgStr.toLowerCase(Locale.ROOT);
        // Check for a matching stored message
        for (int i = 0; i < recentMessages.size(); i++) {
            int lastMatchIdx = msgStrLow.lastIndexOf(recentMessages.get(i).getSecond());
            if (lastMatchIdx > 0) {
                // Check for a trigger in the part before the match
                String prefix = msgStr.substring(0, lastMatchIdx);
                for (Trigger trigger : config().getNotifs().get(0).triggers) {
                    Pair<Integer,Integer> prefixMatch = msgContainsStr(prefix, trigger.string, false);
                    if (prefixMatch != null) {
                        // Both conditions are now satisfied
                        // Remove the matching stored message
                        recentMessages.remove(i);
                        // Modify the message string
                        if (!ChatNotify.config().checkOwnMessages) {
                            msgStr = null;
                        }
                        else {
                            msgStr = msgStr.substring(0, prefixMatch.getFirst()) +
                                    msgStr.substring(prefixMatch.getSecond());
                        }
                        return msgStr;
                    }
                }
            }
        }
        return msgStr;
    }

    /**
     * For each trigger of each ChatNotify {@code Notification}, checks whether
     * the trigger matches the given message using {@code msgContainsStr()}.
     * <p>
     * When a trigger matches, checks the exclusion triggers of the
     * {@code Notification} to determine whether to activate the notification.
     * <p>
     * If the notification should be activated, attempts to complete the
     * relevant notification actions.
     * <p>
     * <b>Note:</b> For performance and simplicity reasons, this method only
     * allows one notification to be triggered by a given message.
     * @param message the original message {@code Component}.
     * @param msgStr the message {@code String}.
     * @param checkedMsgStr the owner-checked version of {@code msgStr}.
     * @return a re-styled copy of {@code msg}, or null if no trigger matched.
     */
    private static Component tryNotify(Component message, String msgStr, String checkedMsgStr) {
        for (Notification notif : ChatNotify.config().getNotifs()) {
            if (notif.isEnabled() && !notif.editing) {
                for (Trigger trigger : notif.triggers) {
                    if (!trigger.string.isBlank()) {
                        if (triggerMatched(notif, trigger, message, msgStr, checkedMsgStr)) {
                            boolean excluded = false;
                            if (notif.exclusionEnabled) {
                                for (Trigger exclTrigger : notif.exclusionTriggers) {
                                    if (triggerMatched(notif, exclTrigger, message, msgStr, checkedMsgStr)) {
                                        excluded = true;
                                        break;
                                    }
                                }
                            }
                            if (!excluded) {
                                playSound(notif);
                                sendResponses(notif);
                                return (trigger.isKey() || trigger.isRegex) ?
                                        simpleRestyle(message, notif) :
                                        complexRestyle(message, trigger.string, notif);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean triggerMatched(Notification notif, Trigger trigger,
                                          Component message, String msgStr, String checkedMsgStr) {
        boolean match = false;
        if (trigger.isKey()) {
            if (trigger.string.equals(".")) {
                match = true;
            }
            else if (message.getContents() instanceof TranslatableContents tc) {
                if (tc.getKey().contains(trigger.string)) {
                    match = true;
                }
            }
        }
        else if (msgContainsStr(notif.allowRegex && trigger.isRegex ? msgStr : checkedMsgStr,
                trigger.string, notif.allowRegex && trigger.isRegex) != null) {
            match = true;
        }
        return match;
    }

    /**
     * If {@code strIsRegex} is {@code true}, attempts to compile {@code str}
     * as a regex pattern. Else, compiles
     * {@code (?<!\w)((\W?|(§[a-z0-9])+)(?i)<str>\W?)(?!\w)}. Uses the compiled
     * pattern to search {@code strMsg}.
     * @param strMsg the {@code String} to search in.
     * @param str the {@code String} or regex to search with.
     * @param strIsRegex control flag for whether to compile str as a pattern.
     * @return n integer array [start,end] of the match, or {@code null} if not
     * found or if strIsRegex is true and str does not represent a valid regex.
     */
    private static Pair<Integer,Integer> msgContainsStr(String strMsg, String str, boolean strIsRegex) {
        try {
            Matcher matcher = strIsRegex ? Pattern.compile(str).matcher(strMsg) :
                    Pattern.compile("(?<!\\w)((\\W?|(§[a-z0-9])+)(?i)" +
                    Pattern.quote(str) + "\\W?)(?!\\w)").matcher(strMsg);
            if (matcher.find()) {
                return Pair.of(matcher.start(), matcher.end());
            }
        } catch (PatternSyntaxException e) {
            ChatNotify.LOG.warn("ChatNotify: Error processing regex: " + e);
        }
        return null;
    }

    /**
     * Plays the sound of the specified {@code Notification}, if the relevant
     * control is enabled.
     * @param notif the {@code Notification}.
     */
    private static void playSound(Notification notif) {
        if (notif.sound.isEnabled()) {
            Minecraft.getInstance().getSoundManager().play(
                    new SimpleSoundInstance(
                            notif.sound.getResourceLocation(), config().soundSource,
                            notif.sound.getVolume(), notif.sound.getPitch(),
                            SoundInstance.createUnseededRandom(), false, 0,
                            SoundInstance.Attenuation.NONE, 0, 0, 0, true));
        }
    }

    /**
     * Sends all response messages of the specified {@code Notification}, if the
     * relevant control is enabled.
     * @param notif the {@code Notification}.
     */
    private static void sendResponses(Notification notif) {
        if (notif.responseEnabled) {
            Minecraft minecraft = Minecraft.getInstance();
            Screen oldScreen = minecraft.screen;
            minecraft.setScreen(new ChatScreen(""));
            for (String response : notif.responseMessages) {
                if (minecraft.screen instanceof ChatScreen cs) {
                    cs.handleChatInput(response, false);
                }
            }
            minecraft.setScreen(oldScreen);
        }
    }

    /**
     * If the color or format controls of the specified {@code Notification} are
     * enabled, uses {@code applyStyle()} to destructively fill the style of
     * the specified {@code Component} with the {@code Style} of the
     * {@code Notification}.
     * @param msg the {@code Component} to restyle.
     * @param notif the {@code Notification} to draw the {@code Style} from.
     * @return the restyled {@code Component}.
     */
    private static Component simpleRestyle(Component msg, Notification notif) {
        if (notif.textStyle.isEnabled()) {
            msg = msg.copy().setStyle(applyStyle(msg.getStyle(), notif.textStyle));
        }
        return msg;
    }

    /**
     * If the color or format controls of the specified {@code Notification} are
     * enabled, initiates a recursive {@code Component} break-down algorithm
     * to restyle only the part of the specified {@code Component} that matches
     * the specified trigger.
     * @param msg the {@code Component} to restyle.
     * @param trigger the {@code String} to restyle in the specified
     * {@code Component}.
     * @param notif the {@code Notification} to draw the {@code Style} from.
     * @return the restyled {@code Component}.
     */
    private static Component complexRestyle(Component msg, String trigger, Notification notif) {
        if (notif.textStyle.isEnabled()) {
            msg = restyleComponent(msg.copy(), trigger, notif.textStyle);
        }
        return msg;
    }

    /**
     * Recursively deconstructs the specified {@code MutableComponent} to
     * find and restyle only the specified trigger.
     * @param msg the {@code MutableComponent} to restyle.
     * @param trigger the {@code String} to restyle.
     * @param textStyle the {@code TextStyle} to apply.
     * @return the {@code MutableComponent}, restyled if possible.
     */
    private static MutableComponent restyleComponent(MutableComponent msg, String trigger,
                                                     TextStyle textStyle) {

        if (msg.getContents() instanceof PlainTextContents) {
            // PlainTextContents is typically the lowest level
            msg = restyleContents(msg, trigger, textStyle);
        }
        else if (msg.getContents() instanceof TranslatableContents contents) {
            // Recurse for all args
            Object[] args = contents.getArgs();
            for (int i = 0; i < contents.getArgs().length; i++) {
                if (args[i] instanceof Component argComponent) {
                    args[i] = restyleComponent(argComponent.copy(), trigger, textStyle);
                }
                else if (args[i] instanceof String argString) {
                    args[i] = restyleComponent(Component.literal(argString), trigger, textStyle);
                }
            }
            // Reconstruct
            msg = MutableComponent.create(new TranslatableContents(
                    contents.getKey(), contents.getFallback(), args))
                    .setStyle(msg.getStyle());
        }
        else {
            // Recurse for all siblings
            msg.getSiblings().replaceAll(text -> restyleComponent(text.copy(), trigger, textStyle));
        }
        return msg;
    }

    /**
     * If the contents of the specified {@code MutableComponent} is an instance
     * of {@code PlainTextContents}, deconstructs, restyles and reconstructs
     * the {@code} MutableComponent with the objective of applying the specified
     * {@code Style} to only the occurrence of the specified trigger.
     * @param msg the {@code MutableComponent} to restyle.
     * @param trigger the {@code String} to restyle within the
     *                {@code MutableComponent}.
     * @param textStyle the {@code TextStyle} to apply.
     * @return the {@code MutableComponent}, restyled if possible.
     */
    private static MutableComponent restyleContents(MutableComponent msg,
                                                    String trigger, TextStyle textStyle) {
        if (!(msg.getContents() instanceof PlainTextContents contents)) return msg;

        String msgStr = contents.text();
        Pair<Integer,Integer> triggerMatch = msgContainsStr(msgStr, trigger, false);

        if (triggerMatch == null) {
            // Trigger not found, try siblings
            msg.getSiblings().replaceAll(text -> restyleComponent(text.copy(), trigger, textStyle));
        }
        else {
            // Trigger found, restyle
            List<Component> siblings = msg.getSiblings();

            if (siblings.isEmpty()) {
                // Split, restyle and reconstruct

                int matchFirst = triggerMatch.getFirst();
                int matchLast = triggerMatch.getSecond();

                // Some magic to deal with format codes
                if (msgStr.contains("§")) {
                    String activeCodes = activeFormatCodes(msgStr.substring(0, matchLast-trigger.length()));

                    String msgTriggerFull = msgStr.substring(matchFirst,matchLast);
                    int realStart = startIgnoreCodes(msgTriggerFull,
                            msgTriggerFull.length() - trigger.length());

                    String msgStart = msgStr.substring(0, matchFirst);
                    String msgTrigger = msgTriggerFull.substring(realStart);
                    String msgEnd = msgStr.substring(matchLast);

                    msgStr = msgStart + '\u00a7' + 'r' + msgTrigger + activeCodes + msgEnd;

                    matchLast = matchLast-realStart+2;
                }

                // msgStr before match
                if (matchFirst != 0) {
                    siblings.add(Component.literal(msgStr.substring(0, matchFirst))
                            .setStyle(msg.getStyle()));
                }

                // Match
                siblings.add(Component.literal(msgStr.substring(matchFirst, matchLast))
                        .setStyle(applyStyle(msg.getStyle(), textStyle)));

                // msgStr after match
                if (matchLast != msgStr.length()) {
                    siblings.add(Component.literal(msgStr.substring(matchLast))
                            .setStyle(msg.getStyle()));
                }

                if (siblings.size() == 1) {
                    msg = siblings.get(0).copy();
                }
                else {
                    MutableComponent newMessage = MutableComponent.create(PlainTextContents.EMPTY);
                    newMessage.siblings.addAll(siblings);
                    msg = newMessage;
                }
            }
            else {
                // Unable to restyle without affecting siblings, so add contents
                // as first sibling of a new MutableComponent, followed by other
                // siblings in original order, then restyle that.

                MutableComponent replacement = MutableComponent.create(PlainTextContents.EMPTY);
                replacement.setStyle(msg.getStyle());

                siblings.add(0, MutableComponent.create(msg.getContents()));
                replacement.siblings.addAll(siblings);

                msg = restyleComponent(replacement, trigger, textStyle);
            }
        }
        return msg;
    }

    /**
     * Scans the specified {@code String} and identifies any format codes that
     * are active at the end.
     * @param msgStr the {@code String} to search.
     * @return the active format codes as a {@code String}.
     */
    private static String activeFormatCodes(String msgStr) {
        List<ChatFormatting> activeCodes = new ArrayList<>();
        for (int i = 0; i < msgStr.length(); i++) {
            char c = msgStr.charAt(i);
            if ((int)c == 167) {
                char d = msgStr.charAt(i+1);
                ChatFormatting format = ChatFormatting.getByCode(d);
                if (format != null) {
                    if (format == ChatFormatting.RESET) {
                        activeCodes.clear();
                    }
                    else {
                        if (format.isColor()) {
                            activeCodes.removeIf(ChatFormatting::isColor);
                        }
                        activeCodes.add(format);
                    }
                }
            }
        }

        StringBuilder builder = new StringBuilder();
        for (ChatFormatting cf : activeCodes) {
            builder.append('\u00a7');
            builder.append(cf.getChar());
        }
        return builder.toString();
    }

    /**
     * Workaround method; if a {@code String} passed to {@code msgContainsStr()}
     * contains format codes immediately preceding a match {@code String}, the
     * range returned will include the format codes.
     * <p>
     * This method scans the specified {@code String} to determine the start
     * of the actual match, defined as being the first character after the
     * last format code.
     * @param str the {@code String} with possible format codes.
     * @param maxStart the maximum possible value .
     * @return the index of the first character after the last format code.
     */
    private static int startIgnoreCodes(String str, int maxStart) {
        char[] arr1 = str.toCharArray();
        int realStart = 0;
        for (int i = 0; i <= maxStart; i++) {
            if ((int)arr1[i] == 167 && (((int)arr1[i+1] > 47 && (int)arr1[i+1] < 58) ||
                    ((int)arr1[i+1] > 96 && (int)arr1[i+1] < 123))) {
                realStart = i+2;
            }
        }
        return realStart;
    }

    /**
     * For each enabled field of {@code textStyle}, overrides the corresponding
     * {@code style} field.
     * @param style the {@code Style} to apply to.
     * @param textStyle the {@code TextStyle} to apply.
     * @return {@code style}, with {@code textStyle} applied.
     */
    private static Style applyStyle(Style style, TextStyle textStyle) {
        if (textStyle.bold.isEnabled()) style = style.withBold(textStyle.bold.isOn());
        if (textStyle.italic.isEnabled()) style = style.withItalic(textStyle.italic.isOn());
        if (textStyle.underlined.isEnabled()) style = style.withUnderlined(textStyle.underlined.isOn());
        if (textStyle.strikethrough.isEnabled()) style = style.withStrikethrough(textStyle.strikethrough.isOn());
        if (textStyle.obfuscated.isEnabled()) style = style.withObfuscated(textStyle.obfuscated.isOn());
        if (textStyle.doColor) style = style.withColor(textStyle.getTextColor());
        return style;
    }
}
