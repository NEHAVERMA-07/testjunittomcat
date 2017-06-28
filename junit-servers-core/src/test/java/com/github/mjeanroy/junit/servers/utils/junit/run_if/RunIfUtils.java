/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014-2017 <mickael.jeanroy@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.github.mjeanroy.junit.servers.utils.junit.run_if;

import java.lang.reflect.Method;

/**
 * Static utilities for {@link RunIf} annotation.
 */
final class RunIfUtils {

	// Ensure non instantiation.
	private RunIfUtils() {
	}

	/**
	 * Check if class should be ignored according to {@link RunIf} condition.
	 *
	 * @param klass The class to check.
	 * @return {@code true} if class must be ignored, {@code false} otherwise.
	 */
	static boolean isIgnored(Class<?> klass) {
		return isIgnored(klass.getAnnotation(RunIf.class));
	}

	/**
	 * Check if method should be ignored according to {@link RunIf} condition.
	 *
	 * @param method The method to check.
	 * @return {@code true} if class must be ignored, {@code false} otherwise.
	 */
	static boolean isIgnored(Method method) {
		return isIgnored(method.getAnnotation(RunIf.class));
	}

	private static boolean isIgnored(RunIf runIf) {
		if (runIf == null) {
			return false;
		}

		Class<? extends RunIfCondition> conditionClass = runIf.value();

		try {
			RunIfCondition condition = conditionClass.newInstance();
			return !condition.apply();
		} catch (IllegalAccessException | InstantiationException ex) {
			// Fail with a non-checked exception.
			throw new AssertionError(ex);
		}
	}
}
