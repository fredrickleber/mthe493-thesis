import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//see http://wendykierp.github.io/JTransforms/apidocs/org/jtransforms/dct/DoubleDCT_2D.html
import org.jtransforms.dct.*;

public class Coder implements java.io.Serializable {

	private static final long serialVersionUID = 2L; 	// for serialization
	private static final int BLOCK_SIZE = 8;			// size of DCT blocks (N in thesis)		
	
	// the number of bits that each pixel will take up when encoded
	private static final int[][] fixedBitAllocation = {{8, 7, 6, 4, 1, 0, 0, 0}, 
													   {7, 6, 5, 1, 0, 0, 0, 0}, 
													   {6, 5, 2, 0, 0, 0, 0, 0},
													   {4, 1, 0, 0, 0, 0, 0, 0},
													   {1, 0, 0, 0, 0, 0, 0, 0},
													   {0, 0, 0, 0, 0, 0, 0, 0},
													   {0, 0, 0, 0, 0, 0, 0, 0},
													   {0, 0, 0, 0, 0, 0, 0, 0}};
	
	private int coderRate; // encoder/decoder rate. bit allocation is multiplied by this positive integer
	private double meanCoeffDC = 0;	// sample mean of the DC coefficients produced by the DCT
	private double meanCoeffAC = 0; // sample mean of the AC coefficients produced by the DCT
	private double varCoeffDC = 0;	// sample variance of the DC coefficients produced by the DCT
	private double varCoeffAC = 0;  // sample variance of the AC coefficients produced by the DCT
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
		int imageHeight = grayScalePixelValues.length;
        int imageWidth = grayScalePixelValues[0].length;
		double[] imageBlockCoeffs = new double[BLOCK_SIZE * BLOCK_SIZE]; // required since DCT is applied in-place
		double[] imageCoefficients = new double[imageHeight * imageWidth]; // row-major form
		double[] normBlockCoeffs = new double[BLOCK_SIZE * BLOCK_SIZE];
		List<Byte> encodedData = new ArrayList<Byte>(imageHeight * imageWidth);
		int rowFactor = imageHeight / BLOCK_SIZE; // number of NxN blocks per row
		int colFactor = imageWidth / BLOCK_SIZE; // number of NxN blocks per column
		
