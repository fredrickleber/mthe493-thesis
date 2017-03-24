import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ChannelTest {

	public static void main(String[] args) {
		Channel channel = new Channel(0.1, 5);
		List<Byte> data = new ArrayList<Byte>(Collections.nCopies(100, (byte)(0)));
		List<Byte> output = channel.sendThroughChannel(data);
		for (Byte bit : output)
			System.out.print(bit.byteValue());
	}

}
