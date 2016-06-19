/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package exampleplanarimages;

import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.alg.misc.GPixelMath;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import java.awt.image.BufferedImage;

/**
 *
 * @author User
 */
public class ExamplePlanarImages {

    public static ListDisplayPanel gui = new ListDisplayPanel();
 
	/**
	 * Many operations designed to only work on {@link ImageGray} can be applied
	 * to a Planar image by feeding in each band one at a time.
	 */
	public static void independent( BufferedImage input ) {
		// convert the BufferedImage into a Planar
		Planar<GrayU8> image = ConvertBufferedImage.convertFromMulti(input,null,true,GrayU8.class);
 
		// declare the output blurred image
		Planar<GrayU8> blurred = image.createSameShape();
                
                BlurImageOps.gaussian(image,blurred,-1,5,null);	
		// Declare the BufferedImage manually to ensure that the color bands have the same ordering on input
		// and output
		BufferedImage output = new BufferedImage(image.width,image.height,input.getType());
		ConvertBufferedImage.convertTo(blurred, output,true);
 
		gui.addImage(input,"Input");
		gui.addImage(output,"Gaussian Blur");
	}
 
	/**
	 * Values of pixels can be read and modified by accessing the internal {@link ImageGray}.
	 */
	public static void pixelAccess(  BufferedImage input ) {
		// convert the BufferedImage into a Planar
		Planar<GrayU8> image = ConvertBufferedImage.convertFromMulti(input,null,true,GrayU8.class);
 
		int x = 10, y = 10;
 
		// to access a pixel you first access the gray image for the each band
		for( int i = 0; i < image.getNumBands(); i++ )
			System.out.println("Original "+i+" = "+image.getBand(i).get(x,y));
 
		// change the value in each band
		for( int i = 0; i < image.getNumBands(); i++ )
			image.getBand(i).set(x, y, 100 + i);
 
		// to access a pixel you first access the gray image for the each band
		for( int i = 0; i < image.getNumBands(); i++ )
			System.out.println("Result   "+i+" = "+image.getBand(i).get(x,y));
	}
 
	/**
	 * There is no real perfect way that everyone agrees on for converting color images into gray scale
	 * images.  Two examples of how to convert a Planar image into a gray scale image are shown
	 * in this example.
	 */
	public static void convertToGray( BufferedImage input ) {
		// convert the BufferedImage into a Planar
		Planar<GrayU8> image = ConvertBufferedImage.convertFromMulti(input,null,true,GrayU8.class);
 
		GrayU8 gray = new GrayU8( image.width,image.height);                
		// creates a gray scale image by averaging intensity value across pixels
		GPixelMath.averageBand(image, gray);
		BufferedImage outputAve = ConvertBufferedImage.convertTo(gray,null);
                
 
		// create an output image just from the first band
		BufferedImage outputBand0 = ConvertBufferedImage.convertTo(image.getBand(0),null);                
                
		gui.addImage(outputAve,"Gray Averaged");
		gui.addImage(outputBand0,"Band 0");                
	}
 
	public static void main( String args[] ) {
		BufferedImage input = UtilImageIO.loadImage("carpet.png");
 
		// Uncomment lines below to run each example
 
		ExamplePlanarImages.independent(input);
		ExamplePlanarImages.pixelAccess(input);
		ExamplePlanarImages.convertToGray(input);
 
		ShowImages.showWindow(gui,"Color Planar Image Examples",true);
	}
    
}
