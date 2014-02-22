package de.blanksteg.storyteller.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.blanksteg.storyteller.common.Authentication;
import de.blanksteg.storyteller.common.StorytellerBot;
import de.blanksteg.storyteller.common.StorytellerChannel;
import de.blanksteg.storyteller.common.StorytellerPack;
import de.blanksteg.storyteller.common.StorytellerServer;

public class DatabaseControllerTest {
	private static final String TEST_UNIT = "storyteller-test";

	private Database database;
	private DatabaseController controller;

	@Before
	public void setupDatabase() {
		Database.initialize(TEST_UNIT);
		this.database = Database.getInstance();
		this.controller = this.database.createController();
	}

	@After
	public void closeDatabase() {
		try {
			this.controller.close();
		} catch (IllegalStateException e) {
			// This is fine since some tests close it for us.
		}
		this.database.close();
		try {
			FileUtils.deleteDirectory(new File("storyteller-test.db"));
			FileUtils.deleteQuietly(new File("derby.log"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testServerAddition() {
		try {
			this.controller.addServer("testHost", 8080, "a", "b", "c", Authentication.NICKSERV, "pass", "foo");
			StorytellerServer testHost = this.controller.getServer("testHost");
			assertNotNull(testHost);
			assertEquals("testHost".toLowerCase(), testHost.getHost().toLowerCase());
			assertEquals(8080, testHost.getPort());
			assertEquals("a", testHost.getNickName());
			assertEquals("b", testHost.getUserName());
			assertEquals("c", testHost.getRealName());
			assertEquals(Authentication.NICKSERV, testHost.getAuthentification());
			assertEquals("pass", testHost.getUserPassword());
			assertEquals("foo", testHost.getPassword());

			this.controller.addServer("otherTestHost", 8027, "d", null, null, Authentication.NONE, null, null);
			StorytellerServer otherTestHost = this.controller.getServer("otherTestHost");
			assertNotNull(otherTestHost);
			assertNotEquals("The two servers are not supposed to be equal.", testHost, otherTestHost);
			assertEquals("otherTestHost".toLowerCase(), otherTestHost.getHost().toLowerCase());
			assertEquals(8027, otherTestHost.getPort());
			assertEquals("d", otherTestHost.getUserName());
			assertEquals("d", otherTestHost.getRealName());
			assertNull(otherTestHost.getUserPassword());
			assertNull(otherTestHost.getPassword());
		} finally {
			this.controller.deleteServer("testHost");
			this.controller.deleteServer("otherTestHost");
		}
	}

	@Test
	public void testChannelAddition() {
		try {
			this.controller.addServer("testHost", 8080, "a", "b", "c", Authentication.NICKSERV, "pass", "foo");
			this.controller.addChannel("testHost", "#testChannel", "foo");
			StorytellerChannel testChannel = this.controller.getChannel("testHost", "#testChannel");
			assertNotNull(testChannel);
			assertEquals("#testChannel", testChannel.getName());
			assertEquals("foo", testChannel.getPassword());
			assertTrue(this.controller.getServer("testHost").getRelatedChannels().contains(testChannel));
			assertEquals(testChannel.getParentServer(), this.controller.getServer("testHost"));

			this.controller.addChannel("testHost", "#otherTestChannel", null);
			StorytellerChannel otherTestChannel = this.controller.getChannel("testHost", "#otherTestChannel");
			assertNotNull(otherTestChannel);
			assertNotEquals(testChannel, otherTestChannel);
			assertEquals("#otherTestChannel", otherTestChannel.getName());
			assertNull(otherTestChannel.getPassword());
			assertTrue(this.controller.getServer("testHost").getRelatedChannels().contains(otherTestChannel));
			assertEquals(otherTestChannel.getParentServer(), this.controller.getServer("testHost"));
		} finally {
			this.controller.deleteServer("testHost");
		}
	}

	@Test
	public void testBotAddition() {
		try {
			this.controller.addServer("testHost", 8080, "a", "b", "c", Authentication.NICKSERV, "pass", "foo");
			this.controller.addChannel("testHost", "#testChannel", "foo");
			this.controller.addBot("testHost", "#testChannel", "freamon", false);
			StorytellerBot freamon = this.controller.getBot("testHost", "#testChannel", "freamon");
			assertNotNull(freamon);
			assertEquals("freamon", freamon.getName());
			assertEquals(false, freamon.isListEnabled());
			assertTrue(this.controller.getChannel("testHost", "#testChannel").getRelatedBots().contains(freamon));
			assertEquals(freamon.getChannel(), this.controller.getChannel("testHost", "#testChannel"));

			this.controller.addBot("testHost", "#testChannel", "nomaerf", true);
			StorytellerBot nomaerf = this.controller.getBot("testHost", "#testChannel", "nomaerf");
			assertNotNull(nomaerf);
			assertNotEquals(freamon, nomaerf);
			assertEquals("nomaerf", nomaerf.getName());
			assertEquals(true, nomaerf.isListEnabled());
			assertTrue(this.controller.getChannel("testHost", "#testChannel").getRelatedBots().contains(nomaerf));
			assertEquals(nomaerf.getChannel(), this.controller.getChannel("testHost", "#testChannel"));
		} finally {
			this.controller.deleteServer("testHost");
		}
	}

	@Test
	public void testPackAddition() {
		try {
			this.controller.addServer("testHost", 8080, "a", "b", "c", Authentication.NICKSERV, "pass", "foo");
			this.controller.addChannel("testHost", "#testChannel", "foo");
			this.controller.addBot("testHost", "#testChannel", "freamon", false);
			this.controller.updateOrAddPack("testHost", "#testChannel", "freamon", 123, "test.pack", "1G", false);
			StorytellerPack testPack = this.controller.getPack("testHost", "#testChannel", "freamon", 123);
			assertNotNull(testPack);
			assertEquals(123, testPack.getPackNumber());
			assertEquals("test.pack", testPack.getFileName());
			assertEquals("1G", testPack.getFileSize());
			assertTrue(this.controller.getBot("testHost", "#testChannel", "freamon").getRelatedPacks().contains(testPack));
			assertEquals(this.controller.getBot("testHost", "#testChannel", "freamon"), testPack.getOwningBot());

			this.controller.updateOrAddPack("testHost", "#testChannel", "freamon", 321, "othertest.pack", "2G", false);
			StorytellerPack otherTestPack = this.controller.getPack("testHost", "#testChannel", "freamon", 321);
			assertNotNull(otherTestPack);
			assertNotEquals(testPack, otherTestPack);
			assertEquals(321, otherTestPack.getPackNumber());
			assertEquals("othertest.pack", otherTestPack.getFileName());
			assertEquals("2G", otherTestPack.getFileSize());
			assertTrue(this.controller.getBot("testHost", "#testChannel", "freamon").getRelatedPacks().contains(otherTestPack));
			assertEquals(this.controller.getBot("testHost", "#testChannel", "freamon"), otherTestPack.getOwningBot());
		} finally {
			this.controller.deleteServer("testHost");
		}
	}

	@Test
	public void testPackUpdate() {
		try {
			this.controller.addServer("testHost", 8080, "a", "b", "c", Authentication.NICKSERV, "pass", "foo");
			this.controller.addChannel("testHost", "#testChannel", "foo");
			this.controller.addBot("testHost", "#testChannel", "freamon", false);
			this.controller.updateOrAddPack("testHost", "#testChannel", "freamon", 123, "test.pack", "1G", false);
			this.controller.updateOrAddPack("testHost", "#testChannel", "freamon", 321, "othertest.pack", "2G", false);
			this.controller.updateOrAddPack("testHost", "#testChannel", "freamon", 123, "newtest.pack", "5G", false);
			StorytellerPack testPack = this.controller.getPack("testHost", "#testChannel", "freamon", 123);
			assertNotNull(testPack);
			assertEquals("newtest.pack", testPack.getFileName());
			assertEquals("5G", testPack.getFileSize());
			assertTrue(this.controller.getBot("testHost", "#testChannel", "freamon").getRelatedPacks().contains(testPack));
			assertEquals(this.controller.getBot("testHost", "#testChannel", "freamon"), testPack.getOwningBot());
			StorytellerPack otherTestPack = this.controller.getPack("testHost", "#testChannel", "freamon", 321);
			assertEquals("othertest.pack", otherTestPack.getFileName());
		} finally {
			this.controller.deleteServer("testHost");
		}
	}

	@Test
	public void testPackDelete() {
		try {
			this.controller.addServer("testHost", 8080, "a", "b", "c", Authentication.NICKSERV, "pass", "foo");
			this.controller.addChannel("testHost", "#testChannel", "foo");
			this.controller.addBot("testHost", "#testChannel", "freamon", false);
			this.controller.updateOrAddPack("testHost", "#testChannel", "freamon", 123, "test.pack", "1G", false);
			this.controller.updateOrAddPack("testHost", "#testChannel", "freamon", 321, "othertest.pack", "2G", false);
			this.controller.updateOrAddPack("testHost", "#testChannel", "freamon", 123, "newtest.pack", "5G", false);
			this.controller.deletePack("testHost", "#testChannel", "freamon", 123);
			StorytellerPack testPack = this.controller.getPack("testHost", "#testChannel", "freamon", 123);
			assertNull(testPack);
			StorytellerPack otherTestPack = this.controller.getPack("testHost", "#testChannel", "freamon", 321);
			assertNotNull(otherTestPack);
			this.controller.deletePack("testHost", "#testChannel", "freamon", 321);
			StorytellerBot bot = this.controller.getBot("testHost", "#testChannel", "freamon");
			Collection<? extends StorytellerPack> packs = bot.getRelatedPacks();
			boolean contains = packs.contains(otherTestPack);
			assertFalse(contains);
			otherTestPack = this.controller.getPack("testHost", "#testChannel", "freamon", 321);
			assertNull(otherTestPack);
		} finally {
			this.controller.deleteServer("testHost");
		}
	}

	@Test
	public void testBotIntroducingPackAddition() {
		try {
			this.controller.addServer("testHost", 8080, "a", "b", "c", Authentication.NICKSERV, "pass", "foo");
			this.controller.addChannel("testHost", "#testChannel", "foo");
			this.controller.updateOrAddPack("testHost", "#testChannel", "freamon", 123, "test.pack", "1G", true);
			StorytellerBot freamon = this.controller.getBot("testHost", "#testChannel", "freamon");
			assertNotNull(freamon);
			assertEquals("freamon", freamon.getName());
		} finally {
			this.controller.deleteServer("testHost");
		}
	}

	@Test
	public void testBotDelete() {
		try {
			this.controller.addServer("testHost", 8080, "a", "b", "c", Authentication.NICKSERV, "pass", "foo");
			this.controller.addChannel("testHost", "#testChannel", "foo");
			this.controller.addBot("testHost", "#testChannel", "freamon", false);
			this.controller.addBot("testHost", "#testChannel", "nomaerf", true);
			this.controller.deleteBot("testHost", "#testChannel", "freamon");
			StorytellerBot freamon = this.controller.getBot("testHost", "#testChannel", "freamon");
			assertNull(freamon);
			StorytellerBot nomaerf = this.controller.getBot("testHost", "#testChannel", "nomaerf");
			assertNotNull(nomaerf);
		} finally {
			this.controller.deleteServer("testHost");
		}
	}

	@Test
	public void testChannelDelete() {
		try {
			this.controller.addServer("testHost", 8080, "a", "b", "c", Authentication.NICKSERV, "pass", "foo");
			this.controller.addChannel("testHost", "#testChannel", "foo");
			this.controller.addChannel("testHost", "#otherTestChannel", null);
			this.controller.deleteChannel("testHost", "#testChannel");
			StorytellerChannel testChannel = this.controller.getChannel("testHost", "#testChannel");
			assertNull(testChannel);
			StorytellerChannel otherTestChannel = this.controller.getChannel("testHost", "#otherTestChannel");
			assertNotNull(otherTestChannel);
		} finally {
			this.controller.deleteServer("testHost");
		}
	}

	@Test
	public void testServerDelete() {
		try {
			this.controller.addServer("testHost", 8080, "a", "b", "c", Authentication.NICKSERV, "pass", "foo");
			this.controller.addServer("otherTestHost", 8027, "d", null, null, Authentication.NONE, null, null);
			this.controller.deleteServer("testHost");
			StorytellerServer testHost = this.controller.getServer("testHost");
			assertNull(testHost);
			StorytellerServer otherTestHost = this.controller.getServer("otherTestHost");
			assertNotNull(otherTestHost);
		} finally {
			this.controller.deleteServer("otherTestHost");
		}
	}

	@Test
	public void testSameNameChannel() {
		try {
			this.controller.addServer("testHost", 8080, "a", "b", "c", Authentication.NICKSERV, "pass", "foo");
			this.controller.addServer("otherTestHost", 8027, "d", null, null, Authentication.NONE, null, null);
			this.controller.addChannel("testHost", "#testChannel", "foo");
			this.controller.addChannel("otherTestHost", "#testChannel", "foo");
			StorytellerChannel testChannel = this.controller.getChannel("testHost", "#testChannel");
			StorytellerChannel otherHostTestChannel = this.controller.getChannel("otherTestHost", "#testChannel");
			assertNotNull(testChannel);
			assertNotNull(otherHostTestChannel);
			assertNotEquals(testChannel, otherHostTestChannel);
		} finally {
			this.controller.deleteServer("testHost");
			this.controller.deleteServer("otherTestHost");
		}
	}

	@Test
	public void testSameNameBot() {
		try {
			this.controller.addServer("testHost", 8080, "a", "b", "c", Authentication.NICKSERV, "pass", "foo");
			this.controller.addChannel("testHost", "#testChannel", "foo");
			this.controller.addChannel("testHost", "#otherTestChannel", null);
			this.controller.addBot("testHost", "#testChannel", "freamon", false);
			this.controller.addBot("testHost", "#otherTestChannel", "freamon", false);
			StorytellerBot freamon = this.controller.getBot("testHost", "#testChannel", "freamon");
			StorytellerBot otherChannelFreamon = this.controller.getBot("testHost", "#otherTestChannel", "freamon");
			assertNotNull(freamon);
			assertNotNull(otherChannelFreamon);
			assertNotEquals(freamon, otherChannelFreamon);
		} finally {
			this.controller.deleteServer("testHost");
		}
	}

	@Test
	public void testSameNumberPack() {
		try {
			this.controller.addServer("testHost", 8080, "a", "b", "c", Authentication.NICKSERV, "pass", "foo");
			this.controller.addChannel("testHost", "#testChannel", "foo");
			this.controller.addChannel("testHost", "#otherTestChannel", null);
			this.controller.addBot("testHost", "#testChannel", "freamon", false);
			this.controller.addBot("testHost", "#testChannel", "nomaerf", false);
			this.controller.updateOrAddPack("testHost", "#testChannel", "freamon", 123, "test.pack", "1G", false);
			this.controller.updateOrAddPack("testHost", "#testChannel", "nomaerf", 123, "othertest.pack", "2G", false);
			StorytellerPack testPack = this.controller.getPack("testHost", "#testChannel", "freamon", 123);
			StorytellerPack otherBotTestPack = this.controller.getPack("testHost", "#testChannel", "nomaerf", 123);
			assertNotNull(testPack);
			assertNotNull(otherBotTestPack);
			assertNotEquals(testPack, otherBotTestPack);
		} finally {
			this.controller.deleteServer("testHost");
		}
	}

	@Test
	public void testSetServerIdentity() {
		try {
			this.controller.addServer("testHost", 8080, "a", "b", "c", Authentication.NICKSERV, "pass", "foo");
			this.controller.setServerIdentity("testHost", "d", null, null, Authentication.NONE, null);
			StorytellerServer testHost = this.controller.getServer("testHost");
			assertNotNull(testHost);
			assertEquals("d", testHost.getNickName());
			assertEquals("d", testHost.getUserName());
			assertEquals("d", testHost.getRealName());
			assertEquals(Authentication.NONE, testHost.getAuthentification());
			assertNull(testHost.getUserPassword());
		} finally {
			this.controller.deleteServer("testHost");
		}
	}

	@Test
	public void testSetServerPort() {
		try {
			this.controller.addServer("testHost", 8080, "a", "b", "c", Authentication.NICKSERV, "pass", "foo");
			this.controller.setServerPort("testHost", 10084);
			StorytellerServer testHost = this.controller.getServer("testHost");
			assertNotNull(testHost);
			assertEquals(10084, testHost.getPort());
		} finally {
			this.controller.deleteServer("testHost");
		}
	}

	@Test
	public void testSetServerPassword() {
		try {
			this.controller.addServer("testHost", 8080, "a", "b", "c", Authentication.NICKSERV, "pass", "foo");
			this.controller.setServerPassword("testHost", "bar");
			StorytellerServer testHost = this.controller.getServer("testHost");
			assertNotNull(testHost);
			assertEquals("bar", testHost.getPassword());
		} finally {
			this.controller.deleteServer("testHost");
		}
	}

	@Test
	public void testSetChannelName() {
		try {
			this.controller.addServer("testHost", 8080, "a", "b", "c", Authentication.NICKSERV, "pass", "foo");
			this.controller.addChannel("testHost", "#testChannel", "foo");
			this.controller.setChannelPassword("testHost", "#testChannel", "bar");
			StorytellerChannel testChannel = this.controller.getChannel("testHost", "#testChannel");
			assertNotNull(testChannel);
			assertEquals("bar", testChannel.getPassword());
		} finally {
			this.controller.deleteServer("testHost");
		}
	}

	@Test
	public void testSetBotListingEnabled() {
		try {
			this.controller.addServer("testHost", 8080, "a", "b", "c", Authentication.NICKSERV, "pass", "foo");
			this.controller.addChannel("testHost", "#testChannel", "foo");
			this.controller.addBot("testHost", "#testChannel", "freamon", false);
			this.controller.setBotListEnabled("testHost", "#testChannel", "freamon", true);
			StorytellerBot freamon = this.controller.getBot("testHost", "#testChannel", "freamon");
			assertNotNull(freamon);
			assertTrue(freamon.isListEnabled());
		} finally {
			this.controller.deleteServer("testHost");
		}
	}

	@Test
	public void testMoveBot() {
		try {
			this.controller.addServer("testHost", 8080, "a", "b", "c", Authentication.NICKSERV, "pass", "foo");
			this.controller.addChannel("testHost", "#testChannel", "foo");
			this.controller.addChannel("testHost", "#otherTestChannel", "foo");
			this.controller.addBot("testHost", "#testChannel", "freamon", false);
			this.controller.setBotChannel("testHost", "#testChannel", "#otherTestChannel", "freamon");
			StorytellerBot freamon = this.controller.getBot("testHost", "#otherTestChannel", "freamon");
			StorytellerChannel testChannel = this.controller.getChannel("testHost", "#testChannel");
			StorytellerChannel otherTestChannel = this.controller.getChannel("testHost", "#otherTestChannel");
			assertNotNull(freamon);
			assertNotNull(testChannel);
			assertNotNull(otherTestChannel);
			assertEquals(freamon.getChannel(), otherTestChannel);
			assertNotEquals(freamon.getChannel(), testChannel);
			assertTrue(otherTestChannel.getRelatedBots().contains(freamon));
			assertFalse(testChannel.getRelatedBots().contains(freamon));
		} finally {
			this.controller.deleteServer("testHost");
		}
	}

	@Test
	public void testPackSearch() {
		try {
			this.controller.addServer("testHost", 8080, "a", "b", "c", Authentication.NICKSERV, "pass", "foo");
			this.controller.addChannel("testHost", "#testChannel", "foo");
			this.controller.addBot("testHost", "#testChannel", "freamon", false);
			this.controller.updateOrAddPack("testHost", "#testChannel", "freamon", 123, "test.pack", "1G", false);
			this.controller.updateOrAddPack("testHost", "#testChannel", "freamon", 321, "not.pack", "2G", false);
			List<? extends StorytellerPack> results = this.controller.findPack(".pack");
			assertTrue(results.contains(this.controller.getPack("testHost", "#testChannel", "freamon", 123)));
			assertTrue(results.contains(this.controller.getPack("testHost", "#testChannel", "freamon", 321)));

			results = this.controller.findPackOnServer("testHost", ".pack");
			assertTrue(results.contains(this.controller.getPack("testHost", "#testChannel", "freamon", 123)));
			assertTrue(results.contains(this.controller.getPack("testHost", "#testChannel", "freamon", 321)));

			results = this.controller.findPackInChannel("testHost", "#testChannel", ".pack");
			assertTrue(results.contains(this.controller.getPack("testHost", "#testChannel", "freamon", 123)));
			assertTrue(results.contains(this.controller.getPack("testHost", "#testChannel", "freamon", 321)));

			results = this.controller.findPackByBot("testHost", "#testChannel", "freamon", ".pack");
			assertTrue(results.contains(this.controller.getPack("testHost", "#testChannel", "freamon", 123)));
			assertTrue(results.contains(this.controller.getPack("testHost", "#testChannel", "freamon", 321)));

			results = this.controller.findPack("Unobtainium");
			assertTrue(results.isEmpty());

			results = this.controller.findPack("test.pack");
			assertTrue(results.contains(this.controller.getPack("testHost", "#testChannel", "freamon", 123)));
			assertFalse(results.contains(this.controller.getPack("testHost", "#testChannel", "freamon", 321)));
		} finally {
			this.controller.deleteServer("testHost");
		}
	}
}
