package twilightforest.tileentity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.Tag;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import twilightforest.block.BlockTFTowerTranslucent;
import twilightforest.block.TFBlocks;

import java.util.Random;

import static twilightforest.TwilightForestMod.prefix;

public class TileEntityTFAntibuilder extends TileEntity implements ITickableTileEntity {
	private static final Tag<Block> BLACKLIST = new BlockTags.Wrapper(prefix("antibuilder_blacklist"));
	private static final int REVERT_CHANCE = 10;

	private final int radius = 4;
	private final int diameter = 2 * radius + 1;
	private final double requiredPlayerRange = 16.0;

	private final Random rand = new Random();

	private int tickCount;
	private boolean slowScan;
	private int ticksSinceChange;

	private BlockState[] blockData;

	public TileEntityTFAntibuilder() {
		super(TFTileEntities.ANTIBUILDER.get());
	}

	@Override
	public void tick() {
		if (this.anyPlayerInRange()) {
			this.tickCount++;

			if (this.world.isRemote) {
				double x = this.pos.getX() + this.world.rand.nextFloat();
				double y = this.pos.getY() + this.world.rand.nextFloat();
				double z = this.pos.getZ() + this.world.rand.nextFloat();
//				this.world.spawnParticle("smoke", x, y, z, 0.0D, 0.0D, 0.0D);
				this.world.addParticle(RedstoneParticleData.REDSTONE_DUST, x, y, z, 0.0D, 0.0D, 0.0D);

				// occasionally make a little red dust line to outline our radius
				if (this.rand.nextInt(10) == 0) {
					makeRandomOutline();
					makeRandomOutline();
					makeRandomOutline();
				}
			} else {

				// new plan, take a snapshot of the world when we are first activated, and then rapidly revert changes
				if (blockData == null && world.isAreaLoaded(this.pos, this.radius)) {
					captureBlockData();
					this.slowScan = true;
				}

				if (blockData != null && (!this.slowScan || this.tickCount % 20 == 0)) {
					if (scanAndRevertChanges()) {
						this.slowScan = false;
						this.ticksSinceChange = 0;
					} else {
						ticksSinceChange++;

						if (ticksSinceChange > 20) {
							this.slowScan = true;
						}
					}
				}
			}
		} else {
			// remove data
			this.blockData = null;
			this.tickCount = 0;
		}
	}


	/**
	 * Display a random one of the 12 possible outlines
	 */
	private void makeRandomOutline() {
		makeOutline(this.rand.nextInt(12));
	}

