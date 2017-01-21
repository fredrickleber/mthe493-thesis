import java.util.LinkedList;
import java.util.Random;

public class Channel {

	private final double EPSILON, DELTA;
	private final int M;
	private LinkedList<Byte> history;

	/**
	 * 
	 * @param eps amount of channel noise (channel bit error rate), must be between 0 and 1
	 * @param del amount of "bursty" behavior, higher is more
	 * @param m order of markov process (remembers last m results)
	 */
	public Channel(double eps, double del, int m) {
		this.EPSILON = eps;
		this.DELTA = del;
		this.M = m;
	}

	public byte[] sendThroughChannel(byte[] encodedImage) {
		initializeQueue();
		byte[] channelOutput = new byte[encodedImage.length];
		
		// can be initialized with seed if testing requires it
		Random rng = new Random();
		
		for (int i = 0; i < encodedImage.length; i++) {
			// calculate error prob (from Julian's thesis)
			double errorProb = (EPSILON + (sumHistory() * DELTA)) / (1 + (M * DELTA));

			double randNumber = rng.nextDouble();
			
			// check whether or not the bit was flipped based on calculated prob
			byte potentiallyFlippedBit;
			if (randNumber <= errorProb)
				potentiallyFlippedBit = modTwoAddOne(encodedImage[i]);
			else
				potentiallyFlippedBit = encodedImage[i];
			
			// update output and history queue
			channelOutput[i] = potentiallyFlippedBit;
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
		for (int i = 0; i < M; i++)
			history.add((byte) 0);
	}

	private byte modTwoAddOne(byte bitToFlip) {
		if (bitToFlip == 0)
			return 1;
		return 0;
	}
}
