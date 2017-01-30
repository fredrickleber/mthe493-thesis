// Exception class for when Markov memory (parameter M) is not defined as intended.
public class MarkovMemoryException extends Exception {
	
	public MarkovMemoryException()
	{
		super("Invalid Markov memory parameter M.");
	}
	
	public MarkovMemoryException(String message)
    {
       super(message);
    }

}
