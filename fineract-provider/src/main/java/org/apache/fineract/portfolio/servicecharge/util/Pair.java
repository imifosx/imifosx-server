/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.servicecharge.util;

public class Pair<U, V> {
	private final U first; // first field of a Pair
	private final V second; // second field of a Pair

	// Constructs a new Pair with specified values
	private Pair(U first, V second) {
		this.first = first;
		this.second = second;
	}

	@Override
	// Checks specified object is "equal to" current object or not
	public boolean equals(Object o) {
		if (this == o)
			return true;

		if (o == null || getClass() != o.getClass())
			return false;

		Pair<?, ?> pair = (Pair<?, ?>) o;

		// call equals() method of the underlying objects
		if (!first.equals(pair.first))
			return false;
		return second.equals(pair.second);
	}

	@Override
	// Computes hash code for an object to support hash tables
	public int hashCode() {
		// use hash codes of the underlying objects
		return 31 * first.hashCode() + second.hashCode();
	}

	@Override
	public String toString() {
		return "(" + first + ", " + second + ")";
	}

	public U getFirst() {
		return first;
	}

	public V getSecond() {
		return second;
	}

	// Factory method for creating a Typed Pair instance
	public static <U, V> Pair<U, V> of(U a, V b) {
		// calls private constructor
		return new Pair<>(a, b);
	}
}
