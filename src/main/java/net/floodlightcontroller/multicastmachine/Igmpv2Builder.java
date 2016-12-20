package net.floodlightcontroller.multicastmachine;

import java.net.Inet4Address;
import java.util.Arrays;

public class Igmpv2Builder {

	private static final double MAX_RESPONSE_TIME = 25.5;
	private static final double MIN_RESPONSE_TIME = 0.0;
	private static final String WRONG_RESPONSE_TIME = "The response time must be set in the interval of"
			+ " <" + MIN_RESPONSE_TIME + "," + MAX_RESPONSE_TIME + ">";
	
	private final ExceptionBuffer buffer = new ExceptionBuffer();
	private Igmpv2Type type;
	private int maxResponseTime;
	private Inet4Address groupAddress;
	
	public Igmpv2Builder(Igmpv2Type type, int maxResponseTime, Inet4Address groupAddress) throws ExceptionBuffer {
		setType(type);
		setMaxResponseTime(maxResponseTime);
		setGroupAddress(groupAddress);
		buffer.throwIfItIsNeeded();
	}
	
	private void setType(Igmpv2Type type) {
		this.type = type;
	}
	
	private void setMaxResponseTime(double maxResponseTime) {
		if(maxResponseTime>=MIN_RESPONSE_TIME && maxResponseTime<=MAX_RESPONSE_TIME) {
			this.maxResponseTime = (int)(maxResponseTime*10.0);	
		} else {
			buffer.addException(new IllegalArgumentException("'" + maxResponseTime + "': " + WRONG_RESPONSE_TIME));
		}
	}
	
	private void setGroupAddress(Inet4Address groupAddress) {
		this.groupAddress = groupAddress;
	}
	
	public FlexStack build() {
		FlexBytes[] array = new FlexBytes[4];
		array[0] = new FlexBytes(type.getCode(),1);
		array[1] = new FlexBytes(maxResponseTime,1);
		array[2] = new FlexBytes(0,2);
		array[3] = new FlexBytes(groupAddress.getAddress());
		
		FlexStack stack1 = new FlexStack();
		Arrays.stream(array).forEachOrdered(stack1::pushElementUnchecked);
		long checksum = calculateChecksum(stack1.createFlexArrayImage());
		array[2] = new FlexBytes(checksum,2);
		
		FlexStack stack2 = new FlexStack();
		Arrays.stream(array).forEachOrdered(stack2::pushElementUnchecked);		
		return stack2;
	}
	
	private long calculateChecksum(FlexBytes byteArray) {
		int length = byteArray.getSize();
		int i = 0;
		long sum = 0;
		while (length > 0) {
			sum += (byteArray.getByteByIndex(i++) & 0xff) << 8;
			if ((--length) == 0)
				break;
			sum += (byteArray.getByteByIndex(i++) & 0xff);
			--length;
		}
		return (~((sum & 0xFFFF) + (sum >> 16))) & 0xFFFF;
	}
	
}
