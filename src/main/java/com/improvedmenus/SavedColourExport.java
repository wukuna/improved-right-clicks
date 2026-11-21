package com.improvedmenus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class SavedColourExport
{
	final String rules;
	final String retained;

	SavedColourExport(String saved)
	{
		List<MenuRules.Rule> parsed = MenuRules.parseColors(saved);
		Map<String, String> compatible = new LinkedHashMap<>();
		List<String> unsupported = new ArrayList<>();
		for (String raw : saved.split(";"))
		{
			if (raw.isBlank())
				continue;
			String[] parts = raw.trim().split(":");
			String target = parts[0].trim(), action = parts[1].trim();
			String key = action.toLowerCase(Locale.ROOT);
			if (!target.equals("*") || action.contains("*") || action.length() > 64
					|| action.contains("=") || action.contains("\n") || action.contains("\r")
					|| (!compatible.containsKey(key) && compatible.size() == 50)
					|| parsed.stream().anyMatch(rule -> rule.option.matches(action)
							&& !rule.color.equalsIgnoreCase(parts[2].trim().substring(1))))
			{
				unsupported.add(raw.trim());
				continue;
			}
			compatible.putIfAbsent(key, action + "=" + parts[2].trim());
		}
		rules = String.join("\n", compatible.values());
		retained = String.join("\n", unsupported);
	}
}
