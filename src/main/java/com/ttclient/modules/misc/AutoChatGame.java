package com.ttclient.modules.misc;

import com.ttclient.client.NotificationManager;
import com.ttclient.modules.Category;
import com.ttclient.modules.Module;
import com.ttclient.settings.BoolSetting;
import com.ttclient.settings.NumberSetting;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Auto-solves server chat games only. Heavily filtered so normal player chat
 * is never echoed (old versions matched bare "say" / random math).
 */
public class AutoChatGame extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Ticks before answering", 10, 0, 60, 1));
    public final BoolSetting unscramble = addSetting(new BoolSetting("Unscramble", "Solve word scrambles", true));
    public final BoolSetting math = addSetting(new BoolSetting("Math", "Solve simple math prompts", true));
    public final BoolSetting sayThis = addSetting(new BoolSetting("SayThis", "Answer explicit say-this / type-this prompts", true));
    public final BoolSetting fillBlank = addSetting(new BoolSetting("FillBlank", "Solve fill-in-the-blank prompts", true));
    public final BoolSetting notify = addSetting(new BoolSetting("Notify", "Toast when answering", true));
    public final BoolSetting strict = addSetting(new BoolSetting("Strict", "Require clear game keywords (recommended)", true));

    private String pendingAnswer;
    private int pendingTicks = -1;
    private long lastAnswerAt;
    private String lastAnswer = "";
    private String lastPrompt = "";

    private static final Pattern GAME_HINT = Pattern.compile(
            "(?i)(unscramble|scrambled?\\s+word|chat\\s*game|first\\s+to|"
                    + "type\\s+this|say\\s+this|fill\\s+in|"
                    + "what\\s+is\\s+-?\\d|solve\\s*:|calculate\\s*:|"
                    + "word\\s*[:=]|prize|react\\s+first|quickest|decode|rearrange|anagram)");

    private static final Pattern PLAYER_CHAT = Pattern.compile(
            "(?i)^\\s*(?:[<\\[][^\\]>\\]]+[>\\]]\\s*|[\\w.]{2,16}\\s*(?:»|->|:)\\s+|\\w{2,16}\\s+whispers?\\s*:)");

    // hyphen at end of char class — never \\- in Java strings
    private static final Pattern WORD_LINE = Pattern.compile(
            "(?i)(?:unscramble(?:\\s+the)?(?:\\s+word)?|scrambled?(?:\\s+word)?|word)\\s*[:=-]\\s*([a-z0-9]{3,16})");
    private static final Pattern MATH_LINE = Pattern.compile(
            "(?i)(?:what\\s+is|solve\\s*:?|calculate\\s*:?|compute\\s*:?)[^0-9-]*(-?\\d+\\s*[+*/x-]\\s*\\d+(?:\\s*[+*/x-]\\s*\\d+)?)");
    private static final Pattern SAY_LINE = Pattern.compile(
            "(?i)(?:say\\s+this|type\\s+this|type\\s+the\\s+(?:word|phrase)|repeat\\s+this)\\s*[:=-]\\s*[\"']?([^\"'\\n]{1,40}?)[\"']?\\s*$");
    private static final Pattern FILL_LINE = Pattern.compile(
            "(?i)(?:fill\\s+in|complete\\s+the\\s+word|missing\\s+letters)?[^\\n]*?([A-Za-z](?:_[A-Za-z]){2,}|_{2,}[A-Za-z](?:_[A-Za-z])*)");

    private static final String[] DICT = {
            "apple","banana","orange","grape","melon","berry","peach","mango","lemon","cherry",
            "house","horse","heart","earth","water","fire","stone","block","sword","shield",
            "diamond","emerald","gold","iron","copper","nether","ender","creeper","zombie","skeleton",
            "spider","villager","dragon","wither","totem","elytra","beacon","portal","obsidian","bedrock",
            "balance","blank","blade","bread","brick","bridge","bright","bring","broken","build",
            "beeks","beek","bees","beach","bean","bear","beast","beat","become","before",
            "chicken","potato","carrot","wheat","sugar","cookie","cake","pizza","burger","salad",
            "school","player","server","client","window","button","screen","mouse","keyboard","chat",
            "green","blue","red","yellow","purple","white","black","brown","pink",
            "table","chair","phone","clock","watch","light","night","morning","evening","friend",
            "family","money","power","speed","jump","walk","run","swim","fly","mine",
            "craft","game","play","win","lose","prize","first","second","third","quick",
            "answer","question","problem","number","letter","word","sentence","title","name","place",
            "world","ocean","river","mountain","forest","desert","island","city","town","village",
            "king","queen","prince","knight","castle","armor","helmet","boots","legs","chest",
            "happy","angry","brave","smart","lucky","magic","spell","potion","enchant","level",
            "health","hunger","damage","attack","defend","guard","scout","hunter","farmer","miner",
            "keep","keeps","week","weeks","seek","seeks","bee","key","keys",
            "bake","bakes","sake","lake","make","makes","take","takes","cakes",
            "ebook","books","book","spoke","smoke","broke","brake","break","breed",
            "phantom","guardian","drowned","husk","stray","pillager","vindicator","ravager",
            "shulker","enderman","blaze","ghast","magma","slime","warden","allay","axolotl","frog",
            "balloon","bandit","banner","barrel","basket","battle","beauty",
            "code","java","forge","fabric","neoforge","minecraft","spigot","paper","bukkit","velocity"
    };

    private static final Map<String, List<String>> ANAGRAM_INDEX = new HashMap<>();

    static {
        for (String w : DICT) {
            ANAGRAM_INDEX.computeIfAbsent(sortLetters(w), k -> new ArrayList<>()).add(w);
        }
    }

    public AutoChatGame() {
        super("AutoChatGame",
                "Auto-answer server chat games only (not player chat)",
                Category.MISC);
    }

    public void handleChat(String raw) {
        if (!isEnabled() || raw == null || raw.isBlank()) return;

        String plain = stripFormatting(raw).trim();
        if (plain.isEmpty() || plain.length() > 200) return;

        if (!lastAnswer.isEmpty() && plain.equalsIgnoreCase(lastAnswer)) return;
        if (System.currentTimeMillis() - lastAnswerAt < 2500) return;

        if (looksLikePlayerChat(plain)) return;

        if (strict.get() && !GAME_HINT.matcher(plain).find()) {
            if (!(unscramble.get() && WORD_LINE.matcher(plain).find() && plain.toLowerCase(Locale.ROOT).contains("word"))) {
                return;
            }
        }

        String promptKey = plain.toLowerCase(Locale.ROOT);
        if (promptKey.equals(lastPrompt) && System.currentTimeMillis() - lastAnswerAt < 15000) return;

        String answer = null;
        if (unscramble.get()) answer = solveUnscramble(plain);
        if (answer == null && math.get()) answer = solveMath(plain);
        if (answer == null && sayThis.get()) answer = solveSay(plain);
        if (answer == null && fillBlank.get()) answer = solveFill(plain);

        if (answer == null || answer.isBlank()) return;
        if (answer.length() > 48) return;
        if (answer.contains("http") || answer.contains("://")) return;
        if (looksLikePlayerChat(answer)) return;

        pendingAnswer = answer.trim();
        pendingTicks = delay.getInt();
        lastPrompt = promptKey;
        if (notify.get()) NotificationManager.push("ChatGame", pendingAnswer);
    }

    @Override
    public void onTick() {
        if (pendingTicks < 0 || pendingAnswer == null) return;
        if (pendingTicks > 0) {
            pendingTicks--;
            return;
        }
        String send = pendingAnswer;
        pendingAnswer = null;
        pendingTicks = -1;
        Minecraft minecraft = mc();
        if (minecraft == null || minecraft.player == null || minecraft.player.connection == null) return;
        try {
            minecraft.player.connection.sendChat(send);
            lastAnswer = send;
            lastAnswerAt = System.currentTimeMillis();
        } catch (Throwable ignored) {}
    }

    private static boolean looksLikePlayerChat(String s) {
        if (PLAYER_CHAT.matcher(s).find()) return true;
        if (s.matches("(?i)^\\s*\\[[^\\]]{1,20}\\]\\s*\\w{2,16}\\s*[»:].+")) return true;
        if (s.matches("(?i)^\\s*\\w{2,16}\\s*[—–-]\\s+.+")) return true;
        return false;
    }

    private String solveUnscramble(String plain) {
        Matcher m = WORD_LINE.matcher(plain);
        if (!m.find()) return null;
        String low = plain.toLowerCase(Locale.ROOT);
        if (strict.get()
                && !low.contains("unscramble")
                && !low.contains("scramble")
                && !low.contains("word")
                && !low.contains("anagram")) {
            return null;
        }
        return unscrambleToken(m.group(1));
    }

    private static String unscrambleToken(String token) {
        String normalized = leetToLetters(token).toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        if (normalized.length() < 3 || normalized.length() > 16) return null;
        List<String> hits = ANAGRAM_INDEX.get(sortLetters(normalized));
        if (hits == null || hits.isEmpty()) return null;
        for (String h : hits) {
            if (h.length() == normalized.length()) return capitalize(h);
        }
        return capitalize(hits.get(0));
    }

    private static String leetToLetters(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '0' -> sb.append('o');
                case '1' -> sb.append('i');
                case '3' -> sb.append('e');
                case '4' -> sb.append('a');
                case '5' -> sb.append('s');
                case '7' -> sb.append('t');
                case '@' -> sb.append('a');
                case '$' -> sb.append('s');
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String sortLetters(String w) {
        char[] c = w.toCharArray();
        Arrays.sort(c);
        return new String(c);
    }

    private static String capitalize(String w) {
        if (w == null || w.isEmpty()) return w;
        return Character.toUpperCase(w.charAt(0)) + w.substring(1);
    }

    private String solveMath(String plain) {
        String low = plain.toLowerCase(Locale.ROOT);
        if (!(low.contains("what is") || low.contains("solve") || low.contains("calculate") || low.contains("compute"))) {
            return null;
        }
        Matcher m = MATH_LINE.matcher(plain);
        if (!m.find()) return null;
        Long val = evalSimple(m.group(1));
        return val == null ? null : String.valueOf(val);
    }

    private static Long evalSimple(String expr) {
        try {
            String e = expr.toLowerCase(Locale.ROOT).replace('x', '*').replaceAll("\\s+", "");
            List<String> tokens = new ArrayList<>();
            StringBuilder num = new StringBuilder();
            for (int i = 0; i < e.length(); i++) {
                char c = e.charAt(i);
                if (c == '-' && num.length() == 0) {
                    num.append(c);
                } else if (Character.isDigit(c)) {
                    num.append(c);
                } else if (c == '+' || c == '-' || c == '*' || c == '/') {
                    if (num.length() == 0) return null;
                    tokens.add(num.toString());
                    tokens.add(String.valueOf(c));
                    num.setLength(0);
                } else {
                    return null;
                }
            }
            if (num.length() == 0) return null;
            tokens.add(num.toString());
            long acc = Long.parseLong(tokens.get(0));
            for (int i = 1; i + 1 < tokens.size(); i += 2) {
                String op = tokens.get(i);
                long n = Long.parseLong(tokens.get(i + 1));
                switch (op) {
                    case "+" -> acc += n;
                    case "-" -> acc -= n;
                    case "*" -> acc *= n;
                    case "/" -> {
                        if (n == 0) return null;
                        acc /= n;
                    }
                    default -> {
                        return null;
                    }
                }
            }
            return acc;
        } catch (Exception ex) {
            return null;
        }
    }

    private String solveSay(String plain) {
        Matcher m = SAY_LINE.matcher(plain);
        if (!m.find()) return null;
        String t = m.group(1).trim();
        t = t.replaceAll("(?i)\\s+(to\\s+win|for\\s+a\\s+prize|in\\s+chat).*$", "").trim();
        if (t.length() < 1 || t.length() > 40) return null;
        String low = t.toLowerCase(Locale.ROOT);
        if (low.startsWith("say") || low.startsWith("type") || low.contains("http")) return null;
        return t;
    }

    private String solveFill(String plain) {
        String low = plain.toLowerCase(Locale.ROOT);
        if (strict.get() && !(low.contains("fill") || low.contains("blank") || low.contains("complete") || low.contains("_"))) {
            return null;
        }
        Matcher m = FILL_LINE.matcher(plain);
        if (!m.find()) return null;
        String best = m.group(1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < best.length(); i++) {
            char c = best.charAt(i);
            if (Character.isLetter(c)) sb.append(c);
        }
        if (sb.length() < 3) return null;
        String word = sb.toString();
        String lower = word.toLowerCase(Locale.ROOT);
        for (String d : DICT) {
            if (d.equalsIgnoreCase(lower)) return capitalize(d);
        }
        return Character.isUpperCase(word.charAt(0)) ? word : capitalize(lower);
    }

    private static String stripFormatting(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\u00A7' && i + 1 < s.length()) {
                i++;
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
