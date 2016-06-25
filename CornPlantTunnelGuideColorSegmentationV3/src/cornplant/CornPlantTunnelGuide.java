/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cornplant;

import boofcv.alg.color.ColorHsv;
import boofcv.alg.filter.blur.BlurImageOps;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;

import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_I32;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 * Example which demonstrates how color can be used to segment an image. The
 * color space is converted from RGB into HSV. HSV separates intensity from
 * color and allows you to search for a specific color based on two values
 * independent of lighting conditions. Other color spaces are supported, such as
 * YUV, XYZ, and LAB.
 *
 * @author Peter Abeles
 */
public class CornPlantTunnelGuide {

    /**
     * Shows a color image and allows the user to select a pixel, convert it to
     * HSV, print the HSV values, and calls the function below to display
     * similar pixels.
     */
    private static String filename = "sample-images/3";

    public static void printClickedColor(final BufferedImage image) {
        ImagePanel gui1 = new ImagePanel(image);
        BufferedImage imageFiltered = null;
        try {
            imageFiltered = FilterBlur(image);
        } catch (IOException ex) {
            Logger.getLogger(CornPlantTunnelGuide.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            DetectCenterGravity("Selected", imageFiltered);
        } catch (IOException ex) {
            Logger.getLogger(CornPlantTunnelGuide.class.getName()).log(Level.SEVERE, null, ex);
        }
        Planar< GrayF32> input = ConvertBufferedImage.convertFromMulti(image, null, true, GrayF32.class);
        Planar< GrayF32> blurred = input.createSameShape();
        BlurImageOps.gaussian(input, blurred, -1, 5, null);
//  BlurImageOps.median(input, blurred, 5);
        BlurImageOps.median(blurred, blurred, 5);
        ImagePanel gui2 = new ImagePanel(ConvertBufferedImage.convertTo_F32(blurred, null, true));
//  ShowImages.showWindow(gui2,"blurred_all");
        ShowImages.showWindow(gui1, "Color Selector");
    }

    private static void DetectCenterGravity(String name, BufferedImage image) throws IOException {
        Planar< GrayF32> input = ConvertBufferedImage.convertFromMulti(image, null, true, GrayF32.class);
        BufferedImage output = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);
        List< Point2D_I32> gravityPoints = new ArrayList< Point2D_I32>();
        Graphics2D g2 = output.createGraphics();
        LinearRegression centerLinear = null;
        List< Double> LinearX = new ArrayList< Double>();
        List< Double> LinearY = new ArrayList< Double>();

        for (int y = 0; y < input.height; y++) {
            int sumPixelNumber = 0;
            int sumDetectRow = 0;
            for (int x = 0; x < input.width; x++) {
                if (input.getBand(2).get(x, y) >= 20) {
                    sumPixelNumber += x;
                    sumDetectRow++;
                    output.setRGB(x, y, 255);
                }
            }
            if (sumDetectRow > 0) {
                sumPixelNumber /= sumDetectRow;
                gravityPoints.add(new Point2D_I32(sumPixelNumber, y));
                LinearX.add((double) sumPixelNumber);
                LinearY.add((double) y);
            }
        }
        if (gravityPoints.size() > 0) {
            Double[] LinearXArray = new Double[LinearX.size()];
            Double[] LinearYArray = new Double[LinearY.size()];

            LinearX.toArray(LinearXArray);
            LinearY.toArray(LinearYArray);
            centerLinear = new LinearRegression(LinearYArray, LinearXArray);
            g2.setColor(Color.red);
            g2.drawLine((int) centerLinear.predict(input.height), input.height, (int) centerLinear.predict(0), 0);
        }
        if (gravityPoints.size() > 0) {
            VisualizeFeatures.drawPoints(g2, Color.white, gravityPoints, 1);
            VisualizeFeatures.drawPoint(g2, (int) centerLinear.predict(LinearY.get(0)), LinearY.get(0).intValue(), Color.yellow);
        }
        ShowImages.showWindow(output, name);
        ImageIO.write(output, "png", new File(filename + "_draw.png"));
    }

    public static Point2D.Float getIntersectionPoint(Line2D.Float line1, Line2D.Float line2) {
        //if (! line1.intersectsLine(line2) ) return null;
        double px = line1.getX1(),
                py = line1.getY1(),
                rx = line1.getX2() - px,
                ry = line1.getY2() - py;
        double qx = line2.getX1(),
                qy = line2.getY1(),
                sx = line2.getX2() - qx,
                sy = line2.getY2() - qy;

        double det = sx * ry - sy * rx;
        if (det == 0) {
            return null;
        } else {
            double z = (sx * (qy - py) + sy * (px - qx)) / det;
            if (z == 0 || z == 1) {
                return null; // intersection at end point!
            }
            return new Point2D.Float(
                    (float) (px + z * rx), (float) (py + z * ry));
        }
    } // end intersection line-line 

    public static BufferedImage FilterBlur(BufferedImage image) throws IOException {
        Planar< GrayF32> input = ConvertBufferedImage.convertFromMulti(image, null, true, GrayF32.class);
        Planar< GrayF32> rgb = new Planar< GrayF32>(GrayF32.class, input.width, input.height, 3);
        BufferedImage output = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);
        // Convert into HSV
        ColorHsv.rgbToHsv_F32(input, rgb);
        // Extract hue and saturation bands which are independent of intensity
        GrayF32 H = rgb.getBand(0);
        GrayF32 S = rgb.getBand(1);

  // Adjust the relative importance of Hue and Saturation.
        // Hue has a range of 0 to 2*PI and Saturation from 0 to 1.  
        for (int y = 0; y < rgb.height; y++) {
            for (int x = 0; x < rgb.width; x++) {
                int R = (int) input.getBand(0).get(x, y);
                int G = (int) input.getBand(1).get(x, y);
                int B = (int) input.getBand(2).get(x, y);
                if (MatchDirtColor(R, G, B)) {
                    output.setRGB(x, y, 255);
                } else {
                    output.setRGB(x, y, 0);
                }
            }
        }
        ShowImages.showWindow(output, "Before FilterBlured ");
        ImageIO.write(output, "png", new File(filename + "_before_blurred.png"));
        output = GaussianBlurPlanar(ConvertBufferedImage.convertFromMulti(output, null, true, GrayU8.class));
        ShowImages.showWindow(output, "After FilterBlured ");
        ImageIO.write(output, "png", new File(filename + "_after.png"));
        return output;
    }

    public static void main(String[] args) {
        BufferedImage image = UtilImageIO.loadImage(filename + ".png");

        printClickedColor(image);
    }
    
    private static BufferedImage GaussianBlurPlanar(Planar< GrayU8> input) {
        int blurRadius = 20;
        Planar< GrayU8> blurred = input.createSameShape();
        BufferedImage output = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB);
        // Apply Gaussian blur to each band in the image
        BlurImageOps.gaussian(input, blurred, 0, blurRadius, null);
        BlurImageOps.median(blurred, blurred, blurRadius);
        //        BlurImageOps.mean(blurred, blurred, blurRadius, null);

        ConvertBufferedImage.convertTo(blurred, output, true);
        return output;
    }

    private static boolean MatchDirtColor(int R, int G, int B) {
        if(R == 0){
            R = 1;
        }
        if(G == 0){
            G = 1;
        }
        if(B == 0){
            B = 1;
        }
        if(R/G >= 1.12 && R/G <= 2.9){
            return true;
        }

        return false;
    }
}
