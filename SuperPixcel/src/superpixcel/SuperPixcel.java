/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package superpixcel;
import boofcv.abst.segmentation.ImageSuperpixels;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.segmentation.ComputeRegionMeanColor;
import boofcv.alg.segmentation.ImageSegmentationOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.segmentation.ConfigFh04;
import boofcv.factory.segmentation.FactoryImageSegmentation;
import boofcv.factory.segmentation.FactorySegmentationAlg;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.feature.VisualizeRegions;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
//import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.feature.ColorQueue_F32;
import boofcv.struct.image.*;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.awt.image.BufferedImage;

/**
 *
 * @author User
 */
public class SuperPixcel {

    
	/**
	 * Segments and visualizes the image
	 */
	public static <T extends ImageBase>
	void performSegmentation( ImageSuperpixels<T> alg , T color )
	{
		// Segmentation often works better after blurring the image.  Reduces high frequency image components which
		// can cause over segmentation
		GBlurImageOps.gaussian(color, color, 0.5, -1, null);

		// Storage for segmented image.  Each pixel will be assigned a label from 0 to N-1, where N is the number
		// of segments in the image
		ImageSInt32 pixelToSegment = new ImageSInt32(color.width,color.height);

		// Segmentation magic happens here
		alg.segment(color,pixelToSegment);

		// Displays the results
		visualize(pixelToSegment,color,alg.getTotalSuperpixels());
	}

	/**
	 * Visualizes results three ways.  1) Colorized segmented image where each region is given a random color.
	 * 2) Each pixel is assigned the mean color through out the region. 3) Black pixels represent the border
	 * between regions.
	 */
	public static <T extends ImageBase>
	void visualize( ImageSInt32 pixelToRegion , T color , int numSegments  )
	{
		// Computes the mean color inside each region
		ImageType<T> type = color.getImageType();
		ComputeRegionMeanColor<T> colorize = FactorySegmentationAlg.regionMeanColor(type);

		FastQueue<float[]> segmentColor = new ColorQueue_F32(type.getNumBands());
		segmentColor.resize(numSegments);

		GrowQueue_I32 regionMemberCount = new GrowQueue_I32();
		regionMemberCount.resize(numSegments);

		ImageSegmentationOps.countRegionPixels(pixelToRegion, numSegments, regionMemberCount.data);
		colorize.process(color,pixelToRegion,regionMemberCount,segmentColor);

		// Draw each region using their average color
		BufferedImage outColor = VisualizeRegions.regionsColor(pixelToRegion,segmentColor,null);
		// Draw each region by assigning it a random color
		BufferedImage outSegments = VisualizeRegions.regions(pixelToRegion, numSegments, null);

		// Make region edges appear red
		BufferedImage outBorder = new BufferedImage(color.width,color.height,BufferedImage.TYPE_INT_RGB);
		ConvertBufferedImage.convertTo(color, outBorder, true);
		VisualizeRegions.regionBorders(pixelToRegion,0xFF0000,outBorder);

		// Show the visualization results
		ListDisplayPanel gui = new ListDisplayPanel();
		gui.addImage(outColor,"Color of Segments");
		gui.addImage(outBorder, "Region Borders");
		gui.addImage(outSegments, "Regions");
		//ShowImages.showWindow(gui,"Superpixels", true);
                ShowImages.showWindow(gui, "Superpixels");
	}

	public static void main(String[] args) {
		BufferedImage image = UtilImageIO.loadImage("corn1.png");
                //BufferedImage image = UtilImageIO.loadImage("horses.jpg");
//		BufferedImage image = UtilImageIO.loadImage(UtilIO.pathExample("segment/berkeley_kangaroo.jpg"));
//		BufferedImage image = UtilImageIO.loadImage(UtilIO.pathExample("segment/berkeley_man.jpg"));
//		BufferedImage image = UtilImageIO.loadImage(UtilIO.pathExample("segment/mountain_pines_people.jpg"));
//		BufferedImage image = UtilImageIO.loadImage(UtilIO.pathExample("particles01.jpg"));

		// you probably don't want to segment along the image's alpha channel and the code below assumes 3 channels
		//image = ConvertBufferedImage(image);
                //MultiSpectral<ImageFloat32> input = ConvertBufferedImage.convertFromMulti(image,null,true,ImageFloat32.class);

		// Select input image type.  Some algorithms behave different depending on image type
		ImageType<MultiSpectral<ImageFloat32>> imageType = ImageType.ms(3, ImageFloat32.class);
//		ImageType<MultiSpectral<ImageUInt8>> imageType = ImageType.ms(3,ImageUInt8.class);
//		ImageType<ImageFloat32> imageType = ImageType.single(ImageFloat32.class);
//		ImageType<ImageUInt8> imageType = ImageType.single(ImageUInt8.class);

//		ImageSuperpixels alg = FactoryImageSegmentation.meanShift(null, imageType);
//		ImageSuperpixels alg = FactoryImageSegmentation.slic(new ConfigSlic(400), imageType);
		ImageSuperpixels alg = FactoryImageSegmentation.fh04(new ConfigFh04(100,30), imageType);
//		ImageSuperpixels alg = FactoryImageSegmentation.watershed(null,imageType);

		// Convert image into BoofCV format
		ImageBase color = imageType.createImage(image.getWidth(),image.getHeight());
		ConvertBufferedImage.convertFrom(image, color, true);

		// Segment and display results
		performSegmentation(alg,color);
	}
    
}
