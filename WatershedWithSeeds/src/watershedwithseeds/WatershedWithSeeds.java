/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package watershedwithseeds;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.alg.segmentation.watershed.WatershedVincentSoille1991;
import boofcv.factory.segmentation.FactorySegmentationAlg;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeRegions;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;

import java.awt.image.BufferedImage;
/**
 *
 * @author User
 */
public class WatershedWithSeeds {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        BufferedImage image = UtilImageIO.loadImage("corn1.png");
		ImageUInt8 input = ConvertBufferedImage.convertFromSingle(image, null, ImageUInt8.class);

		// declare working data
		ImageUInt8 binary = new ImageUInt8(input.width,input.height);
		ImageSInt32 label = new ImageSInt32(input.width,input.height);

		// Try using the mean pixel value to create a binary image then erode it to separate the particles from
		// each other
		double mean = ImageStatistics.mean(input);
		ThresholdImageOps.threshold(input, binary, (int) mean, true);
		ImageUInt8 filtered = BinaryImageOps.erode8(binary, 2, null);
		int numRegions = BinaryImageOps.contour(filtered, ConnectRule.EIGHT, label).size() + 1;
		// +1 to regions because contour only counts blobs and not the background

		// The labeled image can be used as is.  A precondition for seeded watershed is that all seeds have an
		// ID > 0.  Luckily, a value of 0 was used for background pixels in the contour algorithm.
		WatershedVincentSoille1991 watershed = FactorySegmentationAlg.watershed(ConnectRule.FOUR);

		watershed.process(input,label);

		ImageSInt32 output = watershed.getOutput();

		BufferedImage outLabeled = VisualizeBinaryData.renderLabeledBG(label, numRegions, null);
		VisualizeRegions.watersheds(output,image,3);

		// Removing the watersheds and update the region count
		// NOTE: watershed.getTotalRegions() does not return correct results if seeds are used!
		watershed.removeWatersheds();
		numRegions -= 1;
		BufferedImage outRegions = VisualizeRegions.regions(output,numRegions,null);

		ListDisplayPanel gui = new ListDisplayPanel();
		gui.addImage(image, "Watersheds");
		gui.addImage(outRegions, "Regions");
		gui.addImage(outLabeled, "Seeds");
		ShowImages.showWindow(gui, "Watershed");

		// Additional processing would be needed for this example to be really useful.
		// The watersheds can be used to characterize the background while the seed binary image the particles
		// From this the particles could be more accurately classified by assigning each pixel one of the two
		// just mentioned groups based distance
    }
    
}
