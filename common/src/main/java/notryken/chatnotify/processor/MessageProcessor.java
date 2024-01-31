package notryken.chatnotify.processor;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import notryken.chatnotify.ChatNotify;
import notryken.chatnotify.config.Notification;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static notryken.chatnotify.ChatNotify.config;
import static notryken.chatnotify.ChatNotify.recentMessages;

/**
 * Complete message processing algorithm for {@code ChatNotify}, designed to
 * read an incoming message, identify the relevant {@code Notification}
 * (if any), and complete all notification actions, including playing a sound
 * and modifying the message style (as applicable).
 */
public class MessageProcessor {

    /**
     * Initiates the message processing algorithm.
     * @param msg The original message.
     * @return A modified copy of {@code msg}, or the original if no modifying
     * was required.
     */
    public static Component processMessage(Component msg) {
        String msgStr = msg.getString();
        if (msgStr.isBlank()) return msg; // Ignore blank messages
        String checkedMsgStr = checkOwner(msgStr);
        Component modifiedMsg = null;

        if (checkedMsgStr != null) {
            modifiedMsg = tryNotify(msg.copy(), msgStr, checkedMsgStr);
        }

        return (modifiedMsg == null ? msg : modifiedMsg);
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
                for (String trigger : ChatNotify.config().getNotif(0).getTriggers()) {
                    Pair<Integer,Integer> prefixMatch = msgContainsStr(prefix, trigger, false);
                    if (prefixMatch != null) {
                        // Both conditions are now satisfied
                        // Remove the matching stored message
                        recentMessages.remove(i);
                        // Modify the message string
                        if (ChatNotify.config().ignoreOwnMessages) {
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
            if (notif.isEnabled()) {
                /*
                triggerIsKey indicates that the Notification should only be
                triggered by messages with a TranslatableContents key matching
                a trigger of the Notification.
                The exception to this is the key ".", which should trigger its
                notification irrespective of the message content.
                 */
                if (notif.triggerIsKey) {
                    if (notif.getTrigger().equals(".")) {
                        playSound(notif);
                        sendResponses(notif);
                        return simpleRestyle(message, notif);
                    }
                    else {
                        if (message.getContents() instanceof TranslatableContents ttc) {
                            if (!notif.getTrigger().isBlank() && ttc.getKey().contains(notif.getTrigger())) {
                                playSound(notif);
                                sendResponses(notif);
                                return simpleRestyle(message, notif);
                            }
                        }
                    }
                } else {
                    for (String trigger : notif.getTriggers()) {
                        // Check trigger
                        // Note: Use the unmodified msgStr if regexEnabled
                        if (!trigger.isBlank() && msgContainsStr(
                                (notif.regexEnabled ? msgStr : checkedMsgStr),
                                trigger, notif.regexEnabled) != null) {
                            // Matched trigger, check exclusions
                            boolean exclude = false;
                            if (notif.exclusionEnabled) {
                                for (String exTrig : notif.getExclusionTriggers()) {
                                    if (msgContainsStr((notif.regexEnabled ? msgStr : checkedMsgStr),
                                            exTrig, notif.regexEnabled) != null) {
                                        exclude = true;
                                        break;
                                    }
                                }
                            }
                            if (!exclude) {
                                playSound(notif);
                                sendResponses(notif);
                                return (notif.regexEnabled ? simpleRestyle(message, notif) :
                                        complexRestyle(message, trigger, notif));
                            }
                        }
                    }
                }
            }
        }
        return null;
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
            ChatNotify.LOG.warn("Error processing regex: " + e);
        }
        return null;
    }

    /**
     * Plays the sound of the specified {@code Notification}, if the relevant
     * control is enabled.
     * @param notif the {@code Notification}.
     */
    private static void playSound(Notification notif) {
        if (notif.getControl(2)) {
            Minecraft.getInstance().getSoundManager().play(
                    new SimpleSoundInstance(
                            notif.getSound(), config().notifSoundSource,
                            notif.getSoundVolume(), notif.getSoundPitch(),
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
            Minecraft client = Minecraft.getInstance();
            Screen oldScreen = client.screen;
            for (String response : notif.getResponseMessages()) {
                client.setScreen(new ChatScreen(response));
                if (client.screen instanceof ChatScreen cs) {
                    cs.handleChatInput(response, true);
                }
                client.setScreen(oldScreen);
            }
        }
    }

    /**
     * Constructs a {@code Style} from the text color and format fields of the
     * specified {@code Notification}.
     * @param notif the {@code Notification}.
     * @return the created {@code Style}.
     */
    private static Style getStyle(Notification notif) {
        return Style.create(
                ((!notif.getControl(0)) ?
                        Optional.empty() : Optional.of(notif.getColor())),
                Optional.of(notif.getFormatControl(0)),
                Optional.of(notif.getFormatControl(1)),
                Optional.of(notif.getFormatControl(2)),
                Optional.of(notif.getFormatControl(3)),
                Optional.of(notif.getFormatControl(4)),
                Optional.empty(),
                Optional.empty());
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
        if (notif.getControl(0) || notif.getControl(1)) {
            msg = msg.copy().setStyle(applyStyle(msg.getStyle(), getStyle(notif)));
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
        if (notif.getControl(0) || notif.getControl(1)) {
            msg = restyleComponent(msg.copy(), trigger, getStyle(notif));
        }
        return msg;
    }

    /**
     * Recursively deconstructs the specified {@code MutableComponent} to
     * find and restyle only the specified trigger.
     * @param msg the {@code MutableComponent} to restyle.
     * @param trigger the {@code String} to restyle.
     * @param style the {@code Style} to apply.
     * @return the {@code MutableComponent}, restyled if possible.
     */
    private static MutableComponent restyleComponent(MutableComponent msg, String trigger, Style style) {

        if (msg.getContents() instanceof LiteralContents) {
            // LiteralContents is typically the lowest level
            msg = restyleContents(msg, trigger, style);
        }
        else if (msg.getContents() instanceof TranslatableContents contents) {
            // Recurse for all args
            Object[] args = contents.getArgs();
            for (int i = 0; i < contents.getArgs().length; i++) {
                if (args[i] instanceof Component argText) {
                    args[i] = restyleComponent(argText.copy(), trigger, style);
                }
                else if (args[i] instanceof String argString) {
                    args[i] = Component.literal(argString).setStyle(style);
                }
            }
            // Reconstruct
            msg = MutableComponent.create(new TranslatableContents(
                    contents.getKey(), contents.getFallback(), args))
                    .setStyle(msg.getStyle());
        }
        else {
            // Recurse for all siblings
            msg.getSiblings().replaceAll(text -> restyleComponent(text.copy(), trigger, style));
        }
        return msg;
    }

    /**
     * If the contents of the specified {@code MutableComponent} is an instance
     * of {@code LiteralContents},
     * @param msg the {@code MutableComponent} to restyle.
     * @param trigger the {@code String} to restyle.
     * @param style the {@code Style} to apply.
     * @return the {@code MutableComponent}, restyled if possible.
     */
    private static MutableComponent restyleContents(MutableComponent msg, String trigger, Style style) {
        if (msg.getContents() instanceof LiteralContents contents) {

            // Only process if the message or its siblings contain the match
            String msgStr = contents.text();
            Pair<Integer,Integer> triggerMatch = msgContainsStr(msgStr, trigger, false);

            if (triggerMatch == null) {
                msg.getSiblings().replaceAll(text -> restyleComponent(text.copy(), trigger, style));
            }
            else {
                List<Component> siblings = msg.getSiblings();

                if (siblings.isEmpty())
                {
                    int matchFirst = triggerMatch.getFirst();
                    int matchLast = triggerMatch.getSecond();

                    /*
                    If no siblings, the match must exist in the LiteralContents,
                    so it is deconstructed and the parts individually re-styled
                    before being re-built into a new MutableComponent.
                     */

                    if (msgStr.contains("§")) {

                        String activeCodes = activeFormatCodes(msgStr.substring(0, matchLast-trigger.length()));

                        String msgTriggerFull = msgStr.substring(matchFirst,matchLast);
                        int realStart = startIgnoreCodes(msgTriggerFull, msgTriggerFull.length()-trigger.length());

                        String msgStart = msgStr.substring(0, matchFirst);
                        String msgTrigger = msgTriggerFull.substring(realStart);
                        String msgEnd = msgStr.substring(matchLast);

                        msgStr = msgStart + '\u00a7' + 'r' + msgTrigger + activeCodes + msgEnd;

                        matchLast = matchLast-realStart+2;
                    }

                    /*
                    Split msgStr around the match and add the parts as
                    Components.
                     */

                    // msgStr after match
                    if (matchLast != msgStr.length()) {
                        siblings.add(0, Component.literal(
                                        msgStr.substring(matchLast))
                                .setStyle(msg.getStyle()));
                    }

                    // Match
                    siblings.add(0, Component.literal(msgStr.substring(
                            matchFirst, matchLast)).setStyle(
                                    applyStyle(msg.getStyle(), style)));

                    // msgStr before match
                    if (matchFirst != 0) {
                        siblings.add(0, Component.literal(
                                        msgStr.substring(0, matchFirst))
                                .setStyle(msg.getStyle()));
                    }

                    if (siblings.size() == 1) {
                        msg = siblings.get(0).copy();
                    }
                    else {
                        MutableComponent newMessage = MutableComponent.create(ComponentContents.EMPTY);
                        newMessage.siblings.addAll(siblings);
                        msg = newMessage;
                    }
                }
                else {
                    /*
                    If the message has siblings, it cannot be directly
                    re-styled. Instead, it is replaced by a new, empty
                    MutableComponent, with the original LiteralContents added as
                    the first sibling, and all other siblings subsequently
                    added in their original order. The new MutableComponent is
                    then recursively processed.
                     */

                    MutableComponent replacement = MutableComponent.create(ComponentContents.EMPTY);

                    siblings.add(0, MutableComponent.create(msg.getContents()));

                    replacement.setStyle(msg.getStyle());
                    replacement.siblings.addAll(siblings);

                    msg = restyleComponent(replacement, trigger, style);
                }
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
     * For each non-{@code null}, {@code true} or default field of
     * {@code newStyle}, overrides the corresponding {@code oldStyle} field.
     * @param oldStyle the {@code Style} to apply to.
     * @param newStyle the {@code Style} to apply.
     * @return {@code oldStyle}, with {@code newStyle} applied.
     */
    private static Style applyStyle(Style oldStyle, Style newStyle)
    {
        Style result = oldStyle
                .withBold((newStyle.isBold() ||
                        oldStyle.isBold()))
                .withItalic((newStyle.isItalic() ||
                        oldStyle.isItalic()))
                .withUnderlined((newStyle.isUnderlined() ||
                        oldStyle.isUnderlined()))
                .withStrikethrough((newStyle.isStrikethrough() ||
                        oldStyle.isStrikethrough()))
                .withObfuscated((newStyle.isObfuscated() ||
                        oldStyle.isObfuscated()));
        if (newStyle.getColor() != null) {
            result = result.withColor(newStyle.getColor());
        }
        if (newStyle.getClickEvent() != null) {
            result = result.withClickEvent(newStyle.getClickEvent());
        }
        if (newStyle.getHoverEvent() != null) {
            result = result.withHoverEvent(newStyle.getHoverEvent());
        }
        if (newStyle.getInsertion() != null) {
            result = result.withInsertion(newStyle.getInsertion());
        }
        if (newStyle.getFont() != Style.DEFAULT_FONT) {
            result = result.withFont(newStyle.getFont());
        }
        return result;
    }
}
