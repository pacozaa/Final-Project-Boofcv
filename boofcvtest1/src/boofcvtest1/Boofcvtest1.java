/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package boofcvtest1;

import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.Contour;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.filter.binary.ThresholdImageOps;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageUInt8;
import java.awt.image.BufferedImage;
import java.util.List;

public class Boofcvtest1{
 
	public static void main( String args[] ) {
		// load and convert the image into a usable format
//		BufferedImage image = UtilImageIO.loadImage("/home/paco/NetBeansProjects/boofcvtest1/build/classes/boofcvtest1/Particles01.jpg");
 BufferedImage image = UtilImageIO.loadImage("Particles01.jpg");
		// convert into a usable format
		ImageFloat32 input = ConvertBufferedImage.convertFromSingle(image, null, ImageFloat32.class);
		ImageUInt8 binary = new ImageUInt8(input.width,input.height);
		ImageSInt32 label = new ImageSInt32(input.width,input.height);
 
		// Select a global threshold using Otsu's method.
		double threshold = GThresholdImageOps.computeOtsu(input, 0, 256);
 
		// Apply the threshold to create a binary image
		ThresholdImageOps.threshold(input,binary,(float)threshold,true);
 
		// remove small blobs through erosion and dilation
		// The null in the input indicates that it should internally declare the work image it needs
		// this is less efficient, but easier to code.
		ImageUInt8 filtered = BinaryImageOps.erode8(binary, 1, null);
		filtered = BinaryImageOps.dilate8(filtered, 1, null);
 
		// Detect blobs inside the image using an 8-connect rule
		List<Contour> contours = BinaryImageOps.contour(filtered, ConnectRule.EIGHT, label);
 
		// colors of contours
		int colorExternal = 0xFFFFFF;
		int colorInternal = 0xFF2020;
 
		// display the results
		BufferedImage visualBinary = VisualizeBinaryData.renderBinary(binary, null);
		BufferedImage visualFiltered = VisualizeBinaryData.renderBinary(filtered, null);
		BufferedImage visualLabel = VisualizeBinaryData.renderLabeledBG(label, contours.size(), null);
		BufferedImage visualContour = VisualizeBinaryData.renderContours(contours,colorExternal,colorInternal,
				input.width,input.height,null);
 
		ShowImages.showWindow(visualBinary,"Binary Original");
		ShowImages.showWindow(visualFiltered,"Binary Filtered");
		ShowImages.showWindow(visualLabel,"Labeled Blobs");
		ShowImages.showWindow(visualContour,"Contours");
	}
 
}