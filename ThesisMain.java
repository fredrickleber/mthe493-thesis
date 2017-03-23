import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;

public class ThesisMain {

	public static void main(String[] args) throws IOException{
		Channel testChannel = new Channel(0.1, 10);
		Coder testCoder = CoderFactory.createCoder(testChannel, 1);
		List<Byte> encodedImage = testCoder.encodeImage("Lenna.png");
		List<Byte> distortedEncodedImage = testChannel.sendThroughChannel(encodedImage);
			
		BufferedImage image = testCoder.decodeImage(distortedEncodedImage, 512, 512);
		
		
		//File outputfile = new File("With Channel Coding Lenna eps=0.1 del=10.png");
		//ImageIO.write(image, "png", outputfile);

		
		// swing stuff. not great, but just POC
		// shows the image as a pop-up
		JDialog dialog = new JDialog();
		dialog.setUndecorated(true);
		JLabel label = new JLabel(new ImageIcon(image));
		dialog.add(label);
		dialog.pack();
		dialog.setVisible(true);
	}

}
