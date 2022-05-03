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

package org.quiltmc.qsl.lifecycle.api.server.event;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import org.quiltmc.qsl.base.api.event.Event;

public class ServerBlockEntityEvents {

	/**
	 * An event called whenever a Block Entity is loaded on the server
	 *
	 * <p> Note that this call occurs after the entity finishes loading, not
	 * before or during!
	 */
	public static final Event<Load> BLOCK_ENTITY_LOAD = Event.create(Load.class, callbacks -> ((blockEntity, world) -> {
		for (Load callback : callbacks) {
			callback.onLoad(blockEntity, world);
		}
	}));

	/**
	 *
	 * An event called whenever a Block Entity is unloaded on the server
	 *
	 * <p> Note that this call occurs before the entity is done unloading, not
	 * after or during!
	 */
	public static final Event<Unload> BLOCK_ENTITY_UNLOAD = Event.create(Unload.class, callbacks -> ((blockEntity, world) -> {
		for (Unload callback : callbacks) {
			callback.onUnload(blockEntity, world);
		}
	}));

	@FunctionalInterface
	public interface Load {
		void onLoad(BlockEntity blockEntity, ServerWorld world);
	}

	@FunctionalInterface
	public interface Unload {
		void onUnload(BlockEntity blockEntity, ServerWorld world);
	}
}
