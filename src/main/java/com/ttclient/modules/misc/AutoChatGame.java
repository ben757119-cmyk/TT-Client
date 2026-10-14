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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Auto-solves common server chat games:
 * unscramble, math, "say this", fill-in-the-blank.
 */
public class AutoChatGame extends Module {
    public final NumberSetting delay = addSetting(new NumberSetting("Delay", "Ticks before answering", 8, 0, 40, 1));
    public final BoolSetting unscramble = addSetting(new BoolSetting("Unscramble", "Solve word scrambles", true));
    public final BoolSetting math = addSetting(new BoolSetting("Math", "Solve simple math", true));
    public final BoolSetting sayThis = addSetting(new BoolSetting("SayThis", "Answer say-this prompts", true));
    public final BoolSetting fillBlank = addSetting(new BoolSetting("FillBlank", "Solve fill-in-the-blank", true));
    public final BoolSetting notify = addSetting(new BoolSetting("Notify", "Toast when answering", true));

    private String pendingAnswer;
    private int pendingTicks = -1;
    private long lastAnswerAt;
    private String lastAnswer = "";

    private static final Pattern WORD_LINE = Pattern.compile(
            "(?i)(?:word|scramble|unscramble|scrambled(?:\\s+word)?)\\s*[:=\-]\\s*([a-z0-9]+)");
    private static final Pattern MATH_LINE = Pattern.compile(
            "(?i)(?:what\\s+is|solve|calculate|math|compute)[^0-9\\-]*([\\-]?\\d+\\s*[+\\-*/x×÷]\\s*\\d+(?:\\s*[+\\-*/x×÷]\\s*\\d+)*)");
    private static final Pattern MATH_BARE = Pattern.compile(
            "(?i)(?:^|\\s)([\\-]?\\d+\\s*[+\\-*/x×÷]\\s*\\d+(?:\\s*[+\\-*/x×÷]\\s*\\d+)*)\\s*\\??\\s*$");
    private static final Pattern SAY_LINE = Pattern.compile(
            "(?i)(?:say\\s+this|type\\s+this|repeat|type|say)\\s*[:=\-]?\\s*[\"'\u201c\u201d]?([^\"'\n]{1,40})[\"'\u201c\u201d]?");
    private static final Pattern FILL_LINE = Pattern.compile(
            "([_\\W]*[A-Za-z](?:[_\\W]+[A-Za-z]){2,}[_\\W]*)");

    /** Common chat-game / everyday words (lowercase). */
    private static final String[] DICT = {
            "apple","banana","orange","grape","melon","berry","peach","mango","lemon","cherry",
            "house","horse","heart","earth","water","fire","stone","block","sword","shield",
            "diamond","emerald","gold","iron","copper","nether","ender","creeper","zombie","skeleton",
            "spider","villager","dragon","wither","totem","elytra","beacon","portal","obsidian","bedrock",
            "balance","blank","blade","bread","brick","bridge","bright","bring","broken","build",
            "beeks","beek","bees","beach","bean","bear","beast","beat","become","before",
            "chicken","potato","carrot","wheat","sugar","cookie","cake","pizza","burger","salad",
            "school","player","server","client","window","button","screen","mouse","keyboard","chat",
            "green","blue","red","yellow","purple","white","black","brown","orange","pink",
            "table","chair","phone","clock","watch","light","night","morning","evening","friend",
            "family","money","power","speed","jump","walk","run","swim","fly","mine",
            "craft","game","play","win","lose","prize","first","second","third","quick",
            "answer","question","problem","number","letter","word","sentence","title","name","place",
            "world","ocean","river","mountain","forest","desert","island","city","town","village",
            "king","queen","prince","knight","castle","armor","helmet","boots","legs","chest",
            "happy","angry","brave","smart","lucky","magic","spell","potion","enchant","level",
            "health","hunger","damage","attack","defend","guard","scout","hunter","farmer","miner",
            "keep","keeps","week","weeks","seek","seeks","bee","bees","key","keys",
            "bake","bakes","sake","lake","make","makes","take","takes","cake","cakes",
            "ebook","books","book","spoke","smoke","broke","brake","break","bread","breed",
            "skeleton","wither","phantom","guardian","drowned","husk","stray","pillager","vindicator","ravager",
            "shulker","enderman","blaze","ghast","magma","slime","warden","allay","axolotl","frog",
            "balance","balloon","banana","bandit","banner","barrel","basket","battle","beacon","beauty",
            "code","java","forge","fabric","neoforge","minecraft","spigot","paper","bukkit","velocity"
    };

    private static final Map<String, List<String>> ANAGRAM_INDEX = new HashMap<>();

    static {
        for (String w : DICT) {
            String key = sortLetters(w);
            ANAGRAM_INDEX.computeIfAbsent(key, k -> new ArrayList<>()).add(w);
        }
    }

    public AutoChatGame() {
        super("AutoChatGame",
                "Auto-answer unscramble, math, say-this, and fill-in-blank chat games",
                Category.MISC);
    }

