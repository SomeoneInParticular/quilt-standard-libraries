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

/**
 * A standard API for managing attribute lookup, for attributes bound to
 * item stacks.
 *
 * @param <A> The type of the attribute to query with this lookup.
 * @param <C> The type of context object required to execute the lookup.
 */
public interface ItemAttributeLookup<A, C> {
	static <A, C> ItemAttributeLookup<A, C> get(Identifier id, Class<A> attributeClass, Class<C> contextClass) throws IllegalAccessException {
		return ItemAttributeLookupImpl.get(id, attributeClass, contextClass);
	}

	@Nullable
	A find(ItemStack stack, C context);

	void registerSelf(ItemConvertible... items);

	void registerForItems(ItemAttributeProvider<A, C> provider, ItemConvertible... items);

	void registerFallback(ItemAttributeProvider<A, C> provider);

	Identifier getId();

	Class<A> attributeClass();

	Class<C> contextClass();

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
		 * attribute if found.
		 */
		@Nullable
		A find(ItemStack itemStack, C context);
	}
}
