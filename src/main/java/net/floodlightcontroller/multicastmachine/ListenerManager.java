package net.floodlightcontroller.multicastmachine;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;

import net.floodlightcontroller.core.IOFSwitch;

public class ListenerManager extends HostManagerTemplate {

	private final ExceptionBuffer excBuffer = new ExceptionBuffer();
	private int queryInterval;
	private int maxResponseTime;
	private int queryFrequency;
	
	public ListenerManager(MacAddress routerMac, IPv4Address routerIp, int queryInterval, int maxResponseTime,
			int queryFrequency, Logger logger, FlowManager flowManager)
			throws ExceptionBuffer {
		super(routerMac, routerIp, logger, flowManager);
		setQueryInteval(queryInterval);
		setMaxResponseTime(maxResponseTime);
		setQueryFrequency(queryFrequency);
		excBuffer.throwIfItIsNeeded();
	}
	
	private void setQueryInteval(int queryInterval) {
		if(queryInterval>0) {
			this.queryInterval = queryInterval;
		} else {
			excBuffer.addException(new IllegalArgumentException());
		}
	}
	
	private void setMaxResponseTime(int maxResponseTime) {
		if(maxResponseTime>0) {
			this.maxResponseTime = maxResponseTime;
		} else {
			excBuffer.addException(new IllegalArgumentException());
		}
	}
	
	private void setQueryFrequency(int queryFrequency) {
		if(queryFrequency>0) {
			this.queryFrequency = queryFrequency;
		} else {
			excBuffer.addException(new IllegalArgumentException());
		}
	}
	
	@Override
	public void startMachine() {
		executor.scheduleAtFixedRate(() -> {
			long stamp = lock.writeLock();
			try {
				HashSet<HostEntry> forRemoval = new HashSet<>();
				HashSet<HostEntry> forQuery = new HashSet<>();
				list.forEach(listener -> {
					int actualAge = listener.incrementAndGet();
					if(actualAge==queryInterval+maxResponseTime) {
						forRemoval.add(listener);
						IOFSwitch sw = listener.getQueryEntry().getSwitch();
						IPv4Address group = listener.getQueryEntry().getGroupAddress();
						OFPort dstPort = listener.getQueryEntry().getSourcePort();
						manageFlowTableRemove(sw, dstPort, group);
					} else if(actualAge>=queryInterval && 
							(actualAge-queryInterval)%queryFrequency==0) {
						forQuery.add(listener);
					}
				});
				list.removeAll(forRemoval);
				forQuery.forEach(entry -> {
					try {
						entry.getQueryEntry().generateIgmpQuery(maxResponseTime, routerMac, routerIp);
					} catch (ExceptionBuffer e1) {
						logger.error(e1.getMessage());
					}
				});
			} finally {
				lock.unlock(stamp);
			}		
		}, SCHEDULED_START, DELAY, TimeUnit.SECONDS);
	}
	
	@Override
	public void manageFlowTableAdd(IOFSwitch sw, OFPort port, IPv4Address group) {
		List<OFPort> listOfPorts = other.getPortsForMulticastGroup(sw,group);
		listOfPorts.forEach(p -> flowManager.addFlow(sw, p, port, group));
	}

	@Override
	public void manageFlowTableRemove(IOFSwitch sw, OFPort port, IPv4Address group) {
		flowManager.removeFlow(sw, port, group);
	}
	
}
