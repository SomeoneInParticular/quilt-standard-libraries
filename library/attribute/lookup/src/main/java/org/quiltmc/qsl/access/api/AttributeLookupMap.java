package org.quiltmc.qsl.access.api;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.ApiStatus;
import org.quiltmc.qsl.access.impl.AttributeLookupMapImpl;

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
	 * Create a new lookup map for an attribute type
	 *
	 * @param lookupConstructor The constructor used to create attribute accessor instances
	 */
	static <L> AttributeLookupMap<L> create(LookupConstructor<L> lookupConstructor) {
		Objects.requireNonNull(lookupConstructor, "Lookup constructor may not be null");

		return new AttributeLookupMapImpl<>(lookupConstructor);
	}

	L getLookup(Identifier id, Class<?> lookupClass, Class<?> contextClass) throws IllegalAccessException;

	@FunctionalInterface
	interface LookupConstructor<L> {
		/**
		 * Create a new attribute accessor.
		 *
		 * @param identifier The identifier for this accessor.
		 * @param attributeClass The attribute class passed to {@link #getLookup}.
		 * @param contextClass The context class passed to {@link #getLookup}.
		 */
		L get(Identifier identifier, Class<?> attributeClass, Class<?> contextClass);
	}
}
