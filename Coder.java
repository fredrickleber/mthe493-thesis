import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//see http://wendykierp.github.io/JTransforms/apidocs/org/jtransforms/dct/DoubleDCT_2D.html
import org.jtransforms.dct.*;

public class Coder implements java.io.Serializable {

	private static final long serialVersionUID = 2L; // for serialization
	
	// the number of bits that each pixel will take up when encoded
	private static final int[][] fixedBitAllocation = {{8, 7, 6, 4, 0, 0, 0, 0}, 
													 {7, 6, 5, 1, 0, 0, 0, 0}, 
													 {6, 5, 2, 0, 0, 0, 0, 0},
													 {4, 1, 0, 0, 0, 0, 0, 0},
													 {0, 0, 0, 0, 0, 0, 0, 0},
													 {0, 0, 0, 0, 0, 0, 0, 0},
													 {0, 0, 0, 0, 0, 0, 0, 0},
													 {0, 0, 0, 0, 0, 0, 0, 0}};
	
	private int coderRate; // encoder/decoder rate. bit allocation is multiplied by this positive integer
	private Map<Integer, COSQ> cosqs;


	public Coder(Map<Integer, COSQ> cosqs, int coderRate) {
		this.cosqs = cosqs;
		this.coderRate = coderRate;
	} // end constructor

	/**
	 * Encodes the image.
	 * @param filename The filename of the image to be encoded.
	 * @return The encoded data, as a List of Bytes.
	 */
	public List<Byte> encodeImage(String filename) {
		double[][] grayScalePixelValues = ImageManager.getGrayScaleValuesFromFilename(filename);
		DoubleDCT_2D dct = new DoubleDCT_2D(grayScalePixelValues.length, grayScalePixelValues[0].length);
		dct.forward(grayScalePixelValues, true); // performs the dct in-place on the given array
		return encodeCoefficients(grayScalePixelValues);
	} // end encodeImage()
	
	/**
	 * Decodes an encoded image into a BufferedImage.
	 * @param encodedData The encoded image data.
	 * @param imageHeight The height of the image to be decoded, in pixels.
	 * @param imageWidth The width of the image to be decoded, in pixels.
	 * @return A decoded BufferedImage.
	 */
	public BufferedImage decodeImage(List<Byte> encodedData, int imageHeight, int imageWidth) {
		List<Double> decodedData = decodeCoefficients(encodedData, imageHeight, imageWidth);
		double[] dctCoeffs = new double[imageHeight * imageWidth]; // row-major order array
		for (int i = 0; i < dctCoeffs.length; i++)
			dctCoeffs[i] = decodedData.get(i);
		
		DoubleDCT_2D dct = new DoubleDCT_2D(imageHeight, imageWidth);
		dct.inverse(dctCoeffs, true); // performs the inverse dct in-place on the given array
		return ImageManager.getBufferedImageFromGrayScaleValues(dctCoeffs, imageHeight);
	} // end decodeImage()
	
	/**
	 * Encodes the image data.
	 * @param dctData The data given after applying the discrete cosine transform.
	 * @return The encoded data, as a List of Bytes.
	 */
	private List<Byte> encodeCoefficients(double[][] dctData) {
		List<Byte> encodedData = new ArrayList<>();
		for (int row = 0; row < dctData.length; row++) {
			for (int col = 0; col < dctData[0].length; col++) {
				if ((row % 8 == 0) && (col % 8 == 0))
					encodedData.addAll(cosqs.get(-1).encodeSourceWord(dctData[row][col])); // dc pixel
				else if (fixedBitAllocation[row % 8][col % 8] != 0) // make sure we are supposed to encode the value
					encodedData.addAll(cosqs.get(fixedBitAllocation[row % 8][col % 8] * coderRate).encodeSourceWord(dctData[row][col]));	
			}
		}
		return encodedData;
	} // end encode()

	/**
	 * Decodes a given piece of encoded data.
	 * @param encodedData The encoded data, as a List of Bytes.
	 * @param imageHeight The height of the image to be decoded, in pixels.
	 * @param imageWidth The width of the image to be decoded, in pixels.
	 * @return The List of Doubles to be fed into the inverse DCT.
	 */
	private List<Double> decodeCoefficients(List<Byte> encodedData, int imageHeight, int imageWidth) {
		List<Double> decodedData = new ArrayList<>();
		
		int bitsDecoded = 0;
		for (int row = 0; row < imageHeight; row++) {
			for (int col = 0; col < imageWidth; col++) {
				if ((row % 8 == 0) && (col % 8 == 0)) {
					decodedData.add(cosqs.get(-1).decodeCodeWord(encodedData.subList(bitsDecoded, bitsDecoded + fixedBitAllocation[0][0] * coderRate))); // dc pixel
					bitsDecoded += fixedBitAllocation[0][0] * coderRate;
				}
				else if (fixedBitAllocation[row % 8][col % 8] != 0) {
					decodedData.add(cosqs.get(fixedBitAllocation[row % 8][col % 8] * coderRate).
							decodeCodeWord(encodedData.subList(bitsDecoded, bitsDecoded + (fixedBitAllocation[row % 8][col % 8] * coderRate))));
					bitsDecoded += fixedBitAllocation[row % 8][col % 8] * coderRate;
				}
				else
					decodedData.add((double) 0); // if we didn't encode that pixel's value
			}
		}
		return decodedData;
	} // end decode()

	/**
	 * Sets the coder rate.
	 * @param coderRate The new coder rate.
	 */
	public void setCoderRate(int coderRate) {
		this.coderRate = coderRate;
	} // end setCoderRate()
	
}
