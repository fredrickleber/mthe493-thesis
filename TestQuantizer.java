import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class TestQuantizer {

	public static List<Double> generateRandomGaussianSourceVectors(int numSourceVectors) {
		List<Double> sourceVectors = new ArrayList<>();
		Random randomNumberGenerator = new Random(123456789);
		for (int i = 0; i < numSourceVectors; i++)
			sourceVectors.add(randomNumberGenerator.nextGaussian());
		return sourceVectors;
	}
	
	public static void main(String[] args) throws FileNotFoundException {
		Quantizer scalarQuantizer = new Quantizer();
		List<Double> sourceVectors = generateRandomGaussianSourceVectors(10000);
		List<Map<Double, Double>> codebooks = new ArrayList<>();
		codebooks.add(scalarQuantizer.quantize(sourceVectors, 1));
		codebooks.add(scalarQuantizer.quantize(sourceVectors, 2));
		codebooks.add(scalarQuantizer.quantize(sourceVectors, 4));
		codebooks.add(scalarQuantizer.quantize(sourceVectors, 8));
		codebooks.add(scalarQuantizer.quantize(sourceVectors, 16));

	    PrintWriter pw = new PrintWriter(new File("quantizer_data.csv"));
	    StringBuilder sb = new StringBuilder();

	    sb.append("Source Data:");
	    for (Double sourceVector : sourceVectors) {
	      	sb.append(',');
		    sb.append(sourceVector);
	    }
	    sb.append("\n\n");
	    
	    for (Map<Double, Double> codebook : codebooks) {
		    List<Double> codeVectors = new ArrayList<Double>(new HashSet<>(codebook.values()));
		    codeVectors.sort((x,y)-> x.compareTo(y));	    
		    sb.append("Quantized Data:");
			for (Double codeVector : codeVectors) {
				sb.append(',');
			    sb.append(codeVector);
			}
		    sb.append('\n');
	    }

	    

	    /*
		for (Map.Entry<Double, Double> entry : codebook.entrySet()) {
			sb.append('\n');
		    sb.append(entry.getKey());
		    sb.append(',');
		    sb.append(entry.getValue());;
		}
		*/
	    pw.write(sb.toString());
	    pw.close();

	}

}
