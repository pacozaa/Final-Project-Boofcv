/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cornplantlevel1;

import boofcv.core.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.MultiSpectral;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 *
 * @author User
 */
public class CornPlantLevel1 {

    static int HalfWidth;    
    static int MaxWidth;    
    ArrayList<Integer> TargetColorList = new ArrayList<>();
    public static void main(String[] args) {
        BufferedImage image = UtilImageIO.loadImage("CornDemo1.png");
        MultiSpectral<ImageFloat32> input = ConvertBufferedImage.convertFromMulti(image, null, true, ImageFloat32.class);
        HalfWidth = input.width/2;
        MaxWidth = input.width;
    }
    
}
