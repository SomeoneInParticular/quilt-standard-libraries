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

import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.qsl.access.api.custom.ThreadSafeQueryMap;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ThreadSafeQueryHashMap<K, V> implements ThreadSafeQueryMap<K, V> {
	// The backing map which actually does everything for us
	private volatile Reference2ReferenceMap<K, V> contents = new Reference2ReferenceOpenHashMap<>();

	@Override
	public int size() {
		return contents.size();
	}

	@Override
	public boolean isEmpty() {
		return contents.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		Objects.requireNonNull(key, "Key may not be null");

		return contents.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		Objects.requireNonNull(value, "Value may not be null");

		return contents.containsValue(value);
	}

	@Override
	public V get(Object key) {
		Objects.requireNonNull(key, "Key may not be null");

		return contents.get(key);
	}

	@Nullable
	@Override
	public synchronized V put(K key, V value) {
		Objects.requireNonNull(key, "Key may not be null");
		Objects.requireNonNull(value, "Value may not be null");

		// Copy-on-write to avoid collision when read or write is occurring simultaneously
		Reference2ReferenceMap<K, V> newCopy = new Reference2ReferenceOpenHashMap<>(contents);
		V result = newCopy.put(key, value);
		contents = newCopy;

		return result;
	}

	@Override
	public synchronized V putIfAbsent(K key, V value) {
		Objects.requireNonNull(key, "Key may not be null");
		Objects.requireNonNull(value, "Value may not be null");

		// Copy-on-write to avoid collision when read or write is occurring simultaneously
		Reference2ReferenceMap<K, V> newCopy = new Reference2ReferenceOpenHashMap<>(contents);
		V result = newCopy.putIfAbsent(key, value);
		contents = newCopy;

		return result;
	}

	@Override
	public synchronized V remove(Object key) {
		Objects.requireNonNull(key, "Key may not be null");

		// Copy-on-write to avoid collision when read or write is occurring simultaneously
		Reference2ReferenceMap<K, V> newCopy = new Reference2ReferenceOpenHashMap<>(contents);
		V result = newCopy.remove(key);
		contents = newCopy;

		return result;
	}

	@Override
	public synchronized void putAll(@NotNull Map<? extends K, ? extends V> m) {
		// Make sure no one is trying to sneak a null value past us
		if (m.containsKey(null)) {
			String msg = "Null keys cannot be written to a ThreadSafeQueryHashMap";
			throw new NullPointerException(msg);
		} else if (m.containsValue(null)) {
			String msg = "Null values cannot be written to a ThreadSafeQueryHashMap";
			throw new NullPointerException(msg);
		}

		// Copy-on-write to avoid collision when read or write is occurring simultaneously
		Reference2ReferenceMap<K, V> newCopy = new Reference2ReferenceOpenHashMap<>(contents);
		newCopy.putAll(m);
		contents = newCopy;
	}

	@Override
	public synchronized Map<? extends K, ? extends V> putAllIfAbsent(@NotNull Map<? extends K, ? extends V> m) {
		// Make sure no one is trying to sneak a null value past us
		if (m.containsKey(null)) {
			String msg = "Null keys cannot be written to a ThreadSafeQueryHashMap";
			throw new NullPointerException(msg);
		} else if (m.containsValue(null)) {
			String msg = "Null values cannot be written to a ThreadSafeQueryHashMap";
			throw new NullPointerException(msg);
		}

		// Put the contents into the map, filtering out entries which already exist within it
		Reference2ReferenceMap<K, V> returnMap = new Reference2ReferenceOpenHashMap<>();
		Reference2ReferenceMap<K, V> newCopy = new Reference2ReferenceOpenHashMap<>(contents);
		for (Reference2ReferenceMap.Entry<K, V> entry : Reference2ReferenceMaps.fastIterable(contents)) {
			K key = entry.getKey();
			// Only add entries if they are associated with new keys
			if (!newCopy.containsKey(key)) {
				newCopy.put(key, entry.getValue());
			}
			// Otherwise, report the existing values for these keys instead
			else {
				V value = newCopy.get(key);
				returnMap.put(key, value);
			}
		}

		// Tidy up with the re-assignment and return
		contents = newCopy;
		return returnMap;
	}

	@Override
	public synchronized void clear() {
		// Copy-on-write to avoid collision when read or write is occurring simultaneously
		contents = new Reference2ReferenceOpenHashMap<>();
	}

	@NotNull
	@Override
	public Set<K> keySet() {
		return contents.keySet();
	}

	@NotNull
	@Override
	public Collection<V> values() {
		return contents.values();
	}

	@NotNull
	@Override
	public Set<Entry<K, V>> entrySet() {
		return contents.entrySet();
	}
}
