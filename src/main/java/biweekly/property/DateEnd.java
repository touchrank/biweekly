package biweekly.property;

import java.util.Date;

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
 * <p>
 * Defines the date that an event or free/busy component ends.
 * </p>
 * <p>
 * <b>Examples:</b>
 * 
 * <pre>
 * //date and time
 * Date datetime = ...
 * DateEnd dtend = new DateEnd(datetime);
 * 
 * //date
 * Date date = ...
 * DateEnd dtend = new DateEnd(date, false);
 * 
 * //with timezone 
 * Date datetime = ... 
 * DateEnd dtend = new DateEnd(datetime); 
 * dtend.setTimezoneId("America/New_York");
 * </pre>
 * 
 * </p>
 * @author Michael Angstadt
 * @rfc 5545 p.95-6
 */
public class DateEnd extends DateOrDateTimeProperty {
	/**
	 * Creates a date end property.
	 * @param endDate the end date
	 */
	public DateEnd(Date endDate) {
		this(endDate, true);
	}

	/**
	 * Creates a date end property.
	 * @param endDate the end date
	 * @param hasTime true to include the time component of the date, false not
	 * to
	 */
	public DateEnd(Date endDate, boolean hasTime) {
		super(endDate, hasTime);
	}
}
