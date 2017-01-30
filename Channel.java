import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Channel {

	private final double BIT_ERROR_RATE, BURST_LEVEL;
	private final int MARKOV_ORDER;
	private LinkedList<Byte> history;


	/**
	 * 
	 * @param bitErrorRate amount of channel noise (channel bit error rate), must be between 0 and 1 (epsilon in thesis)
	 * @param burstLevel amount of "bursty" behavior, higher is more (delta in theis)
	 * @param markovOrder order of markov process (remembers last M results)
	 */
	public Channel(double bitErrorRate, double burstLevel, int markovOrder) {
		this.BIT_ERROR_RATE = bitErrorRate;
		this.BURST_LEVEL = burstLevel;
		this.MARKOV_ORDER = markovOrder;
	}
	
	public double getBitErrorRate(){
		return BIT_ERROR_RATE;
	}
	
	public double getBurstLevel(){
		return BURST_LEVEL;
	}
	
	public int getMarkovOrder(){
		return MARKOV_ORDER;
	}

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
			if (randNumber <= errorProb)
				potentiallyFlippedBit = (byte) ((encodedImage.get(i) + 1) % 2);
			else
				potentiallyFlippedBit = encodedImage.get(i);
			
			// update output and history queue
			channelOutput.add(potentiallyFlippedBit);
			history.add(potentiallyFlippedBit);
			history.remove();
		}
		return channelOutput;
	}	
	
	private byte sumHistory() {
		byte sum = 0;
		for (Byte bit : history)
			sum += bit;
		return sum;
	}

	private void initializeQueue() {
		this.history = new LinkedList<Byte>();
		for (int i = 0; i < MARKOV_ORDER; i++)
			history.add((byte) 0);
	}

}
