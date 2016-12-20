package net.floodlightcontroller.multicastmachine;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;

import net.floodlightcontroller.core.IOFSwitch;

abstract class HostManagerTemplate implements HostManager {

	protected static final int SCHEDULED_START = 0;
	protected static final int AWAIT_TERMINATION = 5;
	protected static final int DELAY = 1;	
	protected final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	protected final HashSet<HostEntry> list = new HashSet<>();
	protected final StampedLock lock = new StampedLock();
	protected IPv4Address routerIp;
	protected MacAddress routerMac;
	protected Logger logger;
	protected HostManager other;
	protected FlowManager flowManager;
	
	public HostManagerTemplate(MacAddress routerMac, IPv4Address routerIp, Logger logger, 
			FlowManager flowManager) {
		this.routerMac = routerMac;
		this.routerIp = routerIp;
		this.logger = logger;		
		this.flowManager = flowManager;
	}
	
	public void registerOtherManager(HostManager other) {
		this.other = other;
	}
	
	@Override
	public List<OFPort> getPortsForMulticastGroup(IOFSwitch sw, IPv4Address group) {
		List<OFPort> output = list.stream()
			.map(e -> e.getPortForGroupAddress(sw,group))
			.filter(e -> e!=null)
			.collect(Collectors.toList());
		return output;
	}
	
	@Override
	public HashSet<HostEntry> getSetOfHostsByGroup(IPv4Address group) {
		return list.stream()
			.filter(listener -> listener.getQueryEntry().getGroupAddress().equals(group))
			.collect(Collectors.toCollection(HashSet::new));
	}
	
	@Override
	public HashSet<HostEntry> getList() {
		return list;
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
				manageFlowTableAdd(sw, port, group);
			}
			return addNewEntry;
		} finally {
			lock.unlock(stamp);
		}
	}
	
	@Override
	public boolean removeHost(IOFSwitch sw, OFPort port, MacAddress srcMac, IPv4Address group) {
		long stamp = lock.writeLock();
		try {
			boolean removed = list.removeIf(e -> e.compareEntry(sw, port, srcMac, group));
			if(removed) {
				manageFlowTableRemove(sw, port, group);
			}
			return removed;
		} finally {
			lock.unlock(stamp);
		}
	}
	
	@Override
	public void stopMachine() {
		if(executor!=null) {
			try {
				executor.shutdown();
				executor.awaitTermination(AWAIT_TERMINATION, TimeUnit.SECONDS);
			} catch(InterruptedException e) {
			} finally {
				executor.shutdownNow();
			}
		}
	}
	
}
