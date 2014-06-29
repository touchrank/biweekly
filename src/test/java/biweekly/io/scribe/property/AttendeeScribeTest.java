package biweekly.io.scribe.property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import biweekly.ICalDataType;
import biweekly.ICalVersion;
import biweekly.io.scribe.property.Sensei.Check;
import biweekly.parameter.ParticipationLevel;
import biweekly.parameter.ParticipationStatus;
import biweekly.parameter.Role;
import biweekly.property.Attendee;

/*
 Copyright (c) 2013, Michael Angstadt
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met: 

 1. Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer. 
 2. Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation
 and/or other materials provided with the distribution. 

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * @author Michael Angstadt
 */
public class AttendeeScribeTest {
	private final AttendeeScribe scribe = new AttendeeScribe();
	private final Sensei<Attendee> sensei = new Sensei<Attendee>(scribe);

	private final String name = "John Doe";
	private final String email = "jdoe@example.com";
	private final String uri = "http://example.com/jdoe";

	private final Attendee withEmail = new Attendee(null, email);
	private final Attendee withNameEmail = new Attendee(name, email);
	private final Attendee withNameEmailUri = new Attendee(name, email);
	{
		withNameEmailUri.setUri(uri);
	}

	private final Attendee withRoleUri = new Attendee(uri);
	{
		withRoleUri.setRole(Role.ATTENDEE);
	}

	@Test
	public void prepareParameters_cn() {
		Attendee property = new Attendee(name, email);
		sensei.assertPrepareParams(property).versions(ICalVersion.V1_0).run();
		sensei.assertPrepareParams(property).versions(ICalVersion.V2_0_DEPRECATED, ICalVersion.V2_0).expected("CN", name).run();
	}

	@Test
	public void prepareParameters_rsvp() {
		Attendee property = new Attendee(uri);

		property.setRsvp(true);
		sensei.assertPrepareParams(property).versions(ICalVersion.V1_0).expected("RSVP", "YES").run();
		sensei.assertPrepareParams(property).versions(ICalVersion.V2_0_DEPRECATED, ICalVersion.V2_0).expected("RSVP", "TRUE").run();

		property.setRsvp(false);
		sensei.assertPrepareParams(property).versions(ICalVersion.V1_0).expected("RSVP", "NO").run();
		sensei.assertPrepareParams(property).versions(ICalVersion.V2_0_DEPRECATED, ICalVersion.V2_0).expected("RSVP", "FALSE").run();
	}

	@Test
	public void prepareParameters_level() {
		Attendee property = new Attendee(uri);
		property.setParticipationLevel(ParticipationLevel.OPTIONAL);
		sensei.assertPrepareParams(property).versions(ICalVersion.V1_0).expected("EXPECT", "REQUEST").run();
		sensei.assertPrepareParams(property).versions(ICalVersion.V2_0_DEPRECATED, ICalVersion.V2_0).expected("ROLE", "OPT-PARTICIPANT").run();
	}

	@Test
	public void prepareParameters_level_chair_role() {
		Attendee property = new Attendee(uri);
		property.setParticipationLevel(ParticipationLevel.OPTIONAL);
		property.setRole(Role.CHAIR);

		sensei.assertPrepareParams(property).versions(ICalVersion.V1_0).expected("EXPECT", "REQUEST").expected("ROLE", "CHAIR").run();
		sensei.assertPrepareParams(property).versions(ICalVersion.V2_0_DEPRECATED, ICalVersion.V2_0).expected("ROLE", "CHAIR").run();
	}

	@Test
	public void prepareParameters_status() {
		Attendee property = new Attendee(uri);
		property.setParticipationStatus(ParticipationStatus.ACCEPTED);
		sensei.assertPrepareParams(property).versions(ICalVersion.V1_0).expected("STATUS", "ACCEPTED").run();
		sensei.assertPrepareParams(property).versions(ICalVersion.V2_0_DEPRECATED, ICalVersion.V2_0).expected("PARTSTAT", "ACCEPTED").run();
	}

