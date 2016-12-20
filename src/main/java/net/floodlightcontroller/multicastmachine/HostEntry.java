package net.floodlightcontroller.multicastmachine;

import java.util.concurrent.atomic.AtomicInteger;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import net.floodlightcontroller.core.IOFSwitch;

@JsonSerialize(using=HostSerialiser.class)
public class HostEntry {
	
	private final AtomicInteger activeTime = new AtomicInteger(0);	
	private MacAddress srcMac;
	private IPv4Address srcIp;
	private QueryEntry queryEntry;
	
	public HostEntry(IOFSwitch sw, OFPort port, MacAddress srcMac, 
			IPv4Address srcIp, IPv4Address group) {
		this.srcMac = srcMac;
		this.srcIp = srcIp;
		queryEntry = new QueryEntry(group, sw, port);
	}
	
	public String printSrcMac() {
		return srcMac.toString();
	}
	
	public String printSrcIp() {
		return srcIp.toString();
	}
	
	public int printActiveTime() {
		return activeTime.get();
	}
	
	public String printGroupAddress() {
		return queryEntry.printGroupAddress();
	}
	
	public String printSwitchId() {
		return queryEntry.printSwitchId();
	}
	
	public int printPortId() {
		return queryEntry.printPortId();
	}
	
	public Integer incrementAndGet() {
		return activeTime.incrementAndGet();
	}
	
	public boolean compareAndUpdateEntry(IOFSwitch sw, OFPort port, MacAddress srcMac, 
			IPv4Address srcIp, IPv4Address group) {
		if(compareEntry(sw, port, srcMac, group)) {
			if(!this.srcIp.equals(srcIp)) {
				this.srcIp = srcIp;
			}
			activeTime.set(0);
			return true;
		}
		return false;
	}
	
	public boolean compareEntry(IOFSwitch sw, OFPort port, MacAddress srcMac, IPv4Address group) {
		return queryEntry.compareEntry(group, sw, port) && this.srcMac.equals(srcMac);
	}
	
	public OFPort getPortForGroupAddress(IOFSwitch sw, IPv4Address group) {
		return queryEntry.getPortForGroupAddress(sw, group);
	}
	
	public QueryEntry getQueryEntry() {
		return queryEntry;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((queryEntry == null) ? 0 : queryEntry.hashCode());
		result = prime * result + ((srcIp == null) ? 0 : srcIp.hashCode());
		result = prime * result + ((srcMac == null) ? 0 : srcMac.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		HostEntry other = (HostEntry) obj;
		if(this.hashCode()==other.hashCode()) {
			return true;
		}
		return false;
	}	
	
}
