/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fiducialsquarebinary;

import boofcv.abst.fiducial.FiducialDetector;
import boofcv.factory.fiducial.ConfigFiducialBinary;
import boofcv.factory.fiducial.FactoryFiducial;
import boofcv.gui.fiducial.VisualizeFiducial;
import boofcv.factory.filter.binary.
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.calib.IntrinsicParameters;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageType;
import georegression.struct.se.Se3_F64;

import java.awt.*;
import java.awt.image.BufferedImage;
public class FiducialSquareBinary {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        		String directory = UtilIO.pathExample("fiducial/binary");
 
		// load the lens distortion parameters and the input image
		IntrinsicParameters param = UtilIO.loadXML(directory , "intrinsic.xml");
		BufferedImage input = UtilImageIO.loadImage(directory , "image0000.jpg");
		ImageFloat32 original = ConvertBufferedImage.convertFrom(input,true, ImageType.single(ImageFloat32.class));
 
		// Detect the fiducial
		FiducialDetector<ImageFloat32> detector = FactoryFiducial.squareBinary(
				new ConfigFiducialBinary(0.1), ConfigThreshold.local(ThresholdType.LOCAL_SQUARE, 10), ImageFloat32.class);
//				new ConfigFiducialBinary(0.1), ConfigThreshold.fixed(100),ImageFloat32.class);
 
		detector.setIntrinsic(param);
 
		detector.detect(original);
 
		// print the results
		Graphics2D g2 = input.createGraphics();
		Se3_F64 targetToSensor = new Se3_F64();
		for (int i = 0; i < detector.totalFound(); i++) {
			System.out.println("Target ID = "+detector.getId(i));
			detector.getFiducialToCamera(i, targetToSensor);
			System.out.println("Location:");
			System.out.println(targetToSensor);
 
			VisualizeFiducial.drawCube(targetToSensor,param,detector.getWidth(i), 3, g2);
			VisualizeFiducial.drawLabelCenter(targetToSensor,param,""+detector.getId(i), g2);
		}
 
		ShowImages.showWindow(input,"Fiducials",true);
    }
    
}
