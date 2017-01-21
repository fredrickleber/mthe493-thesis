
public class ChannelTest {

	public static void main(String[] args) {
		Channel channel = new Channel(0.05, 10, 10);

		byte[] data = new byte[1000];
		
		byte[] output = channel.sendThroughChannel(data);
		
		for (byte bit : output)
			System.out.print(bit);
		
	}

}
