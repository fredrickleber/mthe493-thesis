import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;

public class ThesisMain {

	public static void main(String[] args) {
		Channel testChannel = new Channel(0.2, 5);
		Coder testCoder = CoderFactory.createCoder(testChannel, 1);
		List<Byte> encodedImage = testCoder.encodeImage("Lenna.png");
		List<Byte> distortedEncodedImage = testChannel.sendThroughChannel(encodedImage);
		
		
		BufferedImage image = testCoder.decodeImage(encodedImage, 512, 512);
		
		// swing stuff. not great, but just POC
		// shows the image as a pop-up
		JDialog dialog = new JDialog();
		dialog.setUndecorated(true);
		JLabel label = new JLabel( new ImageIcon(image) );
		dialog.add( label );
		dialog.pack();
		dialog.setVisible(true);
	}

}
