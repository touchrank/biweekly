package biweekly.property;

import java.util.List;

import biweekly.component.ICalComponent;

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
 * Defines a to-do task's level of completion.
 * </p>
 * <p>
 * <b>Examples:</b>
 * 
 * <pre>
 * PercentComplete percentComplete = new PercentComplete(50); //50%
 * 
 * VTodo todo = new VTodo();
 * todo.setPercentComplete(50);
 * </pre>
 * 
 * </p>
 * @author Michael Angstadt
 * @rfc 5545 p.88-9
 */
public class PercentComplete extends IntegerProperty {
	/**
	 * Creates a percent complete property.
	 * @param percent the percentage (e.g. "50" for 50%)
	 */
	public PercentComplete(Integer percent) {
		super(percent);
	}

	@Override
	protected void validate(List<ICalComponent> components, List<String> warnings) {
		super.validate(components, warnings);
		if (value != null && (value < 0 || value > 100)) {
			warnings.add("Value should be between 1 and 100 inclusive: " + value);
		}
	}
}
