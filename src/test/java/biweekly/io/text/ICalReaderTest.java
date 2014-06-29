package biweekly.io.text;

import static biweekly.util.StringUtils.NEWLINE;
import static biweekly.util.TestUtils.assertDateEquals;
import static biweekly.util.TestUtils.assertIntEquals;
import static biweekly.util.TestUtils.assertSize;
import static biweekly.util.TestUtils.assertValidate;
import static biweekly.util.TestUtils.assertWarnings;
import static biweekly.util.TestUtils.date;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import biweekly.ICalDataType;
import biweekly.ICalVersion;
import biweekly.ICalendar;
import biweekly.Warning;
import biweekly.component.DaylightSavingsTime;
import biweekly.component.ICalComponent;
import biweekly.component.RawComponent;
import biweekly.component.StandardTime;
import biweekly.component.VAlarm;
import biweekly.component.VEvent;
import biweekly.component.VFreeBusy;
import biweekly.component.VJournal;
import biweekly.component.VTimezone;
import biweekly.component.VTodo;
import biweekly.io.scribe.component.ICalComponentScribe;
import biweekly.io.scribe.property.CannotParseScribe;
import biweekly.io.scribe.property.ICalPropertyScribe;
import biweekly.io.scribe.property.SkipMeScribe;
import biweekly.parameter.CalendarUserType;
import biweekly.parameter.ICalParameters;
import biweekly.parameter.ParticipationLevel;
import biweekly.parameter.ParticipationStatus;
import biweekly.parameter.Role;
import biweekly.property.Attachment;
import biweekly.property.Attendee;
import biweekly.property.ICalProperty;
import biweekly.property.ProductId;
import biweekly.property.RawProperty;
import biweekly.property.RecurrenceRule;
import biweekly.property.Summary;
import biweekly.util.DateTimeComponents;
import biweekly.util.Duration;
import biweekly.util.IOUtils;
import biweekly.util.Period;
import biweekly.util.Recurrence;
import biweekly.util.Recurrence.DayOfWeek;
import biweekly.util.Recurrence.Frequency;
import biweekly.util.UtcOffset;

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
@SuppressWarnings("resource")
public class ICalReaderTest {
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	private final DateFormat utcFormatter;
	{
		utcFormatter = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
		utcFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
	}

