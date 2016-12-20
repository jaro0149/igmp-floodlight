package net.floodlightcontroller.multicastmachine;

import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.locks.StampedLock;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.core.IOFSwitch;

public class FlowManager {

	private final HashSet<FlowEntry> flows = new HashSet<>();
	private final StampedLock lock = new StampedLock();
	private short idleTimeout;
	
	public FlowManager(short idleTimeout) {
		this.idleTimeout = idleTimeout;
	}
	
	public void addFlow(IOFSwitch sw, OFPort srcPort, OFPort dstPort, IPv4Address group) {
		long stamp = lock.writeLock();
		try {
			Optional<FlowEntry> optFlow = flows.stream()
					.filter(e -> e.compareSwitchAndGroup(sw, group))
					.findAny();
			if(optFlow.isPresent()) {
				FlowEntry flow = optFlow.get();
				if(!flow.findDstPort(dstPort)) {
					flow.removeActualFlowEntry();
					flow.addDstPort(dstPort);
					flow.insertActualFlowEntry();
				}				
			} else {
				FlowEntry newEntry = new FlowEntry(sw, group, srcPort, dstPort, idleTimeout);
				newEntry.insertActualFlowEntry();
				flows.add(newEntry);
			}			
		} finally {
			lock.unlock(stamp);
		}
	}
	
	public void removeFlow(IOFSwitch sw, OFPort dstPort, IPv4Address group) {
		long stamp = lock.writeLock();
		try {
			Optional<FlowEntry> optFlow = flows.stream()
					.filter(e -> e.compareSwitchAndGroup(sw, group))
					.findAny();
			if(optFlow.isPresent()) {
				FlowEntry flow = optFlow.get();
				if(flow.findDstPort(dstPort)) {
					flow.removeActualFlowEntry();
					boolean removed = flow.removeDstPort(dstPort);
					if(removed) {
						if(flow.isAlive()) {
							flow.insertActualFlowEntry();
						} else {
							flows.remove(flow);
						}
					}					
				}
			}
		} finally {
			lock.unlock(stamp);
		}
	}
	
	public void removeAllFlows(IOFSwitch sw, IPv4Address group) {
		Optional<FlowEntry> specificFlowPresention = flows.stream()
			.filter(e -> e.compareSwitchAndGroup(sw, group))
			.findAny();
		if(specificFlowPresention.isPresent()) {
			FlowEntry specificFlow = specificFlowPresention.get();
			specificFlow.removeActualFlowEntry();
			flows.remove(specificFlow);
		}	
	}
	
}
