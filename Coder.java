import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

//see http://wendykierp.github.io/JTransforms/apidocs/org/jtransforms/dct/DoubleDCT_2D.html
import org.jtransforms.dct.*;

public class Coder implements java.io.Serializable {

	private static final long serialVersionUID = 2L; // for serialization
	
	// TODO: Assign something to this, from the thesis
	// the number of bits that each pixel will take up when encoded, in row-major order
	private static final int[] fixedBitAllocation; 
	
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
		dct.forward(grayScalePixelValues, false); // performs the dct in-place on the given array
		
		List<Double> dctData = new ArrayList<>();
		for (double[] rowArray : grayScalePixelValues){
			for (double dctCoeff : rowArray){
				dctData.add(dctCoeff);
			}
		}
		return encodeCoefficients(dctData);
	} // end encodeImage()
	
	/**
	 * Decodes an encoded image into a BufferedImage.
	 * @param encodedData The encoded image data.
	 * @param imageHeight The height of the image to be decoded, in pixels.
	 * @return A decoded BufferedImage.
	 */
	public BufferedImage decodeImage(List<Byte> encodedData, int imageHeight) {
		List<Double> decodedData = decodeCoefficients(encodedData);
		int imageWidth = decodedData.size() / imageHeight;
		double[] dctCoeffs = new double[imageHeight * imageWidth]; // row-major order array
		for (int i = 0; i < dctCoeffs.length; i++)
			dctCoeffs[i] = decodedData.get(i);
		
		DoubleDCT_2D dct = new DoubleDCT_2D(imageHeight, imageWidth);
		dct.inverse(dctCoeffs, false); // performs the inverse dct in-place on the given array
		return ImageManager.getBufferedImageFromGrayScaleValues(dctCoeffs, imageHeight);
	} // end decodeImage()
	
	/**
	 * Encodes the image data.
	 * @param dctData The data given after applying the discrete cosine transform.
	 * @return The encoded data, as a List of Bytes.
	 */
	private List<Byte> encodeCoefficients(List<Double> dctData) {
		List<Byte> encodedData = new ArrayList<>();
		encodedData.addAll(cosqs.get(-1).encodeSourceWord(dctData.get(0))); // dc pixel
		for (int i = 1; i < dctData.size(); i++) { // ac pixels
			encodedData.addAll(cosqs.get(fixedBitAllocation[i] * coderRate).encodeSourceWord(dctData.get(i)));
		}
		return encodedData;
	} // end encode()

	/**
	 * Decodes a given piece of encoded data.
	 * @param encodedData The encoded data, as a List of Bytes.
	 * @return The List of Doubles to be fed into the inverse DCT.
	 */
	private List<Double> decodeCoefficients(List<Byte> encodedData) {
		List<Double> decodedData = new ArrayList<>();
		decodedData.add(cosqs.get(-1).decodeCodeWord(encodedData.subList(0, fixedBitAllocation[0] * coderRate))); // dc pixel
		
		int subIndex = fixedBitAllocation[0] * coderRate; // skip the bits used for the dc pixel
		for (int i = 1; i < fixedBitAllocation.length; i++) { // ac pixels
			// codeword i is encoded with (fixedBitAllocation[i] * coderRate) bits
			decodedData.add(cosqs.get(fixedBitAllocation[i] * coderRate).
					decodeCodeWord(encodedData.subList(subIndex, subIndex + (fixedBitAllocation[i] * coderRate))));
			subIndex += fixedBitAllocation[i] * coderRate;
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
