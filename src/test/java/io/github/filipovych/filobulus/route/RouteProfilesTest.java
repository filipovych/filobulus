package io.github.filipovych.filobulus.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RouteProfilesTest {
	@Test
	void thereIsAlwaysOneProfileToStartWith() {
		RouteProfiles profiles = new RouteProfiles();
		assertEquals(List.of("Main"), profiles.names());
		assertEquals("Main", profiles.active());
		assertTrue(profiles.choices().autoLoad("JASP_1"));
	}

	@Test
	void aNewProfileIsACopyThatChangesOnItsOwn() {
		RouteProfiles profiles = new RouteProfiles();
		profiles.choices().pick("JASP_1", "jasper_failsayfe");
		assertEquals(Optional.of("Test"), profiles.add("  Test "));
		assertEquals("Test", profiles.active());
		assertEquals("jasper_failsayfe", profiles.choices().preferredRouteId("JASP_1"));
		profiles.choices().pick("JASP_1", "jasper_friend");
		profiles.choices().setAutoLoad("RUBY_1", true);
		assertTrue(profiles.select("Main"));
		assertEquals("jasper_failsayfe", profiles.choices().preferredRouteId("JASP_1"));
		assertFalse(profiles.choices().autoLoad("RUBY_1"));
	}

	@Test
	void aNameThatIsBlankOrTakenIsRefused() {
		RouteProfiles profiles = new RouteProfiles();
		assertEquals(Optional.empty(), profiles.add("   "));
		assertEquals(Optional.empty(), profiles.add("main"));
		assertEquals(List.of("Main"), profiles.names());
		assertFalse(profiles.select("Nope"));
	}

	@Test
	void aLongNameIsCut() {
		RouteProfiles profiles = new RouteProfiles();
		String name = profiles.add("a".repeat(60)).orElseThrow();
		assertEquals(RouteProfiles.MAX_NAME, name.length());
	}

	@Test
	void theLastProfileStaysAndRemovingTheOneInUseFallsBackToTheFirst() {
		RouteProfiles profiles = new RouteProfiles();
		assertFalse(profiles.remove("Main"));
		profiles.add("Test");
		assertTrue(profiles.remove("Test"));
		assertEquals("Main", profiles.active());
		assertEquals(List.of("Main"), profiles.names());
	}

	@Test
	void aDeletedRouteIsUnpickedEverywhere() {
		RouteProfiles profiles = new RouteProfiles();
		profiles.choices().pick("JASP_1", "jasper_friend");
		profiles.add("Test");
		profiles.unpick("JASP_1", "jasper_friend");
		assertEquals("", profiles.choices().preferredRouteId("JASP_1"));
		profiles.select("Main");
		assertEquals("", profiles.choices().preferredRouteId("JASP_1"));
	}

	@Test
	void savedProfilesComeBackWithTheOneThatWasInUse() {
		Map<String, RouteChoices> saved = new LinkedHashMap<>();
		saved.put("Main", new RouteChoices());
		RouteChoices test = new RouteChoices();
		test.pick("RUBY_1", "ruby_1_fast");
		saved.put("Test", test);
		RouteProfiles profiles = new RouteProfiles();
		profiles.restore(saved, "Test");
		assertEquals(List.of("Main", "Test"), profiles.names());
		assertEquals("ruby_1_fast", profiles.choices().preferredRouteId("RUBY_1"));
		profiles.restore(saved, "Gone");
		assertEquals("Main", profiles.active());
		profiles.restore(Map.of(), "Test");
		assertEquals(List.of("Main", "Test"), profiles.names());
	}
}
