import java.util.ArrayList;
import java.util.List;

public class IndexMapTrainer{
	
	// Simulated annealing parameters from Julian's thesis
	private final double TEMP_INIT = 10.0;
	private final double TEMP_FINAL = 0.0005;
	private final double COOLING_MULTIPLIER = 0.97;
	private final double MAX_PERTURBATIONS = 100;
	
	// Channel parameters
	private final double BIT_ERROR_RATE, BURST_LEVEL;		// epsilon, delta in thesis
	private final int MARKOV_ORDER;							// M in thesis
	private final double PROB00, PROB01, PROB10, PROB11;	// Transition probabilities given by probAB := P(A|B)
	
	private final int NUM_BITS; // number of bits needed to represent codewords
	private List<List<Byte>> indexMap;
	
	
	public IndexMapTrainer (List<Double> codebook, Channel trainingChannel) throws MarkovMemoryException {
		int m = trainingChannel.getMarkovOrder();
		if (m != 1)
			throw new MarkovMemoryException("Error: IndexMapTrainer only supports M = 1");
		else
			this.MARKOV_ORDER = m;
		this.BIT_ERROR_RATE = trainingChannel.getBitErrorRate();
		this.BURST_LEVEL = trainingChannel.getBurstLevel();
		this.PROB00 = (1 - BIT_ERROR_RATE + BURST_LEVEL) / (1 + BURST_LEVEL);
		this.PROB01 = (1 - BIT_ERROR_RATE) / (1 + BURST_LEVEL);
		this.PROB10 = BIT_ERROR_RATE / (1 +  BURST_LEVEL);
		this.PROB11 = (BIT_ERROR_RATE + BURST_LEVEL) / (1 + BURST_LEVEL);
		this.NUM_BITS = (int) (Math.log(codebook.size())/Math.log(2));
		this.indexMap = initializeIndexMap(codebook.size());
	}
		
	private List<List<Byte>> initializeIndexMap(int size){
		List<List<Byte>> indexMap = new ArrayList<List<Byte>>(size);
		return indexMap;
	}	
	
	public List<List<Byte>> trainIndexMap(List<Double> sourceWords){
		return indexMap;
	}	
	
	/**
	 * Computes the conditional probability
	 * @param j
	 * @param i
	 * @return P(j|b(i))
	 */
	public double conditionalProb(int j, int i) {
		return 1;
	}
	
}
