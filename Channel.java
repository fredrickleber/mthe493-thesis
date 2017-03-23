import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Channel {

	private final double BIT_ERROR_RATE, BURST_LEVEL;
	private final int MARKOV_ORDER; // the order of the markov process (remembers last M results)
	private final double PROB00, PROB01, PROB10, PROB11;	// Transition probabilities given by probAB := P(A|B)
	private LinkedList<Byte> history;


	/**
	 * This constructor assumes the markovOrder is 1.
	 * @param bitErrorRate The amount of channel noise (channel bit error rate), must be between 0 and 1 (epsilon in thesis).
	 * @param burstLevel The amount of "bursty" behavior, higher is more (delta in thesis).
	 */
	public Channel(double bitErrorRate, double burstLevel) {
		this.BIT_ERROR_RATE = bitErrorRate;
		this.BURST_LEVEL = burstLevel;
		this.MARKOV_ORDER = 1;
		this.PROB00 = (1 - BIT_ERROR_RATE + BURST_LEVEL) / (1 + BURST_LEVEL);
		this.PROB01 = (1 - BIT_ERROR_RATE) / (1 + BURST_LEVEL);
		this.PROB10 = BIT_ERROR_RATE / (1 +  BURST_LEVEL);
		this.PROB11 = (BIT_ERROR_RATE + BURST_LEVEL) / (1 + BURST_LEVEL);
	} // end two-parameter constructor
	
	public double getBitErrorRate(){
		return BIT_ERROR_RATE;
	} // end bitErrorRate accessor
	
	public double getBurstLevel(){
		return BURST_LEVEL;
	} // end burstLevel accessor
	
	public int getMarkovOrder(){
		return MARKOV_ORDER;
	} // end markovOrder accessor

	/**
	 * Send the data through the channel.
	 * @param encodedImage List of Bytes, representing the bit stream.
	 * @return channelOutput ArrayList of Bytes, representing the output bit stream.
	 */
	public List<Byte> sendThroughChannel(List<Byte> encodedImage) {
		initializeQueue();
		List<Byte> channelOutput = new ArrayList<Byte>(encodedImage.size());
		
		// can be initialized with seed if testing requires it
		Random rng = new Random();
		
		for (int i = 0; i < encodedImage.size(); i++) {
			// calculate error prob (from Julian's thesis)
			double errorProb = (BIT_ERROR_RATE + (sumHistory() * BURST_LEVEL)) / (1 + (MARKOV_ORDER * BURST_LEVEL));
			double randNumber = rng.nextDouble();
			
			// check whether or not the bit was flipped based on calculated prob
			byte potentiallyFlippedBit;
			if (randNumber <= errorProb) {
				potentiallyFlippedBit = (byte) ((encodedImage.get(i) + 1) % 2);
				history.add((byte) 1);
			}
			else {
				potentiallyFlippedBit = encodedImage.get(i);
				history.add((byte) 0);
			}
			
			// update output and history queue
			channelOutput.add(potentiallyFlippedBit);
			history.remove();
		}
		return channelOutput;
	} // end sendThroughChannel()
	
	private byte sumHistory() {
		byte sum = 0;
		for (Byte bit : history)
			sum += bit;
		return sum;
	} // end sumHistory()

	private void initializeQueue() {
		this.history = new LinkedList<Byte>();
		for (int i = 0; i < MARKOV_ORDER; i++)
			history.add((byte) 0);
	} // end initializeQueue()
	
	/**
	 * Compute all transition probabilities using the attributes of the channel.
	 * @return	Matrix containing these transition probabilities.
	 */
	public double[][] initializeConditionalProb(int size) {
		double[][] conditionalProb = new double[size][size];
		int errorWord;
		boolean pastError;
		double probError;
		for (int i = 0; i < size; i++) { 			// i = word sent
			for (int j = 0; j < size; j++) {		// j = word received
				errorWord = i ^ j;
				if ((errorWord & 1) == 0) {
					probError = 0.5;
					pastError = false;
				}
				else {
					probError = 0.5;
					pastError = true;
				}
				for (int k = 1; k < size; k++) {
					if (((errorWord >> k) & 1) == 0) {
						probError *= pastError ? PROB00 : PROB00;
						pastError = false;
					}
					else {
						probError *= pastError ? PROB11 : PROB10;
						pastError = true;
					}
				}
				conditionalProb[i][j] = probError;
			}
		}
		return conditionalProb;
	}

}
