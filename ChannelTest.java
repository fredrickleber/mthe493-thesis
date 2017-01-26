
public class ChannelTest {

	public static void main(String[] args) {
		Channel channel = new Channel(0.2, 5, 2);

		byte[] data = new byte[100];
		
		byte[] output = channel.sendThroughChannel(data);
		
		for (byte bit : output)
			System.out.print(bit);
		
	}

}