	@Test
	public void prepareParameters_status_needs_action() {
		Attendee property = new Attendee(uri);
		property.setParticipationStatus(ParticipationStatus.NEEDS_ACTION);
		sensei.assertPrepareParams(property).versions(ICalVersion.V1_0).expected("STATUS", "NEEDS ACTION").run();
		sensei.assertPrepareParams(property).versions(ICalVersion.V2_0_DEPRECATED, ICalVersion.V2_0).expected("PARTSTAT", "NEEDS-ACTION").run();
	}

	@Test
	public void dataType() {
		Attendee property = new Attendee(name, email);
		sensei.assertDataType(property).versions(ICalVersion.V1_0).run(null);
		sensei.assertDataType(property).versions(ICalVersion.V2_0_DEPRECATED, ICalVersion.V2_0).run(ICalDataType.CAL_ADDRESS);
	}

	@Test
	public void dataType_uri() {
		Attendee property = new Attendee(uri);
		sensei.assertDataType(property).versions(ICalVersion.V1_0).run(ICalDataType.URL);
		sensei.assertDataType(property).versions(ICalVersion.V2_0_DEPRECATED, ICalVersion.V2_0).run(ICalDataType.CAL_ADDRESS);
	}

	@Test
	public void writeText() {
		sensei.assertWriteText(withEmail).version(ICalVersion.V1_0).run(email);
		sensei.assertWriteText(withEmail).version(ICalVersion.V2_0_DEPRECATED).run("mailto:" + email);
		sensei.assertWriteText(withEmail).version(ICalVersion.V2_0).run("mailto:" + email);

		sensei.assertWriteText(withNameEmail).version(ICalVersion.V1_0).run(name + " <" + email + ">");
		sensei.assertWriteText(withNameEmail).version(ICalVersion.V2_0_DEPRECATED).run("mailto:" + email);
		sensei.assertWriteText(withNameEmail).version(ICalVersion.V2_0).run("mailto:" + email);

		sensei.assertWriteText(withNameEmailUri).version(ICalVersion.V1_0).run(uri);
		sensei.assertWriteText(withNameEmailUri).version(ICalVersion.V2_0_DEPRECATED).run(uri);
		sensei.assertWriteText(withNameEmailUri).version(ICalVersion.V2_0).run(uri);
	}

	@Test
	public void parseText() {
		sensei.assertParseText(uri).versions(ICalVersion.V1_0).dataType(ICalDataType.URL).run(check(null, null, uri));
		sensei.assertParseText(uri).versions(ICalVersion.V1_0).run(check(null, uri, null));

		sensei.assertParseText(name + " <" + email + ">").versions(ICalVersion.V1_0).run(check(name, email, null));
		sensei.assertParseText(name + " <" + email + ">").versions(ICalVersion.V2_0_DEPRECATED, ICalVersion.V2_0).run(check(null, null, name + " <" + email + ">"));

		sensei.assertParseText("mailto:" + email).versions(ICalVersion.V1_0).run(check(null, "mailto:" + email, null));
		sensei.assertParseText("mailto:" + email).param("CN", name).versions(ICalVersion.V2_0_DEPRECATED, ICalVersion.V2_0).run(check(name, email, null));
	}

	private Check<Attendee> check(final String name, final String email, final String uri) {
		return new Check<Attendee>() {
			public void check(Attendee property) {
				assertTrue(property.getParameters().isEmpty());
				assertEquals(name, property.getCommonName());
				assertEquals(email, property.getEmail());
				assertEquals(uri, property.getUri());
			}
		};
	}

