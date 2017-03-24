import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CoderFactory {

	private static final int RNG_SEED = 123456789; // used to generate training vectors
	private static final int NUM_TRAINING_VECTORS = 50000;
	
	private static final double MU = 0; // for the Laplacian distribution to have zero mean
	private static final double BETA = Math.pow(2, -0.5); // for the Laplacian distribution to have unit variance
	
	//TODO: Assign values to these
	private static final int UNIQUE_DC_PIXEL_QUANTIZER_RATE = 8;
	// an array of all the unique rates used in the fixed bit allocation array in the Coder class
	private static final int[] UNIQUE_AC_PIXEL_QUANTIZER_RATES = {1, 2, 4, 5, 6, 7}; // note even though it's final, array values can be changed


	/**
	 * Checks if a Coder is cached and returns it if so
	 * @param trainingChannel The channel with which the coder was trained.
	 * @param coderRate The overall rate of the coder.
	 * @return A Coder object.
	 */
	public static Coder loadCoder(Channel channel, int coderRate) {
		String potentialFilename = "coder-" + channel.getBitErrorRate() + "-" + channel.getBurstLevel() + ".ser";
		try (
			FileInputStream fileIn = new FileInputStream(potentialFilename);
			ObjectInputStream in = new ObjectInputStream(fileIn)
		) {
			Coder deserializedCoder = (Coder) in.readObject();
			deserializedCoder.setCoderRate(coderRate);
			System.out.println("Loaded coder successfully!");
			return deserializedCoder;
		} catch (IOException i) {
			System.out.println("Coder class not found");
			return null;
		} catch (ClassNotFoundException c) {
			System.out.println("Coder class not found");
			c.printStackTrace();
			return null;
		}		
	} // end loadCoder()
	
	/**
	 * Creates a new coder.
	 * @param channel Training channel.
	 * @param coderRate Rate of the coder.
	 * @return New coder.
	 */
	public static Coder makeCoder(Channel channel, int coderRate) {
		String potentialFilename = "coder-" + channel.getBitErrorRate() + "-" + channel.getBurstLevel() + ".ser";
		System.out.println("Making new coder(s)!");
		Coder newCoder = new Coder(generateCOSQs(channel, coderRate), coderRate);
		serializeNewCoder(newCoder, potentialFilename);
		return newCoder;
	} // end makeCoder()
	
	/**
	 * Trains and serializes multiple coders of rate 1.
	 * @param bitErrorRates Array of the bit error rates associated with each coder.
	 * @param coderRate The overall rate of the coder.
	 */
	public static void createMultipleCoders(double[] bitErrorRates, double burstLevel) {
		Arrays.sort(bitErrorRates);
		List<Double> dcTrainingData = generateDCTrainingData(NUM_TRAINING_VECTORS);
		List<Double> acTrainingData = generateACTrainingData(NUM_TRAINING_VECTORS);
		Map<Integer, List<Double>> codebooks = new HashMap<>();
		Map<Integer, COSQ> cosqs = new HashMap<>();
		CodeMapTrainer codeMapTrainer = new CodeMapTrainer();
		Channel trainingChannel = new Channel(bitErrorRates[0], burstLevel);
		
		// Initialize the DC pixel codebook
		List<Double> dcCodebook = codeMapTrainer.generateInitialCodebook(dcTrainingData, (int) Math.pow(2, UNIQUE_DC_PIXEL_QUANTIZER_RATE));
		IndexMapTrainer dcIndexMapTrainer = new IndexMapTrainer(dcCodebook, trainingChannel);
		dcCodebook = dcIndexMapTrainer.train();
		
		// Initialize the AC pixel codebooks
		for (int rate : UNIQUE_AC_PIXEL_QUANTIZER_RATES) {
			List<Double> acCodebook = codeMapTrainer.generateInitialCodebook(acTrainingData, (int) Math.pow(2, rate));
			IndexMapTrainer acIndexMapTrainer = new IndexMapTrainer(acCodebook, trainingChannel);
			acCodebook = acIndexMapTrainer.train();
			codebooks.put(rate, acCodebook);
		}
		
		for (double bitErrorRate: bitErrorRates) {
			System.out.println("Currently making the Coder for BER = " + bitErrorRate);
			// update training Channel
			trainingChannel = new Channel(bitErrorRate, burstLevel);
			
			// update DC codebook & generate the DC pixel COSQ 
			dcCodebook = codeMapTrainer.generateUpdatedCodebook(dcTrainingData, dcCodebook, trainingChannel);
			cosqs.put(-1, new COSQ(dcCodebook));

			// update AC codebook & generate the AC pixel COSQs
			for (int rate : UNIQUE_AC_PIXEL_QUANTIZER_RATES) {
				List<Double> acCodebook = codeMapTrainer.generateUpdatedCodebook(acTrainingData, codebooks.get(rate), trainingChannel);
				cosqs.put(rate, new COSQ(acCodebook));
			}
			
			String filename = "coder-" + bitErrorRate + "-" + burstLevel + ".ser";
			serializeNewCoder(new Coder(cosqs, 1), filename);
		}
		System.out.println("Done!");
	} // end createMultipleCoders()
	
	
	private static void serializeNewCoder(Coder coder, String filename) {
		try (
			FileOutputStream fileOut = new FileOutputStream(filename);
			ObjectOutputStream out = new ObjectOutputStream(fileOut)
		) {
			out.writeObject(coder);
		} catch (IOException i) {
			i.printStackTrace();
		}
	} // end serializeNewCoder()
	
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
		List<Double> dcCodebook = codeMapTrainer.generateInitialCodebook(dcTrainingData, (int) Math.pow(2, UNIQUE_DC_PIXEL_QUANTIZER_RATE * coderRate));
		IndexMapTrainer dcIndexMapTrainer = new IndexMapTrainer(dcCodebook, trainingChannel);
		dcCodebook = dcIndexMapTrainer.train();
		
		// give the DC pixel COSQ a key of -1 to ensure it is unique
		cosqs.put(-1, new COSQ(dcCodebook));

		// generate the AC pixel COSQs
		for (int rate : UNIQUE_AC_PIXEL_QUANTIZER_RATES) {
			List<Double> acCodebook = codeMapTrainer.generateInitialCodebook(acTrainingData, (int) Math.pow(2, rate * coderRate));
			IndexMapTrainer acIndexMapTrainer = new IndexMapTrainer(acCodebook, trainingChannel);
			acCodebook = acIndexMapTrainer.train();
			cosqs.put(rate * coderRate, new COSQ(acCodebook));
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
