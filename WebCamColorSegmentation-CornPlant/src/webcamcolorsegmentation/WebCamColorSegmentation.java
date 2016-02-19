/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package webcamcolorsegmentation;

import boofcv.alg.color.ColorHsv;
import boofcv.core.image.ConvertBufferedImage;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.MultiSpectral;
import com.github.sarxos.webcam.Webcam;
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
 *
 * @author User
 */
public class WebCamColorSegmentation {
//0,0 is top left
    static boolean status = false;
    static float hue = 1f;
    static float saturation = 1f;
    static int ClickX;
    static int ClickY;                  
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //initialize LinearRegression                                
        // TODO code application logic here
        // tune the tracker for the image size and visual appearance        
        // Open a webcam at a resolution close to 640x480
        Webcam webcam = UtilWebcamCapture.openDefault(640, 480);
        BufferedImage image;
        MultiSpectral<ImageFloat32> input;
        MultiSpectral<ImageFloat32> hsv;
        BufferedImage output;
        // Create the panel used to display the image and
        ImagePanel gui = new ImagePanel();        
        gui.setPreferredSize(webcam.getViewSize());
        gui.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                status = !status; 
                ClickX = e.getX();
                ClickY = e.getY();
            }
        });
        ShowImages.showWindow(gui, "PACOZAA");            
        status = false;
        while (true) {
            if (status) {
                /**
                 * Selectively displays only pixels which have a similar hue and
                 * saturation values to what is provided. This is intended to be
                 * a simple example of color based segmentation. Color based
                 * segmentation can be done in RGB color, but is more
                 * problematic due to it not being intensity invariant. More
                 * robust techniques can use Gaussian models instead of a
                 * uniform distribution, as is done below.
                 */                                
                LinearRegression LeftLinear = null;                
                LinearRegression RightLinear = null;
                image = webcam.getImage();
                input = ConvertBufferedImage.convertFromMulti(image, null, true, ImageFloat32.class);                
                hsv = new MultiSpectral<ImageFloat32>(ImageFloat32.class, input.width, input.height, 3);
                List<Point2D_I32> points = new ArrayList<Point2D_I32>(); 
                // Convert into HSV
                ColorHsv.rgbToHsv_F32(input, hsv);
                
                // Euclidean distance squared threshold for deciding which pixels are members of the selected set
                float maxDist2 = 0.2f * 0.2f;

                // Extract hue and saturation bands which are independent of intensity
                ImageFloat32 H = hsv.getBand(0);
                ImageFloat32 S = hsv.getBand(1);

                // Adjust the relative importance of Hue and Saturation.
                // Hue has a range of 0 to 2*PI and Saturation from 0 to 1.
                float adjustUnits = (float) (Math.PI / 2.0);

                // step through each pixel and mark how close it is to the selected color
                output = new BufferedImage(input.width, input.height, BufferedImage.TYPE_INT_RGB); 
                List<List<Integer[]>> AllTargetXY = new ArrayList<List<Integer[]>>();  
                List<Double> LeftLinearX = new ArrayList<Double>();
                List<Double> LeftLinearY = new ArrayList<Double>();
                List<Double> RightLinearX = new ArrayList<Double>();
                List<Double> RightLinearY = new ArrayList<Double>(); 
                boolean isDetectedLeft = false;
                boolean isDetectedRight = false;
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
                             isDetectedLeft = true;   
                            }
                            if(x > input.width/2+1){
                             isDetectedRight = true;
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
                if(isDetectedLeft && isDetectedRight){
                    g2.drawLine(0,(int)LeftLinear.predict(0),input.width/2,(int)LeftLinear.predict(input.width/2));
                    g2.drawLine((input.width/2)+1,(int)RightLinear.predict((input.width/2)+1),input.width,(int)RightLinear.predict(input.width));
                }                
                VisualizeFeatures.drawPoints(g2, Color.red, points, 1);  
                gui.setBufferedImageSafe(output);
            } 
            else {
                image = webcam.getImage();      
                float[] color = new float[3];   
                int rgb = image.getRGB(ClickX,ClickY);
                ColorHsv.rgbToHsv((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, color);
                //linearRegression = new LinearRegression(TargetX,TargetY);                
                hue = color[0];
                saturation = color[1];
                gui.setBufferedImageSafe(image);
            }
        }
    }

}
