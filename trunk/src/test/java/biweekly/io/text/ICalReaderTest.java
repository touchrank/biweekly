package biweekly.io.text;

import static biweekly.util.TestUtils.assertWarnings;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import biweekly.ICalendar;
import biweekly.component.DaylightSavingsTime;
import biweekly.component.ICalComponent;
import biweekly.component.StandardTime;
import biweekly.component.VEvent;
import biweekly.component.VTimezone;
import biweekly.component.VTodo;
import biweekly.component.marshaller.ICalComponentMarshaller;
import biweekly.io.CannotParseException;
import biweekly.io.SkipMeException;
import biweekly.parameter.ICalParameters;
import biweekly.parameter.Role;
import biweekly.property.Attendee;
import biweekly.property.ICalProperty;
import biweekly.property.ProductId;
import biweekly.property.RecurrenceRule;
import biweekly.property.RecurrenceRule.DayOfWeek;
import biweekly.property.RecurrenceRule.Frequency;
import biweekly.property.Summary;
import biweekly.property.marshaller.ICalPropertyMarshaller;

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
public class ICalReaderTest {
	final String NEWLINE = System.getProperty("line.separator");

	@Test
	public void basic() throws Exception {
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

		ICalReader reader = new ICalReader(new StringReader(ical));
		ICalendar icalendar = reader.readNext();

		assertEquals("-//xyz Corp//NONSGML PDA Calendar Version 1.0//EN", icalendar.getProductId().getValue());
		assertEquals("2.0", icalendar.getVersion().getMaxVersion());
		assertEquals("a test", icalendar.getExperimentalProperty("X-TEST").getValue());

		assertEquals(1, icalendar.getEvents().size());
		VEvent event = icalendar.getEvents().get(0);

		assertEquals("Networld+Interop Conference", event.getSummary().getValue());
		assertEquals("en", event.getSummary().getLanguage());

		assertEquals("Networld+Interop Conferenceand Exhibit" + NEWLINE + "Atlanta World Congress Center" + NEWLINE + "Atlanta, Georgia", event.getDescription().getValue());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void read_multiple() throws Exception {
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

		ICalReader reader = new ICalReader(new StringReader(ical));

		{
			ICalendar icalendar = reader.readNext();

			assertEquals("prodid", icalendar.getProductId().getValue());
			assertEquals("2.0", icalendar.getVersion().getMaxVersion());

			assertEquals(1, icalendar.getEvents().size());
			VEvent event = icalendar.getEvents().get(0);
			assertEquals("event summary", event.getSummary().getValue());

			assertWarnings(0, reader.getWarnings());
		}

		{
			ICalendar icalendar = reader.readNext();

			assertEquals("prodid", icalendar.getProductId().getValue());
			assertEquals("2.0", icalendar.getVersion().getMaxVersion());

			assertEquals(1, icalendar.getTodos().size());
			VTodo todo = icalendar.getTodos().get(0);
			assertEquals("todo summary", todo.getSummary().getValue());

			assertWarnings(0, reader.getWarnings());
		}

		assertNull(reader.readNext());
	}

	@Test
	public void caret_encoding_enabled() throws Exception {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"PRODID;X-TEST=^'test^':prodid\r\n" +
			"VERSION:2.0\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(new StringReader(ical));
		reader.setCaretDecodingEnabled(true);
		reader.registerMarshaller(new TestPropertyMarshaller());

		ICalendar icalendar = reader.readNext();

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertEquals("\"test\"", icalendar.getProductId().getParameter("X-TEST"));
		assertEquals("2.0", icalendar.getVersion().getMaxVersion());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void caret_encoding_disabled() throws Exception {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"PRODID;X-TEST=^'test^':prodid\r\n" +
			"VERSION:2.0\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(new StringReader(ical));
		reader.setCaretDecodingEnabled(false);
		reader.registerMarshaller(new TestPropertyMarshaller());

		ICalendar icalendar = reader.readNext();

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertEquals("^'test^'", icalendar.getProductId().getParameter("X-TEST"));
		assertEquals("2.0", icalendar.getVersion().getMaxVersion());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void missing_vcalendar_component_no_components() throws Exception {
		//@formatter:off
		String ical =
		"PRODID:prodid\r\n" +
		"VERSION:2.0\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(new StringReader(ical));
		ICalendar icalendar = reader.readNext();

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertEquals("2.0", icalendar.getVersion().getMaxVersion());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void missing_vcalendar_component_with_component() throws Exception {
		//@formatter:off
		String ical =
		"PRODID:prodid\r\n" +
		"VERSION:2.0\r\n" +
		"BEGIN:VEVENT\r\n" +
			"SUMMARY:summary\r\n" +
		"END:VEVENT\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(new StringReader(ical));
		ICalendar icalendar = reader.readNext();

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertEquals("2.0", icalendar.getVersion().getMaxVersion());

		assertEquals(1, icalendar.getEvents().size());
		VEvent event = icalendar.getEvents().get(0);

		assertEquals("summary", event.getSummary().getValue());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void missing_end_property() throws Exception {
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

		ICalReader reader = new ICalReader(new StringReader(ical));
		ICalendar icalendar = reader.readNext();

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertEquals("2.0", icalendar.getVersion().getMaxVersion());

		assertEquals(1, icalendar.getEvents().size());
		VEvent event = icalendar.getEvents().get(0);
		assertEquals("summary", event.getSummary().getValue());

		assertEquals(1, icalendar.getTodos().size());
		VTodo todo = icalendar.getTodos().get(0);
		assertEquals("two", todo.getSummary().getValue());

		assertEquals(1, event.getComponents(VTodo.class).size());
		assertEquals("one", event.getComponents(VTodo.class).get(0).getSummary().getValue());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void invalid_end_property() throws Exception {
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

		ICalReader reader = new ICalReader(new StringReader(ical));
		ICalendar icalendar = reader.readNext();

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertEquals("2.0", icalendar.getVersion().getMaxVersion());

		assertEquals(1, icalendar.getEvents().size());
		VEvent event = icalendar.getEvents().get(0);
		assertEquals("summary", event.getSummary().getValue());

		assertWarnings(1, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void experimental_property() throws Exception {
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

		ICalReader reader = new ICalReader(new StringReader(ical));
		ICalendar icalendar = reader.readNext();

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertEquals("2.0", icalendar.getVersion().getMaxVersion());

		assertEquals("one", icalendar.getExperimentalProperty("X-TEST1").getValue());

		assertEquals(2, icalendar.getExperimentalProperties("X-TEST1").size());
		assertEquals("one", icalendar.getExperimentalProperties("X-TEST1").get(0).getValue());
		assertEquals("one point five", icalendar.getExperimentalProperties("X-TEST1").get(1).getValue());

		assertEquals(1, icalendar.getEvents().size());
		VEvent event = icalendar.getEvents().get(0);
		assertEquals("summary", event.getSummary().getValue());

		assertEquals("two", event.getExperimentalProperty("X-TEST2").getValue());

		assertEquals(1, event.getExperimentalProperties("X-TEST2").size());
		assertEquals("two", event.getExperimentalProperties("X-TEST2").get(0).getValue());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void experiemental_property_marshaller() throws Exception {
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

		ICalReader reader = new ICalReader(new StringReader(ical));
		reader.registerMarshaller(new TestPropertyMarshaller());

		ICalendar icalendar = reader.readNext();

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertEquals("2.0", icalendar.getVersion().getMaxVersion());

		assertEquals(2, icalendar.getProperties(TestProperty.class).size());
		assertEquals(Integer.valueOf(1), icalendar.getProperties(TestProperty.class).get(0).getNumber());
		assertEquals(Integer.valueOf(2), icalendar.getProperties(TestProperty.class).get(1).getNumber());

		assertEquals(1, icalendar.getEvents().size());
		VEvent event = icalendar.getEvents().get(0);
		assertEquals("summary", event.getSummary().getValue());

		assertEquals(1, event.getProperties(TestProperty.class).size());
		assertEquals(Integer.valueOf(3), event.getProperties(TestProperty.class).get(0).getNumber());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void experimental_component() throws Exception {
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

		ICalReader reader = new ICalReader(new StringReader(ical));
		ICalendar icalendar = reader.readNext();

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertEquals("2.0", icalendar.getVersion().getMaxVersion());

		assertEquals(1, icalendar.getExperimentalComponents("X-VPARTY").size());
		assertEquals("Johnny D", icalendar.getExperimentalComponents("X-VPARTY").get(0).getExperimentalProperty("X-DJ").getValue());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void experiemental_component_marshaller() throws Exception {
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

		ICalReader reader = new ICalReader(new StringReader(ical));
		reader.registerMarshaller(new PartyMarshaller());
		ICalendar icalendar = reader.readNext();

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertEquals("2.0", icalendar.getVersion().getMaxVersion());

		assertEquals(1, icalendar.getComponents(Party.class).size());
		assertEquals("Johnny D", icalendar.getComponents(Party.class).get(0).getExperimentalProperty("X-DJ").getValue());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void override_property_marshaller() throws Exception {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"PRODID:the product id\r\n" +
			"VERSION:2.0\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(new StringReader(ical));
		reader.registerMarshaller(new MyProductIdMarshaller());
		ICalendar icalendar = reader.readNext();

		assertEquals("THE PRODUCT ID", icalendar.getProductId().getValue());
		assertEquals("2.0", icalendar.getVersion().getMaxVersion());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void override_component_marshaller() throws Exception {
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

		ICalReader reader = new ICalReader(new StringReader(ical));
		reader.registerMarshaller(new MyEventMarshaller());

		ICalendar icalendar = reader.readNext();

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertEquals("2.0", icalendar.getVersion().getMaxVersion());

		assertEquals(0, icalendar.getEvents().size());

		assertEquals(1, icalendar.getComponents().size());
		MyVEvent event = icalendar.getComponents(MyVEvent.class).get(0);
		assertEquals("event summary", event.getProperty(Summary.class).getValue());

		assertWarnings(0, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void invalid_line() throws Exception {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"PRODID:prodid\r\n" +
			"bad-line\r\n" +
			"VERSION:2.0\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(new StringReader(ical));
		reader.registerMarshaller(new MyEventMarshaller());

		ICalendar icalendar = reader.readNext();

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertEquals("2.0", icalendar.getVersion().getMaxVersion());

		assertWarnings(1, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void property_warning() throws Exception {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"PRODID:prodid\r\n" +
			"VERSION:2.0\r\n" +
			"X-TEST:four\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(new StringReader(ical));
		reader.registerMarshaller(new TestPropertyMarshaller());

		ICalendar icalendar = reader.readNext();

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertEquals("2.0", icalendar.getVersion().getMaxVersion());

		assertEquals(1, icalendar.getProperties(TestProperty.class).size());
		assertEquals(Integer.valueOf(4), icalendar.getProperties(TestProperty.class).get(0).getNumber());

		assertWarnings(1, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void warnings_cleared_between_reads() throws Exception {
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

		ICalReader reader = new ICalReader(new StringReader(ical));
		reader.registerMarshaller(new TestPropertyMarshaller());

		{
			ICalendar icalendar = reader.readNext();

			assertEquals("prodid", icalendar.getProductId().getValue());
			assertEquals("2.0", icalendar.getVersion().getMaxVersion());

			assertEquals(1, icalendar.getProperties(TestProperty.class).size());
			assertEquals(Integer.valueOf(4), icalendar.getProperties(TestProperty.class).get(0).getNumber());

			assertWarnings(1, reader.getWarnings());
		}

		{
			ICalendar icalendar = reader.readNext();

			assertEquals("prodid", icalendar.getProductId().getValue());
			assertEquals("2.0", icalendar.getVersion().getMaxVersion());

			assertEquals(1, icalendar.getProperties(TestProperty.class).size());
			assertEquals(Integer.valueOf(4), icalendar.getProperties(TestProperty.class).get(0).getNumber());

			assertWarnings(1, reader.getWarnings());
		}

		assertNull(reader.readNext());
	}

	@Test
	public void skipMeException() throws Exception {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"PRODID:prodid\r\n" +
			"VERSION:2.0\r\n" +
			"X-TEST:one hundred\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(new StringReader(ical));
		reader.registerMarshaller(new TestPropertyMarshaller());

		ICalendar icalendar = reader.readNext();

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertEquals("2.0", icalendar.getVersion().getMaxVersion());

		assertEquals(0, icalendar.getProperties(TestProperty.class).size());
		assertEquals(0, icalendar.getExperimentalProperties().size());

		assertWarnings(1, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void cannotParseException() throws Exception {
		//@formatter:off
		String ical =
		"BEGIN:VCALENDAR\r\n" +
			"PRODID:prodid\r\n" +
			"VERSION:2.0\r\n" +
			"X-TEST:flower\r\n" +
		"END:VCALENDAR\r\n";
		//@formatter:on

		ICalReader reader = new ICalReader(new StringReader(ical));
		reader.registerMarshaller(new TestPropertyMarshaller());

		ICalendar icalendar = reader.readNext();

		assertEquals("prodid", icalendar.getProductId().getValue());
		assertEquals("2.0", icalendar.getVersion().getMaxVersion());

		//parsed as a RawProperty instead
		assertEquals(0, icalendar.getProperties(TestProperty.class).size());
		assertEquals(1, icalendar.getExperimentalProperties().size());
		assertEquals("flower", icalendar.getExperimentalProperty("X-TEST").getValue());

		assertWarnings(1, reader.getWarnings());
		assertNull(reader.readNext());
	}

	@Test
	public void outlook2010() throws Exception {
		ICalReader reader = new ICalReader(getClass().getResourceAsStream("outlook-2010.ics"));
		ICalendar ical = reader.readNext();

		assertEquals(4, ical.getProperties().size());
		assertEquals("-//Microsoft Corporation//Outlook 14.0 MIMEDIR//EN", ical.getProductId().getValue());

		assertEquals("2.0", ical.getVersion().getMaxVersion());
		assertEquals(null, ical.getVersion().getMinVersion());

		assertEquals("REQUEST", ical.getMethod().getValue());
		assertEquals("TRUE", ical.getExperimentalProperty("X-MS-OLK-FORCEINSPECTOROPEN").getValue());

		assertEquals(2, ical.getComponents().size());
		VTimezone timezone = ical.getTimezones().get(0);
		{
			assertEquals(1, timezone.getProperties().size());
			assertEquals("Eastern Standard Time", timezone.getTimezoneIdentifier().getValue());

			assertEquals(2, timezone.getComponents().size());

			StandardTime standard = timezone.getStandardTimes().get(0);
			{
				assertEquals(4, standard.getProperties().size());
				assertDateEquals("16011104T020000", standard.getDateStart().getValue());

				RecurrenceRule rrule = standard.getRecurrenceRule();
				assertEquals(Frequency.YEARLY, rrule.getFrequency());
				assertEquals(Arrays.asList(1), rrule.getByDayPrefixes());
				assertEquals(Arrays.asList(DayOfWeek.SUNDAY), rrule.getByDay());
				assertEquals(Arrays.asList(11), rrule.getByMonth());

				assertEquals(Integer.valueOf(-4), standard.getTimezoneOffsetFrom().getHourOffset());
				assertEquals(Integer.valueOf(0), standard.getTimezoneOffsetFrom().getMinuteOffset());

				assertEquals(Integer.valueOf(-5), standard.getTimezoneOffsetTo().getHourOffset());
				assertEquals(Integer.valueOf(0), standard.getTimezoneOffsetTo().getMinuteOffset());

				assertEquals(0, standard.getComponents().size());
			}

			DaylightSavingsTime daylight = timezone.getDaylightSavingsTime().get(0);
			{
				assertEquals(4, daylight.getProperties().size());
				assertDateEquals("16010311T020000", daylight.getDateStart().getValue());

				RecurrenceRule rrule = daylight.getRecurrenceRule();
				assertEquals(Frequency.YEARLY, rrule.getFrequency());
				assertEquals(Arrays.asList(2), rrule.getByDayPrefixes());
				assertEquals(Arrays.asList(DayOfWeek.SUNDAY), rrule.getByDay());
				assertEquals(Arrays.asList(3), rrule.getByMonth());

				assertEquals(Integer.valueOf(-5), daylight.getTimezoneOffsetFrom().getHourOffset());
				assertEquals(Integer.valueOf(0), daylight.getTimezoneOffsetFrom().getMinuteOffset());

				assertEquals(Integer.valueOf(-4), daylight.getTimezoneOffsetTo().getHourOffset());
				assertEquals(Integer.valueOf(0), daylight.getTimezoneOffsetTo().getMinuteOffset());

				assertEquals(0, daylight.getComponents().size());
			}
		}

		VEvent event = ical.getEvents().get(0);
		{
			assertEquals(24, event.getProperties().size());

			Attendee attendee = event.getAttendees().get(0);
			assertEquals("Doe, John", attendee.getCommonName());
			assertEquals(Role.OPT_PARTICIPANT, attendee.getRole());
			assertEquals(Boolean.FALSE, attendee.getRsvp());
			assertEquals("mailto:johndoe@example.com", attendee.getValue());

			attendee = event.getAttendees().get(1);
			assertEquals("Doe, Jane", attendee.getCommonName());
			assertEquals(Role.CHAIR, attendee.getRole());
			assertEquals(Boolean.TRUE, attendee.getRsvp());
			assertEquals("mailto:janedoe@example.com", attendee.getValue());

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

			assertEquals("mailto:bobsmith@example.com", event.getOrganizer().getValue());
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

		assertNull(reader.readNext());
	}

	private class TestPropertyMarshaller extends ICalPropertyMarshaller<TestProperty> {
		public TestPropertyMarshaller() {
			super(TestProperty.class, "X-TEST");
		}

		@Override
		protected String _writeText(TestProperty property) {
			return property.getNumber().toString();
		}

		@Override
		protected TestProperty _parseText(String value, ICalParameters parameters, List<String> warnings) {
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
				warnings.add("too high");
			} else if (value.equals("one hundred")) {
				throw new SkipMeException("really too high");
			} else {
				throw new CannotParseException("wat");
			}
			prop.setNumber(number);
			return prop;
		}
	}

	private class TestProperty extends ICalProperty {
		private Integer number;

		public Integer getNumber() {
			return number;
		}

		public void setNumber(Integer number) {
			this.number = number;
		}
	}

	private class PartyMarshaller extends ICalComponentMarshaller<Party> {
		public PartyMarshaller() {
			super(Party.class, "X-VPARTY");
		}

		@Override
		public Party newInstance() {
			return new Party();
		}
	}

	private class Party extends ICalComponent {
		//empty
	}

	private class MyProductIdMarshaller extends ICalPropertyMarshaller<ProductId> {

		public MyProductIdMarshaller() {
			super(ProductId.class, "PRODID");
		}

		@Override
		protected String _writeText(ProductId property) {
			return property.getValue();
		}

		@Override
		protected ProductId _parseText(String value, ICalParameters parameters, List<String> warnings) {
			return new ProductId(value.toUpperCase());
		}
	}

	private class MyVEvent extends ICalComponent {
		//empty
	}

	private class MyEventMarshaller extends ICalComponentMarshaller<MyVEvent> {
		public MyEventMarshaller() {
			super(MyVEvent.class, "VEVENT");
		}

		@Override
		public MyVEvent newInstance() {
			return new MyVEvent();
		}
	}

	private static void assertDateEquals(String expected, Date actual) throws ParseException {
		//TODO move to TestUtils

		if (expected.contains("Z")) {
			expected = expected.replace("Z", "+0000");
		}

		SimpleDateFormat df;
		if (expected.contains("T")) {
			if (expected.contains("-") || expected.contains("+")) {
				df = new SimpleDateFormat("yyyyMMdd'T'HHmmssZ");
			} else {
				df = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
			}
		} else {
			df = new SimpleDateFormat("yyyyMMdd");
		}

		assertEquals(df.parse(expected), actual);
	}

	private static void assertIntEquals(int expected, Integer actual) {
		//TODO move to TestUtils
		assertEquals(Integer.valueOf(expected), actual);
	}
}