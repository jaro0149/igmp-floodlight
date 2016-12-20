package net.floodlightcontroller.multicastmachine;

import java.util.HashSet;
import java.util.List;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.core.IOFSwitch;

public interface HostManager {
	
	public void registerOtherManager(HostManager other);
	public void manageFlowTableAdd(IOFSwitch sw, OFPort port, IPv4Address group);
	public void manageFlowTableRemove(IOFSwitch sw, OFPort port, IPv4Address group);
	public boolean addOrRefreshHost(IOFSwitch sw, OFPort port, MacAddress srcMac, IPv4Address srcIp, IPv4Address group);
	public boolean removeHost(IOFSwitch sw, OFPort port, MacAddress srcMac, IPv4Address group);
	public List<OFPort> getPortsForMulticastGroup(IOFSwitch sw, IPv4Address group);
	public HashSet<HostEntry> getSetOfHostsByGroup(IPv4Address group);	
	public HashSet<HostEntry> getList();
	public void startMachine();
	public void stopMachine();
		
}
