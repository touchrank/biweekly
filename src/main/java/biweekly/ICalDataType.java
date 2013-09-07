package biweekly;

import java.util.Collection;

import biweekly.util.CaseClasses;

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
 * Defines the data type of a property's value.
 * @author Michael Angstadt
 * @rfc 5545 p.29-50
 */
public class ICalDataType {
	private static final CaseClasses<ICalDataType, String> enums = new CaseClasses<ICalDataType, String>(ICalDataType.class) {
		@Override
		protected ICalDataType create(String value) {
			return new ICalDataType(value);
		}

		@Override
		protected boolean matches(ICalDataType dataType, String value) {
			return dataType.name.equalsIgnoreCase(value);
		}
	};

	public static final ICalDataType BINARY = new ICalDataType("BINARY");
	public static final ICalDataType BOOLEAN = new ICalDataType("BOOLEAN");
	public static final ICalDataType CAL_ADDRESS = new ICalDataType("CAL-ADDRESS");
	public static final ICalDataType DATE = new ICalDataType("DATE");
	public static final ICalDataType DATE_TIME = new ICalDataType("DATE-TIME");
	public static final ICalDataType DURATION = new ICalDataType("DURATION");
	public static final ICalDataType FLOAT = new ICalDataType("FLOAT");
	public static final ICalDataType INTEGER = new ICalDataType("INTEGER");
	public static final ICalDataType PERIOD = new ICalDataType("PERIOD");
	public static final ICalDataType RECUR = new ICalDataType("RECUR");
	public static final ICalDataType TEXT = new ICalDataType("TEXT");
	public static final ICalDataType TIME = new ICalDataType("TIME");
	public static final ICalDataType URI = new ICalDataType("URI");
	public static final ICalDataType UTC_OFFSET = new ICalDataType("UTC-OFFSET");

	private final String name;

	private ICalDataType(String name) {
		this.name = name;
	}

	/**
	 * Gets the name of the data type.
	 * @return the name of the data type (e.g. "text")
	 */
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * Searches for a parameter value that is defined as a static constant in
	 * this class.
	 * @param value the parameter value
	 * @return the object or null if not found
	 */
	public static ICalDataType find(String value) {
		return enums.find(value);
	}

	/**
	 * Searches for a parameter value and creates one if it cannot be found. All
	 * objects are guaranteed to be unique, so they can be compared with
	 * {@code ==} equality.
	 * @param value the parameter value
	 * @return the object
	 */
	public static ICalDataType get(String value) {
		return enums.get(value);
	}

	/**
	 * Gets all of the parameter values that are defined as static constants in
	 * this class.
	 * @return the parameter values
	 */
	public static Collection<ICalDataType> all() {
		return enums.all();
	}
}
