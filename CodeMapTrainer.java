import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeMapTrainer {

	public static final double EPSILON = 0.001; // a small number
	
	private double dStar; // average distortion
	private List<Double> sourceVectors, codeVectors;
	private Map<Double, Double> codebook;
	
	/**
	 * Performs scalar quantization on a set of (1-dimensional) source vectors
	 * @param sourceVectors The source data to be quantized.
	 * @param desiredNumCodeVectors The minimum desired number of code vectors. Note that the
	 * number of code vectors must be a power of two, so this number is only a lower bound.
	 * @return The list of code vectors.
	 */
	public List<Double> generateCodebook(List<Double> sourceVectors, int desiredNumCodeVectors) {
		this.sourceVectors = sourceVectors;
		this.codeVectors = new ArrayList<>();
		double prevAvgDistortion; // D^(i-1) on data-compression.com
		double currAvgDistortion;// D^(i) on data-compression.com
		
		// set up the initial code vector, as seen in Step 2 on data-compression.com
		double initialCodeVector = (1.0 / sourceVectors.size()) * 
				(sourceVectors.stream().reduce(0.0, (x,y) -> x+y));
		codeVectors.add(initialCodeVector);
		
		// set up the initial distortion, as seen in Step 2 on data-compression.com
		dStar = 0;
		for (Double sourceVector : sourceVectors)
			dStar += Math.pow(sourceVector - initialCodeVector, 2);
		dStar *= (1.0 / sourceVectors.size());
		
		while (codeVectors.size() < desiredNumCodeVectors) {
			splitCodeVectors();
			currAvgDistortion = dStar; // this could go out of the loop, but is here for clarity
			do {
				prevAvgDistortion = currAvgDistortion;
				updateCodebook();
				updateCodeVectors();
				currAvgDistortion = calculateAverageDistortion();
			} while((prevAvgDistortion - currAvgDistortion) /
					prevAvgDistortion > CodeMapTrainer.EPSILON);
			dStar = currAvgDistortion;
		}
		updateCodebook();
		
		Collections.sort(codeVectors);
		return codeVectors;
	} // end generateCodebook()
	
	
	// LEFT AS A LEGACY METHOD SO THAT THE TESTING CLASS STILL WORKS
	/**
	 * Performs scalar quantization on a set of (1-dimensional) source vectors
	 * @param sourceVectors The source data to be quantized.
	 * @param desiredNumCodeVectors The minimum desired number of code vectors. Note that the
	 * number of code vectors must be a power of two, so this number is only a lower bound.
	 * @return The codebook, mapping the input source data to its corresponding codeword.
	 */
	public Map<Double, Double> generateCodeMap(List<Double> sourceVectors, int desiredNumCodeVectors) {
		this.sourceVectors = sourceVectors;
		this.codeVectors = new ArrayList<>();
		double prevAvgDistortion; // D^(i-1) on data-compression.com
		double currAvgDistortion;// D^(i) on data-compression.com
		
		// set up the initial code vector, as seen in Step 2 on data-compression.com
		double initialCodeVector = (1.0 / sourceVectors.size()) * 
				(sourceVectors.stream().reduce(0.0, (x,y) -> x+y));
		codeVectors.add(initialCodeVector);
		
		// set up the initial distortion, as seen in Step 2 on data-compression.com
		dStar = 0;
		for (Double sourceVector : sourceVectors)
			dStar += Math.pow(sourceVector - initialCodeVector, 2);
		dStar *= (1.0 / sourceVectors.size());
		
		while (codeVectors.size() < desiredNumCodeVectors) {
			splitCodeVectors();
			currAvgDistortion = dStar; // this could go out of the loop, but is here for clarity
			do {
				prevAvgDistortion = currAvgDistortion;
				updateCodebook();
				updateCodeVectors();
				currAvgDistortion = calculateAverageDistortion();
			} while((prevAvgDistortion - currAvgDistortion) /
					prevAvgDistortion > CodeMapTrainer.EPSILON);
			dStar = currAvgDistortion;
		}
		updateCodebook();
		return codebook;
	} // end generateCodeMap()

	private void splitCodeVectors() {
		List<Double> tempCodeVectors = new ArrayList<>();
		for (Double codeVector : codeVectors) {
			tempCodeVectors.add((1 + CodeMapTrainer.EPSILON) * codeVector);
			tempCodeVectors.add((1 - CodeMapTrainer.EPSILON) * codeVector);
		}
		codeVectors = tempCodeVectors;
	} // end splitCodeVectors()

	private void updateCodebook() {
		this.codebook = new HashMap<>();
		for (Double sourceVector : sourceVectors) {
			double currentMinimumDistanceVector = codeVectors.get(0);
			for (Double codeVector : codeVectors) {
				// the following is left as pow instead of abs just to be consistent with
				// data-compression.com, although abs would work the same and likely be (in theory)
				// slightly faster
				if (Math.pow(sourceVector - codeVector, 2) <
						Math.pow(sourceVector - currentMinimumDistanceVector, 2))
					currentMinimumDistanceVector = codeVector;
			}
			codebook.put(sourceVector, currentMinimumDistanceVector);
		}
	} // end updateCodebook()

	private void updateCodeVectors() {
		List<Double> tempCodeVectors = new ArrayList<>();
		for (Double codeVector : codeVectors) {
			double numeratorSum = 0;
			double denominatorSum = 0;
			for (Double sourceVector : sourceVectors) {
				if (codebook.get(sourceVector).equals(codeVector)) {
					numeratorSum += sourceVector;
					denominatorSum++;
				}
			}
			tempCodeVectors.add(numeratorSum / denominatorSum);
		}
		codeVectors = tempCodeVectors;
	} // end updateCodeVectors()

	private double calculateAverageDistortion() {
		double averageDistortion = 0;
		for (Double sourceVector : sourceVectors)
			averageDistortion += Math.pow(sourceVector - codebook.get(sourceVector), 2);
		averageDistortion *= 1.0 / sourceVectors.size();
		return averageDistortion;
	} // end calculateAverageDistortion()
}
