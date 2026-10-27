/*
 * Adapted portions from Loot Filters by riktenx.
 *
 * BSD 2-Clause License
 * 
 * Copyright (c) 2025, riktenx
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
// Adapted from riktenx/loot-filters (BSD-2-Clause); see loot-filters-LICENSE.txt.
package com.improvedmenus.rs2f.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TextUtil {
    private TextUtil() {}

    public static String quote(String text) {
        return '"' + text + '"';
    }

    public static boolean isNumeric(char c) {
        return c >= '0' && c <= '9';
    }

    public static boolean isAlpha(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    public static boolean isLegalIdent(char c) {
        return c == '_' || isAlpha(c) || isNumeric(c);
    }

    public static String abbreviate(long value) {
        if (value >= 1e9) { // > 1b
            return String.format("%.2fB", (float)value / 1e9);
        } else if (value >= 1e8) { // > 100m
            return String.format("%.0fM", (float)value / 1e6);
        } else if (value >= 1e7) { // > 10m
            return String.format("%.1fM", (float)value / 1e6);
        } else if (value >= 1e6) { // > 1m
            return String.format("%.2fM", (float)value / 1e6);
        } else if (value >= 1e5) { // > 100k
            return String.format("%.0fK", (float)value / 1e3);
        } else if (value >= 1e4) { // > 10k
            return String.format("%.1fK", (float)value / 1e3);
        } else if (value >= 1e3) { // > 1k
            return String.format("%.2fK", (float)value / 1e3);
        }
        return Long.toString(value);
    }

    public static String abbreviateValue(long value) {
        return value < 1000 ? value + "gp" : abbreviate(value);
    }

    public static String withParentheses(String value) {
        return "(" + value + ")";
    }

    public static String normalizeCrlf(String str) {
        return str
                .replaceAll("\r\n", "\n")
                .replaceAll("\r", "\n");
    }

    public static String setCsv(String csv, String value) {
        if (csv.isBlank()) {
            return value;
        }
        return Stream.concat(Arrays.stream(csv.split(",")), Stream.of(value))
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.joining(","));
    }

    public static String unsetCsv(String csv, String value) {
        if (csv.isBlank()) {
            return "";
        }
        return Arrays.stream(csv.split(","))
                .filter(it -> !it.equalsIgnoreCase(value))
                .collect(Collectors.joining(","));
    }

    public static String toggleCsv(String csv, String item) {
        if (Arrays.stream(csv.split(",")).anyMatch(it -> it.trim().equalsIgnoreCase(item))) {
            return unsetCsv(csv, item);
        } else {
            return setCsv(csv, item);
        }
    }

    public static boolean isInfixWildcard(String str) {
        var index = -1;
        for (var i = 0; i < str.length(); ++i) {
            if (str.charAt(i) == '*') {
                if (index != -1) {
                    return false;
                }
                index = i;
            }
        }
        return index != -1 && index < str.length() - 1;
    }
}
