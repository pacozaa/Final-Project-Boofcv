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
 * Example which demonstrates how color can be used to segment an image.  The color space is converted from RGB into
 * HSV.  HSV separates intensity from color and allows you to search for a specific color based on two values
 * independent of lighting conditions.  Other color spaces are supported, such as YUV, XYZ, and LAB.
 *
 * @author Peter Abeles
 */
public class CornPlantTunnelGuide {

   	/**
	 * Shows a color image and allows the user to select a pixel, convert it to HSV, print
	 * the HSV values, and calls the function below to display similar pixels.
	 */        
        private static String filename = "sample-images/4";
	public static void printClickedColor( final BufferedImage image ) {
		ImagePanel gui1 = new ImagePanel(image);
		gui1.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				float[] color = new float[3];
				int rgb = image.getRGB(e.getX(),e.getY());
                                System.out.println("X is :"+e.getX()+" Y is :"+e.getY());
				ColorHsv.rgbToHsv((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, color);
//				System.out.println("H = " + color[0]+" S = "+color[1]+" V = "+color[2]);
                                // Euclidean distance squared threshold for deciding which pixels are members of the selected set
                                float maxDist2 = 0.8f*0.8f;  
                                BufferedImage imageFiltered = null;
                                try {
                                    imageFiltered = FilterBlur(image,color[0],color[1],maxDist2);
                                } catch (IOException ex) {
                                    Logger.getLogger(CornPlantTunnelGuide.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                try {
                                    //DetectTunnelGuidePattern("Selected",imageFiltered,color[0],color[1],maxDist2);
                                    DetectCenterGravity("Selected",imageFiltered);
                                } catch (IOException ex) {
                                    Logger.getLogger(CornPlantTunnelGuide.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                ColorHsv.hsvToRgb(color[0], color[1], color[2], color);
                                System.out.println("R = " + color[0]+" G = "+color[1]+" B = "+color[2]);				                                
			}

                    
		});                      
                Planar<GrayF32> input = ConvertBufferedImage.convertFromMulti(image,null,true,GrayF32.class); 
                Planar<GrayF32> blurred = input.createSameShape();                
                BlurImageOps.gaussian(input,blurred,-1,5,null);
                BlurImageOps.median(blurred, blurred, 5);
                ImagePanel gui2 = new ImagePanel(ConvertBufferedImage.convertTo_F32(blurred, null, true));
                gui2.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {				
                            float rBand = blurred.getBand(0).get(e.getX(),e.getY());
                            float gBand = blurred.getBand(1).get(e.getX(),e.getY());
                            float bBand = blurred.getBand(2).get(e.getX(),e.getY());
                            System.out.println("R-band : "+rBand+" ,G-band : "+gBand+" ,B-band : "+bBand);
			}                    
		});                      
//                ShowImages.showWindow(gui2,"blurred_all");
		ShowImages.showWindow(gui1,"Color Selector");                
	}
        private static void DetectCenterGravity(String name , BufferedImage image) throws IOException {
            Planar<GrayF32> input = ConvertBufferedImage.convertFromMulti(image,null,true,GrayF32.class);            
            BufferedImage output = new BufferedImage(input.width,input.height,BufferedImage.TYPE_INT_RGB);            
            List<Point2D_I32> gravityPoints = new ArrayList<Point2D_I32>(); 
            Graphics2D g2 = output.createGraphics();
            LinearRegression centerLinear = null;
            List<Double> LinearX = new ArrayList<Double>();
            List<Double> LinearY = new ArrayList<Double>();
                        
            for (int y = 0; y < input.height; y++) {                
               int sumPixelNumber = 0;
               int sumDetectRow = 0;
               for (int x = 0; x < input.width; x++) {      
                   if (input.getBand(2).get(x, y) >= 100) {                         
                       sumPixelNumber+=x;
                       sumDetectRow++;
                       output.setRGB(x, y, 255);
                   }                                           
               }
               if(sumDetectRow > 0){
                   sumPixelNumber/=sumDetectRow;
                   gravityPoints.add(new Point2D_I32(sumPixelNumber,y));
                   LinearX.add((double)sumPixelNumber);
                   LinearY.add((double)y);
               }
            }
            if(gravityPoints.size() > 0){
                Double[] LinearXArray = new Double[LinearX.size()];
                Double[] LinearYArray = new Double[LinearY.size()];
                
                LinearX.toArray(LinearXArray);
                LinearY.toArray(LinearYArray);
                centerLinear = new LinearRegression(LinearYArray,LinearXArray);
                g2.setColor(Color.red);
                g2.drawLine((int)centerLinear.predict(input.height), input.height, (int)centerLinear.predict(0), 0);
            }
            if(gravityPoints.size() > 0){
                VisualizeFeatures.drawPoints(g2, Color.white, gravityPoints, 1);
                VisualizeFeatures.drawPoint(g2, (int)centerLinear.predict(LinearY.get(0)), LinearY.get(0).intValue(), Color.yellow);
                System.out.println("Point X is: "+(int)centerLinear.predict(LinearY.get(0))+" Point Y is: "+LinearY.get(0).intValue());
            }            
            ShowImages.showWindow(output, name);
            ImageIO.write(output, "png", new File(filename+"_draw.png"));
        }
        public static Point2D.Float getIntersectionPoint(Line2D.Float line1, Line2D.Float line2) {
          //if (! line1.intersectsLine(line2) ) return null;
            double px = line1.getX1(),
                  py = line1.getY1(),
                  rx = line1.getX2()-px,
                  ry = line1.getY2()-py;
            double qx = line2.getX1(),
                  qy = line2.getY1(),
                  sx = line2.getX2()-qx,
                  sy = line2.getY2()-qy;

            double det = sx*ry - sy*rx;
            if (det == 0) {
              return null;
            } else {
              double z = (sx*(qy-py)+sy*(px-qx))/det;
              if (z==0 ||  z==1) return null;  // intersection at end point!
              return new Point2D.Float(
                (float)(px+z*rx), (float)(py+z*ry));
            }
        } // end intersection line-line
        public static BufferedImage FilterAreaPercent(BufferedImage image, float hue, float saturation, float maxDist2) {
            Planar<GrayF32> input = ConvertBufferedImage.convertFromMulti(image,null,true,GrayF32.class);
            Planar<GrayF32> hsv = new Planar<GrayF32>(GrayF32.class,input.width,input.height,3);
            BufferedImage output = new BufferedImage(input.width,input.height,BufferedImage.TYPE_INT_RGB);
            // Convert into HSV
            ColorHsv.rgbToHsv_F32(input,hsv);           
            // Extract hue and saturation bands which are independent of intensity
            GrayF32 H = hsv.getBand(0);
            GrayF32 S = hsv.getBand(1);

            // Adjust the relative importance of Hue and Saturation.
            // Hue has a range of 0 to 2*PI and Saturation from 0 to 1.
            float adjustUnits = (float)(Math.PI/2.0);
            for (int y = 0; y < hsv.height; y++) {                      
                    for (int x = 0; x < hsv.width; x++) {                        
                        if (matchColorHSV(x,y,H,S,hue,saturation,adjustUnits,maxDist2)) {                            
                            int weight = 0;
                            double diversity = 0.5;
                            int scanRange = 10;
                            for(int i = 0;i < scanRange; i++){
                                for(int j = 0;j < scanRange; j++){  
                                    if(okPixcel(x-scanRange/2+j,y-scanRange/2+i,hsv.width,hsv.height)){
                                        if(matchColorHSV(x-scanRange/2+j,y-scanRange/2+i,H,S,hue,saturation,adjustUnits,maxDist2)){
                                            weight++;
                                        }
                                    }                                    
                                }
                            }
                            if(weight > 81*diversity){
                                output.setRGB(x, y, image.getRGB(x, y));   
                            }                 
                            else{
                                output.setRGB(x, y, 0);   
                            }
                        }                        
                    }                                                                                                                        
                } 
            ShowImages.showWindow(output,"After FilterAreaPercent ");
            return output;
        }
        public static BufferedImage FilterBlur(BufferedImage image, float hue, float saturation, float maxDist2) throws IOException {
            Planar<GrayF32> input = ConvertBufferedImage.convertFromMulti(image,null,true,GrayF32.class);
            Planar<GrayF32> hsv = new Planar<GrayF32>(GrayF32.class,input.width,input.height,3);
            BufferedImage output = new BufferedImage(input.width,input.height,BufferedImage.TYPE_INT_RGB);
            // Convert into HSV
            ColorHsv.rgbToHsv_F32(input,hsv);            
            // Extract hue and saturation bands which are independent of intensity
            GrayF32 H = hsv.getBand(0);
            GrayF32 S = hsv.getBand(1);

            // Adjust the relative importance of Hue and Saturation.
            // Hue has a range of 0 to 2*PI and Saturation from 0 to 1.
            float adjustUnits = (float)(Math.PI/2.0);
            
            for (int y = 0; y < hsv.height; y++) {                      
                    for (int x = 0; x < hsv.width; x++) {                        
                        if (matchColorHSV(x,y,H,S,hue,saturation,adjustUnits,maxDist2)) {                            
                            output.setRGB(x, y, 255);
                        }    
                        else{
                            output.setRGB(x, y, 0);
                        }
                    }                                                                                                                        
                }
            ShowImages.showWindow(output,"Before FilterBlured ");   
            ImageIO.write(output, "png", new File(filename+"_before_blurred.png"));
            output = GaussianBlurPlanar(ConvertBufferedImage.convertFromMulti(output, null, true, GrayU8.class));          
            ShowImages.showWindow(output,"After FilterBlured ");
            ImageIO.write(output, "png", new File(filename+"_after.png"));
            return output;
        }
        public static void DetectTunnelGuidePattern( String name , BufferedImage image , float hue , float saturation, float maxDist2 ) {
                LinearRegression LeftLinear = null;                
                LinearRegression RightLinear = null;
		Planar<GrayF32> input = ConvertBufferedImage.convertFromMulti(image,null,true,GrayF32.class);
		Planar<GrayF32> hsv = new Planar<GrayF32>(GrayF32.class,input.width,input.height,3);
                List<Point2D_I32> points = new ArrayList<Point2D_I32>(); 
		// Convert into HSV
		ColorHsv.rgbToHsv_F32(input,hsv);
              
		// Extract hue and saturation bands which are independent of intensity
		GrayF32 H = hsv.getBand(0);
		GrayF32 S = hsv.getBand(1);

		// Adjust the relative importance of Hue and Saturation.
		// Hue has a range of 0 to 2*PI and Saturation from 0 to 1.
		float adjustUnits = (float)(Math.PI/2.0);

		// step through each pixel and mark how close it is to the selected color
		BufferedImage output = new BufferedImage(input.width,input.height,BufferedImage.TYPE_INT_RGB);
		List<List<Integer[]>> AllTargetXY = new ArrayList<List<Integer[]>>();  
                List<Double> LeftLinearX = new ArrayList<Double>();
                List<Double> LeftLinearY = new ArrayList<Double>();
                List<Double> RightLinearX = new ArrayList<Double>();
                List<Double> RightLinearY = new ArrayList<Double>(); 
                boolean isDetected = false;                
                for (int y = 0; y < hsv.height; y++) {  
                    List<Integer[]> rowTargetXY = new ArrayList<Integer[]>(); 
                    for (int x = 0; x < hsv.width; x++) {                        
                        if (matchColorHSV(x,y,H,S,hue,saturation,adjustUnits,maxDist2)) {   
                            output.setRGB(x, y, image.getRGB(x, y));                            
                            rowTargetXY.add(new Integer[]{x,y});                             
                            isDetected = true;                                                                          
                        }                        
                    }                                        
                    AllTargetXY.add(y,rowTargetXY);                                                            
                }            
                int MaxCounter = 0;                
                for(List<Integer[]> row:AllTargetXY){
                    if(!row.isEmpty()){
                        int minLeft = input.width;          
                        int maxRight = 0;
                        for(Integer[] pixel:row){                            
                            if(pixel[0] < minLeft){
                                minLeft = pixel[0];
                            }                                                         
                            if(pixel[0] > maxRight){
                                maxRight = pixel[0];
                            }                            
                        }
                        
                            
                            points.add(new Point2D_I32(minLeft,MaxCounter));
                            
                            LeftLinearX.add((double)minLeft);
                            LeftLinearY.add((double)MaxCounter);
                            
                            Double[] XArrayLeft = new Double[LeftLinearX.size()];
                            Double[] YArrayLeft = new Double[LeftLinearY.size()];
                            
                            LeftLinearX.toArray(XArrayLeft);
                            LeftLinearY.toArray(YArrayLeft);
                            
                            LeftLinear = new LinearRegression(XArrayLeft,YArrayLeft);
                       
                            
                            points.add(new Point2D_I32(maxRight,MaxCounter));
                            
                            RightLinearX.add((double)maxRight);
                            RightLinearY.add((double)MaxCounter);
                            
                            Double[] XArrayRight = new Double[RightLinearX.size()];
                            Double[] YArrayRight = new Double[RightLinearY.size()];
                            
                            RightLinearX.toArray(XArrayRight);
                            RightLinearY.toArray(YArrayRight);
                            
                            RightLinear = new LinearRegression(XArrayRight,YArrayRight);
                                                                                               
                    }                    
                    MaxCounter++;
                }
                // Draw the tracks
                Graphics2D g2 = output.createGraphics();
                
                double RightX1 = 0;
                double RightY1 = RightLinear.predict(0);
                double RightX2 = input.width;
                double RightY2 = RightLinear.predict(input.width);
                
                double LeftX1 = 0;
                double LeftY1 = LeftLinear.predict(0);
                double LeftX2 = input.width;
                double LeftY2 = LeftLinear.predict(input.width);
                Point2D intersectionPoint = getIntersectionPoint(new Line2D.Float((float)RightX1,(float)RightY1,(float)RightX2,(float)RightY2), new Line2D.Float((float)LeftX1,(float)LeftY1,(float)LeftX2,(float)LeftY2));
                try{
                    if(isDetected){
                        g2.setColor(Color.BLUE);
                        g2.setStroke(new BasicStroke(3)); 
                        g2.drawLine((int)LeftX1,(int)LeftY1,(int)LeftX2,(int)LeftY2);
                        g2.drawLine((int)RightX1,(int)RightY1,(int)RightX2,(int)RightY2);
                        g2.setColor(Color.GREEN);
                        g2.setStroke(new BasicStroke(5)); 
                        g2.drawLine(0, 0,(int)intersectionPoint.getX(),(int)intersectionPoint.getY());
                        g2.drawLine(input.width, 0,(int)intersectionPoint.getX(),(int)intersectionPoint.getY());
                        g2.drawLine(0, input.height,(int)intersectionPoint.getX(),(int)intersectionPoint.getY());
                        g2.drawLine(input.width, input.height,(int)intersectionPoint.getX(),(int)intersectionPoint.getY());
                        VisualizeFeatures.drawPoints(g2, Color.red, points, 3);                                   
                        VisualizeFeatures.drawPoint(g2, (int)intersectionPoint.getX(), (int)intersectionPoint.getY(), 10, Color.MAGENTA);
                    }
                }
                catch(Exception e){
                    System.out.println(e.toString());
                }                                                                
		ShowImages.showWindow(output,"Showing "+name);
	}
    public static void main(String[] args) {        
        BufferedImage image = UtilImageIO.loadImage(filename+".png");
        	
        printClickedColor(image);
    }

