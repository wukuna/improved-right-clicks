package com.improvedmenus;

import org.junit.Test;
import static org.junit.Assert.*;

public class SavedColourExportTest
{
	@Test
	public void exportsOnlyEquivalentActionRules()
	{
		SavedColourExport export = new SavedColourExport("*:Take:#ff0000;*:Talk-to:#00ff00;Goblin:Attack:#112233;*:Chop*:#334455");
		assertEquals("Take=#ff0000\nTalk-to=#00ff00", export.rules);
		assertEquals("Goblin:Attack:#112233\n*:Chop*:#334455", export.retained);
	}

	@Test
	public void doesNotBroadenOverlappingOrTargetSpecificRules()
	{
		SavedColourExport export = new SavedColourExport("Goblin:Attack:#ff0000;*:Attack:#00ff00;*:T*:#ff0000;*:Take:#00ff00");
		assertEquals("", export.rules);
		assertEquals(4, export.retained.split("\n").length);
	}

	@Test
	public void deduplicatesIdenticalRulesAndRejectsStylerSeparators()
	{
		SavedColourExport export = new SavedColourExport("*:Take:#ff0000;*:take:#ff0000;*:A=B:#00ff00");
		assertEquals("Take=#ff0000", export.rules);
		assertEquals("*:A=B:#00ff00", export.retained);
	}

	@Test
	public void reportsRulesBeyondStylerLimits()
	{
		StringBuilder rules = new StringBuilder();
		for (int i = 0; i < 51; i++)
			rules.append("*:Action").append(i).append(":#ff0000;");
		rules.append("*:").append("A".repeat(65)).append(":#ffffff");
		SavedColourExport export = new SavedColourExport(rules.toString());
		assertEquals(50, export.rules.split("\n").length);
		assertEquals(2, export.retained.split("\n").length);
	}
}
