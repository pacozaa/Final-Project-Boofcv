/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package takeapicture;

import com.github.sarxos.webcam.Webcam;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;

/**
 *
 * @author User
 */
public class TakeAPicture {
public static void main(String[] args) throws IOException {                
                final Runnable runnableFinish = (Runnable) Toolkit.getDefaultToolkit().getDesktopProperty("win.sound.default");                
		// get default webcam and open it
		Webcam webcam = Webcam.getDefault();
                webcam.setViewSize(new Dimension(640, 480));
		webcam.open();                
		// get image
		BufferedImage image = webcam.getImage();
                Date date = new Date() ;
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss") ;
		// save image to PNG file
		ImageIO.write(image, "PNG", new File(dateFormat.format(date)+".png"));     
                if (runnableFinish != null){
                    runnableFinish.run();
                }                
	}
    
}
