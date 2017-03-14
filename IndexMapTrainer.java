import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IndexMapTrainer{
	
	// Simulated annealing parameters from Julian's thesis
	private final double TEMP_INIT = 10.0;
	private final double TEMP_FINAL = 0.0025;
	private final double COOLING_MULTIPLIER = 0.97;
	private final double MAX_PERTURBATIONS = 200;
	
	// Channel parameters
	private final double BIT_ERROR_RATE, BURST_LEVEL;		// epsilon, delta in thesis
	private final double PROB00, PROB01, PROB10, PROB11;	// Transition probabilities given by probAB := P(A|B)
	public final double[][] CONDITIONAL_PROB; 				// [i][j] : Conditional probability of changing from index i to index j
	
	// Codebook & relevant parameters
	private final List<Double> codebook;
	private final int NUM_BITS; 							// number of bits needed to represent codewords
	private final int SIZE; 								// number of codewords
	
	
	/**
	 * Constructs an IndexMapTrainer that utilizes the simulated annealing algorithm presented in the thesis.
	 * @param codebook List containing all codewords.
	 * @param trainingChannel Channel used to train data.
	 */
	public IndexMapTrainer (List<Double> codebook, Channel trainingChannel) {
		this.BIT_ERROR_RATE = trainingChannel.getBitErrorRate();
		this.BURST_LEVEL = trainingChannel.getBurstLevel();
		this.codebook = codebook;
		this.SIZE = codebook.size();
		this.NUM_BITS = (int) (Math.log(codebook.size())/Math.log(2));

		this.PROB00 = (1 - BIT_ERROR_RATE + BURST_LEVEL) / (1 + BURST_LEVEL);
		this.PROB01 = (1 - BIT_ERROR_RATE) / (1 + BURST_LEVEL);
		this.PROB10 = BIT_ERROR_RATE / (1 +  BURST_LEVEL);
		this.PROB11 = (BIT_ERROR_RATE + BURST_LEVEL) / (1 + BURST_LEVEL);
		this.CONDITIONAL_PROB = initializeConditionalProb();
	}
	
	/**
	 * Train the index map. Uses the Simulated Annealing (SA) algorithm presented in Julian's thesis.
	 * Note that "state" is the index map in a non-binary form, and the name comes from SA convention.
	 * Additionally, note that the term "energy" is equivalent to expected distortion, but is named as such due to SA convention.
	 * @param trainingData Data used to train the map.
	 * @return Index map, where each index is represented in binary via a byte list.
	 */
	public List<List<Byte>> train(List<Double> trainingData){
		// Initialize State
		ArrayList<Integer> state = new ArrayList<Integer>(SIZE);
		ArrayList<Integer> nextState;
		for (int i = 0; i < SIZE; i++)
			state.add(i);
		ArrayList<Integer> bestState = new ArrayList<Integer>(state);
		
		// Initialize System
		double temp = TEMP_INIT;
		int numPertubations = 0;
		Collections.shuffle(state); // Random initial state
		double energy = expectedDistortion(trainingData, state);
		double newEnergy;
		double oldEnergy = energy;
		double changeInEnergy; 		// delta in thesis
		
		System.out.println("Simulating annealing for rate " + this.NUM_BITS);
		
		// SA algorithm
		while(temp >= TEMP_FINAL) {
			numPertubations = 0;
			while(numPertubations++ < MAX_PERTURBATIONS) {
				// Randomly select new state
				nextState = new ArrayList<Integer>(state);
				Collections.shuffle(nextState);
				newEnergy = expectedDistortion(trainingData, nextState);
				changeInEnergy = newEnergy - energy;
				
				// Decide whether to accept new state
				if (changeInEnergy < 0) {
					state = new ArrayList<Integer>(nextState);
					energy = newEnergy;
				}
				else if (acceptNewState(changeInEnergy, temp)) {
					state = new ArrayList<Integer>(nextState);
					energy = newEnergy;
				}
				
				// Check to see if energy has dropped
				if (energy < oldEnergy) {
					numPertubations = 0;
					oldEnergy = energy;
					bestState = state;
				}
			}
			
			temp *= COOLING_MULTIPLIER; // cool system
			System.out.println(temp);
		}
		return convertToBinary(bestState);
	}
	
	/**
	 * Computes whether the next state will be accepted or not.
	 * @param changeInEnergy Change in energy between new state and old state.
	 * @param temp Temperature of the system.
	 * @return	Boolean informing the algorithm whether or not to accept the new state.
	 */
	private boolean acceptNewState(double changeInEnergy, double temp) {
		double random = Math.random();
		return (random > Math.exp(-changeInEnergy / temp));
	}

	/**
	 * Computes the expected distortion (energy) using the user-provided set of training data.
	 * @param trainingData	List of source words generated by some distribution.
	 * @param state	Current state of the system.
	 * @return Expected distortion (energy).
	 */
	private double expectedDistortion(List<Double> trainingData, ArrayList<Integer> state) {
		double expectedDistortion = 0;
		int mappedIndex;
		int numSourceWords = trainingData.size();
		for (double sourceWord : trainingData) {
			for (int j = 0; j < SIZE; j ++) {
				mappedIndex = state.get(encodedIndex(sourceWord));
				expectedDistortion += CONDITIONAL_PROB[mappedIndex][j] * Math.pow(sourceWord - codebook.get(j), 2) / numSourceWords;
			}
		}
		return expectedDistortion;
	}
	
	/**
	 * Determine the index of the encoded source word.
	 * @param sourceWord
	 * @return Codeword index.
	 */
	private int encodedIndex(double sourceWord) {
		int bestIndex = 0;
		for (int i = 0; i < SIZE; i++) {
			if (Math.abs(codebook.get(i) - sourceWord) < Math.abs(codebook.get(bestIndex) - sourceWord))
				bestIndex = i;
		}
		return bestIndex;
	}
	
	/**
	 * Compute all transition probabilities using the attributes of the channel.
	 * @return	Matrix containing these transition probabilities.
	 */
	private double[][] initializeConditionalProb() {
		double[][] conditionalProb = new double[SIZE][SIZE];
		int errorWord;
		boolean pastError;
		double probError;
		for (int i = 0; i < SIZE; i++) { 			// i = word sent
			for (int j = 0; j < SIZE; j++) {		// j = word received
				errorWord = i ^ j;
				if ((errorWord & 1) == 0) {
					probError = 0.5;
					pastError = false;
				}
				else {
					probError = 0.5;
					pastError = true;
				}
				for (int k = 1; k < NUM_BITS; k++) {
					if (((errorWord >> k) & 1) == 0) {
						probError *= pastError ? PROB01 : PROB00;
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
	
	/**
	 * Converts the index map (state) to a binary (byte list) representation.
	 * @param indexMap
	 * @return	Index map with entries represented in a binary (byte list) form.
	 */
	private List<List<Byte>> convertToBinary(List<Integer> indexMap) {
		List<Byte> binaryForm;
		List<List<Byte>> binaryIndexMap = new ArrayList<List<Byte>>(SIZE);
		for(int index : indexMap) {
			binaryForm = new ArrayList<Byte>(NUM_BITS);
			for (int k = 0; k < NUM_BITS; k++) {
				if (((index >> k) & 1) == 0)
					binaryForm.add(0, (byte) 0);
				else
					binaryForm.add(0, (byte) 1);
			}
			binaryIndexMap.add(binaryForm);
		}
		return binaryIndexMap;
	}
}
