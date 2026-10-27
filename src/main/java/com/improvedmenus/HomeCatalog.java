/*
 * Adapted portions from Home Improvement by Mike-U5.
 *
 * Copyright 2024 Mike-U5
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.improvedmenus;
import java.util.Map;
import java.util.Set;
// Furniture catalog adapted from Mike-U5/rl-plugin-home-improvement; see bundled license.
final class HomeCatalog {
    static final Set<String> OPTIONS = Set.of("Remove", "Upgrade", "Revert", "Remove-room", "Remove-decorations", "Build-in", "Build");
    static final Set<Integer> REGIONS = Set.of(7534,7535,7790,7791,8046,8047,8302,8303);
    static final Map<Integer, Integer> REPAIRS = Map.ofEntries(Map.entry(40002,40017),Map.entry(40003,40025),Map.entry(40004,40043),Map.entry(40005,40031),Map.entry(40006,40031),Map.entry(40288,40037),Map.entry(40007,40031),Map.entry(40008,40031),Map.entry(40009,40017),Map.entry(40291,40050),Map.entry(40292,40115),Map.entry(40012,40017),Map.entry(40013,40043),Map.entry(40014,40062),Map.entry(40015,40062),Map.entry(40294,40056),Map.entry(40095,40151),Map.entry(40096,40151),Map.entry(40097,40102),Map.entry(40098,40102),Map.entry(40084,40102),Map.entry(40085,40108),Map.entry(40086,40115),Map.entry(40087,40121),Map.entry(40088,40121),Map.entry(40090,40102),Map.entry(40091,40128),Map.entry(40092,40134),Map.entry(40093,40143),Map.entry(40094,39967),Map.entry(40165,40187),Map.entry(40166,40187),Map.entry(40167,40200),Map.entry(40169,40207),Map.entry(40156,39948),Map.entry(40157,40115),Map.entry(40160,40181),Map.entry(40161,40108),Map.entry(40162,40108),Map.entry(40171,40187),Map.entry(40172,40187),Map.entry(40173,39899),Map.entry(40174,39905),Map.entry(40175,40193),Map.entry(40176,40108),Map.entry(39981,39892),Map.entry(39983,39899),Map.entry(39984,39905),Map.entry(39985,39911),Map.entry(39986,39911),Map.entry(39987,39917),Map.entry(39988,39923),Map.entry(39989,39929),Map.entry(39990,39911),Map.entry(39991,39935),Map.entry(39992,39942),Map.entry(39993,39923),Map.entry(39994,39948),Map.entry(39996,39955),Map.entry(39997,39961),Map.entry(39998,39974),Map.entry(39999,39948),Map.entry(40000,39967),Map.entry(40001,39935));
    private HomeCatalog() {}
}