	/**
	 * Display a specific outline
	 */
	private void makeOutline(int outline) {
		// src
		double sx = this.pos.getX();
		double sy = this.pos.getY();
		double sz = this.pos.getZ();
		// dest
		double dx = this.pos.getX();
		double dy = this.pos.getY();
		double dz = this.pos.getZ();

		switch (outline) {
			case 0:
			case 8:
				sx -= radius;
				dx += radius + 1;
				sz -= radius;
				dz -= radius;
				break;
			case 1:
			case 9:
				sx -= radius;
				dx -= radius;
				sz -= radius;
				dz += radius + 1;
				break;
			case 2:
			case 10:
				sx -= radius;
				dx += radius + 1;
				sz += radius + 1;
				dz += radius + 1;
				break;
			case 3:
			case 11:
				sx += radius + 1;
				dx += radius + 1;
				sz -= radius;
				dz += radius + 1;
				break;
			case 4:
				sx -= radius;
				dx -= radius;
				sz -= radius;
				dz -= radius;
				break;
			case 5:
				sx += radius + 1;
				dx += radius + 1;
				sz -= radius;
				dz -= radius;
				break;
			case 6:
				sx += radius + 1;
				dx += radius + 1;
				sz += radius + 1;
				dz += radius + 1;
				break;
			case 7:
				sx -= radius;
				dx -= radius;
				sz += radius + 1;
				dz += radius + 1;
				break;
		}

		switch (outline) {
			case 0:
			case 1:
			case 2:
			case 3:
				sy += radius + 1;
				dy += radius + 1;
				break;
			case 4:
			case 5:
			case 6:
			case 7:
				sy -= radius;
				dy += radius + 1;
				break;
			case 8:
			case 9:
			case 10:
			case 11:
				sy -= radius;
				dy -= radius;
				break;
		}

		if (rand.nextBoolean()) {
			drawParticleLine(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, dx, dy, dz);
		} else {
			drawParticleLine(sx, sy, sz, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
		}
		drawParticleLine(sx, sy, sz, dx, dy, dz);
	}

	private void drawParticleLine(double srcX, double srcY, double srcZ, double destX, double destY, double destZ) {
		// make particle trail
		int particles = 16;
		for (int i = 0; i < particles; i++) {
			double trailFactor = i / (particles - 1.0D);

			double tx = srcX + (destX - srcX) * trailFactor + rand.nextFloat() * 0.005;
			double ty = srcY + (destY - srcY) * trailFactor + rand.nextFloat() * 0.005;
			double tz = srcZ + (destZ - srcZ) * trailFactor + rand.nextFloat() * 0.005;
			world.addParticle(RedstoneParticleData.REDSTONE_DUST, tx, ty, tz, 0, 0, 0);
		}
	}

	private boolean scanAndRevertChanges() {
		int index = 0;
		boolean reverted = false;

		for (int x = -radius; x <= radius; x++) {
			for (int y = -radius; y <= radius; y++) {
				for (int z = -radius; z <= radius; z++) {
					BlockState stateThere = world.getBlockState(pos.add(x, y, z));

					if (blockData[index].getBlock() != stateThere.getBlock()) {
						if (revertBlock(pos.add(x, y, z), stateThere, blockData[index])) {
							reverted = true;
						} else {
							blockData[index] = stateThere;
						}
					}

					index++;
				}
			}
		}

		return reverted;
	}

	private boolean revertBlock(BlockPos pos, BlockState stateThere, BlockState replaceWith) {
		if (stateThere.getBlock() == Blocks.AIR && !replaceWith.getMaterial().blocksMovement()) {
			return false;
		}
		if (stateThere.getBlockHardness(world, pos) < 0 || isUnrevertable(stateThere, replaceWith)) {
			return false;
		} else if (this.rand.nextInt(REVERT_CHANCE) == 0) {
			// don't revert everything instantly
			if (replaceWith.getBlock() != Blocks.AIR) {
				replaceWith = TFBlocks.reverter_replacement.get().getDefaultState();
			}

			world.setBlockState(pos, replaceWith, 2);

			// play a little animation
			if (stateThere.getBlock() == Blocks.AIR) {
				world.playEvent(2001, pos, Block.getStateId(replaceWith));
			} else if (replaceWith.getBlock() == Blocks.AIR) {
				world.playEvent(2001, pos, Block.getStateId(stateThere));
				//stateThere.getBlock().dropBlockAsItem(world, pos, stateThere, 0);
			}
		}

		return true;
	}

	private boolean isUnrevertable(BlockState stateThere, BlockState replaceWith) {
		if (stateThere.getBlock() instanceof BlockTFTowerTranslucent || replaceWith.getBlock() instanceof BlockTFTowerTranslucent) {
			return true;
		}
		if (stateThere.getBlock() instanceof BlockTFTowerTranslucent && stateThere.getBlock() != TFBlocks.reverter_replacement.get()
				|| (replaceWith.getBlock() instanceof BlockTFTowerTranslucent && stateThere.getBlock() != TFBlocks.reverter_replacement.get())) {
			return true;
		}

		return BLACKLIST.contains(stateThere.getBlock()) || BLACKLIST.contains(replaceWith.getBlock());
	}

	private void captureBlockData() {
		blockData = new BlockState[diameter * diameter * diameter];

		int index = 0;

		for (int x = -radius; x <= radius; x++) {
			for (int y = -radius; y <= radius; y++) {
				for (int z = -radius; z <= radius; z++) {
					blockData[index] = world.getBlockState(pos.add(x, y, z));
					index++;
				}
			}
		}
	}

	private boolean anyPlayerInRange() {
		return this.world.isPlayerWithin(this.pos.getX() + 0.5D, this.pos.getY() + 0.5D, this.pos.getZ() + 0.5D, this.requiredPlayerRange);
	}
}
