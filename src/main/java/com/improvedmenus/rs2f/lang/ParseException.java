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

import lombok.Getter;

import static com.improvedmenus.rs2f.lang.Location.UNKNOWN_SOURCE_NAME;

@Getter
public class ParseException extends CompileException {
    private final Token token;

    public ParseException(String message, Token token) {
        super(message + " " + formatToken(token));
        this.token = token;
    }

    public ParseException(String message) {
        super(message);
        this.token = null;
    }

    private static String formatToken(Token token) {
        // Format a message with source names only if available
        if (token.getLocation().getMacroSourceLocation() != null) {
            var tokenLocation = token.getLocation();
            var macroLocation = tokenLocation.getMacroSourceLocation();
            var expansionErrorAndLocation = String.format("'%s' after expanding macro %s on L%d, C%d%s.",
                    token.getValue(),
                    macroLocation.getMacroName(),
                    tokenLocation.getLineNumber(),
                    tokenLocation.getCharNumber(),
                    sourceNameFragment(tokenLocation.getSourceName()));

            var macroDefineAndMacroErrorLocation = "";
            if (!macroLocation.getSourceName().equals(UNKNOWN_SOURCE_NAME)) {
                macroDefineAndMacroErrorLocation = String.format(" %s is defined in '%s', error was on L%d, C%d.",
                        macroLocation.getMacroName(),
                        macroLocation.getSourceName(),
                        macroLocation.getLineNumber(),
                        macroLocation.getCharNumber()
                );
            }

            return expansionErrorAndLocation + macroDefineAndMacroErrorLocation;
        } else {
            return String.format("'%s' on L%s, C%s%s",
                    token.getValue(),
                    token.getLocation().getLineNumber(),
                    token.getLocation().getCharNumber(),
                    sourceNameFragment(token.getLocation().getSourceName())
            );
        }
    }

    private static String sourceNameFragment(String sourceName) {
        return sourceName.equals(UNKNOWN_SOURCE_NAME) ? "" : String.format(" in '%s'", sourceName);
    }
}
