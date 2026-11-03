package com.improvedmenus;

import java.util.*;
import net.runelite.client.util.Text;

final class MenuRules
{
    static final int MAX_LENGTH = 16384;
    static final int MAX_RULES = 200;
    enum Context { ALL, INVENTORY, BANK, GROUND }
    static final class Rule
    {
        final Glob target;
        final Glob option;
        final String color;
        final boolean wildcard;
        final Context context;
        Rule(String target, String option, String color)
        { this(target,option,color,Context.ALL); }
        Rule(String target, String option, String color, Context context)
        {
            this.target = glob(target); this.option = glob(option); this.color = color;
            this.wildcard = target.contains("*") || option.contains("*");
            this.context = context;
        }
        boolean applies(boolean inventory,boolean bank,boolean ground)
        {
            switch (context) { case INVENTORY: return inventory; case BANK: return inventory && bank; case GROUND: return ground; default: return true; }
        }
        boolean matches(String target, String option)
        {
            return this.target.matches(plain(target)) && this.option.matches(plain(option));
        }
    }
    static String plain(String text) { return text == null ? "" : Text.removeTags(text).trim(); }
    static Glob glob(String input)
    {
        if (input.length() > 256 || input.chars().filter(c -> c == '*').count() > 8) { throw new IllegalArgumentException("Use names of at most 256 characters and 8 wildcards"); }
        return new Glob(input.trim());
    }
    static final class Glob
    {
        private final String pattern;
        Glob(String pattern) { this.pattern=pattern; }
        boolean matches(String value)
        {
            int p=0, v=0, star=-1, restart=0;
            while (v < value.length())
            {
                if (p < pattern.length() && pattern.charAt(p) != '*'
                    && Character.toLowerCase(pattern.charAt(p)) == Character.toLowerCase(value.charAt(v))) { p++; v++; }
                else if (p < pattern.length() && pattern.charAt(p) == '*') { star=p++; restart=v; }
                else if (star >= 0) { p=star+1; v=++restart; }
                else { return false; }
            }
            while (p < pattern.length() && pattern.charAt(p) == '*') { p++; }
            return p == pattern.length();
        }
    }
    static List<Rule> parseItems(String input)
    {
        bounded(input);
        List<Rule> result = new ArrayList<>();
        int line = 0;
        for (String raw : input.split("\\R"))
        {
            line++;
            String s = raw.trim();
            if (s.isEmpty() || s.startsWith("#")) { continue; }
            Context context = context(s);
            s = stripContext(s);
            int comma = s.indexOf(',');
            if (comma < 1 || comma == s.length() - 1) { throw new IllegalArgumentException("Item rule line " + line + ": use action,item name"); }
            result.add(new Rule(s.substring(comma + 1), s.substring(0, comma), null, context));
            if (result.size() > MAX_RULES) { throw new IllegalArgumentException("At most 200 rules are allowed"); }
        }
        result.sort(Comparator.comparing(rule -> rule.wildcard));
        return Collections.unmodifiableList(result);
    }
    static List<Rule> parseColors(String input)
    {
        bounded(input);
        List<Rule> result = new ArrayList<>();
        for (String raw : input.split(";"))
        {
            if (raw.trim().isEmpty()) { continue; }
            String[] p = raw.trim().split(":", -1);
            if (p.length != 3 || p[0].trim().isEmpty() || p[1].trim().isEmpty() || !p[2].trim().matches("#[0-9a-fA-F]{6}"))
            { throw new IllegalArgumentException("Colors use target:action:#RRGGBB"); }
            result.add(new Rule(p[0], p[1], p[2].trim().substring(1).toLowerCase(Locale.ROOT)));
            if (result.size() > MAX_RULES) { throw new IllegalArgumentException("At most 200 colors are allowed"); }
        }

        result.sort(Comparator.comparing(rule -> rule.wildcard));
        return Collections.unmodifiableList(result);
    }
    static List<Rule> parseExceptions(String input)
    {
        bounded(input);
        List<Rule> result = new ArrayList<>();
        for (String raw : input.split("\\R"))
        {
            if (!raw.trim().isEmpty()) { result.add(new Rule(stripContext(raw.trim()),"*",null,context(raw.trim()))); }
            if (result.size() > MAX_RULES) { throw new IllegalArgumentException("At most 200 exceptions are allowed"); }
        }
        return Collections.unmodifiableList(result);
    }
    private static Context context(String text)
    {
        if (!text.startsWith("[")) { return Context.ALL; }
        int end=text.indexOf(']');
        if (end < 0) { throw new IllegalArgumentException("Use [inventory], [bank], [ground] or [all]"); }
        try { return Context.valueOf(text.substring(1,end).trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Use [inventory], [bank], [ground] or [all]"); }
    }
    private static String stripContext(String text)
    {
        if (!text.startsWith("[")) { return text; }
        context(text);
        String result=text.substring(text.indexOf(']')+1).trim();
        if (result.isEmpty()) { throw new IllegalArgumentException("Add a rule after the context"); }
        return result;
    }
    static void bounded(String input)
    {
        if (input == null || input.length() > MAX_LENGTH) { throw new IllegalArgumentException("Settings exceed the supported size"); }
    }
    private MenuRules() {}
}
