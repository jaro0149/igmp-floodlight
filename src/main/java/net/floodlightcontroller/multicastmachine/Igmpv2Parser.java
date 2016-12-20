package net.floodlightcontroller.multicastmachine;

import java.net.Inet4Address;
import java.net.UnknownHostException;

public class Igmpv2Parser {

	private static final int SIZE = 8;
	
	private FlexBytes structure;
	private int offset;
	private boolean valid;
	
	public Igmpv2Parser(FlexBytes structure) {
		setElement(structure);
	}
	
	private void setElement(FlexBytes structure) {
		if(structure.getSize()>=SIZE) {
			this.structure = structure;
			offset = SIZE;
			valid = true;
		} else {
			valid = false;
		}
	}
	
	public Igmpv2Type getType() {
		return Igmpv2Type.parseType(structure.getIntegerByIndex(0));
	}
	
	public double getMaxResponseTime() {
		return (double)structure.getIntegerByIndex(1)/10.0;
	}
	
	public int getChecksum() {
		return structure.getIntegerFromRange(2,2);
	}
	
	public Inet4Address getGroupAddress() throws UnknownHostException, IllegalArgumentException {
		return (Inet4Address) Inet4Address.getByAddress(structure.createByteSubArray(4,4));
	}
	
	public boolean isValid() {
		return valid;
	}
	
	public int getOffset() {
		return offset;
	}
	
	public boolean testChecksum() {
		FlexBytes subArray = structure.linkFlexSubArray(0,SIZE);
		return calculateChecksum(subArray)==0l ? true : false;		
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
