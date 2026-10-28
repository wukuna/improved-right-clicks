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
package com.improvedmenus.rs2f.ast.leaf;

import com.improvedmenus.rs2f.FilterContext;
import com.improvedmenus.rs2f.ast.LeafCondition;
import com.improvedmenus.rs2f.model.ItemFacts;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

import static com.improvedmenus.rs2f.util.TextUtil.isInfixWildcard;

@EqualsAndHashCode(callSuper = false)
@ToString
public class ItemNameCondition extends LeafCondition {
    private final List<String> names;

    public ItemNameCondition(List<String> names) {
        this.names = names;
    }

    public ItemNameCondition(String name) {
        this.names = List.of(name);
    }

    @Override
    public boolean test(FilterContext plugin, ItemFacts item) {
        return names.stream().anyMatch(it -> test(item.getName(), it));
    }

    private boolean test(String name, String target) {
        if (target.equals("*")) {
            return true;
        } else if (target.startsWith("*") && target.endsWith("*")) {
            return name.toLowerCase().contains(target.toLowerCase().substring(1, target.length() - 1));
        } else if (target.startsWith("*")) {
            return name.toLowerCase().endsWith(target.toLowerCase().substring(1));
        } else if (target.endsWith("*")) {
            return name.toLowerCase().startsWith(target.toLowerCase().substring(0, target.length() - 1));
        } else if (isInfixWildcard(target)) {
            var lowercase = name.toLowerCase();
            var index = target.indexOf('*');
            var before = target.substring(0, index).toLowerCase();
            var after = target.substring(index + 1).toLowerCase();
            return lowercase.startsWith(before) && lowercase.endsWith(after);
        }
        return name.equalsIgnoreCase(target);
    }
}
