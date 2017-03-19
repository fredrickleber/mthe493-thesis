import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class COSQTest {
	
	private static final int RNG_SEED = 123456789; // used to generate training vectors
	private static final int NUM_TRAINING_VECTORS = 10000;
	private static final int RATE = 3;
	private static final double ERROR_RATE = 0.05;	// Bit error rate (epislon in thesis)
	private static final double BURST_LEVEL = 10;	// Burst parameter (delta in thesis)
	
	private static final double MU = 0; // for the Laplacian distribution to have zero mean
	private static final double BETA = Math.pow(2, -0.5); // for the Laplacian distribution to have unit variance

	public static void main(String[] args) {
		List<Double> dcTrainingData = generateDCTrainingData(NUM_TRAINING_VECTORS);
		List<Double> acTrainingData = generateACTrainingData(NUM_TRAINING_VECTORS);
		Channel trainingChannel = new Channel(ERROR_RATE, BURST_LEVEL);
		
		COSQ dcCOSQ = trainCOSQ(dcTrainingData, trainingChannel);
		COSQ acCOSQ = trainCOSQ(acTrainingData, trainingChannel);
		double dcSNR = computeSNR(dcCOSQ, dcTrainingData, trainingChannel);
		double acSNR = computeSNR(acCOSQ, acTrainingData, trainingChannel);
		
		System.out.println("SNR for DC COSQ: " + dcSNR);
		System.out.println("SNR for AC COSQ: " + acSNR);
	}
	
	/**
	 * Computes the signal to noise ratio (SNR) in decibels. 
	 * @param cosq Channel-optimized scalar quantizer being used to encode and decode the data.
	 * @param sourceWords Source words to be encoded.
	 * @param channel The communication channel being used.
	 * @return Signal-to-noise ratio (SNR) in decibels.
	 */
	private static double computeSNR(COSQ cosq, List<Double> sourceWords, Channel channel) {
		double avgDistortion = 0;
		double reproducedWord;
		for (double sourceWord : sourceWords) {
			reproducedWord = cosq.decodeCodeWord(channel.sendThroughChannel(cosq.encodeSourceWord(sourceWord)));
			avgDistortion += Math.pow(sourceWord - reproducedWord, 2) / sourceWords.size();
		}
		System.out.println(avgDistortion);
		return - 10 * Math.log10(avgDistortion);
	} // end computeSNR
	
	/**
	 * Train a new COSQ.
	 * @param trainingData Data used to train the COSQ.
	 * @param trainingChannel Channel used to train the COSQ.
	 * @return New COSQ.
	 */
	private static COSQ trainCOSQ(List<Double> trainingData, Channel trainingChannel) {
		CodeMapTrainer codeMapTrainer = new CodeMapTrainer();
		List<Double> codebook = codeMapTrainer.generateCodebook(trainingData, (int) Math.pow(2, RATE));
		IndexMapTrainer indexMapTrainer = new IndexMapTrainer(codebook, trainingChannel);
		return new COSQ(codebook, indexMapTrainer.train(trainingData));
	} // end trainCOSQ()
	
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
