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

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.quiltmc.qsl.access.impl.custom.AttributeLookupMapImpl;

import java.util.Objects;

/**
 * A map for handling the lookups for custom attributes given a specific context.
 *
 * Attributes are the equivalents of an API in Fabric API's API-lookup-API, and
 * can be treated identically for most purposes
 *
 * WIP; Breaking changes are to be expected
 *
 * @param <L> The type of attribute lookup handled by this map. Each lookup
 *            handles a single combination of an attribute and the context
 *            needed to query it.
 */
@ApiStatus.Experimental
public interface AttributeLookupMap<L> extends Iterable<L> {
	/**
	 * Create a new lookup map for an attribute type {@code <L>}
	 *
	 * @param lookupConstructor The constructor used to create attribute lookup instances
	 */
	static <L> AttributeLookupMap<L> create(LookupConstructor<L> lookupConstructor) {
		Objects.requireNonNull(lookupConstructor, "Lookup constructor may not be null");

		return new AttributeLookupMapImpl<>(lookupConstructor);
	}

	/**
	 * Retrieves the attribute lookup associated with an identifier
	 *
	 * @param id The identifier of the lookup
	 * @param attributeClass The class of the attribute the lookup handles
	 * @param contextClass The class of context the lookup requires to function
	 * @return The unique lookup associated with the parameters above
	 * @throws IllegalAccessException if one of the arguments is {@code null}
	 */
	L getLookup(Identifier id, Class<?> attributeClass, Class<?> contextClass) throws IllegalAccessException;

	@FunctionalInterface
	interface LookupConstructor<L> {
		/**
		 * Create a new attribute lookup.
		 *
		 * @param identifier The identifier for this lookup.
		 * @param attributeClass The attribute class passed to {@link #getLookup}.
		 * @param contextClass The context class passed to {@link #getLookup}.
		 */
		L get(Identifier identifier, Class<?> attributeClass, Class<?> contextClass);
	}
}
