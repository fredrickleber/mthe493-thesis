import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class ImageManager {

	/**
	 * Returns an array in which each element corresponds to the grayscale value for that pixel.
	 * @param filename The filename of the image.
	 * @return The 2d array of grayscale pixel values.
	 */
	public static double[][] getGrayScaleValuesFromFilename(String filename) {
		BufferedImage img = getImageFromFilename(filename);
		int height = img.getHeight();
		int width = img.getWidth();
		
		double[][] grayScaleValues = new double[height][width];
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++)
				grayScaleValues[i][j] = getGrayScaleValueFromRGB(img.getRGB(j, i));
		}
		return grayScaleValues;
	} // end getGrayScalePixelValues()
	
	/**
	 * Generates an image, given the grayscale values for each pixel.
	 * @param grayScaleValues The grayscale values for each pixel, in row-major order.
	 * @param imageHeight The height of the image, in pixels.
	 * @return A generated BufferedImage.
	 */
	public static BufferedImage getBufferedImageFromGrayScaleValues(double[] grayScaleValues, int imageHeight) {
		int imageWidth = grayScaleValues.length / imageHeight;
		BufferedImage recoveredImg = new BufferedImage (imageWidth, imageHeight, BufferedImage.TYPE_BYTE_GRAY);
        recoveredImg.getRaster().setPixels(0, 0, imageWidth, imageHeight, grayScaleValues);
        return recoveredImg;
	} // end getBufferedImageFromGrayScaleValues()
	
	private static BufferedImage getImageFromFilename(String filename) {
		BufferedImage img = null;
		try {
		    img = ImageIO.read(new File(filename));
		} catch (IOException e) {
			System.out.println("Invalid image filename was input.");
		}
		return img;
	} // end getImageFromFilename()
	
	// see http://stackoverflow.com/questions/15972490/bufferedimage-getting-the-value-of-a-pixel-in-grayscale-color-model-picture
	private static double getGrayScaleValueFromRGB(int rgb) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = (rgb & 0xFF);
		return  (r + g + b) / 3.0;
	} // end getGrayScaleValueFromRGB()

}