		// apply DCT on NxN grids
		DoubleDCT_2D dct = new DoubleDCT_2D(BLOCK_SIZE, BLOCK_SIZE);
		for (int i = 0; i < rowFactor; i++) {
			for (int j = 0; j < colFactor; j++) {
				// get pixelValues into NxN array
				for (int row = 0; row < BLOCK_SIZE; row++) {
					for (int col = 0; col < BLOCK_SIZE; col++) {
						imageBlockCoeffs[row * BLOCK_SIZE + col] = grayScalePixelValues[i * BLOCK_SIZE + row][j * BLOCK_SIZE + col];
					}
				}
				dct.forward(imageBlockCoeffs, true); // performs the dct in-place on the given array	
				// store coefficients in row-major form
				for (int row = 0; row < BLOCK_SIZE; row++) {
					for (int col = 0; col < BLOCK_SIZE; col++) {
						imageCoefficients[(((i * BLOCK_SIZE) + row) * imageWidth) + (j * BLOCK_SIZE) + col] = imageBlockCoeffs[row * BLOCK_SIZE + col];
					}
				}
			}
		}
		calcSampleMean(imageCoefficients, rowFactor, colFactor);		// compute the sample mean of the dct coefficients
		calcSampleVariance(imageCoefficients, rowFactor, colFactor);	// compute the sample variance of the dct coefficients
		// normalize and encode blocks
		for (int i = 0; i < rowFactor; i++) {
			for (int j = 0; j < colFactor; j++) {
				normBlockCoeffs = normalizeCoefficients(imageCoefficients, i, j, imageWidth);
				encodedData.addAll(encodeCoefficients(normBlockCoeffs)); // encode the block
			}
		}
		return encodedData;
	} // end encodeImage()
	
	/**
	 * Decodes an encoded image into a BufferedImage.
	 * @param encodedData The encoded image data.
	 * @param imageHeight The height of the image to be decoded, in pixels.
	 * @param imageWidth The width of the image to be decoded, in pixels.
	 * @return A decoded BufferedImage.
	 */
	public BufferedImage decodeImage(List<Byte> encodedData, int imageHeight, int imageWidth) {
		int blockArea = BLOCK_SIZE * BLOCK_SIZE;
		List<Double> decodedBlock;
		double[] dctBlock = new double[blockArea];
		double[] dctCoeffs = new double[imageHeight * imageWidth];
		DoubleDCT_2D dct = new DoubleDCT_2D(BLOCK_SIZE, BLOCK_SIZE);
		
		int rowFactor = imageHeight / BLOCK_SIZE;
		int colFactor = imageWidth / BLOCK_SIZE;
		
		// apply inverse DCT for each NxN grids
		int bitsDecoded = 0;
		for (int i = 0; i < rowFactor; i++) {
			for (int j = 0; j < colFactor; j++) {
				// get coefficients for NxN block
				decodedBlock = decodeCoefficients(encodedData.subList(bitsDecoded, bitsDecoded + blockArea));
				dctBlock[0] = Math.sqrt(varCoeffDC) * decodedBlock.get(0) + meanCoeffDC; // de-normalize DC coefficient
				for (int k = 1; k < blockArea; k++)
					dctBlock[k] =  Math.sqrt(varCoeffAC) * decodedBlock.get(k) + meanCoeffAC; // de-normalize AC coefficients
				
				dct.inverse(dctBlock, true); // performs the inverse dct in-place on the given array
				
				// imports the block into the dctCoeffs matrix, converting to row-major form
				for (int row = 0; row < BLOCK_SIZE; row++) {
					for (int col = 0; col < BLOCK_SIZE; col++) {
						dctCoeffs[(((i * BLOCK_SIZE) + row) * imageWidth) + (j * BLOCK_SIZE) + col] = dctBlock[row * BLOCK_SIZE + col];
					}
				}
				bitsDecoded += blockArea;
			}
		}
		return ImageManager.getBufferedImageFromGrayScaleValues(dctCoeffs, imageHeight);
	} // end decodeImage()
	
	/**
	 * Encodes a BLOCK_SIZE by BLOCK_SIZE array of image data.
	 * @param dctData The data given after applying the discrete cosine transform.
	 * @return The encoded data, as a List of Bytes.
	 */
	private List<Byte> encodeCoefficients(double[] dctData) {
		List<Byte> encodedData = new ArrayList<>();
		for (int row = 0; row < BLOCK_SIZE; row++) {
			for (int col = 0; col < BLOCK_SIZE; col++) {
				if ((row == 0) && (col == 0))
					encodedData.addAll(cosqs.get(-1).encodeSourceWord(dctData[row * BLOCK_SIZE + col])); // dc pixel
				else if (fixedBitAllocation[row][col] != 0) // make sure we are supposed to encode the value
					encodedData.addAll(cosqs.get(fixedBitAllocation[row][col] * coderRate).encodeSourceWord(dctData[row * BLOCK_SIZE + col]));	
			}
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
		
		int bitsDecoded = 0;
		for (int row = 0; row < BLOCK_SIZE; row++) {
			for (int col = 0; col < BLOCK_SIZE; col++) {
				if ((row == 0) && (col == 0)) {
					decodedData.add(cosqs.get(-1).decodeCodeWord(encodedData.subList(bitsDecoded, bitsDecoded + fixedBitAllocation[0][0] * coderRate))); // dc pixel
					bitsDecoded += fixedBitAllocation[0][0] * coderRate;
				}
				else if (fixedBitAllocation[row][col] != 0) {
					decodedData.add(cosqs.get(fixedBitAllocation[row][col] * coderRate).
							decodeCodeWord(encodedData.subList(bitsDecoded, bitsDecoded + (fixedBitAllocation[row][col] * coderRate))));
					bitsDecoded += fixedBitAllocation[row][col] * coderRate;
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
	
	/**
	 * Calculate the sample mean of the DCT coefficients.
	 * @param coefficients DCT coefficients.
	 * @param rowFactor	Number of NxN blocks spanning the height of the image.
	 * @param colFactor Number of NxN blocks spanning the width of the image.
	 */
	private void calcSampleMean(double[] coefficients, int rowFactor, int colFactor) {
		int imageWidth = BLOCK_SIZE * colFactor; // width of image (pixels)
		int numPixelDC = rowFactor * colFactor; // number of DC pixels
		int numPixelAC = coefficients.length  - numPixelDC; // number of AC pixels
		int index; // index in row-major form
		for (int i = 0; i < rowFactor; i++) {
			for (int j = 0; j < colFactor; j++) {
				// Iterate through the NxN block
				for (int row = 0; row < BLOCK_SIZE; row++) {
					for (int col = 0; col < BLOCK_SIZE; col++) {
						index = ((i * BLOCK_SIZE) + row) * imageWidth + j * BLOCK_SIZE + col;
						if (row == 0 && col == 0)
							meanCoeffDC += coefficients[index] / numPixelDC;
						else
							meanCoeffAC += coefficients[index] / numPixelAC;
					}
				}
			}
		}
	} // end calcSampleMean()
	
	/**
	 * Calculate the sample variance of the DCT coefficients.
	 * @param coefficients DCT coefficients.
	 * @param rowFactor	Number of NxN blocks spanning the height of the image.
	 * @param colFactor Number of NxN blocks spanning the width of the image.
	 */
	private void calcSampleVariance(double[] coefficients, int rowFactor, int colFactor) {
		int imageWidth = BLOCK_SIZE * colFactor; // width of image (pixels)
		int numPixelDC = rowFactor * colFactor; // number of DC pixels
		int numPixelAC = coefficients.length - numPixelDC; // number of AC pixels
		int index; // index in row-major form
		for (int i = 0; i < rowFactor; i++) {
			for (int j = 0; j < colFactor; j++) {
				// Iterate through the NxN block
				for (int row = 0; row < BLOCK_SIZE; row++) {
					for (int col = 0; col < BLOCK_SIZE; col++) {
						index = ((i * BLOCK_SIZE) + row) * imageWidth + j * BLOCK_SIZE + col;
						if (row == 0 && col == 0)
							varCoeffDC += Math.pow(coefficients[index] - meanCoeffDC, 2) / numPixelDC;
						else
							varCoeffAC += Math.pow(coefficients[index] - meanCoeffAC, 2) / numPixelAC;
					}
				}
			}
		}
	} // end calcSampleVariance()
	
	/**
	 * Normalize the DCT coefficients.
	 * @param coefficients DCT coefficients.
	 * @param blockRow	Row index of the block being encoded.
	 * @param blockCol  Column index of the block being encoded.
	 */
	private double[] normalizeCoefficients(double[] coefficients, int blockRow, int blockCol, int imageWidth) {
		double[] normCoeffs = new double[coefficients.length]; // normalized coefficients
		int index; // index in row-major form
		// get pixelValues into NxN array
		for (int row = 0; row < BLOCK_SIZE; row++) {
			for (int col = 0; col < BLOCK_SIZE; col++) {
				index = ((blockRow * BLOCK_SIZE) + row) * imageWidth + blockCol * BLOCK_SIZE + col;
				if (row == 0 && col == 0)
					normCoeffs[row * BLOCK_SIZE + col] = (coefficients[index] - meanCoeffDC) / Math.sqrt(varCoeffDC);
				else
					normCoeffs[row * BLOCK_SIZE + col] = (coefficients[index] - meanCoeffAC) / Math.sqrt(varCoeffAC);
			}
		}
		return normCoeffs;
	} // end normalizeCoefficients()
}