	@Test
	public void parseText_level() {
		sensei.assertParseText(uri).param("EXPECT", "REQUIRE").versions(ICalVersion.V1_0).run(checkLevel(ParticipationLevel.REQUIRED));
		sensei.assertParseText(uri).param("EXPECT", "REQUEST").versions(ICalVersion.V1_0).run(checkLevel(ParticipationLevel.OPTIONAL));
		sensei.assertParseText(uri).param("EXPECT", "FYI").versions(ICalVersion.V1_0).run(checkLevel(ParticipationLevel.FYI));
		sensei.assertParseText(uri).param("EXPECT", "invalid").versions(ICalVersion.V1_0).run(checkLevel(ParticipationLevel.get("invalid")));
		sensei.assertParseText(uri).param("EXPECT", "REQUIRE").versions(ICalVersion.V2_0_DEPRECATED, ICalVersion.V2_0).run(new Check<Attendee>() {
			public void check(Attendee property) {
				assertEquals("REQUIRE", property.getParameter("EXPECT"));
				assertNull(property.getParticipationLevel());
			}
		});

		sensei.assertParseText(uri).param("ROLE", "REQ-PARTICIPANT").versions(ICalVersion.V2_0_DEPRECATED, ICalVersion.V2_0).run(checkLevel(ParticipationLevel.REQUIRED));
		sensei.assertParseText(uri).param("ROLE", "OPT-PARTICIPANT").versions(ICalVersion.V2_0_DEPRECATED, ICalVersion.V2_0).run(checkLevel(ParticipationLevel.OPTIONAL));
		sensei.assertParseText(uri).param("ROLE", "NON-PARTICIPANT").versions(ICalVersion.V2_0_DEPRECATED, ICalVersion.V2_0).run(checkLevel(ParticipationLevel.FYI));
		sensei.assertParseText(uri).param("ROLE", "invalid").versions(ICalVersion.V2_0_DEPRECATED, ICalVersion.V2_0).run(new Check<Attendee>() {
			public void check(Attendee property) {
				assertTrue(property.getParameters().isEmpty());
				assertNull(property.getParticipationLevel());
				assertEquals(Role.get("invalid"), property.getRole());
			}
		});
		sensei.assertParseText(uri).param("ROLE", "REQ-PARTICIPANT").versions(ICalVersion.V1_0).run(new Check<Attendee>() {
			public void check(Attendee property) {
				assertTrue(property.getParameters().isEmpty());
				assertEquals(Role.get("REQ-PARTICIPANT"), property.getRole());
				assertNull(property.getParticipationLevel());
			}
		});
	}

	private Check<Attendee> checkLevel(final ParticipationLevel level) {
		return new Check<Attendee>() {
			public void check(Attendee property) {
				assertTrue(property.getParameters().isEmpty());
				assertEquals(level, property.getParticipationLevel());
			}
		};
	}

	@Test
	public void parseText_role() {
		sensei.assertParseText(uri).param("ROLE", "OPT-PARTICIPANT").versions(ICalVersion.V2_0_DEPRECATED, ICalVersion.V2_0).run(checkRole(ParticipationLevel.OPTIONAL, null));
		sensei.assertParseText(uri).param("ROLE", "CHAIR").versions(ICalVersion.V2_0_DEPRECATED, ICalVersion.V2_0).run(checkRole(null, Role.CHAIR));
		sensei.assertParseText(uri).param("ROLE", "ATTENDEE").run(checkRole(null, Role.ATTENDEE));
		sensei.assertParseText(uri).param("ROLE", "invalid").run(checkRole(null, Role.get("invalid")));
	}

	private Check<Attendee> checkRole(final ParticipationLevel level, final Role role) {
		return new Check<Attendee>() {
			public void check(Attendee property) {
				assertNull(property.getParameter("ROLE"));
				assertEquals(level, property.getParticipationLevel());
				assertEquals(role, property.getRole());
			}
		};
	}

	@Test
	public void parseText_rsvp() {
		sensei.assertParseText(uri).run(checkRsvp(null, null));
		sensei.assertParseText(uri).param("RSVP", "YES").versions(ICalVersion.V1_0).run(checkRsvp(null, true));
		sensei.assertParseText(uri).param("RSVP", "NO").versions(ICalVersion.V1_0).run(checkRsvp(null, false));
		sensei.assertParseText(uri).param("RSVP", "TRUE").versions(ICalVersion.V2_0_DEPRECATED, ICalVersion.V2_0).run(checkRsvp(null, true));
		sensei.assertParseText(uri).param("RSVP", "FALSE").versions(ICalVersion.V2_0_DEPRECATED, ICalVersion.V2_0).run(checkRsvp(null, false));
		sensei.assertParseText(uri).param("RSVP", "invalid").run(checkRsvp("invalid", null));
	}

	private Check<Attendee> checkRsvp(final String paramValue, final Boolean value) {
		return new Check<Attendee>() {
			public void check(Attendee property) {
				assertEquals(paramValue, property.getParameter("RSVP"));
				assertEquals(value, property.getRsvp());
			}
		};
	}
}
