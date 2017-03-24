import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeMapTrainer {

	public static final double EPSILON = 0.0005; // a small number
	
	private List<Double> sourceVectors, codeVectors;
	private Map<Double, Double> codemap;
	private Map<Double, Integer> codeIndexMap;
	private double[][] transitionMatrix;
	
	/**
	 * Performs scalar quantization on a set of (1-dimensional) source vectors
	 * @param sourceVectors The source data to be quantized.
	 * @param desiredNumCodeVectors The minimum desired number of code vectors. Note that the
	 * number of code vectors must be a power of two, so this number is only a lower bound.
	 * @return The list of code vectors.
	 */
	public List<Double> generateInitialCodebook(List<Double> sourceVectors, int desiredNumCodeVectors) {
		this.sourceVectors = sourceVectors;
		this.codeVectors = new ArrayList<>();
		double prevAvgDistortion; // D^(i-1) on data-compression.com
		double currAvgDistortion;// D^(i) on data-compression.com
		
		// set up the initial code vector, as seen in Step 2 on data-compression.com
		double initialCodeVector = (1.0 / sourceVectors.size()) * 
				(sourceVectors.stream().reduce(0.0, (x,y) -> x+y));
		codeVectors.add(initialCodeVector);
		
		updateCodemap();
		currAvgDistortion = calculateAverageDistortion();
		
		while (codeVectors.size() < desiredNumCodeVectors) {
			splitCodeVectors();
			do {
				prevAvgDistortion = currAvgDistortion;
				updateCodemap();
				updateCodeVectors();
				currAvgDistortion = calculateAverageDistortion();
			} while((prevAvgDistortion - currAvgDistortion) /
					prevAvgDistortion > CodeMapTrainer.EPSILON);
		}
		updateCodemap();
		
		Collections.sort(codeVectors);
		return codeVectors;
	} // end generateInitialCodebook()
	
	/**
	 * This generates an updated codebook for a new channel.
	 * @param sourceVectors The training data.
	 * @param initialCodebook The old codebook, trained on another channel.
	 * @param channel The new channel that the codebook will be trained for.
	 * @return
	 */
	public List<Double> generateUpdatedCodebook(List<Double> sourceVectors, List<Double> initialCodebook, Channel channel) {
		this.sourceVectors = sourceVectors;
		this.codeVectors = initialCodebook;
		this.transitionMatrix = channel.initializeConditionalProb(this.codeVectors.size());
		
		updateCodemapWithProbability();
		double prevAvgDistortion; // D^(i-1) on data-compression.com
		double currAvgDistortion = calculateAverageDistortionWithProbability(); // D^(i) on data-compression.com
		
		do {
			prevAvgDistortion = currAvgDistortion;
			updateCodemapWithProbability();
			updateCodeVectorsWithProbability();
			currAvgDistortion = calculateAverageDistortionWithProbability();
		} while((prevAvgDistortion - currAvgDistortion) /
				prevAvgDistortion > CodeMapTrainer.EPSILON);
		
		updateCodemapWithProbability();
		
		// Collections.sort(codeVectors); Please not this is NOT used in this case
		return codeVectors;		
	} // end generateUpdatedCodebook()

	private void splitCodeVectors() {
		List<Double> tempCodeVectors = new ArrayList<>();
		for (Double codeVector : codeVectors) {
			tempCodeVectors.add((1 + CodeMapTrainer.EPSILON) * codeVector);
			tempCodeVectors.add((1 - CodeMapTrainer.EPSILON) * codeVector);
		}
		codeVectors = tempCodeVectors;
	} // end splitCodeVectors()

	private void updateCodemap() {
		this.codemap = new HashMap<>();
		for (Double sourceVector : sourceVectors) {
			double currentMinimumDistanceVector = codeVectors.get(0);
			for (Double codeVector : codeVectors) {
				// the following is left as pow instead of abs just to be consistent with
				// data-compression.com and MSE, although abs would work the same and
				// likely be (in theory) slightly faster
				if (Math.pow(sourceVector - codeVector, 2) <
						Math.pow(sourceVector - currentMinimumDistanceVector, 2))
					currentMinimumDistanceVector = codeVector;
			}
			codemap.put(sourceVector, currentMinimumDistanceVector);
		}
	} // end updateCodemap()

	/**
	 * Updates codemap and codeIndexMap which map each sourceword to the appropriate codeword and index, respectively.
	 */
	private void updateCodemapWithProbability() {
		this.codemap = new HashMap<>(sourceVectors.size());
		this.codeIndexMap = new HashMap<>(sourceVectors.size());
		int bestIndex;
		for (double sourceVector : sourceVectors) {
			bestIndex = calcBestIndex(sourceVector);
			codeIndexMap.put(sourceVector, bestIndex);
			codemap.put(sourceVector, codeVectors.get(bestIndex));
		}
	} // end updateCodemapWithProbability()
	
	// see page 30 of thesis
	private void updateCodeVectors() {
		List<Double> tempCodeVectors = new ArrayList<>();
		double numeratorSum, denominatorSum;
		for (Double codeVector : codeVectors) {
			numeratorSum = 0;
			denominatorSum = 0;
			for (Double sourceVector : sourceVectors) {
				if (codemap.get(sourceVector).equals(codeVector)) {
					numeratorSum += sourceVector;
					denominatorSum++;
				}
			}
			tempCodeVectors.add(numeratorSum / denominatorSum);
		}
		codeVectors = tempCodeVectors;
	} // end updateCodeVectors()

	private void updateCodeVectorsWithProbability() {
		List<Double> tempCodeVectors = new ArrayList<>();
		double numeratorSum, denominatorSum;
		for (int j = 0; j < codeVectors.size(); j++) {
			numeratorSum = 0;
			denominatorSum = 0;
			for (int i = 0; i < codeVectors.size(); i++) {
				for (double sourceVector : sourceVectors) {
					if (codemap.get(sourceVector).equals(codeVectors.get(i))) {
						numeratorSum += transitionMatrix[i][j] * sourceVector / sourceVectors.size();
						denominatorSum += transitionMatrix[i][j] / sourceVectors.size();
					}
				}
			}
			tempCodeVectors.add(numeratorSum / denominatorSum);
		}
		if (codeVectors.size() == 4)
			System.out.println(tempCodeVectors);
		codeVectors = tempCodeVectors;	
	} // end updateCodeVectorsWithProbability() 
	
	private double calculateAverageDistortion() {
		double averageDistortion = 0;
		for (Double sourceVector : sourceVectors)
			averageDistortion += Math.pow(sourceVector - codemap.get(sourceVector), 2);
		averageDistortion *= 1.0 / sourceVectors.size();
		return averageDistortion;
	} // end calculateAverageDistortion()
	
	private double calculateAverageDistortionWithProbability() {
		double averageDistortion = 0;
		for (int j = 0; j < codeVectors.size(); j++) {
			for (double sourceVector : sourceVectors) {
					averageDistortion += transitionMatrix[codeIndexMap.get(sourceVector)][j] * Math.pow(sourceVector - codeVectors.get(j), 2);
			}
		}
		averageDistortion *= 1.0 / sourceVectors.size();
		return averageDistortion;
	} // end calculateAverageDistortion()
	
	/**
	 * Determine the index of the encoded source word according to the generalized nearest neighbor condition
	 * @param sourceWord
	 * @return Codeword index.
	 */
	private int calcBestIndex(double sourceWord) {
		int bestIndex = 0;
		double bestDistortion = -1; // inf
		double currentDistortion;
		for (int l = 0; l < codeVectors.size(); l++) {
			currentDistortion = 0;			
			for (int j = 0; j < codeVectors.size(); j++)
				currentDistortion += Math.pow(sourceWord - codeVectors.get(j), 2) * transitionMatrix[l][j];
			if (bestDistortion == -1 || bestDistortion > currentDistortion) {
				bestDistortion = currentDistortion;
				bestIndex = l;
			}
		}
		return bestIndex;
	}
}
