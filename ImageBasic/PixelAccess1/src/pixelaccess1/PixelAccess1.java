/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pixelaccess1;

import boofcv.struct.image.ImageFloat32;

/**
 *
 * @author User
 */
public class PixelAccess1 {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ImageFloat32 image = new ImageFloat32(50, 60);

        int x = 0;
        int y = 0;

// using accessors
        image.set(x, y, 46);
        float value1 = image.get(x, y);

// direct access
        float value2 = image.data[image.startIndex + y * image.stride + x] ;
        //Y*image.stride means ===> at Identify 
        float startindex =  image.startIndex;
        float stride = image.stride;
        System.out.println("Index : " + (y*image.stride+x));
        System.out.println("AllRow : " + image.data.length);
        System.out.println("startindex : " + startindex);
        System.out.println("stride : " + stride);
        System.out.println(value1 + " " + value2);
    }

}
