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
		/*
		long timeInit = System.currentTimeMillis();
		double[] bitErrorRates = {0.005, 0.01, 0.1};
		CoderFactory.createMultipleCoders(bitErrorRates, 0);
		long runTime = (System.currentTimeMillis() - timeInit) / 1000;
		System.out.println("All done everything, took " + runTime + " seconds!");
		*/
		
		// CoderFactory.makeCoder(new Channel(0,0), 1);
		testCoder(0.05, 5);
	}
	
	private static void testCoder(double bitErrorRate, double burstLevel) throws IOException {
		Channel testChannel = new Channel(bitErrorRate, burstLevel);
		Coder testCoder = CoderFactory.loadCoder(testChannel, 1);
		List<Byte> encodedImage = testCoder.encodeImage("lenna.png");
		List<Byte> distortedEncodedImage = testChannel.sendThroughChannel(encodedImage);
			
		BufferedImage image = testCoder.decodeImage(distortedEncodedImage, 512, 512);
		
		File outputfile = new File("With1 Channel Coding Lenna eps=" + bitErrorRate + " del=" + burstLevel + ".png");
		ImageIO.write(image, "png", outputfile);

		
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
