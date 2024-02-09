package com.notryken.chatnotify.gui.component.listwidget;

import com.notryken.chatnotify.config.Notification;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import com.notryken.chatnotify.ChatNotify;
import com.notryken.chatnotify.gui.component.widget.SilentButton;

/**
 * {@code ConfigListWidget} containing controls for sound of the specified
 * {@code Notification}.
 */
public class SoundConfigListWidget extends ConfigListWidget {
    private final Notification notif;

    public SoundConfigListWidget(Minecraft minecraft, int width, int height,
                                 int y, int itemHeight,
                                 int entryRelX, int entryWidth, int entryHeight,
                                 int scrollWidth, Notification notif) {
        super(minecraft, width, height, y, itemHeight, 
                entryRelX, entryWidth, entryHeight, scrollWidth);
        this.notif = notif;

        addEntry(new Entry.SoundFieldEntry(entryX, entryWidth, entryHeight, notif));

        addEntry(new ConfigListWidget.Entry.DoubleSliderEntry(entryX, 0, entryWidth, entryHeight, 0, 1, 2,
                "Volume: ", null, "OFF", null,
                () -> (double)notif.sound.getVolume(),
                (value) -> notif.sound.setVolume(value.floatValue())));

        addEntry(new ConfigListWidget.Entry.DoubleSliderEntry(entryX, 0, entryWidth, entryHeight, 0.5, 2, 2,
                "Pitch: ", null, null, null,
                () -> (double)notif.sound.getPitch(),
                (value) -> notif.sound.setPitch(value.floatValue())));

        addEntry(new ConfigListWidget.Entry.SilentActionButtonEntry(entryX, 0, entryWidth, entryHeight,
                Component.literal("> Click to Test Sound <"),
                Tooltip.create(Component.literal("Volume category currently set to ")
                        .append(Component.translatable("soundCategory."
                                + ChatNotify.config().soundSource.getName()))), -1,
                button -> playNotifSound()));

        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("Noteblock Sounds"), null, -1));
        String[][] noteblockSounds = {
                        {"block.note_block.banjo", "Banjo"},
                        {"block.note_block.bass", "Bass"},
                        {"block.note_block.basedrum", "Bass Drum"},
                        {"block.note_block.bell", "Bell"},
                        {"block.note_block.bit", "Bit"},
                        {"block.note_block.chime", "Chime"},
                        {"block.note_block.cow_bell", "Cow Bell"},
                        {"block.note_block.didgeridoo", "Didgeridoo"},
                        {"block.note_block.flute", "Flute"},
                        {"block.note_block.guitar", "Guitar"},
                        {"block.note_block.harp", "Harp"},
                        {"block.note_block.hat", "Hat"},
                        {"block.note_block.iron_xylophone", "Iron Xylophone"},
                        {"block.note_block.pling", "Pling"},
                        {"block.note_block.snare", "Snare"},
                        {"block.note_block.xylophone", "Xylophone"},
                };
        for (String[] s : noteblockSounds) {
            addEntry(new Entry.SoundOption(entryX, entryWidth, entryHeight, notif, this, s[0], s[1]));
        }

        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("Power/Portal Sounds"), null, -1));
        String[][] powerSounds = new String[][]{
                        {"block.beacon.activate", "Beacon Activate"},
                        {"block.beacon.deactivate", "Beacon Deactivate"},
                        {"block.beacon.power_select", "Beacon Power Select"},
                        {"block.conduit.activate", "Conduit Activate"},
                        {"block.conduit.deactivate", "Conduit Deactivate"},
                        {"block.end_portal_frame.fill", "End Portal Eye"},
                        {"block.portal.travel", "Portal Travel"},
                        {"block.portal.trigger", "Portal Trigger"},
                        {"entity.enderman.teleport", "Teleport"},
                        {"item.trident.return", "Trident Return"},
                        {"entity.elder_guardian.curse", "Elder Guardian Curse"},
                        {"entity.warden.sonic_boom", "Warden Sonic Boom"},
                        {"entity.evoker.cast_spell", "Evoker Cast Spell"},
                        {"entity.evoker.prepare_summon", "Evoker Summon"},
                        {"entity.evoker.prepare_attack", "Evoker Attack"},
                        {"entity.zombie_villager.converted", "Villager Cured"},
                };
        for (String[] s : powerSounds) {
            addEntry(new Entry.SoundOption(entryX, entryWidth, entryHeight, notif, this, s[0], s[1]));
        }

        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("Explosion Sounds"), null, -1));
        String[][] explosionSounds = new String[][]{
                        {"entity.tnt.primed", "TNT Ignite"},
                        {"entity.generic.explode", "TNT Explode"},
                        {"entity.lightning_bolt.thunder", "Thunder"},
                        {"item.firecharge.use", "Fire Charge"},
                        {"block.fire.extinguish", "Fire Extinguish"},
                        {"entity.firework_rocket.blast", "Firework 1"},
                        {"entity.firework_rocket.large_blast", "Firework 2"},
                        {"entity.firework_rocket.twinkle", "Firework 3"},
                };
        for (String[] s : explosionSounds) {
            addEntry(new Entry.SoundOption(entryX, entryWidth, entryHeight, notif, this, s[0], s[1]));
        }

        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("Illager Sounds"), null, -1));
        String[][] villagerSounds = new String[][]{
                        {"entity.villager.ambient", "Villager"},
                        {"entity.villager.yes", "Villager Yes"},
                        {"entity.villager.no", "Villager No"},
                        {"entity.villager.trade", "Villager Trade"},
                        {"entity.pillager.ambient", "Pillager"},
                        {"entity.vindicator.ambient", "Vindicator"},
                        {"entity.evoker.ambient", "Evoker"},

                };
        for (String[] s : villagerSounds) {
            addEntry(new Entry.SoundOption(entryX, entryWidth, entryHeight, notif, this, s[0], s[1]));
        }

        addEntry(new ConfigListWidget.Entry.TextEntry(entryX, entryWidth, entryHeight,
                Component.literal("Misc Sounds"), null, -1));
        String[][] miscSounds = new String[][]{
                        {"entity.arrow.hit_player", "Arrow Hit"},
                        {"block.bell.use", "Bell"},
                        {"block.amethyst_block.hit", "Amethyst 1"},
                        {"block.amethyst_cluster.place", "Amethyst 2"},
                        {"entity.allay.item_thrown", "Allay Throw"},
                        {"entity.iron_golem.repair", "Iron Repair"},
                        {"block.anvil.land", "Anvil Land"},
                        {"item.shield.block", "Shield Block"},
                        {"item.shield.break", "Shield Break"},
                        {"entity.player.death", "Player Death"},
                        {"entity.goat.screaming.prepare_ram", "Screaming Goat"},
                        {"ui.button.click", "UI Button Click"},
                };
        for (String[] s : miscSounds) {
            addEntry(new Entry.SoundOption(entryX, entryWidth, entryHeight, notif, this, s[0], s[1]));
        }
    }

    @Override
    public SoundConfigListWidget resize(int width, int height, int y,
                                        int itemHeight, double scrollAmount) {
        SoundConfigListWidget newListWidget = new SoundConfigListWidget(
                minecraft, width, height, y, itemHeight,
                entryRelX, entryWidth, entryHeight, scrollWidth, notif);
        newListWidget.setScrollAmount(scrollAmount);
        return newListWidget;
    }

    private void refreshSoundField() {
        ConfigListWidget.Entry entry = getEntry(0);
        if (entry instanceof Entry.SoundFieldEntry soundFieldEntry) {
            soundFieldEntry.updateValue();
        }
    }

    private void playNotifSound() {
        minecraft.getSoundManager().play(
                new SimpleSoundInstance(notif.sound.getResourceLocation(),
                        ChatNotify.config().soundSource,
                        notif.sound.getVolume(), notif.sound.getPitch(),
                        SoundInstance.createUnseededRandom(), false, 0,
                        SoundInstance.Attenuation.NONE, 0, 0, 0, true));

    }

    private abstract static class Entry extends ConfigListWidget.Entry {

        private static class SoundFieldEntry extends Entry {
            private final Notification notif;
            private final EditBox soundField;

            SoundFieldEntry(int x, int width, int height, Notification notif) {
                super();
                this.notif = notif;
                soundField = new EditBox(Minecraft.getInstance().font, x, 0, width, height,
                        Component.literal("Notification Sound"));
                soundField.setMaxLength(120);
                soundField.setValue(notif.sound.getId());
                soundField.setResponder((sound) -> notif.sound.setId(sound.strip()));
                elements.add(soundField);
            }

            public void updateValue() {
                soundField.setValue(notif.sound.getId());
            }
        }

        private static class SoundOption extends Entry {
            SoundOption(int x, int width, int height, Notification notif, SoundConfigListWidget listWidget,
                        String sound, String soundName) {
                super();
                elements.add(new SilentButton(x, 0, width, height, Component.literal(soundName),
                        (button) -> {
                            notif.sound.setId(sound);
                            listWidget.refreshSoundField();
                            listWidget.playNotifSound();
                        }));
            }
        }
    }
}