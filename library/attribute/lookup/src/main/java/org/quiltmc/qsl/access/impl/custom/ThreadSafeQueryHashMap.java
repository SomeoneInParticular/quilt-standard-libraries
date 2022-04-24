/*
 * Copyright 2021-2022 QuiltMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.quiltmc.qsl.access.impl.custom;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.quiltmc.qsl.access.api.custom.ThreadSafeQueryMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

public class ThreadSafeQueryHashMap<K, V> implements ThreadSafeQueryMap<K, V> {
	// A logger to report any not-quite-errors to the user
	private static final Logger LOGGER = LoggerFactory.getLogger("qsl-attribute-lookup-api");

	// The backing map which actually does everything for us
	private volatile Map<K, V> contents = Collections.emptyMap();

	@Override
	public V get(K key) {
		Objects.requireNonNull(key, "Key may not be null");

		return contents.get(key);
	}

	@Override
	public synchronized V putIfAbsent(K key, V value) {
		Objects.requireNonNull(key, "Key may not be null");
		Objects.requireNonNull(value, "Value may not be null");

		// Copy-on-write to avoid collision when read or write is occurring simultaneously
		IdentityHashMap<K, V> newCopy = new IdentityHashMap<>(contents.size()+1);
		newCopy.putAll(contents);
		V result = newCopy.putIfAbsent(key, value);
		contents = Collections.unmodifiableMap(newCopy);

		return result;
	}

	@Override
	public synchronized Map<? extends K, ? extends V> putWhereAbsent(@NotNull Map<? extends K, ? extends V> m) {
		// Make sure no one is trying to sneak a null value past us
		if (m.containsKey(null)) {
			String msg = "Null keys cannot be written to a ThreadSafeQueryHashMap";
			throw new NullPointerException(msg);
		} else if (m.containsValue(null)) {
			String msg = "Null values cannot be written to a ThreadSafeQueryHashMap";
			throw new NullPointerException(msg);
		}

		// Put the contents into the map, filtering out entries which already exist within it
		IdentityHashMap<K, V> returnMap = new IdentityHashMap<>();
		IdentityHashMap<K, V> newCopy = new IdentityHashMap<>(Math.max(contents.size(), m.size()));
		newCopy.putAll(contents);
		for (Map.Entry<? extends K, ? extends V> entry : m.entrySet()) {
			K key = entry.getKey();
			// Only add entries if they are associated with new keys
			if (!newCopy.containsKey(key)) {
				newCopy.put(key, entry.getValue());
			}
			// Otherwise, report the existing values for these keys instead
			else {
				V value = newCopy.get(key);
				returnMap.put(key, value);
				String msg = String.format(
						"Tried to register an attribute provider for key '%s' where one already exists. New entry was ignored.",
						key
				);
				LOGGER.warn(msg);
			}
		}

		// Tidy up with the re-assignment and return
		contents = Collections.unmodifiableMap(newCopy);
		return returnMap;
	}

	@Override
	public @Unmodifiable Map<K, V> getBackingMap() {
		return contents;
	}
}
