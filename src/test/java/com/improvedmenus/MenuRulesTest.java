package com.improvedmenus;

import org.junit.Test;
import static org.junit.Assert.*;

public class MenuRulesTest
{
	@Test
	public void wildcardIsCaseInsensitiveAndTagsIgnored()
	{
		assertTrue(
				MenuRules.parseItems("# comment\nWIELD,Rune*").get(0).matches("<col=ffff00>Rune sword</col>", "Wield"));
	}

	@Test
	public void regexCharactersAreLiteral()
	{
		assertTrue(MenuRules.glob("Potion(4)").matches("Potion(4)"));
		assertFalse(MenuRules.glob("Potion(4)").matches("Potion4"));
	}

	@Test
	public void itemOnItemTargetsAreSupported()
	{
		assertTrue(MenuRules.parseItems("Use,* -> Torn clue scroll*").get(0)
				.matches("Knife -> Torn clue scroll (part 1)", "Use"));
	}

	@Test
	public void allWildcardMatchesEmptyAndNonempty()
	{
		assertTrue(MenuRules.glob("*").matches(""));
		assertTrue(MenuRules.glob("*").matches("Rune sword"));
	}

	@Test(timeout = 1000)
	public void adversarialGlobHasBoundedMatchingCost()
	{
		assertFalse(
				MenuRules.glob("*a*a*a*a*a*a*a*aZ").matches(String.join("", java.util.Collections.nCopies(200, "a"))));
	}

	@Test(expected = IllegalArgumentException.class)
	public void missingTargetRejected()
	{
		MenuRules.parseItems("Eat,");
	}

	@Test(expected = IllegalArgumentException.class)
	public void missingSeparatorRejected()
	{
		MenuRules.parseItems("Eat");
	}

	@Test(expected = IllegalArgumentException.class)
	public void invalidColorRejected()
	{
		MenuRules.parseColors("Bank:Use:javascript");
	}

	@Test(expected = IllegalArgumentException.class)
	public void excessWildcardsRejected()
	{
		MenuRules.glob("*********");
	}

	@Test
	public void exactColorsPrecedeWildcard()
	{
		assertEquals("ff0000", MenuRules.parseColors("*:Take:#00ff00;Coins:Take:#ff0000").get(0).color);
	}

	@Test
	public void exceptionsAreLiteralGlobs()
	{
		assertTrue(MenuRules.parseExceptions("Rune*\nCoins").get(1).matches("Coins", ""));
	}

	@Test
	public void optionalRuleContextsParse()
	{
		assertEquals(MenuRules.Context.BANK, MenuRules.parseItems("[bank] Eat,Shark").get(0).context);
	}

	@Test
	public void optionalExceptionContextsParse()
	{
		assertEquals(MenuRules.Context.GROUND, MenuRules.parseExceptions("[ground] Coins").get(0).context);
	}

	@Test(expected = IllegalArgumentException.class)
	public void unsupportedContextsRejected()
	{
		MenuRules.parseItems("[combat] Attack,*");
	}

	@Test(expected = IllegalArgumentException.class)
	public void ruleCountBounded()
	{
		MenuRules.parseItems(String.join("\n", java.util.Collections.nCopies(201, "Eat,*")));
	}
}
