package org.quiltmc.qsl.access.api;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.qsl.access.impl.ThreadSafeQueryHashMap;

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
public interface ThreadSafeQueryMap<K, V> extends Map<K, V> {
	/**
	 * Create a new instance, defaulting to a HashMap implementation
	 */
	static <K, V> ThreadSafeQueryMap<K, V> create() {
		return new ThreadSafeQueryHashMap<>();
	}

	/**
	 * If the specified key is not already associated with a provider, associate
	 * it with this value and return {@code null}
	 *
	 * If a value already exists for the specified key, returns that value instead
	 *
	 * @throws NullPointerException if the key or value is {@code null}
	 */
	@Nullable
	V putIfAbsent(K key, V value) throws NullPointerException;

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
	 */
	Map<? extends K, ? extends V> putAllIfAbsent(@NotNull Map<? extends K, ? extends V> m);
}
