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

package org.quiltmc.qsl.access.impl.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.qsl.access.api.custom.AttributeLookupMap;
import org.quiltmc.qsl.access.api.custom.ThreadSafeQueryMap;
import org.quiltmc.qsl.access.api.item.ItemAttributeLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public class ItemAttributeLookupImpl<A, C> implements ItemAttributeLookup<A, C> {
	// The central map managing the item lookups for us
	private static final AttributeLookupMap<ItemAttributeLookup<?, ?>> LOOKUPS = AttributeLookupMap.create(ItemAttributeLookupImpl::new);

	// The attributes of the lookup
	private final Identifier id;
	private final Class<A> attributeClass;
	private final Class<C> contextClass;

	// Provider tracking objects, both for "ideal" and fallback cases
	private final ThreadSafeQueryMap<Item, ItemAttributeProvider<A, C>> providerMap = ThreadSafeQueryMap.create();
	private final List<ItemAttributeProvider<A, C>> fallbackProviders = new CopyOnWriteArrayList<>();

	// A logger to report any not-quite-errors to the user
	private static final Logger LOGGER = LoggerFactory.getLogger("qsl-attribute-lookup-api/item");

	@SuppressWarnings("unchecked")
	public static <A, C> ItemAttributeLookup<A, C> get(Identifier id, Class<A> attributeClass, Class<C> contextClass) throws IllegalAccessException {
		return (ItemAttributeLookup<A, C>) LOOKUPS.getLookup(id, attributeClass, contextClass);
	}

	@SuppressWarnings("unchecked")
	private ItemAttributeLookupImpl(Identifier id, Class<?> attributeClass, Class<?> contextClass) {
		this.id = id;
		this.attributeClass = (Class<A>) attributeClass;
		this.contextClass = (Class<C>) contextClass;
	}

	@Override
	public @Nullable A find(ItemStack stack, C context) {
		// Sanity check the stack
		Objects.requireNonNull(stack, "ItemStack may not be null");

		// Try to find a provider in the primary lookup map
		ItemAttributeProvider<A, C> provider = providerMap.get(stack.getItem());
		if (provider != null) {
			A instance = provider.find(stack, context);
			if (instance != null) {
				return instance;
			}
		}

		// If the prior failed, try each of the fallbacks
		for (ItemAttributeProvider<A, C> fallbackProvider : fallbackProviders) {
			A instance = fallbackProvider.find(stack, context);

			if (instance != null) {
				return instance;
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void registerSelf(ItemConvertible... items) {
		for (ItemConvertible itemConvertible : items) {
			Item item = itemConvertible.asItem();
			// Raise an error if the item-like objects are not compatible with the attribute class
			if (!attributeClass.isAssignableFrom(item.getClass())) {
				String errorMessage = String.format(
						"Failed to register self-implementing items. Attribute class %s is not assignable from item class %s.",
						attributeClass.getCanonicalName(),
						item.getClass().getCanonicalName()
				);
				throw new IllegalArgumentException(errorMessage);
			}
		}

		registerForItems((stack, context) -> (A) stack.getItem(), items);
	}

	@Override
	public void registerForItems(ItemAttributeProvider<A, C> provider, ItemConvertible... items) {
		// Sanity check some requirements for the to-be-registered items
		Objects.requireNonNull(provider, "ItemApiProvider may not be null");

		if (items.length == 0) {
			throw new IllegalArgumentException("At least one ItemConvertable must be provided when registering an ItemAttributeProvider instance.");
		}

		// Register the new item/attribute-providers, skipping over duplicates
		for (ItemConvertible itemConvertible : items) {
			Item item = itemConvertible.asItem();
			Objects.requireNonNull(item, "ItemConvertable should not produce a null value when registering them with an ItemAttributeProvider");
			if (providerMap.putIfAbsent(item, provider) != null) {
				String msg = String.format(
						"Tried to register an attribute provider for item '%s' where one already exists. New entry was ignored.",
						Registry.ITEM.getId(item)
				);
				LOGGER.warn(msg);
			}
		}
	}

	@Override
	public void registerFallback(ItemAttributeProvider<A, C> provider) {
		Objects.requireNonNull(provider, "ItemAttributeProvider may not be null");

		fallbackProviders.add(provider);
	}

	@Override
	public Identifier getId() {
		return id;
	}

	@Override
	public Class<A> attributeClass() {
		return attributeClass;
	}

	@Override
	public Class<C> contextClass() {
		return contextClass;
	}

	@Override
	public @Nullable ItemAttributeProvider<A, C> getProvider(Item item) {
		return providerMap.get(item);
	}
}
