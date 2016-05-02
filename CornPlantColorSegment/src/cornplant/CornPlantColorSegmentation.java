/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cornplant;

import boofcv.alg.color.ColorHsv;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.MultiSpectral;
import georegression.metric.UtilAngle;
import georegression.struct.point.Point2D_I32;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Example which demonstrates how color can be used to segment an image.  The color space is converted from RGB into
 * HSV.  HSV separates intensity from color and allows you to search for a specific color based on two values
 * independent of lighting conditions.  Other color spaces are supported, such as YUV, XYZ, and LAB.
 *
 * @author Peter Abeles
 */
public class CornPlantColorSegmentation {

   	/**
	 * Shows a color image and allows the user to select a pixel, convert it to HSV, print
	 * the HSV values, and calls the function below to display similar pixels.
	 */
	public static void printClickedColor( final BufferedImage image ) {
		ImagePanel gui = new ImagePanel(image);
		gui.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				float[] color = new float[3];
				int rgb = image.getRGB(e.getX(),e.getY());
				ColorHsv.rgbToHsv((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, color);
				System.out.println("H = " + color[0]+" S = "+color[1]+" V = "+color[2]);

				showSelectedColorInSideRim("Selected",image,color[0],color[1]);
                                showSelectedColorOutSideRim("Selected",image,color[0],color[1]);
			}
		});

		ShowImages.showWindow(gui,"Color Selector");
	}

	/**
	 * Selectively displays only pixels which have a similar hue and saturation values to what is provided.
	 * This is intended to be a simple example of color based segmentation.  Color based segmentation can be done
	 * in RGB color, but is more problematic due to it not being intensity invariant.  More robust techniques
	 * can use Gaussian models instead of a uniform distribution, as is done below.
	 */
	public static void showSelectedColorInSideRim( String name , BufferedImage image , float hue , float saturation ) {
                LinearRegression LeftLinear = null;                
                LinearRegression RightLinear = null;
		MultiSpectral<ImageFloat32> input = ConvertBufferedImage.convertFromMulti(image,null,true,ImageFloat32.class);
		MultiSpectral<ImageFloat32> hsv = new MultiSpectral<ImageFloat32>(ImageFloat32.class,input.width,input.height,3);
                List<Point2D_I32> points = new ArrayList<Point2D_I32>(); 
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
		List<List<Integer[]>> AllTargetXY = new ArrayList<List<Integer[]>>();  
                List<Double> LeftLinearX = new ArrayList<Double>();
                List<Double> LeftLinearY = new ArrayList<Double>();
                List<Double> RightLinearX = new ArrayList<Double>();
                List<Double> RightLinearY = new ArrayList<Double>(); 
                boolean isDetecedLeft = false;
                boolean isDetecedRight = false;
                for (int y = 0; y < hsv.height; y++) {  
                    List<Integer[]> rowTargetXY = new ArrayList<Integer[]>(); 
                    for (int x = 0; x < hsv.width; x++) {
                        // Hue is an angle in radians, so simple subtraction doesn't work
                        float dh = UtilAngle.dist(H.unsafe_get(x, y), hue);
                        float ds = (S.unsafe_get(x, y) - saturation) * adjustUnits;
                        
                        // this distance measure is a bit naive, but good enough for to demonstrate the concept
                        float dist2 = dh * dh + ds * ds;
                        if (dist2 <= maxDist2) {   
                            output.setRGB(x, y, image.getRGB(x, y));                            
                            rowTargetXY.add(new Integer[]{x,y}); 
                            if(x < input.width/2){
                             isDetecedLeft = true;   
                            }
                            if(x > input.width/2+1){
                             isDetecedRight = true;
                            }                                                        
                        }                        
                    }                                        
                    AllTargetXY.add(y,rowTargetXY);                                                            
                }            
                int MaxCounter = 0;
                List<Integer[]> MaxXY = new ArrayList<Integer[]>();     
                for(List<Integer[]> row:AllTargetXY){
                    if(!row.isEmpty()){
                        int maxLeft = 0;          
                        int minRight = input.width;
                        for(Integer[] pixel:row){
                            if(pixel[0] < input.width/2){
                                if(pixel[0] > maxLeft){
                                    maxLeft = pixel[0];
                                } 
                            }     
                            if(pixel[0] > input.width/2){
                                if(pixel[0] < minRight){
                                    minRight = pixel[0];
                                }
                            }
                        }
                        if(maxLeft != 0){
                            MaxXY.add(new Integer[]{maxLeft,MaxCounter});
                            points.add(new Point2D_I32(maxLeft,MaxCounter));
                            LeftLinearX.add((double)maxLeft);
                            LeftLinearY.add((double)MaxCounter);
                            Double[] XArray = new Double[LeftLinearX.size()];
                            Double[] YArray = new Double[LeftLinearY.size()];
                            LeftLinearX.toArray(XArray);
                            LeftLinearY.toArray(YArray);
                            LeftLinear = new LinearRegression(XArray,YArray);
                        }
                        if(minRight != 0){
                            MaxXY.add(new Integer[]{minRight,MaxCounter});
                            points.add(new Point2D_I32(minRight,MaxCounter));
                            RightLinearX.add((double)minRight);
                            RightLinearY.add((double)MaxCounter);
                            Double[] XArray = new Double[RightLinearX.size()];
                            Double[] YArray = new Double[RightLinearY.size()];
                            RightLinearX.toArray(XArray);
                            RightLinearY.toArray(YArray);
                            RightLinear = new LinearRegression(XArray,YArray);
                        }                                                                       
                    }                    
                    MaxCounter++;
                }
                // Draw the tracks
                Graphics2D g2 = output.createGraphics();
                g2.setColor(Color.BLUE);
                g2.setStroke(new BasicStroke(5));
                if(isDetecedLeft && isDetecedRight){
                    g2.drawLine(0,(int)LeftLinear.predict(0),input.width/2,(int)LeftLinear.predict(input.width/2));
                    g2.drawLine((input.width/2)+1,(int)RightLinear.predict((input.width/2)+1),input.width,(int)RightLinear.predict(input.width));
//                    g2.drawLine(input.width/2,0,input.width/2,input.height);
                }                
                VisualizeFeatures.drawPoints(g2, Color.red, points, 1);  

		ShowImages.showWindow(output,"Showing "+name);
	}
        public static void showSelectedColorOutSideRim( String name , BufferedImage image , float hue , float saturation ) {
                LinearRegression LeftLinear = null;                
                LinearRegression RightLinear = null;
		MultiSpectral<ImageFloat32> input = ConvertBufferedImage.convertFromMulti(image,null,true,ImageFloat32.class);
		MultiSpectral<ImageFloat32> hsv = new MultiSpectral<ImageFloat32>(ImageFloat32.class,input.width,input.height,3);
                List<Point2D_I32> points = new ArrayList<Point2D_I32>(); 
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
		List<List<Integer[]>> AllTargetXY = new ArrayList<List<Integer[]>>();  
                List<Double> LeftLinearX = new ArrayList<Double>();
                List<Double> LeftLinearY = new ArrayList<Double>();
                List<Double> RightLinearX = new ArrayList<Double>();
                List<Double> RightLinearY = new ArrayList<Double>(); 
                boolean isDetected = false;                
                for (int y = 0; y < hsv.height; y++) {  
                    List<Integer[]> rowTargetXY = new ArrayList<Integer[]>(); 
                    for (int x = 0; x < hsv.width; x++) {
                        // Hue is an angle in radians, so simple subtraction doesn't work
                        float dh = UtilAngle.dist(H.unsafe_get(x, y), hue);
                        float ds = (S.unsafe_get(x, y) - saturation) * adjustUnits;
                        
                        // this distance measure is a bit naive, but good enough for to demonstrate the concept
                        float dist2 = dh * dh + ds * ds;
                        if (dist2 <= maxDist2) {   
                            output.setRGB(x, y, image.getRGB(x, y));                            
                            rowTargetXY.add(new Integer[]{x,y});                             
                            isDetected = true;                                                                          
                        }                        
                    }                                        
                    AllTargetXY.add(y,rowTargetXY);                                                            
                }            
                int MaxCounter = 0;
                List<Integer[]> MaxXY = new ArrayList<Integer[]>();     
                for(List<Integer[]> row:AllTargetXY){
                    if(!row.isEmpty()){
                        int maxLeft = 0;          
                        int minRight = input.width;
                        for(Integer[] pixel:row){                            
                            if(pixel[0] > maxLeft){
                                maxLeft = pixel[0];
                            }                                                         
                            if(pixel[0] < minRight){
                                minRight = pixel[0];
                            }                            
                        }
                        if(maxLeft != 0){
                            MaxXY.add(new Integer[]{maxLeft,MaxCounter});
                            points.add(new Point2D_I32(maxLeft,MaxCounter));
                            LeftLinearX.add((double)maxLeft);
                            LeftLinearY.add((double)MaxCounter);
                            Double[] XArray = new Double[LeftLinearX.size()];
                            Double[] YArray = new Double[LeftLinearY.size()];
                            LeftLinearX.toArray(XArray);
                            LeftLinearY.toArray(YArray);
                            LeftLinear = new LinearRegression(XArray,YArray);
                        }
                        if(minRight != 0){
                            MaxXY.add(new Integer[]{minRight,MaxCounter});
                            points.add(new Point2D_I32(minRight,MaxCounter));
                            RightLinearX.add((double)minRight);
                            RightLinearY.add((double)MaxCounter);
                            Double[] XArray = new Double[RightLinearX.size()];
                            Double[] YArray = new Double[RightLinearY.size()];
                            RightLinearX.toArray(XArray);
                            RightLinearY.toArray(YArray);
                            RightLinear = new LinearRegression(XArray,YArray);
                        }                                                                       
                    }                    
                    MaxCounter++;
                }
                // Draw the tracks
                Graphics2D g2 = output.createGraphics();
                g2.setColor(Color.BLUE);
                g2.setStroke(new BasicStroke(5));
                if(isDetected){
                    g2.drawLine(0,(int)RightLinear.predict(0),input.width/2,(int)RightLinear.predict(input.width/2));
                    g2.drawLine((input.width/2)+1,(int)LeftLinear.predict((input.width/2)+1),input.width,(int)LeftLinear.predict(input.width));
//                    g2.drawLine(input.width/2,0,input.width/2,input.height);
                }                
                VisualizeFeatures.drawPoints(g2, Color.red, points, 1);  

		ShowImages.showWindow(output,"Showing "+name);
	}
    public static void main(String[] args) {
        //BufferedImage image = UtilImageIO.loadImage("b.jpg");
        BufferedImage image01 = UtilImageIO.loadImage("corn1.png");
        BufferedImage image02 = UtilImageIO.loadImage("corn2.png");

		// Let the user select a color
		printClickedColor(image01);                
                printClickedColor(image02);                
		// Display pre-selected colors
//		showSelectedColorOutSideRim("Dirt",image01,6.2f,0.37f);
//		showSelectedColorInSideRim("Green",image01,1.5f,0.65f);
//                
//                showSelectedColorOutSideRim("Dirt",image02,6.2f,0.37f);
//		showSelectedColorInSideRim("Green",image02,1.5f,0.65f);
    }
}
