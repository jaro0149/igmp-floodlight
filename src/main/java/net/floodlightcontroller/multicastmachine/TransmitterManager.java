package net.floodlightcontroller.multicastmachine;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;

import net.floodlightcontroller.core.IOFSwitch;

public class TransmitterManager extends HostManagerTemplate {

	private static final String ILLEGAL_MAX_AGE = "The maximum age length must be a positive integer.";
	private int maxAge;
	
	public TransmitterManager(MacAddress routerMac, IPv4Address routerIp, int maxAge, Logger logger, 
			FlowManager flowManager) throws IllegalArgumentException {
		super(routerMac, routerIp, logger, flowManager);
		setMaxIdleTime(maxAge);
	}
	
	private void setMaxIdleTime(int maxAge) throws IllegalArgumentException {
		if(maxAge>0) {
			this.maxAge = maxAge;
		} else {
			throw new IllegalArgumentException("'" + maxAge + "' : " + ILLEGAL_MAX_AGE);
		}
	}
	
	@Override
	public void startMachine() {
		executor.scheduleAtFixedRate(() -> {
			long stamp = lock.writeLock();
			try {
				HashSet<HostEntry> forRemoval = new HashSet<>();
				list.forEach(transmitter -> {
					int actualAge = transmitter.incrementAndGet();
					if(actualAge==maxAge) {						
						forRemoval.add(transmitter);
						IOFSwitch sw = transmitter.getQueryEntry().getSwitch();
						IPv4Address group = transmitter.getQueryEntry().getGroupAddress();
						manageFlowTableRemove(sw, null, group);
					}
				});
				list.removeAll(forRemoval);
			} finally {
				lock.unlock(stamp);
			}			
		}, SCHEDULED_START, DELAY, TimeUnit.SECONDS);
	}
	
	@Override
	public boolean addOrRefreshHost(IOFSwitch sw, OFPort port, MacAddress srcMac, 
			IPv4Address srcIp, IPv4Address group) {
		long stamp = lock.writeLock();
		try {
			boolean addNewEntry = list.stream()
					.allMatch(e -> !e.compareAndUpdateEntry(sw, port, srcMac, srcIp, group));
			if(addNewEntry) {
				list.add(new HostEntry(sw, port, srcMac, srcIp, group));
			}			
			manageFlowTableAdd(sw, port, group);
			return addNewEntry;
		} finally {
			lock.unlock(stamp);
		}
	}
	
	@Override
	public void manageFlowTableAdd(IOFSwitch sw, OFPort port, IPv4Address group) {
		List<OFPort> listOfDstPorts = other.getPortsForMulticastGroup(sw, group);
		listOfDstPorts.forEach(dstPort -> flowManager.addFlow(sw, port, dstPort, group));
	}

	@Override
	public void manageFlowTableRemove(IOFSwitch sw, OFPort port, IPv4Address group) {
		flowManager.removeAllFlows(sw, group);
	}
	
}
