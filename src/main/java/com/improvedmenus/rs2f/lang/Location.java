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
package com.improvedmenus.rs2f.lang;

import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@RequiredArgsConstructor
public class Location {
    public static Location UNKNOWN = new Location("unknown", 0, 0);
    public static String UNKNOWN_SOURCE_NAME = "unknown";

    String sourceName;
    int lineNumber;
    int charNumber;
    @NonFinal
    Location macroSourceLocation = null;
    @NonFinal
    String macroName = null;

    public Location withMacroName(String name) {
        var loc = new Location(sourceName, lineNumber, charNumber);
        loc.macroName = name;
        return loc;
    }

    public Location withMacroSourceLocation(Location macroSourceLocation) {
        var loc = new Location(sourceName, lineNumber, charNumber);
        loc.macroSourceLocation = macroSourceLocation;
        return loc;
    }

    @Override
    public String toString() {
        var base = String.format("Location(sourceName=%s, lineNumber=%d, charNumber=%d", sourceName, lineNumber, charNumber);
        if (macroSourceLocation == null) {
            return base + ")";
        } else {
            return base + ", macroSourceLocation=" + macroSourceLocation + ")";
        }
    }
}
