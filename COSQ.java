import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class COSQ implements java.io.Serializable {

	private static final long serialVersionUID = 1L; // for serialization

	// these are just inverses of each other. a bit ugly, but quite convenient
	private Map<Double, List<Byte>> encoderMap = new HashMap<>();
	private Map<List<Byte>, Double> decoderMap = new HashMap<>();


	public COSQ(List<Double> codeWords, List<List<Byte>> binaryRepresentation) {
		for (int i = 0; i < codeWords.size(); i++) {
			encoderMap.put(codeWords.get(i), binaryRepresentation.get(i));
			decoderMap.put(binaryRepresentation.get(i), codeWords.get(i));
		}
	} // end constructor
	
	/**
	 * Finds the closest key in the map to the sourceWord, then returns what that key maps to.
	 * @param sourceWord The source word that is to be encoded.
	 * @return A List<Byte> which represents a binary number.
	 */
	public List<Byte> encodeSourceWord(double sourceWord) {
		Double bestKey = null;
		for (Double key : encoderMap.keySet()) {
			if (bestKey == null || Math.abs(key - sourceWord) < Math.abs(bestKey - sourceWord))
				bestKey = key;
		}
		return encoderMap.get(bestKey);
	} // end encodeSourceWord()
	
	/**
	 * Decodes a codeword.
	 * @param codeWord The codeword that is to be decoded.
	 * @return A double which can then be fed into the inverse DCT.
	 */
	public double decodeCodeWord(List<Byte> codeWord) {
		return decoderMap.get(codeWord);
	} // end decodeCodeWord()
	
}