    public void handleChat(String raw) {
        if (!isEnabled() || raw == null || raw.isBlank()) return;
        String plain = stripFormatting(raw).trim();
        if (plain.isEmpty()) return;

        // Avoid reacting to our own answers looping
        if (!lastAnswer.isEmpty() && plain.equalsIgnoreCase(lastAnswer)) return;
        if (System.currentTimeMillis() - lastAnswerAt < 1500) return;

        String answer = null;

        if (unscramble.get() && looksLikeUnscramble(plain)) {
            answer = solveUnscramble(plain);
        }
        if (answer == null && math.get() && looksLikeMath(plain)) {
            answer = solveMath(plain);
        }
        if (answer == null && sayThis.get() && looksLikeSay(plain)) {
            answer = solveSay(plain);
        }
        if (answer == null && fillBlank.get() && looksLikeFill(plain)) {
            answer = solveFill(plain);
        }

        // Standalone scramble line: "Word: eb5ke"
        if (answer == null && unscramble.get()) {
            Matcher m = WORD_LINE.matcher(plain);
            if (m.find()) {
                answer = unscrambleToken(m.group(1));
            }
        }

        if (answer == null || answer.isBlank()) return;

        pendingAnswer = answer.trim();
        pendingTicks = delay.getInt();
        if (notify.get()) {
            NotificationManager.push("ChatGame", pendingAnswer);
        }
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

    private static boolean looksLikeUnscramble(String s) {
        String l = s.toLowerCase(Locale.ROOT);
        return l.contains("unscramble") || l.contains("scramble") || l.contains("scrambled")
                || (l.contains("word") && (l.contains(":") || l.contains("=")));
    }

    private static boolean looksLikeMath(String s) {
        String l = s.toLowerCase(Locale.ROOT);
        return l.contains("what is") || l.contains("solve") || l.contains("calculate")
                || l.contains("math") || MATH_BARE.matcher(s).find();
    }

    private static boolean looksLikeSay(String s) {
        String l = s.toLowerCase(Locale.ROOT);
        return l.contains("say this") || l.contains("type this") || l.startsWith("say ") || l.startsWith("type ");
    }

    private static boolean looksLikeFill(String s) {
        // e.g. __B_a_l_a_n_c_e or B_a_l_a_n_c_e
        int underscores = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == '_') underscores++;
        return underscores >= 2 && s.matches(".*[A-Za-z].*");
    }

    private String solveUnscramble(String plain) {
        Matcher m = WORD_LINE.matcher(plain);
        if (m.find()) return unscrambleToken(m.group(1));
        // fallback: last token that looks scrambled
        String[] parts = plain.split("\\s+");
        for (int i = parts.length - 1; i >= 0; i--) {
            String t = parts[i].replaceAll("[^A-Za-z0-9]", "");
            if (t.length() >= 3 && t.length() <= 16) {
                String sol = unscrambleToken(t);
                if (sol != null) return sol;
            }
        }
        return null;
    }

    private static String unscrambleToken(String token) {
        String normalized = leetToLetters(token).toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        if (normalized.length() < 2) return null;
        String key = sortLetters(normalized);
        List<String> hits = ANAGRAM_INDEX.get(key);
        if (hits != null && !hits.isEmpty()) {
            // Prefer exact-length dictionary word
            for (String h : hits) {
                if (h.length() == normalized.length()) return capitalize(h);
            }
            return capitalize(hits.get(0));
        }
        // Partial: if token has digits mixed, try without non-letters already done
        return null;
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
        Matcher m = MATH_LINE.matcher(plain);
        String expr = null;
        if (m.find()) expr = m.group(1);
        if (expr == null) {
            m = MATH_BARE.matcher(plain);
            if (m.find()) expr = m.group(1);
        }
        if (expr == null) {
            // scan for a+b pattern anywhere
            m = Pattern.compile("([\\-]?\\d+\\s*[+\\-*/x×÷]\\s*\\d+)").matcher(plain);
            if (m.find()) expr = m.group(1);
        }
        if (expr == null) return null;
        Long val = evalSimple(expr);
        return val == null ? null : String.valueOf(val);
    }

    private static Long evalSimple(String expr) {
        try {
            String e = expr.toLowerCase(Locale.ROOT)
                    .replace("×", "*")
                    .replace("x", "*")
                    .replace("÷", "/")
                    .replaceAll("\\s+", "");
            // left-to-right for + - * / (good enough for chat games)
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
                    default -> { return null; }
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
        // strip trailing instructions
        t = t.replaceAll("(?i)\\s+to\\s+chat.*$", "").trim();
        if (t.length() > 48) return null;
        return t;
    }

    private String solveFill(String plain) {
        // Prefer the densest underscore letter pattern
        Matcher m = FILL_LINE.matcher(plain);
        String best = null;
        int bestScore = 0;
        while (m.find()) {
            String cand = m.group(1);
            int score = 0;
            for (int i = 0; i < cand.length(); i++) if (cand.charAt(i) == '_') score++;
            if (score > bestScore) {
                bestScore = score;
                best = cand;
            }
        }
        if (best == null) {
            // whole message might be the pattern
            if (plain.indexOf('_') >= 0) best = plain;
            else return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < best.length(); i++) {
            char c = best.charAt(i);
            if (Character.isLetter(c)) sb.append(c);
        }
        if (sb.length() < 3) return null;
        String word = sb.toString();
        // Match dictionary case-insensitively for nicer casing
        String lower = word.toLowerCase(Locale.ROOT);
        for (String d : DICT) {
            if (d.equalsIgnoreCase(lower)) return capitalize(d);
        }
        return Character.isUpperCase(word.charAt(0)) ? word : capitalize(lower);
    }

    private static String stripFormatting(String s) {
        // strip section signs and hex-like minecraft formatting leftovers
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '§' && i + 1 < s.length()) {
                i++; // skip code
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
