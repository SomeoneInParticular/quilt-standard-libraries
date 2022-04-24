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

package org.quiltmc.qsl.access.api.custom;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.quiltmc.qsl.access.impl.custom.ThreadSafeQueryHashMap;

import java.util.Map;

/**
 * A fast thread-safe-on-query copy-on-write map, built to allow for very fast
 * simultaneous reads without concern of thread-locking. Writes are very
 * expensive, however, and should be kept to a minimum. This is accomplished
 * by using a backing map which is recreated every time a new write is requested,
 * allowing queries occurring at the same time to still access the (not yet
 * recreated) map.
 *
 * Keys are compared by reference, *not* equality (that is, use '{@code ==}'
 * rather than {@link Object#equals}). This is also the case for values, though
 * this should not generally cause any differences in coding requirements.
 *
 * @param <K> The key type of the map, evaluated by reference ({@code ==})
 * @param <V> The value type of the map
 */
@ApiStatus.Experimental
public interface ThreadSafeQueryMap<K, V> {
	/**
	 * Create a new instance, defaulting to a HashMap implementation
	 */
	static <K, V> ThreadSafeQueryMap<K, V> create() {
		return new ThreadSafeQueryHashMap<>();
	}

	/**
	 * Return the provider associated with the specified key within this map,
	 * if one exists. Otherwise returns {@code null}
	 *
	 * @throws NullPointerException if the key is null
	 */
	V get(K key);

	/**
	 * If the specified key is not already associated with a provider, associate
	 * it with this value and return {@code null}
	 *
	 * If a value already exists for the specified key, returns that value instead
	 *
	 * @throws NullPointerException if the key or value is {@code null}
	 */
	@Nullable
	V putIfAbsent(K key, V value);

	/**
	 * Add multiple key-value pairs to the map simultaneously, returning a map
	 * containing the key-value pairs representing elements already present
	 * within this map.
	 *
	 * This should help bypass the very slow write speed of this implementation,
	 * though this method should still be used sparingly.
	 *
	 * @throws NullPointerException if any of the keys or values within the
	 * provided map are {@code null}
	 * @return A map containing the keys that already existed within the map,
	 *  and the values they were associated with
	 */
	Map<? extends K, ? extends V> putWhereAbsent(@NotNull Map<? extends K, ? extends V> m);

	/**
	 * Get the unmodifiable view of the contents of this map, which can be used
	 * in methods that require an object implementing the {@link Map} interface
	 */
	@Unmodifiable Map<K, V> getBackingMap();
}
