import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Channel {

	private final double BIT_ERROR_RATE, BURST_LEVEL;
	private final int MARKOV_ORDER; // the order of the markov process (remembers last M results)
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

}
