/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webcamcolorsegmentation;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.alg.color.ColorHsv;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.MultiSpectral;
import com.github.sarxos.webcam.Webcam;
import georegression.metric.UtilAngle;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
/**
 *
 * @author User
 */
public class WebCamColorSegmentation {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        // tune the tracker for the image size and visual appearance
        ConfigGeneralDetector configDetector = new ConfigGeneralDetector(-1, 8, 1);
        PkltConfig configKlt = new PkltConfig(3, new int[]{1, 2, 4, 8});

        PointTracker<ImageFloat32> tracker = FactoryPointTracker.klt(configKlt, configDetector, ImageFloat32.class, null);

        // Open a webcam at a resolution close to 640x480
        Webcam webcam = UtilWebcamCapture.openDefault(640, 480);

        // Create the panel used to display the image and
        ImagePanel gui = new ImagePanel();
        gui.setPreferredSize(webcam.getViewSize());

        ShowImages.showWindow(gui, "");
        float hue = 1f;
        float saturation = 1f;
        
        while (true) {
            /**
            * Selectively displays only pixels which have a similar hue and saturation values to what is provided.
            * This is intended to be a simple example of color based segmentation.  Color based segmentation can be done
            * in RGB color, but is more problematic due to it not being intensity invariant.  More robust techniques
            * can use Gaussian models instead of a uniform distribution, as is done below.
            */
            //showSelectedColor("Yellow",image,1f,1f);
            //public static void showSelectedColor( String name , BufferedImage image , float hue , float saturation ) {
            BufferedImage image = webcam.getImage();
            MultiSpectral<ImageFloat32> input = ConvertBufferedImage.convertFromMulti(image,null,true,ImageFloat32.class);
            MultiSpectral<ImageFloat32> hsv = new MultiSpectral<ImageFloat32>(ImageFloat32.class,input.width,input.height,3);
            
            // Convert into HSV
            ColorHsv.rgbToHsv_F32(input,hsv);
            
            // Euclidean distance squared threshold for deciding which pixels are members of the selected set
            float maxDist2 = 0.4f*0.4f;

            // Extract hue and saturation bands which are independent of intensity
            ImageFloat32 H = hsv.getBand(0);
            ImageFloat32 S = hsv.getBand(1);

            // Adjust the relative importance of Hue and Saturation.
            // Hue has a range of 0 to 2*PI and Saturation from 0 to 1.
            float adjustUnits = (float)(Math.PI/2.0);
            
            // step through each pixel and mark how close it is to the selected color
            BufferedImage output = new BufferedImage(input.width,input.height,BufferedImage.TYPE_INT_RGB);
            for( int y = 0; y < hsv.height; y++ ) {
                    for( int x = 0; x < hsv.width; x++ ) {
                            // Hue is an angle in radians, so simple subtraction doesn't work
                            float dh = UtilAngle.dist(H.unsafe_get(x,y),hue);
                            float ds = (S.unsafe_get(x,y)-saturation)*adjustUnits;

                            // this distance measure is a bit naive, but good enough for to demonstrate the concept
                            float dist2 = dh*dh + ds*ds;
                            if( dist2 <= maxDist2 ) {
                                    output.setRGB(x,y,image.getRGB(x,y));
                            }
                    }
            }
            
            // Draw the tracks
            Graphics2D g2 = output.createGraphics();            
            
            
            gui.setBufferedImageSafe(output);
        }
    }
    
}
