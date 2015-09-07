/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package thresholding;

import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.alg.misc.ImageStatistics;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.image.ShowImages;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageUInt8;
import org.ddogleg.stats.UtilGaussian;

import java.awt.image.BufferedImage;

/**
 *
 * @author User
 */
public class Thresholding {

    public static void threshold(String imagename) {
        BufferedImage image = UtilImageIO.loadImage(imagename);

        // convert into a usable format
        ImageFloat32 input = ConvertBufferedImage.convertFromSingle(image, null, ImageFloat32.class);
        ImageUInt8 binary = new ImageUInt8(input.width, input.height);

        // Display multiple images in the same window
        ListDisplayPanel gui = new ListDisplayPanel();

        // Global Methods
        GThresholdImageOps.threshold(input, binary, ImageStatistics.mean(input), true);
        gui.addImage(VisualizeBinaryData.renderBinary(binary, null), "Global: Mean");

        GThresholdImageOps.threshold(input, binary, GThresholdImageOps.computeOtsu(input, 0, 256), true);
        gui.addImage(VisualizeBinaryData.renderBinary(binary, null), "Global: Otsu");

        GThresholdImageOps.threshold(input, binary, GThresholdImageOps.computeEntropy(input, 0, 256), true);
        gui.addImage(VisualizeBinaryData.renderBinary(binary, null), "Global: Entropy");

        GThresholdImageOps.adaptiveSquare(input, binary, 28, 0, true, null, null);
        gui.addImage(VisualizeBinaryData.renderBinary(binary, null), "Adaptive: Square");
        
        GThresholdImageOps.adaptiveGaussian(input, binary, 42, 0, true, null, null);
        gui.addImage(VisualizeBinaryData.renderBinary(binary, null), "Adaptive: Gaussian");
        
        GThresholdImageOps.adaptiveSauvola(input, binary, 5, 0.30f, true);
        gui.addImage(VisualizeBinaryData.renderBinary(binary, null), "Adaptive: Sauvola");

        // Sauvola is tuned for text image.  Change radius to make it run better in others.
        // Show the image image for reference
        gui.addImage(ConvertBufferedImage.convertTo(input, null), "Input Image");

        String fileName = imagename.substring(imagename.lastIndexOf('/') + 1);
        ShowImages.showWindow(gui, fileName);

    }

    public static void main(String[] args) {
        // example in which global thresholding works best
        threshold("particles01.jpg");
        // example in which adaptive/local thresholding works best
        threshold("uneven_lighting_squares.jpg");
        // hand written text with non-uniform stained background
        threshold("stained_handwriting.jpg");
        threshold("a.jpg");
        threshold("b.jpg");
    }

}
