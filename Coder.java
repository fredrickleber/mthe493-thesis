import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Coder implements java.io.Serializable {

	private static final long serialVersionUID = 2L; // for serialization
	
	// TODO: Assign something to this, from the thesis
	// the number of bits that each pixel will take up when encoded
	// this is a 1d array because zigzag scanning is used, see http://www.bretl.com/mpeghtml/zigzag.HTM
	private static final int[] fixedBitAllocation; 

	private int coderRate; // encoder/decoder rate. bit allocation is multiplied by this positive integer
	private Map<Integer, COSQ> cosqs;


	public Coder(Map<Integer, COSQ> cosqs, int coderRate) {
		this.cosqs = cosqs;
		this.coderRate = coderRate;
	} // end constructor

	/**
	 * Encodes the image data.
	 * @param dctData The data given after applying the discrete cosine transform.
	 * @return The encoded data, as a List of Bytes.
	 */
	public List<Byte> encode(List<Double> dctData) {
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
	public List<Double> decode(List<Byte> encodedData) {
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

}
