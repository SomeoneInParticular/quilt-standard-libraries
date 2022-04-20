package org.quiltmc.qsl.access.custom.impl;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.quiltmc.qsl.access.custom.api.AttributeLookupMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * @param <L> The type of the lookup handled by the map.
 */
public final class AttributeLookupMapImpl<L> implements AttributeLookupMap<L> {
	private final Map<Identifier, StoredLookup<L>> lookups = new HashMap<>();
	private final LookupConstructor<L> lookupConstructor;

	public AttributeLookupMapImpl(LookupConstructor<L> lookupConstructor) {
		this.lookupConstructor = lookupConstructor;
	}

	@Override
	public synchronized L getLookup(Identifier id, Class<?> attributeClass, Class<?> contextClass) throws IllegalAccessException {
		Objects.requireNonNull(id, "Lookup ID may not be null");
		Objects.requireNonNull(attributeClass, "Attribute class may not be null");
		Objects.requireNonNull(contextClass, "Context class may not be null");

		StoredLookup<L> storedLookup = lookups.computeIfAbsent(id,
				newId -> new StoredLookup<>(lookupConstructor.get(newId, attributeClass, contextClass), attributeClass, contextClass)
		);

		if (storedLookup.lookupClass == attributeClass && storedLookup.contextClass == contextClass) {
			return storedLookup.accessor;
		}

		String errorMessage = String.format(
				"Lookup with id %s is already registered with lookup class %s and context class %s. It can't be registered with lookup class %s and context class %s.",
				id,
				storedLookup.lookupClass.getCanonicalName(),
				storedLookup.contextClass.getCanonicalName(),
				attributeClass.getCanonicalName(),
				contextClass.getCanonicalName()
		);

		throw new IllegalAccessException(errorMessage);
	}

	@NotNull
	@Override
	public synchronized Iterator<L> iterator() {
		return lookups.values().stream().map(storedLookup -> storedLookup.accessor).iterator();
	}

	private record StoredLookup<L>(L accessor, Class<?> lookupClass, Class<?> contextClass) {}
}
