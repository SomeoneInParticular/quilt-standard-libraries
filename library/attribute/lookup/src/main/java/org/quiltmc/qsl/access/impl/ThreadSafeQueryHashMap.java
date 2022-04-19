package org.quiltmc.qsl.access.impl;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.qsl.access.api.ThreadSafeQueryMap;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ThreadSafeQueryHashMap<K, V> implements ThreadSafeQueryMap<K, V> {
	// The backing map which actually does everything for us
	private volatile Map<K, V> contents = new Reference2ReferenceOpenHashMap<>();

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
		Map<K, V> newCopy = new Reference2ReferenceOpenHashMap<>(contents);
		V result = newCopy.put(key, value);
		contents = newCopy;

		return result;
	}

	@Override
	public synchronized V putIfAbsent(K key, V value) {
		Objects.requireNonNull(key, "Key may not be null");
		Objects.requireNonNull(value, "Value may not be null");

		// Copy-on-write to avoid collision when read or write is occurring simultaneously
		Map<K, V> newCopy = new Reference2ReferenceOpenHashMap<>(contents);
		V result = newCopy.putIfAbsent(key, value);
		contents = newCopy;

		return result;
	}

	@Override
	public synchronized V remove(Object key) {
		Objects.requireNonNull(key, "Key may not be null");

		// Copy-on-write to avoid collision when read or write is occurring simultaneously
		Map<K, V> newCopy = new Reference2ReferenceOpenHashMap<>(contents);
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
		Map<K, V> newCopy = new Reference2ReferenceOpenHashMap<>(contents);
		newCopy.putAll(m);
		contents = newCopy;
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
