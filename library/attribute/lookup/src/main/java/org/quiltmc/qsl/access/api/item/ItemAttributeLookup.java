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

package org.quiltmc.qsl.access.api.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.qsl.access.impl.item.ItemAttributeLookupImpl;

import java.util.Map;

/**
 * A standard API for managing attribute lookup, for attributes bound to
 * item stacks.
 *
 * @param <A> The type of the attribute to query with this lookup.
 * @param <C> The type of context object required to execute the lookup.
 */
public interface ItemAttributeLookup<A, C> {
	/**
	 * Gets a {@link ItemAttributeLookup} instance associated with the specified identifier
	 * @param id The unique identifier of the lookup
	 * @param attributeClass The attribute class the lookup should query for
	 * @param contextClass The context required for the lookup to query successfully
	 * @return The unique lookup associated with the specified identifier
	 * @throws IllegalArgumentException If a lookup with matching the identifier
	 *  exists, but for an incorrect {@code attributeClass} or {@code contextClass}
	 */
	static <A, C> ItemAttributeLookup<A, C> get(Identifier id, Class<A> attributeClass, Class<C> contextClass) throws IllegalArgumentException {
		return ItemAttributeLookupImpl.get(id, attributeClass, contextClass);
	}

	/**
	 * Attempt to query the ItemStack for the attribute handled by this lookup
	 *
	 * <p>Note that the attribute that is returned may not allow the original
	 * ItemStack to be modified. Attribute authors are encouraged to create
	 * documentation to establish whether this is the case or not.
	 * <br>Likewise, providers may capture a reference to the original stack,
	 * but should not modify it directly
	 *
	 * @param stack The {@code ItemStack} to query for the attribute
	 * @param context The context of the query, as defined by the type parameter {@code C}
	 * @return The retrieved Attribute instance, or {@code null} if no such
	 *  instance could be queried
	 */
	@Nullable
	A find(ItemStack stack, C context);

	/**
	 * Mark the specified items as being capable of holding the Attribute
	 * associated with this Lookup, for querying via this Lookup
	 *
	 * @param items The item types to associate with the Attribute type
	 * @return A map containing the items already registered in the map, and
	 *  the providers they are associated with.
	 * @throws IllegalArgumentException if the {@code Attribute} is not
	 *  assignable from the class of one of the items provided
	 */
	Map<? extends Item, ? extends ItemAttributeProvider<A, C>> registerSelf(ItemConvertible... items) throws IllegalArgumentException;

	/**
	 * Mark specified items as being capable of holding a specific attribute,
	 * to be handled using the specified {@code ItemAttributeProvider}
	 *
	 * @param provider The provider to register with
	 * @param items    The items to register
	 * @return A map containing the items and their associated providers for
	 *  items already registered within the map
	 */
	Map<? extends Item, ? extends ItemAttributeProvider<A, C>> registerForItems(ItemAttributeProvider<A, C> provider, ItemConvertible... items);

	/**
	 * Register a provider to be used when a standard provider cannot be found
	 * with a provided item. Providers registered this way are checked for all
	 * queries which fail to a provider for the requested item via the usual way
	 * @param provider The provider to register
	 */
	void registerFallback(ItemAttributeProvider<A, C> provider);

	/**
	 * Get the identifier for this lookup
	 */
	Identifier getId();

	/**
	 * Get the attribute class for this lookup
	 */
	Class<A> attributeClass();

	/**
	 * Get the context class for this lookup
	 */
	Class<C> contextClass();

	/**
	 * Return the registered provider associated with the provided item, if one exists
	 * Queries should go through {@link #find}, only use this for the purposes of inspection!
	 */
	@Nullable
	ItemAttributeProvider<A, C> getProvider(Item item);

	@FunctionalInterface
	interface ItemAttributeProvider<A, C> {
		/**
		 * Return an attribute of type {@code A} from the  given item stack with
		 * the given context, if it is available. Returns {@code null} otherwise.
		 *
		 * <p>Note: An attribute may or may not allow the item stack to be modified
		 * by the returned instance. Attribute authors are strongly encouraged
		 * to document this behavior so that implementors can refer to the
		 * attribute's documentation.
		 *
		 * <br>While providers may capture a reference to the stack, it is
		 * expected that they do not modify it directly.
		 *
		 * @param itemStack The item stack to query the attribute from.
		 * @param context Additional context passed to the query.
		 * @return An attribute of type {@code A}, or {@code null} if no
		 *  attribute was found.
		 */
		@Nullable
		A find(ItemStack itemStack, C context);
	}
}