    private static boolean matchColorHSV(int x, int y,GrayF32 H, GrayF32 S, float hue, float saturation, float adjustUnits, float maxDist2) {
        try{
            // Hue is an angle in radians, so simple subtraction doesn't work
            float dh = UtilAngle.dist(H.unsafe_get(x, y), hue);
            float ds = (S.unsafe_get(x, y) - saturation) * adjustUnits;

            // this distance measure is a bit naive, but good enough for to demonstrate the concept
            float dist2 = dh * dh + ds * ds;
            return dist2 <= maxDist2;
        }
        catch(Exception error){
            return false;
        }        
    }

    private static boolean okPixcel(int x, int y, int width, int height) {
        boolean result = true;
        if(x > width || x < 0){
            result = false;
        }
        else if(y > height || y < 0){
            result = false;
        }
        return result;
    }
    private static BufferedImage GaussianBlurPlanar(Planar<GrayU8> input ) {
        int blurRadius = 20;
        Planar<GrayU8> blurred = input.createSameShape();
        BufferedImage output = new BufferedImage(input.width,input.height,BufferedImage.TYPE_INT_RGB);
        // Apply Gaussian blur to each band in the image
        BlurImageOps.gaussian(input,blurred,0,blurRadius,null);
        BlurImageOps.median(blurred, blurred, blurRadius);
//        BlurImageOps.mean(blurred, blurred, blurRadius, null);

        ConvertBufferedImage.convertTo(blurred, output,true);
        return output;
    }
}
