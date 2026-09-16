package com.ttclient.modules.misc;

import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.ModeSetting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatTimestamps extends Module {
    public final ModeSetting format = addSetting(new ModeSetting("Format", "Timestamp format", "HH:mm:ss", "HH:mm:ss", "HH:mm"));

    public ChatTimestamps() {
        super("ChatTimestamps", "Timestamp helper for chat-related modules", Category.MISC);
        setEnabled(true);
    }

    public String stamp() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(format.get());
        return "[" + LocalTime.now().format(fmt) + "] ";
    }
}
