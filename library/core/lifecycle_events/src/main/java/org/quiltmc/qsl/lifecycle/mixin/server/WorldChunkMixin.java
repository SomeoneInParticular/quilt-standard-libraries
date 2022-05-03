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

package org.quiltmc.qsl.lifecycle.mixin.server;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.quiltmc.qsl.lifecycle.api.server.event.ServerBlockEntityEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;

/**
 * Due to MoJank(tm), we are forced to have a dual-mixin implementation with
 * redundant code. This mixin handles events that occur on a dedicated server,
 * as to avoid client-sided functionality bleeding through and breaking
 * due to server-sided constraints.
 *
 * If `instanceOf ClientServer` did not crash when run on a dedicated server,
 * this could be made common instead, avoiding said redundant code.
 */
@Environment(EnvType.SERVER)
@Mixin(WorldChunk.class)
abstract class WorldChunkMixin {
	/**
	 * When a BlockEntity is about to be loaded, run through all event listeners
	 * registered for BlockEntity load events. Despite the use of the "JUMP"
	 * mixin signature, this will trigger regardless of whether the jump
	 * succeeds.
	 *
	 * <p><b>NOTE (Kalum Ost)</b>: Yes, at time of writing the Minecraft Dev
	 * plugin says that this will not work. It does, turns out, hence the
	 * "InvalidInjectorMethodSignature" suppression. As this is significantly
	 * less frail than FAPIs equivalent while retaining the same functionality,
	 * this will stay until the IntelliJ plugin is patched to account for this
	 * error.
	 */
	@SuppressWarnings("InvalidInjectorMethodSignature")
	@Inject(method = "setBlockEntity", at = @At(value = "JUMP", opcode = Opcodes.IFNULL), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	private void onBlockEntityCreated(BlockEntity blockEntity, CallbackInfo ci, BlockPos __, @Nullable BlockEntity removedBlockEntity) {
		if (blockEntity != removedBlockEntity) {
			this.runBlockEntityLoadEvents(blockEntity);
		}
	}

	/**
	 * When a BlockEntity is about to be unloaded, run through all event
	 * listeners registered for BlockEntity load events. This is the first
	 * possible point of block entity unloading, called when a block entity
	 * is about to be replaced by a new one.
	 */
	@Inject(method = "setBlockEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/BlockEntity;markRemoved()V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	private void onBlockEntityReplacement(BlockEntity blockEntity, CallbackInfo info, BlockPos blockPos, @Nullable BlockEntity removedBlockEntity) {
		this.runBlockEntityUnloadEvents(removedBlockEntity);
	}

	/**
	 * When a BlockEntity is about to be unloaded, run through all event
	 * listeners registered for BlockEntity load events. This is the second
	 * possible point of block entity unloading, called when a block entity
	 * is queried for and found to have been marked as removed by some prior
	 * process
	 *
	 * <p>A redirect is used to avoid a redundant method call, though this may
	 * change in the future if mod devs complain enough; there is an easy inject
	 * point for this use case if needed
	 */
	@Redirect(method = "getBlockEntity(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/chunk/WorldChunk$CreationType;)Lnet/minecraft/block/entity/BlockEntity;", at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;"),
			slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/WorldChunk;createBlockEntity(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/entity/BlockEntity;")))
	private <K, V> Object onRemovedBlockEntityQuery(Map<K, V> map, K key) {
		@Nullable final V removed = map.remove(key);

		this.runBlockEntityUnloadEvents((BlockEntity) removed);

		return removed;
	}

	/**
	 * When a BlockEntity is about to be unloaded, run through all event
	 * listeners registered for BlockEntity load events. This is the third
	 * possible point of block entity unloading, called when an explicit request
	 * to remove a block entity is made
	 */
	@Inject(method = "removeBlockEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/entity/BlockEntity;markRemoved()V"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	private void onExplicitBlockEntityRemove(BlockPos pos, CallbackInfo ci, @Nullable BlockEntity removed) {
		this.runBlockEntityUnloadEvents(removed);
	}

	@Shadow
	public abstract World getWorld();

	/**
	 * Run all load event checks, sanity checking that the world is the correct
	 * type and that the block entity being created actually exists
	 */
	protected void runBlockEntityLoadEvents(BlockEntity removedBlockEntity) {
		if (removedBlockEntity != null && this.getWorld() instanceof ServerWorld) {
			System.out.println("==QSLServer_Load==");
			ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.invoker().onLoad(removedBlockEntity, (ServerWorld) this.getWorld());
		}
	}

	/**
	 * Run all unload event checks, sanity checking that the world is the correct
	 * type and that the block entity being removed had existed
	 */
	protected void runBlockEntityUnloadEvents(BlockEntity removedBlockEntity) {
		if (removedBlockEntity != null && this.getWorld() instanceof ServerWorld) {
			System.out.println("==QSLServer_Unload==");
			ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.invoker().onUnload(removedBlockEntity, (ServerWorld) this.getWorld());
		}
	}
}
