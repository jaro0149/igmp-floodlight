package net.floodlightcontroller.multicastmachine;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@SuppressWarnings("serial")
public class ExceptionBuffer extends Exception {

	private final List<Exception> container = new ArrayList<Exception>();
	
	public void addException(Exception exception) {
		this.container.add(exception);
	}
	
	public String getMessage() {
		if(!container.isEmpty()) {
			StringBuilder finalText = new StringBuilder(container.get(0).getMessage());
			IntStream.range(1,container.size())
				.forEachOrdered((i) -> finalText.append("\n" + container.get(i).getMessage()));
			return finalText.toString();
		} else {
			return null;
		}
	}
	
	public void throwIfItIsNeeded() throws ExceptionBuffer {
		if(!container.isEmpty()) {
			throw this;
		}
	}
	
}
