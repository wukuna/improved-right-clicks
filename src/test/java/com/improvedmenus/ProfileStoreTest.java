package com.improvedmenus;

import com.google.gson.Gson;
import com.google.inject.*;
import java.util.*;
import net.runelite.client.config.ConfigManager;
import org.junit.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ProfileStoreTest
{
    private ProfileStore store;
    private ConfigManager manager;
    private final Map<String,String> values=new HashMap<>();
    @Before public void setup()
    {
        manager=mock(ConfigManager.class);
        when(manager.getConfiguration(anyString(),anyString())).thenAnswer(i -> values.get(i.getArgument(0)+"/"+i.getArgument(1)));
        doAnswer(i -> { values.put(i.getArgument(0)+"/"+i.getArgument(1),i.getArgument(2).toString()); return null; }).when(manager).setConfiguration(anyString(),anyString(),any());
        store=Guice.createInjector(new AbstractModule() { protected void configure() { bind(ConfigManager.class).toInstance(manager); bind(Gson.class).toInstance(new Gson()); } }).getInstance(ProfileStore.class);
    }
    @Test public void playerBadgeSettingsRoundTripAndUndoIndependently()
    {
        Map<String,String> settings=new LinkedHashMap<>();
        for (String key : new String[]{"playerFriendBadge","playerIgnoreBadge","playerFriendsChatBadge","playerPartyBadge","playerIronmanBadge","playerHiscores","playerCopyName"})
        { assertEquals("false",store.snapshot().get(key)); settings.put(key,"true"); }
        store.apply(store.previewImport(store.exportSettings(settings)));
        for (String key : settings.keySet()) assertEquals("true",store.snapshot().get(key));
        store.save("Social"); assertEquals("true",store.load("Social").get("playerFriendBadge"));
        assertTrue(store.undo());
        for (String key : settings.keySet()) assertEquals("false",store.snapshot().get(key));
    }
    @Test public void roundTripProfiles()
    { Map<String,String> initial=store.snapshot(); assertEquals(initial,store.previewImport(store.exportSettings(initial))); }
    @Test public void hoverAndSidebarSettingsRoundTripAndUndo()
    {
        Map<String,String> settings=new LinkedHashMap<>(); settings.put("hideSidePanel","true"); settings.put("yellowHover","false");
        store.apply(store.previewImport(store.exportSettings(settings)));
        assertEquals("true",store.snapshot().get("hideSidePanel")); assertEquals("false",store.snapshot().get("yellowHover"));
        assertTrue(store.undo()); assertEquals("false",store.snapshot().get("hideSidePanel")); assertEquals("true",store.snapshot().get("yellowHover"));
    }
    @Test public void importPreviewDoesNotWrite()
    { store.previewImport("{\"version\":1,\"settings\":{\"bankEat\":\"true\"}}"); assertTrue(values.isEmpty()); }
    @Test public void applyAndUndo()
    { store.apply(Collections.singletonMap("bankEat","true")); assertEquals("true",values.get("improvedmenus/bankEat")); assertTrue(store.undo()); assertEquals("false",values.get("improvedmenus/bankEat")); }
    @Test public void entireImportValidatedBeforeAnyWrite()
    {
        Map<String,String> settings=new LinkedHashMap<>(); settings.put("bankEat","true"); settings.put("itemRules","Invalid");
        try { store.apply(settings); fail(); } catch (IllegalArgumentException expected) { assertTrue(values.isEmpty()); }
    }
    @Test(expected=IllegalArgumentException.class) public void cannotImportOtherPluginKeys()
    { store.previewImport("{\"version\":1,\"settings\":{\"script\":\"danger\"}}"); }
    @Test(expected=IllegalArgumentException.class) public void invalidBooleanRejected()
    { store.previewImport("{\"version\":1,\"settings\":{\"bankEat\":\"yes\"}}"); }
    @Test(expected=IllegalArgumentException.class) public void invalidModeRejected()
    { store.previewImport("{\"version\":1,\"settings\":{\"report\":\"AUTO\"}}"); }
    @Test(expected=IllegalArgumentException.class) public void futureVersionRejected()
    { store.previewImport("{\"version\":2,\"settings\":{}}"); }
    @Test(expected=IllegalArgumentException.class) public void nonStringValueRejected()
    { store.previewImport("{\"version\":1,\"settings\":{\"bankEat\":true}}"); }
    @Test(expected=IllegalArgumentException.class) public void malformedJsonRejected()
    { store.previewImport("not JSON"); }
    @Test public void namedProfilesPersistIndependentlyOfCurrentSettings()
    {
        store.apply(Collections.singletonMap("bankEat","true")); store.save("Banking"); store.apply(Collections.singletonMap("bankEat","false"));
        assertEquals("true",store.load("Banking").get("bankEat")); assertEquals(Collections.singletonList("Banking"),store.names());
    }
    @Test public void selectiveResetLeavesOtherSettings()
    {
        store.apply(Collections.singletonMap("bankEat","true")); store.apply(Collections.singletonMap("bankWield","true"));
        store.apply(Collections.singletonMap("bankEat","false")); assertEquals("true",store.snapshot().get("bankWield"));
    }
    @Test public void historyBoundedToTwentyEdits()
    { for (int i=0;i<30;i++) { store.apply(Collections.singletonMap("bankEat",i%2==0 ? "true" : "false")); } int n=0; while (store.undo()) { n++; } assertEquals(20,n); }
    @Test public void legacyImportReadsExactPublishedKeysAndLeavesSourceAlone()
    {
        values.put("noexamine/examineItemGround","true"); values.put("noexamine/examineItemsInventory","false");
        Map<String,String> imported=store.legacyPreview("No Examine"); assertEquals("true",imported.get("examineGround")); assertEquals("false",imported.get("examineInventory"));
        store.apply(imported); assertEquals("true",values.get("noexamine/examineItemGround"));
    }
    @Test public void toolConfigUsesHyphenatedSourceGroup()
    { values.put("tool-required/chopDown","true"); assertEquals("true",store.legacyPreview("Tool Required").get("chop")); }
    @Test public void noSavedLegacySettingsProducesEmptyPreview()
    { assertTrue(store.legacyPreview("Menu Colorizer").isEmpty()); }
    @Test public void clearHistoryDoesNotDeleteProfiles()
    { store.save("Normal"); store.apply(Collections.singletonMap("bankEat","true")); store.clearHistory(); assertFalse(store.undo()); assertEquals(1,store.names().size()); }
    @Test public void newControlsRoundTripAndUndoWithoutTouchingSourceSettings()
    {
        Map<String,String> changes=new LinkedHashMap<>(); changes.put("hideCancel","true"); changes.put("hideWalkHere","true"); changes.put("noUsePlayer","true"); changes.put("noTelegrabNpc","true");
        changes.put("playerUseItems","Knife,Weapon store key"); changes.put("telegrabNpcNames","Custom");
        assertEquals(changes,store.previewImport(store.exportSettings(changes))); store.apply(changes); assertEquals("true",store.snapshot().get("hideCancel"));
        assertTrue(store.undo()); assertEquals("false",store.snapshot().get("hideCancel"));
    }
    @Test public void sourceMenuCleanupSettingsAndTargetAllowListsImport()
    {
        values.put("noexamine/cancelEverywhere","true"); values.put("noexamine/walkHereEverywhere","true");
        assertEquals("true",store.legacyPreview("No Examine").get("hideCancel")); assertEquals("true",store.legacyPreview("No Examine").get("hideWalkHere"));
        values.put("no-use-player/whitelistItemsString","Knife"); values.put("donttelegrabnpcs/telegrabWhitelist","Custom");
        assertEquals("Knife",store.legacyPreview("No Use Player").get("playerUseItems")); assertEquals("Custom",store.legacyPreview("Don't Telegrab NPCs").get("telegrabNpcNames"));
    }
    @Test public void presentationSettingsImportAndUndo()
    {
        values.put("mouseoveroptions/minimumOptions","3"); values.put("mouseoveroptions/includeExamine","false");
        values.put("mouseoveroptions/showOnWidgets","false"); values.put("mouseoveroptions/textColor",Integer.toString(0xff123abc));
        Map<String,String> imported=store.legacyPreview("Mouseover Options+");
        assertEquals("#123abc",imported.get("optionCountColor")); assertEquals("3",imported.get("minimumOptions"));
        assertEquals("false",imported.get("countExamine")); assertEquals("false",imported.get("countOnWidgets"));
        store.apply(imported); assertTrue(store.undo()); assertEquals("1",store.snapshot().get("minimumOptions"));
        values.put("menucolorizer/defaultPickerColor",Integer.toString(0xffaabbcc)); assertEquals("#aabbcc",store.legacyPreview("Menu Colorizer").get("defaultPickerColor"));
        values.put("noexamine/setHeadingEverywhere","true"); assertEquals("true",store.legacyPreview("No Examine").get("hideSetHeading"));
    }
    @Test public void invalidThresholdAndColorRejectEntireImportBeforeWriting()
    {
        for (String bad : Arrays.asList("0","501","1.5","many"))
        {
            try { store.apply(Collections.singletonMap("minimumOptions",bad)); fail(); } catch (IllegalArgumentException expected) { assertTrue(values.isEmpty()); }
        }
        for (String key : Arrays.asList("defaultPickerColor","optionCountColor"))
        {
            try { store.apply(Collections.singletonMap(key,"<img=3>")); fail(); } catch (IllegalArgumentException expected) { assertTrue(values.isEmpty()); }
        }
    }
    @Test public void undoLeavesUnrelatedManualChangesAlone()
    {
        store.apply(Collections.singletonMap("bankEat","true"));
        values.put("improvedmenus/bankWield","true");
        assertTrue(store.undo());
        assertEquals("false",store.snapshot().get("bankEat"));
        assertEquals("true",store.snapshot().get("bankWield"));
    }
    @Test public void undoPreservesLaterChangesToAnEditedSetting()
    {
        store.apply(Collections.singletonMap("colors","Sword:Use:#ff0000"));
        values.put("improvedmenus/colors","Sword:Use:#0000ff");
        assertTrue(store.undo());
        assertEquals("Sword:Use:#0000ff",store.snapshot().get("colors"));
    }
    @Test public void undoOfPartialImportAndSuccessiveEditsIsScoped()
    {
        store.apply(Collections.singletonMap("bankEat","true"));
        Map<String,String> settings=new LinkedHashMap<>();
        settings.put("bankEat","false"); settings.put("bankWield","true");
        store.apply(store.previewImport(store.exportSettings(settings)));
        values.put("improvedmenus/hideSidePanel","true");
        assertTrue(store.undo()); assertEquals("true",store.snapshot().get("bankEat"));
        assertEquals("false",store.snapshot().get("bankWield"));
        assertTrue(store.undo()); assertEquals("false",store.snapshot().get("bankEat"));
        assertEquals("true",store.snapshot().get("hideSidePanel"));
        assertFalse(store.undo());
    }
    @Test public void unchangedSettingsDoNotConsumeUndoHistory()
    {
        store.apply(Collections.singletonMap("bankEat","true"));
        store.apply(Collections.singletonMap("bankEat","true"));
        store.apply(Collections.emptyMap());
        assertTrue(store.undo()); assertEquals("false",store.snapshot().get("bankEat"));
        assertFalse(store.undo());
    }
    @Test public void malformedProfileShapesAreFriendlyAndDoNotWrite()
    {
        String[] invalid={"null","[]","1","{\"version\":null,\"settings\":{}}",
            "{\"version\":[],\"settings\":{}}","{\"version\":{},\"settings\":{}}",
            "{\"version\":\"1\",\"settings\":{}}","{\"version\":1.5,\"settings\":{}}",
            "{\"version\":true,\"settings\":{}}","{\"version\":1,\"settings\":null}",
            "{\"version\":1,\"settings\":[]}","{\"version\":1,\"settings\":{\"bankEat\":null}}"};
        for (String json : invalid)
        {
            try { store.previewImport(json); fail("Accepted " + json); }
            catch (IllegalArgumentException expected) { assertNotNull(expected.getMessage()); }
            assertTrue(values.isEmpty()); assertFalse(store.undo());
        }
    }
}
