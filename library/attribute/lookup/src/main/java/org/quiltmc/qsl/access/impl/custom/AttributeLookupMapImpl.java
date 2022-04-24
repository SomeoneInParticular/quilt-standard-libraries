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

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.quiltmc.qsl.access.api.custom.AttributeLookupMap;

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
	public synchronized L getLookup(Identifier id, Class<?> attributeClass, Class<?> contextClass) throws IllegalArgumentException {
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
				"Lookup with id %s is already registered with lookup class %s and context class %s. It cannot be queried with lookup class %s and context class %s.",
				id,
				storedLookup.lookupClass.getCanonicalName(),
				storedLookup.contextClass.getCanonicalName(),
				attributeClass.getCanonicalName(),
				contextClass.getCanonicalName()
		);

		throw new IllegalArgumentException(errorMessage);
	}

	@NotNull
	@Override
	public synchronized Iterator<L> iterator() {
		return lookups.values().stream().map(storedLookup -> storedLookup.accessor).iterator();
	}

	private record StoredLookup<L>(L accessor, Class<?> lookupClass, Class<?> contextClass) {}
}
