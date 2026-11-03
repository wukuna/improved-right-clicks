/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2024, Lola Egherman
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
 *
 * BSD 2-Clause License
 *
 * Copyright (c) 2025, ldavid432
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
package com.improvedmenus;

import java.util.*;
import javax.inject.Inject;
import net.runelite.api.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;

final class TargetProtections
{
    @Inject private Client client;
    @Inject private ImprovedMenusConfig config;
    @Inject private ItemManager items;

    // Default exceptions from No Use Player (Copyright 2024 Lola Egherman).
    static final String DEFAULT_PLAYER_ITEMS = "neutralising potion, weapon store key, broken shield, half certificate, yellow egg, poisoned egg, spiked/pois. egg, omega egg, old school bond, healing vial(1), healing vial(2), healing vial(3), healing vial(4), scroll";
    // Published useful-target catalog from Don't Telegrab NPCs (Copyright 2025 ldavid432).
    private static final Set<Integer> TELEGRAB_TARGETS = Set.of(
        NpcID.SAILING_TERN_DEAD, NpcID.SAILING_OSPREY_DEAD, NpcID.SAILING_FRIGATEBIRD_DEAD, NpcID.SAILING_ALBATROSS_DEAD,
        NpcID.SAILING_DOLPHIN_DEAD, NpcID.SAILING_SEA_MOGRE_DEAD, NpcID.SAILING_SEA_MOGRE_DEAD_F, NpcID.SAILING_NARWHAL_DEAD, NpcID.SAILING_ORCA_DEAD,
        NpcID.SAILING_PYGMY_KRAKEN_DEAD, NpcID.SAILING_SPINED_KRAKEN_DEAD, NpcID.SAILING_ARMOURED_KRAKEN_DEAD, NpcID.SAILING_VAMPYRE_KRAKEN_DEAD, NpcID.SAILING_VEILED_KRAKEN_DEAD,
        NpcID.SAILING_BULL_SHARK_DEAD, NpcID.SAILING_HAMMERHEAD_SHARK_DEAD, NpcID.SAILING_TIGER_SHARK_DEAD, NpcID.SAILING_GREAT_WHITE_SHARK_DEAD,
        NpcID.SAILING_EAGLE_RAY_DEAD, NpcID.SAILING_BUTTERFLY_RAY_DEAD, NpcID.SAILING_STINGRAY_DEAD, NpcID.SAILING_MANTA_RAY_DEAD,
        NpcID.SAILING_CHANCE_ENCOUNTER_LOST_GOODS_WOOD, NpcID.SAILING_CHANCE_ENCOUNTER_LOST_GOODS_OAK,
        NpcID.SAILING_CHANCE_ENCOUNTER_LOST_GOODS_TEAK, NpcID.SAILING_CHANCE_ENCOUNTER_LOST_GOODS_MAHOGANY,
        NpcID.SAILING_CHANCE_ENCOUNTER_LOST_GOODS_CAMPHOR, NpcID.SAILING_CHANCE_ENCOUNTER_LOST_GOODS_IRONWOOD,
        NpcID.MAGICTRAINING_GUARD_MAZE_INCOMPLETE, NpcID.KR_KEEP_GUARD_HAIR, 16298);

    String reason(MenuEntry entry)
    {
        if (!client.isWidgetSelected()) { return null; }
        MenuAction type = entry.getType();
        if (type != MenuAction.WIDGET_TARGET_ON_PLAYER && type != MenuAction.WIDGET_TARGET_ON_NPC) { return null; }
        Widget selected = client.getSelectedWidget();
        if (selected == null) { return null; }
        if (type == MenuAction.WIDGET_TARGET_ON_PLAYER && config.noUsePlayer()
            && selected.getItemId() > 0 && MenuRules.plain(entry.getOption()).equalsIgnoreCase("Use"))
        {
            ItemComposition item = items.getItemComposition(selected.getItemId());
            if (item == null || item.getMembersName() == null) { return null; }

            return allowedName(config.playerUseItems(),item.getMembersName()) ? null : "item Use on Player protection";
        }
        if (type == MenuAction.WIDGET_TARGET_ON_NPC && config.noTelegrabNpc()
            && selected.getId() == InterfaceID.MagicSpellbook.TELEGRAB)
        {
            NPC npc = entry.getNpc();

            if (npc == null || TELEGRAB_TARGETS.contains(npc.getId()) || npc.getName() == null) { return null; }
            return allowedName(config.telegrabNpcNames(),npc.getName()) ? null : "Telegrab NPC protection";
        }
        return null;
    }

    private static boolean allowedName(String list,String name)
    {

        if (list == null || list.length() > MenuRules.MAX_LENGTH) { return true; }
        String[] names = list.split(",",-1);
        if (names.length > MenuRules.MAX_RULES) { return true; }
        String plain = MenuRules.plain(name);
        for (String allowed : names) { if (!allowed.trim().isEmpty() && MenuRules.plain(allowed).equalsIgnoreCase(plain)) { return true; } }
        return false;
    }
}
