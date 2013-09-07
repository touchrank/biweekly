package biweekly.property;

import java.util.Arrays;
import java.util.Collection;

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
 * Defines the level of sensitivity of the iCalendar data. If not specified, the
 * data should be considered "public".
 * </p>
 * <p>
 * <b>Examples:</b>
 * 
 * <pre>
 * //creating a new property
 * Classification classification = Classification.public_();
 * 
 * if (classification.isPublic()) {
 * 	//its value is &quot;PUBLIC&quot;
 * }
 * </pre>
 * 
 * </p>
 * @author Michael Angstadt
 * @rfc 5545 p.82-3
 */
public class Classification extends EnumProperty {
	private static final String PUBLIC = "PUBLIC";
	private static final String PRIVATE = "PRIVATE";
	private static final String CONFIDENTIAL = "CONFIDENTIAL";

	/**
	 * Creates a new classification property. Use the static factory methods to
	 * create a property with a standard classification level.
	 * @param classification the classification level (e.g. "PUBLIC")
	 */
	public Classification(String classification) {
		super(classification);
	}

	/**
	 * Creates a "public" classification property.
	 * @return the property
	 */
	public static Classification public_() {
		return create(PUBLIC);
	}

	/**
	 * Determines if the classification level is "public".
	 * @return true if it's "public", false if not
	 */
	public boolean isPublic() {
		return is(PUBLIC);
	}

	/**
	 * Creates a "private" classification property.
	 * @return the property
	 */
	public static Classification private_() {
		return create(PRIVATE);
	}

	/**
	 * Determines if the classification level is "private".
	 * @return true if it's "private", false if not
	 */
	public boolean isPrivate() {
		return is(PRIVATE);
	}

	/**
	 * Creates a "confidential" classification property.
	 * @return the property
	 */
	public static Classification confidential() {
		return create(CONFIDENTIAL);
	}

	/**
	 * Determines if the classification level is "confidential".
	 * @return true if it's "confidential", false if not
	 */
	public boolean isConfidential() {
		return is(CONFIDENTIAL);
	}

	private static Classification create(String classification) {
		return new Classification(classification);
	}

	@Override
	protected Collection<String> getStandardValues() {
		return Arrays.asList(PUBLIC, PRIVATE, CONFIDENTIAL);
	}
}
