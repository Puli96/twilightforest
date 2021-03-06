package twilightforest.biomes;

import net.minecraft.util.math.MathHelper;
import twilightforest.TFFeature;

import java.util.Random;

public class TFBiomeEnchantedForest extends TFBiomeBase {

	private final Random colorRNG;

	public TFBiomeEnchantedForest(Builder props) {
		super(props);
		colorRNG = new Random();

		TFBiomeDecorator.addWoodRoots(this);
		TFBiomeDecorator.addOres(this);
		TFBiomeDecorator.addClayDisks(this, 1);
		TFBiomeDecorator.addLakes(this);
		TFBiomeDecorator.addRuins(this);
		TFBiomeDecorator.addSprings(this);
		TFBiomeDecorator.addPlantRoots(this);
		TFBiomeDecorator.addTorchberries(this);
		TFBiomeDecorator.addCanopy(this);
		TFBiomeDecorator.addMultipleTrees(this, TFBiomeDecorator.ENCHANTED_TREES_CONFIG, 10);
		TFBiomeDecorator.addVines(this, 20);
		TFBiomeDecorator.addTwilightGrass(this, TFBiomeDecorator.ENCHANTED_GRASS_CONFIG, 12);
		TFBiomeDecorator.addFlowersMore(this);
		TFBiomeDecorator.addTallFerns(this, 20);
		TFBiomeDecorator.addMushrooms(this);
	}

	@Override
	public int getGrassColorAt(double p_225528_1_, double p_225528_3_) {
		return (super.getGrassColorAt(p_225528_1_, p_225528_3_) & 0xFFFF00) + getEnchantedColor(pos.getX(), pos.getZ());
	}

	@Override
	public int getFoliageColor() {
		return (super.getFoliageColor() & 0xFFFF00) + getEnchantedColor(pos.getX(), pos.getZ());
	}

	/**
	 * Find a number between 0 and 255.  currently returns concentric circles from the biome center
	 */
	private int getEnchantedColor(int x, int z) {
		// center of the biome is at % 256 - 8
		int cx = 256 * Math.round((x - 8) / 256F) + 8;
		int cz = 256 * Math.round((z - 8) / 256F) + 8;

		int dist = (int) MathHelper.sqrt((cx - x) * (cx - x) + (cz - z) * (cz - z));
		int color = dist * 64;
		color %= 512;

		if (color > 255) {
			color = 511 - color;
		}

		color = 255 - color;

		int randomFlicker = colorRNG.nextInt(32) - 16;

		if (0 < color + randomFlicker && color + randomFlicker > 255) {
			color += randomFlicker;
		}

		return color;
	}

//	@Override
//	public void addDefaultFlowers() {
//		for (BlockFlower.EnumFlowerType flowerType : Blocks.YELLOW_FLOWER.getTypeProperty().getAllowedValues()) {
//			addFlower(Blocks.YELLOW_FLOWER.getDefaultState().with(Blocks.YELLOW_FLOWER.getTypeProperty(), flowerType), 10);
//		}
//		for (BlockFlower.EnumFlowerType flowerType : Blocks.RED_FLOWER.getTypeProperty().getAllowedValues()) {
//			addFlower(Blocks.RED_FLOWER.getDefaultState().with(Blocks.RED_FLOWER.getTypeProperty(), flowerType), 10);
//		}
//	}

	@Override
	protected TFFeature getContainedFeature() {
		return TFFeature.QUEST_GROVE;
	}
}
