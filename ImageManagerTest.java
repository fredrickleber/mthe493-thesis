import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;

import org.jtransforms.dct.DoubleDCT_2D;

public class ImageManagerTest {

	public static final String FILENAME = "Lenna.png";
	
	
	public static void main(String[] args) {
		double[][] grayScale = ImageManager.getGrayScaleValuesFromFilename(FILENAME);
		
		int imageHeight = grayScale.length;
		int imageWidth = grayScale[0].length;
		
		double[] rowMajorGrayScale = new double[imageHeight * imageWidth];
		
		int offset = 0; // convert to row-major order
		for (double[] rowArray : grayScale){
			for (int j = 0; j < imageWidth; j++)
				rowMajorGrayScale[j + offset] = rowArray[j];
			offset += imageWidth;
		}
		
		// test dct
		DoubleDCT_2D dct = new DoubleDCT_2D(imageHeight, imageWidth);
		dct.forward(rowMajorGrayScale, true);
		dct.inverse(rowMajorGrayScale, true);
		
		BufferedImage image = ImageManager.getBufferedImageFromGrayScaleValues(rowMajorGrayScale, imageHeight);
		
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
