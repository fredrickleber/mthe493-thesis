import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CoderFactory {

	private static final int RNG_SEED = 123456789; // used to generate training vectors
	private static final int NUM_TRAINING_VECTORS = 10000;
	
	private static final double MU = 0; // for the Laplacian distribution
	private static final double BETA = 1; // for the Laplacian distribution
	
	//TODO: Assign values to these
	private static final int DC_PIXEL_QUANTIZER_RATE;
	// an array of all the unique rates used in the fixed bit allocation array in the Coder class
	private static final int[] AC_PIXEL_QUANTIZER_RATES; // note even though it's final, array values can be changed


	/**
	 * Checks if a Coder is cached and returns it if so. If not, creates a new one and returns that.
	 * @param trainingChannel The channel with which the coder will be trained.
	 * @param coderRate The overall rate of the coder.
	 * @return A Coder object, either created anew or from the cache.
	 */
	public static Coder createCoder(Channel trainingChannel, int coderRate) {
		// TODO: check if coder is cached and return if so
		// aka deal with serialization stuff

		return new Coder(generateCOSQs(trainingChannel, coderRate), coderRate);
	} // end createCoder()
	
	/**
	 * Creates a Map of COSQs, used to instantiate a Coder object.
	 * @param trainingChannel The channel with which the COSQs will be trained.
	 * @param coderRate The overall rate of the coder.
	 * @return A Map from COSQ rates to COSQ objects. 
	 */
	private static Map<Integer, COSQ> generateCOSQs(Channel trainingChannel, int coderRate) {
		Map<Integer, COSQ> cosqs = new HashMap<>();
		List<Double> dcTrainingData = generateDCTrainingData(NUM_TRAINING_VECTORS);
		List<Double> acTrainingData = generateACTrainingData(NUM_TRAINING_VECTORS);
		CodeMapTrainer codeMapTrainer = new CodeMapTrainer();
		
		// generate the DC pixel COSQ
		List<Double> dcCodebook = codeMapTrainer.generateCodebook(dcTrainingData, (int) Math.pow(2, DC_PIXEL_QUANTIZER_RATE * coderRate));
		IndexMapTrainer dcIndexMapTrainer = new IndexMapTrainer(dcCodebook, trainingChannel);
		// give the DC pixel COSQ a key of -1 to ensure it is unique
		cosqs.put(-1, new COSQ(dcCodebook, dcIndexMapTrainer.simulateAnnealing()));

		// generate the AC pixel COSQs
		for (int rate : AC_PIXEL_QUANTIZER_RATES) {
			List<Double> acCodebook = codeMapTrainer.generateCodebook(acTrainingData, (int) Math.pow(2, rate * coderRate));
			IndexMapTrainer acIndexMapTrainer = new IndexMapTrainer(acCodebook, trainingChannel);
			cosqs.put(rate * coderRate, new COSQ(acCodebook, acIndexMapTrainer.simulateAnnealing()));
		}
		
		return cosqs;
	} // end generateCOSQs()
	
	/**
	 * Generates training data for the DC pixels, using a normal distribution. See p.46 of thesis.
	 * @param numToGenerate The number of training vectors to generate.
	 * @return The list of training vectors.
	 */
	private static List<Double> generateDCTrainingData(int numToGenerate) {
		List<Double> trainingVectors = new ArrayList<>();
		Random randomNumberGenerator = new Random(RNG_SEED);
		for (int i = 0; i < numToGenerate; i++)
			trainingVectors.add(randomNumberGenerator.nextGaussian());
		return trainingVectors;
	} // end generateDCTrainingData()
	
	/**
	 * Generates training data for the AC pixels, using a Laplacian distribution. See p.46 of thesis.
	 * @param numToGenerate The number of training vectors to generate.
	 * @return The list of training vectors.
	 */
	private static List<Double> generateACTrainingData(int numToGenerate) {
		List<Double> trainingVectors = new ArrayList<>();
		Random randomNumberGenerator = new Random(RNG_SEED);
		for (int i = 0; i < numToGenerate; i++)
			trainingVectors.add(generateLaplacianDistributedNumber(randomNumberGenerator.nextDouble(), MU, BETA));
		return trainingVectors;
	} // end generateACTrainingData()
	
	/**
	 * Generates a number using the Laplacian distribution. The inverse CDF method is used.
	 * For more details, see 
	 * https://github.com/apache/commons-math/blob/master/src/main/java/org/apache/commons/math4/distribution/LaplaceDistribution.java
	 * @param randomProbability A random number between 0 and 1.
	 * @param mu Expected value.
	 * @param beta Scaling parameter, must be positive.
	 * @return A Laplacian distributed random number.
	 */
	private static double generateLaplacianDistributedNumber(double randomProbability, double mu, double beta) {
		double x = (randomProbability > 0.5) ? -Math.log(2.0 - 2.0 * randomProbability) : Math.log(2.0 * randomProbability);
		return mu + beta * x;
	} // end generateLaplacianDistributedNumber()	

}