	@Test
	public void basic() throws Throwable {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"PRODID:-//xyz Corp//NONSGML PDA Calendar Version 1.0//EN\r\n" +
			"VERSION:2.0\r\n" +
			"X-TEST:a test\r\n" +
			"BEGIN:VEVENT\r\n" +
				"SUMMARY;LANGUAGE=en:Networld+Interop Conference\r\n" +
				"DESCRIPTION:Networld+Interop Conference\r\n" +
				" and Exhibit\\nAtlanta World Congress Center\\n\r\n" +
				" Atlanta\\, Georgia\r\n" +
			"END:VEVENT\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		ICalendar icalendar = reader.readNext();
		assertSize(icalendar, 1, 3);

		assertEquals("-//xyz Corp//NONSGML PDA Calendar Version 1.0//EN", icalendar.getProductId().getValue());
		assertTrue(icalendar.getVersion().isV2_0());
		assertEquals("a test", icalendar.getExperimentalProperty("X-TEST").getValue());

		{
			VEvent event = icalendar.getEvents().get(0);
			assertSize(event, 0, 2);

			assertEquals("Networld+Interop Conference", event.getSummary().getValue());
			assertEquals("en", event.getSummary().getLanguage());

			assertEquals("Networld+Interop Conferenceand Exhibit" + NEWLINE + "Atlanta World Congress Center" + NEWLINE + "Atlanta, Georgia", event.getDescription().getValue());
		}

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void read_multiple() throws Throwable {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"PRODID:prodid\r\n" +
			"VERSION:2.0\r\n" +
			"BEGIN:VEVENT\r\n" +
				"SUMMARY:event summary\r\n" +
			"END:VEVENT\r\n" +
		"END:VCALENDAR\r\n" +
		"BEGIN:VCALENDAR\r\n" +
			"PRODID:prodid\r\n" +
			"VERSION:2.0\r\n" +
			"BEGIN:VTODO\r\n" +
				"SUMMARY:todo summary\r\n" +
			"END:VTODO\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);

		{
			ICalendar icalendar = reader.readNext();
			assertSize(icalendar, 1, 2);

			assertEquals("prodid", icalendar.getProductId().getValue());
			assertTrue(icalendar.getVersion().isV2_0());

			VEvent event = icalendar.getEvents().get(0);
			assertSize(event, 0, 1);
			assertEquals("event summary", event.getSummary().getValue());

			assertWarnings(0, reader.getWarnings());
		}

		{
			ICalendar icalendar = reader.readNext();
			assertSize(icalendar, 1, 2);

			assertEquals("prodid", icalendar.getProductId().getValue());
			assertTrue(icalendar.getVersion().isV2_0());

			VTodo todo = icalendar.getTodos().get(0);
			assertSize(todo, 0, 1);
			assertEquals("todo summary", todo.getSummary().getValue());

			assertWarnings(0, reader.getWarnings());
		}

		assertNull(reader.readNext());
	}

	@Test
	public void caret_encoding_enabled() throws Throwable {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"VERSION:2.0\r\n" +
			"PRODID;X-TEST=^'test^':prodid\r\n" +
			"VERSION:2.0\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		reader.setCaretDecodingEnabled(true);
		reader.registerScribe(new TestPropertyMarshaller());

		ICalendar icalendar = reader.readNext();
		assertSize(icalendar, 0, 3);

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertEquals("\"test\"", icalendar.getProductId().getParameter("X-TEST"));
		assertTrue(icalendar.getVersion().isV2_0());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void caret_encoding_disabled() throws Throwable {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"VERSION:2.0\r\n" +
			"PRODID;X-TEST=^'test^':prodid\r\n" +
			"VERSION:2.0\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		reader.setCaretDecodingEnabled(false);
		reader.registerScribe(new TestPropertyMarshaller());

		ICalendar icalendar = reader.readNext();
		assertSize(icalendar, 0, 3);

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertEquals("^'test^'", icalendar.getProductId().getParameter("X-TEST"));
		assertTrue(icalendar.getVersion().isV2_0());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void missing_vcalendar_component_no_components() throws Throwable {
		//@formatter:off
		String ical =
		"PRODID:prodid\r\n" +
		"VERSION:2.0\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		assertNull(reader.readNext());
	}

	@Test
	public void missing_vcalendar_component_with_component() throws Throwable {
		//@formatter:off
		String ical =
		"PRODID:prodid\r\n" +
		"VERSION:2.0\r\n" +
		"BEGIN:VEVENT\r\n" +
			"SUMMARY:summary\r\n" +
		"END:VEVENT\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		assertNull(reader.readNext());
	}

	@Test
	public void vcalendar_component_not_the_first_line() throws Throwable {
		//@formatter:off
		String ical =
		"PRODID:prodid\r\n" +
		"VERSION:1.0\r\n" +
		"BEGIN:VCALENDAR\r\n" +
			"PRODID:prodid2\r\n" +
			"VERSION:2.0\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		ICalendar icalendar = reader.readNext();
		assertSize(icalendar, 0, 2);

		assertEquals("prodid2", icalendar.getProductId().getValue());
		assertTrue(icalendar.getVersion().isV2_0());
	}

	@Test
	public void missing_end_property() throws Throwable {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
		"PRODID:prodid\r\n" +
		"VERSION:2.0\r\n" +
		"BEGIN:VEVENT\r\n" +
			"SUMMARY:summary\r\n" +
			"BEGIN:VTODO\r\n" +
				"SUMMARY:one\r\n" +
		  //"END:VTODO\r\n" + missing END property
		"END:VEVENT\r\n" +		
		"BEGIN:VTODO\r\n" +
			"SUMMARY:two\r\n" +
		"END:VTODO\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		ICalendar icalendar = reader.readNext();
		assertSize(icalendar, 2, 2);

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertTrue(icalendar.getVersion().isV2_0());

		VEvent event = icalendar.getEvents().get(0);
		assertSize(event, 1, 1);
		assertEquals("summary", event.getSummary().getValue());

		VTodo todo = icalendar.getTodos().get(0);
		assertSize(todo, 0, 1);
		assertEquals("two", todo.getSummary().getValue());

		assertEquals("one", event.getComponent(VTodo.class).getSummary().getValue());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void invalid_end_property() throws Throwable {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"PRODID:prodid\r\n" +
			"VERSION:2.0\r\n" +
			"BEGIN:VEVENT\r\n" +
				"SUMMARY:summary\r\n" +
				"END:FOOBAR\r\n" + //END property does not correspond to a BEGIN property
			"END:VEVENT\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		ICalendar icalendar = reader.readNext();
		assertSize(icalendar, 1, 2);

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertTrue(icalendar.getVersion().isV2_0());

		VEvent event = icalendar.getEvents().get(0);
		assertSize(event, 0, 1);
		assertEquals("summary", event.getSummary().getValue());

		assertWarnings(1, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void experimental_property() throws Throwable {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"PRODID:prodid\r\n" +
			"VERSION:2.0\r\n" +
			"X-TEST1:one\r\n" +
			"X-TEST1:one point five\r\n" +
			"BEGIN:VEVENT\r\n" +
				"SUMMARY:summary\r\n" +
				"X-TEST2:two\r\n" +
			"END:VEVENT\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		ICalendar icalendar = reader.readNext();
		assertSize(icalendar, 1, 4);

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertTrue(icalendar.getVersion().isV2_0());
		assertEquals("one", icalendar.getExperimentalProperties("X-TEST1").get(0).getValue());
		assertEquals("one point five", icalendar.getExperimentalProperties("X-TEST1").get(1).getValue());

		VEvent event = icalendar.getEvents().get(0);
		assertSize(event, 0, 2);
		assertEquals("summary", event.getSummary().getValue());
		assertEquals("two", event.getExperimentalProperty("X-TEST2").getValue());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void experiemental_property_marshaller() throws Throwable {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"PRODID:prodid\r\n" +
			"VERSION:2.0\r\n" +
			"X-TEST:one\r\n" +
			"X-TEST:two\r\n" +
			"BEGIN:VEVENT\r\n" +
				"SUMMARY:summary\r\n" +
				"X-TEST:three\r\n" +
			"END:VEVENT\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		reader.registerScribe(new TestPropertyMarshaller());

		ICalendar icalendar = reader.readNext();
		assertSize(icalendar, 1, 4);

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertTrue(icalendar.getVersion().isV2_0());
		assertIntEquals(1, icalendar.getProperties(TestProperty.class).get(0).number);
		assertIntEquals(2, icalendar.getProperties(TestProperty.class).get(1).number);

		VEvent event = icalendar.getEvents().get(0);
		assertSize(event, 0, 2);
		assertEquals("summary", event.getSummary().getValue());
		assertIntEquals(3, event.getProperty(TestProperty.class).number);

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void experimental_component() throws Throwable {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"PRODID:prodid\r\n" +
			"VERSION:2.0\r\n" +
			"BEGIN:X-VPARTY\r\n" +
				"X-DJ:Johnny D\r\n" +
			"END:X-VPARTY\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		ICalendar icalendar = reader.readNext();
		assertSize(icalendar, 1, 2);

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertTrue(icalendar.getVersion().isV2_0());

		RawComponent component = icalendar.getExperimentalComponent("X-VPARTY");
		assertSize(component, 0, 1);
		assertEquals("Johnny D", component.getExperimentalProperty("X-DJ").getValue());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void experiemental_component_marshaller() throws Throwable {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"PRODID:prodid\r\n" +
			"VERSION:2.0\r\n" +
			"BEGIN:X-VPARTY\r\n" +
				"X-DJ:Johnny D\r\n" +
			"END:X-VPARTY\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		reader.registerScribe(new PartyMarshaller());
		ICalendar icalendar = reader.readNext();
		assertSize(icalendar, 1, 2);

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertTrue(icalendar.getVersion().isV2_0());

		Party component = icalendar.getComponent(Party.class);
		assertSize(component, 0, 1);
		assertEquals("Johnny D", component.getExperimentalProperty("X-DJ").getValue());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void override_property_marshaller() throws Throwable {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"PRODID:the product id\r\n" +
			"VERSION:2.0\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		reader.registerScribe(new MyProductIdMarshaller());
		ICalendar icalendar = reader.readNext();
		assertSize(icalendar, 0, 2);

		assertEquals("THE PRODUCT ID", icalendar.getProductId().getValue());
		assertTrue(icalendar.getVersion().isV2_0());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void override_component_marshaller() throws Throwable {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"PRODID:prodid\r\n" +
			"VERSION:2.0\r\n" +
			"BEGIN:VEVENT\r\n" +
				"SUMMARY:event summary\r\n" +
			"END:VEVENT\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		reader.registerScribe(new MyEventMarshaller());

		ICalendar icalendar = reader.readNext();
		assertSize(icalendar, 1, 2);

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertTrue(icalendar.getVersion().isV2_0());

		MyVEvent event = icalendar.getComponent(MyVEvent.class);
		assertSize(event, 0, 1);
		assertEquals("event summary", event.getProperty(Summary.class).getValue());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void invalid_line() throws Throwable {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"PRODID:prodid\r\n" +
			"bad-line\r\n" +
			"VERSION:2.0\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		reader.registerScribe(new MyEventMarshaller());

		ICalendar icalendar = reader.readNext();
		assertSize(icalendar, 0, 2);

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertTrue(icalendar.getVersion().isV2_0());

		assertWarnings(1, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void property_warning() throws Throwable {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"PRODID:prodid\r\n" +
			"VERSION:2.0\r\n" +
			"X-TEST:four\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		reader.registerScribe(new TestPropertyMarshaller());

		ICalendar icalendar = reader.readNext();
		assertSize(icalendar, 0, 3);

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertTrue(icalendar.getVersion().isV2_0());
		assertIntEquals(4, icalendar.getProperty(TestProperty.class).number);

		assertWarnings(1, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void warnings_cleared_between_reads() throws Throwable {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"PRODID:prodid\r\n" +
			"VERSION:2.0\r\n" +
			"X-TEST:four\r\n" +
		"END:VCALENDAR\r\n" +
		"BEGIN:VCALENDAR\r\n" +
			"PRODID:prodid\r\n" +
			"VERSION:2.0\r\n" +
			"X-TEST:four\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		reader.registerScribe(new TestPropertyMarshaller());

		{
			ICalendar icalendar = reader.readNext();
			assertSize(icalendar, 0, 3);

			assertEquals("prodid", icalendar.getProductId().getValue());
			assertTrue(icalendar.getVersion().isV2_0());
			assertIntEquals(4, icalendar.getProperty(TestProperty.class).number);

			assertWarnings(1, reader.getWarnings());
		}

		{
			ICalendar icalendar = reader.readNext();
			assertSize(icalendar, 0, 3);

			assertEquals("prodid", icalendar.getProductId().getValue());
			assertTrue(icalendar.getVersion().isV2_0());
			assertIntEquals(4, icalendar.getProperty(TestProperty.class).number);

			assertWarnings(1, reader.getWarnings());
		}

		assertNull(reader.readNext());
	}

	@Test
	public void skipMeException() throws Throwable {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"SKIPME:value\r\n" +
			"X-FOO:bar\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		reader.registerScribe(new SkipMeScribe());

		ICalendar icalendar = reader.readNext();
		assertSize(icalendar, 0, 1);

		RawProperty property = icalendar.getExperimentalProperty("X-FOO");
		assertEquals(null, property.getDataType());
		assertEquals("X-FOO", property.getName());
		assertEquals("bar", property.getValue());

		assertWarnings(1, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void cannotParseException() throws Throwable {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"CANNOTPARSE:value\r\n" +
			"X-FOO:bar\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		reader.registerScribe(new CannotParseScribe());

		ICalendar icalendar = reader.readNext();
		assertSize(icalendar, 0, 2);

		RawProperty property = icalendar.getExperimentalProperty("CANNOTPARSE");
		assertEquals(null, property.getDataType());
		assertEquals("CANNOTPARSE", property.getName());
		assertEquals("value", property.getValue());

		property = icalendar.getExperimentalProperty("X-FOO");
		assertEquals(null, property.getDataType());
		assertEquals("X-FOO", property.getName());
		assertEquals("bar", property.getValue());

		assertWarnings(1, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void valueless_parameter() throws Throwable {
		//1.0
		{
			//@formatter:off
			String ical =
			"BEGIN:VCALENDAR\r\n" +
				"VERSION:1.0\r\n" +
				"PRODID;PARAM:value\r\n" +
			"END:VCALENDAR\r\n";
			//@formatter:on

			ICalReader reader = new ICalReader(ical);
			ICalendar icalendar = reader.readNext();
			assertSize(icalendar, 0, 2);

			assertEquals("value", icalendar.getProductId().getValue());
			assertEquals(Arrays.asList(), icalendar.getProductId().getParameters(null));
			assertEquals(Arrays.asList("PARAM"), icalendar.getProductId().getParameters("TYPE"));

			assertWarnings(0, reader.getWarnings());
			assertNull(reader.readNext());
		}

		//2.0
		{
			//@formatter:off
			String ical =
			"BEGIN:VCALENDAR\r\n" +
				"VERSION:2.0\r\n" +
				"PRODID;PARAM:value\r\n" +
			"END:VCALENDAR\r\n";
			//@formatter:on

			ICalReader reader = new ICalReader(ical);
			ICalendar icalendar = reader.readNext();
			assertSize(icalendar, 0, 2);

			assertEquals("value", icalendar.getProductId().getValue());
			assertEquals(Arrays.asList(), icalendar.getProductId().getParameters(null));
			assertEquals(Arrays.asList("PARAM"), icalendar.getProductId().getParameters("TYPE"));

			assertWarnings(1, reader.getWarnings());
			assertNull(reader.readNext());
		}
	}

	@Test
	public void data_types() throws Throwable {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"PRODID:prodid\r\n" +
			"VERSION:2.0\r\n" +
			"X-TEST:one\r\n" +
			"X-TEST;VALUE=INTEGER:one\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		reader.registerScribe(new TestPropertyMarshaller());

		ICalendar icalendar = reader.readNext();
		assertSize(icalendar, 0, 4);

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertTrue(icalendar.getVersion().isV2_0());

		Iterator<TestProperty> it = icalendar.getProperties(TestProperty.class).iterator();

		TestProperty prop = it.next();
		assertEquals(ICalDataType.TEXT, prop.parsedDataType);
		assertNull(prop.getParameters().getValue());

		prop = it.next();
		assertEquals(ICalDataType.INTEGER, prop.parsedDataType);
		assertNull(prop.getParameters().getValue());

		assertFalse(it.hasNext());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void utf8() throws Throwable {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"VERSION:2.0\r\n" +
			"SUMMARY:\u1e66ummary\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on
		File file = tempFolder.newFile();
		Writer writer = IOUtils.utf8Writer(file);
		writer.write(ical);
		writer.close();

		ICalReader reader = new ICalReader(file);
		ICalendar icalendar = reader.readNext();
		assertSize(icalendar, 0, 2);
		assertEquals("\u1e66ummary", icalendar.getProperty(Summary.class).getValue());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	//see: http://stackoverflow.com/questions/33901/best-icalendar-library-for-java/17325369?noredirect=1#comment31110671_17325369
	@Test
	public void large_ical_file_stackoverflow_fix() throws Throwable {
		StringBuilder sb = new StringBuilder();
		sb.append("BEGIN:VCALENDAR\r\n");
		for (int i = 0; i < 100000; i++) {
			sb.append("BEGIN:VEVENT\r\nDESCRIPTION:test\r\n");
		}
		for (int i = 0; i < 100000; i++) {
			sb.append("END:VEVENT\r\n");
		}
		sb.append("END:VCALENDAR\r\n");

		ICalReader reader = new ICalReader(sb.toString());
		reader.readNext();
	}

	@Test
	public void vcal_rrule() throws Throwable {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"VERSION:1.0\r\n" +
			"RRULE:MD1 1 #1 D2  20000101T000000Z  M3\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		ICalendar icalendar = reader.readNext();
		assertSize(icalendar, 0, 4);
		assertTrue(icalendar.getVersion().isV1_0());

		DateFormat df = new SimpleDateFormat("yyyyMMdd'T'HHmmssZ");
		Iterator<RecurrenceRule> rrules = icalendar.getProperties(RecurrenceRule.class).iterator();
		Recurrence expected = new Recurrence.Builder(Frequency.MONTHLY).interval(1).byMonthDay(1).count(1).build();
		assertEquals(expected, rrules.next().getValue());
		expected = new Recurrence.Builder(Frequency.DAILY).interval(2).until(df.parse("20000101T000000+0000")).build();
		assertEquals(expected, rrules.next().getValue());
		expected = new Recurrence.Builder(Frequency.MINUTELY).interval(3).count(2).build();
		assertEquals(expected, rrules.next().getValue());
		assertFalse(rrules.hasNext());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void vcal_daylight_true() throws Throwable {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"VERSION:1.0\r\n" +
			"DAYLIGHT:TRUE;-0400;20140309T020000Z;20141102T020000Z;EST;EDT\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		ICalendar icalendar = reader.readNext();
		assertSize(icalendar, 1, 1);
		assertTrue(icalendar.getVersion().isV1_0());

		{
			VTimezone timezone = icalendar.getTimezones().get(0);
			assertSize(timezone, 2, 1);
			assertEquals("TZ1", timezone.getTimezoneId().getValue());

			{
				StandardTime standard = timezone.getStandardTimes().get(0);
				assertSize(standard, 0, 4);

				assertEquals(date("2014-11-02 02:00:00 +0000"), standard.getDateStart().getValue());
				assertEquals(new UtcOffset(-4, 0), standard.getTimezoneOffsetFrom().getValue());
				assertEquals(new UtcOffset(-5, 0), standard.getTimezoneOffsetTo().getValue());
				assertEquals("EST", standard.getTimezoneNames().get(0).getValue());
			}

			{
				DaylightSavingsTime daylight = timezone.getDaylightSavingsTime().get(0);
				assertSize(daylight, 0, 4);

				assertEquals(date("2014-03-09 02:00:00 +0000"), daylight.getDateStart().getValue());
				assertEquals(new UtcOffset(-5, 0), daylight.getTimezoneOffsetFrom().getValue());
				assertEquals(new UtcOffset(-4, 0), daylight.getTimezoneOffsetTo().getValue());
				assertEquals("EDT", daylight.getTimezoneNames().get(0).getValue());
			}
		}

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void vcal_daylight_false() throws Throwable {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"VERSION:1.0\r\n" +
			"DAYLIGHT:FALSE\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(ical);
		ICalendar icalendar = reader.readNext();
		assertSize(icalendar, 1, 1);
		assertTrue(icalendar.getVersion().isV1_0());

		{
			VTimezone timezone = icalendar.getTimezones().get(0);
			assertSize(timezone, 0, 1);
			assertEquals("TZ1", timezone.getTimezoneId().getValue());
		}

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void outlook2010() throws Throwable {
		ICalReader reader = read("outlook-2010.ics");
		ICalendar ical = reader.readNext();
		assertSize(ical, 2, 4);

		assertEquals("-//Microsoft Corporation//Outlook 14.0 MIMEDIR//EN", ical.getProductId().getValue());
		assertTrue(ical.getVersion().isV2_0());
		assertEquals(null, ical.getVersion().getMinVersion());
		assertEquals("REQUEST", ical.getMethod().getValue());
		assertEquals("TRUE", ical.getExperimentalProperty("X-MS-OLK-FORCEINSPECTOROPEN").getValue());

		{
			VTimezone timezone = ical.getTimezones().get(0);
			assertSize(timezone, 2, 1);

			assertEquals("Eastern Standard Time", timezone.getTimezoneId().getValue());
			{
				StandardTime standard = timezone.getStandardTimes().get(0);
				assertSize(standard, 0, 4);

				assertDateEquals("16011104T020000", standard.getDateStart().getValue());

				Recurrence rrule = standard.getRecurrenceRule().getValue();
				assertEquals(Frequency.YEARLY, rrule.getFrequency());
				assertEquals(Arrays.asList(1), rrule.getByDayPrefixes());
				assertEquals(Arrays.asList(DayOfWeek.SUNDAY), rrule.getByDay());
				assertEquals(Arrays.asList(11), rrule.getByMonth());

				assertIntEquals(-4, standard.getTimezoneOffsetFrom().getHourOffset());
				assertIntEquals(0, standard.getTimezoneOffsetFrom().getMinuteOffset());

				assertIntEquals(-5, standard.getTimezoneOffsetTo().getHourOffset());
				assertIntEquals(0, standard.getTimezoneOffsetTo().getMinuteOffset());
			}
			{
				DaylightSavingsTime daylight = timezone.getDaylightSavingsTime().get(0);
				assertSize(daylight, 0, 4);

				assertDateEquals("16010311T020000", daylight.getDateStart().getValue());

				Recurrence rrule = daylight.getRecurrenceRule().getValue();
				assertEquals(Frequency.YEARLY, rrule.getFrequency());
				assertEquals(Arrays.asList(2), rrule.getByDayPrefixes());
				assertEquals(Arrays.asList(DayOfWeek.SUNDAY), rrule.getByDay());
				assertEquals(Arrays.asList(3), rrule.getByMonth());

				assertIntEquals(-5, daylight.getTimezoneOffsetFrom().getHourOffset());
				assertIntEquals(0, daylight.getTimezoneOffsetFrom().getMinuteOffset());

				assertIntEquals(-4, daylight.getTimezoneOffsetTo().getHourOffset());
				assertIntEquals(0, daylight.getTimezoneOffsetTo().getMinuteOffset());
			}
		}
		{
			VEvent event = ical.getEvents().get(0);
			assertSize(event, 0, 24);

			Attendee attendee = event.getAttendees().get(0);
			assertEquals("Doe, John", attendee.getCommonName());
			assertEquals(ParticipationLevel.OPTIONAL, attendee.getParticipationLevel());
			assertEquals(Boolean.FALSE, attendee.getRsvp());
			assertEquals("johndoe@example.com", attendee.getEmail());

			attendee = event.getAttendees().get(1);
			assertEquals("Doe, Jane", attendee.getCommonName());
			assertEquals(Role.CHAIR, attendee.getRole());
			assertEquals(Boolean.TRUE, attendee.getRsvp());
			assertEquals("janedoe@example.com", attendee.getEmail());

			assertEquals("PUBLIC", event.getClassification().getValue());
			assertDateEquals("20130608T200410Z", event.getCreated().getValue());
			assertEquals("Meeting will discuss objectives for next project." + NEWLINE + "Will include a presentation and food.", event.getDescription().getValue());

			assertDateEquals("20130610T130000", event.getDateEnd().getValue());
			assertEquals("Eastern Standard Time", event.getDateEnd().getTimezoneId());

			assertDateEquals("20130425T155807Z", event.getDateTimeStamp().getValue());

			assertDateEquals("20130610T120000", event.getDateStart().getValue());
			assertEquals("Eastern Standard Time", event.getDateStart().getTimezoneId());

			assertDateEquals("20130608T200410Z", event.getLastModified().getValue());

			assertEquals("Auditorium 16", event.getLocation().getValue());

			assertEquals("bobsmith@example.com", event.getOrganizer().getEmail());
			assertEquals("Smith, Bob", event.getOrganizer().getCommonName());

			assertIntEquals(5, event.getPriority().getValue());
			assertIntEquals(1, event.getSequence().getValue());

			assertEquals("Team Meeting", event.getSummary().getValue());
			assertEquals("en-us", event.getSummary().getLanguage());

			assertEquals(true, event.getTransparency().isOpaque());
			assertEquals("040000009200E00074C5B7101A82E00800000000C0383BE68041CE0100000000000000001000000070D00A2F625AC34BB6542DE0D19E67E1", event.getUid().getValue());
			//@formatter:off
			assertEquals(
			"<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2//EN\">\\n" +
			"<HTML>\\n" +
			"<HEAD>\\n" +
			"<META NAME=\"Generator\" CONTENT=\"MS Exchange Server version 14.02.5004.000\">\\n" +
			"<TITLE></TITLE>\\n" +
			"</HEAD>\\n" +
			"<BODY>\\n" +
			"<!-- Converted from text/rtf format -->\\n" +
			"\\n" +
			"<P DIR=LTR><SPAN LANG=\"en-us\"><B><FONT COLOR=\"#0000FF\" FACE=\"Arial\">Meeting will discuss objectives for next project.\\n" +
			"Will include a presentation and food.</FONT></B></SPAN></P></BODY>\\n" +
			"</HTML>",
			event.getExperimentalProperty("X-ALT-DESC").getValue());
			//@formatter:on
			assertEquals("text/html", event.getExperimentalProperty("X-ALT-DESC").getParameter("FMTTYPE"));

			assertEquals("TENTATIVE", event.getExperimentalProperty("X-MICROSOFT-CDO-BUSYSTATUS").getValue());
			assertEquals("1", event.getExperimentalProperty("X-MICROSOFT-CDO-IMPORTANCE").getValue());
			assertEquals("BUSY", event.getExperimentalProperty("X-MICROSOFT-CDO-INTENDEDSTATUS").getValue());
			assertEquals("TRUE", event.getExperimentalProperty("X-MICROSOFT-DISALLOW-COUNTER").getValue());
			assertEquals("1", event.getExperimentalProperty("X-MS-OLK-APPTLASTSEQUENCE").getValue());
			assertEquals("20130425T124303Z", event.getExperimentalProperty("X-MS-OLK-APPTSEQTIME").getValue());
			assertEquals("0", event.getExperimentalProperty("X-MS-OLK-CONFTYPE").getValue());
		}

		assertValidate(ical).run();

		assertNull(reader.readNext());
	}

	@Test
	public void example1() throws Throwable {
		ICalReader reader = read("rfc5545-example1.ics");
		ICalendar ical = reader.readNext();
		assertSize(ical, 1, 2);

		assertEquals("-//xyz Corp//NONSGML PDA Calendar Version 1.0//EN", ical.getProductId().getValue());
		assertTrue(ical.getVersion().isV2_0());

		{
			VEvent event = ical.getEvents().get(0);
			assertSize(event, 0, 9);

			assertDateEquals("19960704T120000Z", event.getDateTimeStamp().getValue());
			assertEquals("uid1@example.com", event.getUid().getValue());
			assertEquals("jsmith@example.com", event.getOrganizer().getEmail());
			assertDateEquals("19960918T143000Z", event.getDateStart().getValue());
			assertDateEquals("19960920T220000Z", event.getDateEnd().getValue());
			assertTrue(event.getStatus().isConfirmed());
			assertEquals(Arrays.asList("CONFERENCE"), event.getCategories().get(0).getValues());
			assertEquals("Networld+Interop Conference", event.getSummary().getValue());
			assertEquals("Networld+Interop Conferenceand Exhibit\nAtlanta World Congress Center\nAtlanta, Georgia", event.getDescription().getValue());
		}

		assertValidate(ical).run();

		assertNull(reader.readNext());
	}

	@Test
	public void example2() throws Throwable {
		DateFormat nycFormatter = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
		nycFormatter.setTimeZone(TimeZone.getTimeZone("America/New_York"));

		ICalReader reader = read("rfc5545-example2.ics");
		ICalendar ical = reader.readNext();
		assertSize(ical, 2, 2);

		assertEquals("-//RDU Software//NONSGML HandCal//EN", ical.getProductId().getValue());
		assertTrue(ical.getVersion().isV2_0());

		{
			VTimezone timezone = ical.getTimezones().get(0);
			assertSize(timezone, 2, 1);

			assertEquals("America/New_York", timezone.getTimezoneId().getValue());

			{
				StandardTime standard = timezone.getStandardTimes().get(0);
				assertSize(standard, 0, 4);

				assertDateEquals("19981025T020000", standard.getDateStart().getValue());
				assertEquals(new DateTimeComponents(1998, 10, 25, 2, 0, 0, false), standard.getDateStart().getRawComponents());

				assertIntEquals(-4, standard.getTimezoneOffsetFrom().getHourOffset());
				assertIntEquals(0, standard.getTimezoneOffsetFrom().getMinuteOffset());

				assertIntEquals(-5, standard.getTimezoneOffsetTo().getHourOffset());
				assertIntEquals(0, standard.getTimezoneOffsetTo().getMinuteOffset());

				assertEquals("EST", standard.getTimezoneNames().get(0).getValue());
			}
			{
				DaylightSavingsTime daylight = timezone.getDaylightSavingsTime().get(0);
				assertSize(daylight, 0, 4);

				assertDateEquals("19990404T020000", daylight.getDateStart().getValue());
				assertEquals(new DateTimeComponents(1999, 04, 04, 2, 0, 0, false), daylight.getDateStart().getRawComponents());

				assertIntEquals(-5, daylight.getTimezoneOffsetFrom().getHourOffset());
				assertIntEquals(0, daylight.getTimezoneOffsetFrom().getMinuteOffset());

				assertIntEquals(-4, daylight.getTimezoneOffsetTo().getHourOffset());
				assertIntEquals(0, daylight.getTimezoneOffsetTo().getMinuteOffset());

				assertEquals("EDT", daylight.getTimezoneNames().get(0).getValue());
			}
		}
		{
			VEvent event = ical.getEvents().get(0);
			assertSize(event, 0, 12);

			assertDateEquals("19980309T231000Z", event.getDateTimeStamp().getValue());
			assertEquals("guid-1.example.com", event.getUid().getValue());
			assertEquals("mrbig@example.com", event.getOrganizer().getEmail());

			Attendee attendee = event.getAttendees().get(0);
			assertEquals("employee-A@example.com", attendee.getEmail());
			assertTrue(attendee.getRsvp());
			assertEquals(ParticipationLevel.REQUIRED, attendee.getParticipationLevel());
			assertEquals(CalendarUserType.GROUP, attendee.getCalendarUserType());

			assertEquals("Project XYZ Review Meeting", event.getDescription().getValue());
			assertEquals(Arrays.asList("MEETING"), event.getCategories().get(0).getValues());
			assertTrue(event.getClassification().isPublic());
			assertDateEquals("19980309T130000Z", event.getCreated().getValue());
			assertEquals("XYZ Project Review", event.getSummary().getValue());

			assertEquals(nycFormatter.parse("19980312T083000"), event.getDateStart().getValue());
			assertEquals("America/New_York", event.getDateStart().getTimezoneId());

			assertEquals(nycFormatter.parse("19980312T093000"), event.getDateEnd().getValue());
			assertEquals("America/New_York", event.getDateEnd().getTimezoneId());

			assertEquals("1CP Conference Room 4350", event.getLocation().getValue());
		}

		assertValidate(ical).run();

		assertNull(reader.readNext());
	}

	@Test
	public void example3() throws Throwable {
		ICalReader reader = read("rfc5545-example3.ics");
		ICalendar ical = reader.readNext();
		assertSize(ical, 1, 3);

		assertEquals("xyz", ical.getMethod().getValue());
		assertEquals("-//ABC Corporation//NONSGML My Product//EN", ical.getProductId().getValue());
		assertTrue(ical.getVersion().isV2_0());

		{
			VEvent event = ical.getEvents().get(0);
			assertSize(event, 0, 13);

			assertDateEquals("19970324T120000Z", event.getDateTimeStamp().getValue());
			assertIntEquals(0, event.getSequence().getValue());
			assertEquals("uid3@example.com", event.getUid().getValue());
			assertEquals("jdoe@example.com", event.getOrganizer().getEmail());
			assertDateEquals("19970324T123000Z", event.getDateStart().getValue());
			assertDateEquals("19970324T210000Z", event.getDateEnd().getValue());
			assertEquals(Arrays.asList("MEETING", "PROJECT"), event.getCategories().get(0).getValues());
			assertTrue(event.getClassification().isPublic());
			assertEquals("Calendaring Interoperability Planning Meeting", event.getSummary().getValue());
			assertEquals("Discuss how we can test c&s interoperability" + NEWLINE + "using iCalendar and other IETF standards.", event.getDescription().getValue());
			assertEquals("LDB Lobby", event.getLocation().getValue());

			Attachment attach = event.getAttachments().get(0);
			assertEquals("ftp://example.com/pub/conf/bkgrnd.ps", attach.getUri());
			assertEquals("application/postscript", attach.getFormatType());
		}

		assertValidate(ical).run();

		assertNull(reader.readNext());
	}

	@Test
	public void example4() throws Throwable {
		ICalReader reader = read("rfc5545-example4.ics");
		ICalendar ical = reader.readNext();
		assertSize(ical, 1, 2);

		assertTrue(ical.getVersion().isV2_0());
		assertEquals("-//ABC Corporation//NONSGML My Product//EN", ical.getProductId().getValue());

		{
			VTodo todo = ical.getTodos().get(0);
			assertSize(todo, 1, 8);

			assertDateEquals("19980130T134500Z", todo.getDateTimeStamp().getValue());
			assertIntEquals(2, todo.getSequence().getValue());
			assertEquals("uid4@example.com", todo.getUid().getValue());
			assertEquals("unclesam@example.com", todo.getOrganizer().getEmail());

			Attendee attendee = todo.getAttendees().get(0);
			assertEquals("jqpublic@example.com", attendee.getEmail());
			assertEquals(ParticipationStatus.ACCEPTED, attendee.getParticipationStatus());

			assertDateEquals("19980415T000000", todo.getDateDue().getValue());
			assertTrue(todo.getStatus().isNeedsAction());
			assertEquals("Submit Income Taxes", todo.getSummary().getValue());

			{
				VAlarm alarm = todo.getAlarms().get(0);
				assertSize(alarm, 0, 5);

				assertTrue(alarm.getAction().isAudio());
				assertDateEquals("19980403T120000Z", alarm.getTrigger().getDate());

				Attachment attach = alarm.getAttachments().get(0);
				assertEquals("http://example.com/pub/audio-files/ssbanner.aud", attach.getUri());
				assertEquals("audio/basic", attach.getFormatType());

				assertIntEquals(4, alarm.getRepeat().getValue());
				assertEquals(Duration.builder().hours(1).build(), alarm.getDuration().getValue());
			}
		}

		assertValidate(ical).run();

		assertNull(reader.readNext());
	}

	@Test
	public void example5() throws Throwable {
		ICalReader reader = read("rfc5545-example5.ics");
		ICalendar ical = reader.readNext();
		assertSize(ical, 1, 2);

		assertTrue(ical.getVersion().isV2_0());
		assertEquals("-//ABC Corporation//NONSGML My Product//EN", ical.getProductId().getValue());

		{
			VJournal journal = ical.getJournals().get(0);
			assertSize(journal, 0, 7);

			assertDateEquals("19970324T120000Z", journal.getDateTimeStamp().getValue());
			assertEquals("uid5@example.com", journal.getUid().getValue());
			assertEquals("jsmith@example.com", journal.getOrganizer().getEmail());
			assertTrue(journal.getStatus().isDraft());
			assertTrue(journal.getClassification().isPublic());
			assertEquals(Arrays.asList("Project Report", "XYZ", "Weekly Meeting"), journal.getCategories().get(0).getValues());
			assertEquals("Project xyz Review Meeting Minutes" + NEWLINE + "Agenda" + NEWLINE + "1. Review of project version 1.0 requirements." + NEWLINE + "2.Definitionof project processes." + NEWLINE + "3. Review of project schedule." + NEWLINE + "Participants: John Smith, Jane Doe, Jim Dandy" + NEWLINE + "-It wasdecided that the requirements need to be signed off byproduct marketing." + NEWLINE + "-Project processes were accepted." + NEWLINE + "-Project schedule needs to account for scheduled holidaysand employee vacation time. Check with HR for specificdates." + NEWLINE + "-New schedule will be distributed by Friday." + NEWLINE + "-Next weeks meeting is cancelled. No meeting until 3/23.", journal.getDescriptions().get(0).getValue());
		}

		assertValidate(ical).run();

		assertNull(reader.readNext());
	}

	@Test
	public void example6() throws Throwable {
		ICalReader reader = read("rfc5545-example6.ics");
		ICalendar ical = reader.readNext();
		assertSize(ical, 1, 2);

		assertTrue(ical.getVersion().isV2_0());
		assertEquals("-//RDU Software//NONSGML HandCal//EN", ical.getProductId().getValue());

		VFreeBusy freebusy = ical.getFreeBusies().get(0);
		{
			assertSize(freebusy, 0, 7);

			assertEquals("jsmith@example.com", freebusy.getOrganizer().getEmail());
			assertDateEquals("19980313T141711Z", freebusy.getDateStart().getValue());
			assertDateEquals("19980410T141711Z", freebusy.getDateEnd().getValue());
			assertEquals(Arrays.asList(new Period(utcFormatter.parse("19980314T233000"), utcFormatter.parse("19980315T003000Z"))), freebusy.getFreeBusy().get(0).getValues());
			assertEquals(Arrays.asList(new Period(utcFormatter.parse("19980316T153000"), utcFormatter.parse("19980316T163000"))), freebusy.getFreeBusy().get(1).getValues());
			assertEquals(Arrays.asList(new Period(utcFormatter.parse("19980318T030000"), utcFormatter.parse("19980318T040000"))), freebusy.getFreeBusy().get(2).getValues());
			assertEquals("http://www.example.com/calendar/busytime/jsmith.ifb", freebusy.getUrl().getValue());
		}

		//UID and DTSTAMP are missing from VFREEBUSY
		assertValidate(ical).warn(freebusy, 2, 2).run();

		assertNull(reader.readNext());
	}

	private ICalReader read(String file) {
		return new ICalReader(getClass().getResourceAsStream(file));
	}

	private class TestPropertyMarshaller extends ICalPropertyScribe<TestProperty> {
		public TestPropertyMarshaller() {
			super(TestProperty.class, "X-TEST", ICalDataType.TEXT);
		}

		@Override
		protected String _writeText(TestProperty property, ICalVersion version) {
			return property.number.toString();
		}

		@Override
		protected TestProperty _parseText(String value, ICalDataType dataType, ICalParameters parameters, ICalVersion version, List<Warning> warnings) {
			TestProperty prop = new TestProperty();
			Integer number = null;
			if (value.equals("one")) {
				number = 1;
			} else if (value.equals("two")) {
				number = 2;
			} else if (value.equals("three")) {
				number = 3;
			} else if (value.equals("four")) {
				number = 4;
				warnings.add(new Warning("too high"));
			}
			prop.number = number;
			prop.parsedDataType = dataType;
			return prop;
		}
	}

	private class TestProperty extends ICalProperty {
		private Integer number;
		private ICalDataType parsedDataType;
	}

	private class PartyMarshaller extends ICalComponentScribe<Party> {
		public PartyMarshaller() {
			super(Party.class, "X-VPARTY");
		}

		@Override
		protected Party _newInstance() {
			return new Party();
		}
	}

	private class Party extends ICalComponent {
		//empty
	}

	private class MyProductIdMarshaller extends ICalPropertyScribe<ProductId> {
		public MyProductIdMarshaller() {
			super(ProductId.class, "PRODID", ICalDataType.TEXT);
		}

		@Override
		protected String _writeText(ProductId property, ICalVersion version) {
			return property.getValue();
		}

		@Override
		protected ProductId _parseText(String value, ICalDataType dataType, ICalParameters parameters, ICalVersion version, List<Warning> warnings) {
			return new ProductId(value.toUpperCase());
		}
	}

	private class MyVEvent extends ICalComponent {
		//empty
	}

	private class MyEventMarshaller extends ICalComponentScribe<MyVEvent> {
		public MyEventMarshaller() {
			super(MyVEvent.class, "VEVENT");
		}

		@Override
		protected MyVEvent _newInstance() {
			return new MyVEvent();
		}
	}
}
