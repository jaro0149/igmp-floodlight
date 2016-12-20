package net.floodlightcontroller.multicastmachine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

public class FlexStack implements Iterator<FlexBytes>, Iterable<FlexBytes> {
	
	private static final String EMPTY_FLEX_BYTES = "The empty flex-bytes array appeared on the input of the system.";
	private static final String EMPTY_FLEX_STACK = "The empty flex-stack collection appeared on the input of the system.";
	private static final String EMPTY_LOCAL_STACK = "The local stack of the flex collection is empty.";
	
	private List<FlexBytes> stack = new ArrayList<FlexBytes>();
	private int i = 0;
	private int size = 0;
	
	public FlexStack() {}
	
	public FlexStack(FlexBytes fb0) throws IllegalArgumentException {
		if(fb0!=null && fb0.getSize()!=0) {
			stack.add(fb0);
			size = fb0.getSize();
		} else {
			throw new IllegalArgumentException(EMPTY_FLEX_BYTES);
		}		
	}
	
	public FlexStack(FlexStack fs0) throws IllegalArgumentException {
		if(fs0!=null && fs0.stack.size()!=0) {
			fs0.forEach(fb -> {
				stack.add(fb);
				size = fs0.getNumberOfBytes();
			});
		} else {
			throw new IllegalArgumentException(EMPTY_FLEX_STACK);
		}
	}
	
	public void pushElement(FlexBytes fb0) throws IllegalArgumentException {
		if(fb0!=null && fb0.getSize()!=0) {
			stack.add(fb0);
			size += fb0.getSize();
		} else {
			throw new IllegalArgumentException(EMPTY_FLEX_BYTES);
		}
	}
	
	public void pushElementUnchecked(FlexBytes fb0) {
		stack.add(fb0);
		size += fb0.getSize();
	}
	
	public void pushElement(FlexStack fs0) throws IllegalArgumentException {
		if(fs0!=null && fs0.stack.size()!=0) {
			fs0.forEach(fb -> {
				stack.add(fb);
				size += fs0.getNumberOfBytes();
			});
		} else {
			throw new IllegalArgumentException(EMPTY_FLEX_STACK);
		}
	}
	
	public void pushElementUnchecked(FlexStack fs0) {
		fs0.forEach(fb -> {
			stack.add(fb);
			size += fs0.getNumberOfBytes();
		});
	}
	
	public FlexBytes popElement() throws IllegalArgumentException {
		if(this.stack.size()>0) {
			FlexBytes fb = this.getLastElementLink();
			this.stack.remove(this.stack.size()-1);
			size -= fb.getSize();
			return fb;
		} else {
			throw new IllegalArgumentException(EMPTY_LOCAL_STACK);
		}		
	}
	
	public int getCollectionSize() {
		return stack.size();
	}
	
	public int getNumberOfBytes() {
		return size;
	}
	
	public FlexBytes getLastElementLink() throws UnsupportedOperationException {
		if(this.stack.size()>0) {
			return this.stack.get(stack.size()-1);
		} else {
			throw new UnsupportedOperationException(EMPTY_LOCAL_STACK);
		}
	}
	
	public FlexBytes getLastElementCopy() throws UnsupportedOperationException {
		if(this.stack.size()>0) {
			return this.stack.get(stack.size()-1).createCopyOfFlexArray();
		} else {
			throw new UnsupportedOperationException(EMPTY_LOCAL_STACK);
		}
	}
	
	public byte[] createByteArrayImage() throws UnsupportedOperationException {
		if(this.stack.size()>0) {
			int size = getNumberOfBytes();
			byte[] image = new byte[size];
			AtomicInteger i = new AtomicInteger(0);
			stack.forEach(fb -> {
				byte[] imagex = fb.createCopyOfByteArray();
				for(byte b : imagex) {
					image[i.getAndIncrement()] = b;
				}
			});
			return image;
		} else {
			throw new UnsupportedOperationException(EMPTY_LOCAL_STACK);
		}
	}
	
	public FlexBytes createFlexArrayImage() throws UnsupportedOperationException {
		if(this.stack.size()>0) {
			int size = 0;
			for(FlexBytes fb : stack) {
				size += fb.getSize();
			}
			byte[] image = new byte[size];
			AtomicInteger i = new AtomicInteger(0);
			stack.forEach(fb -> {
				byte[] imagex = fb.createCopyOfByteArray();
				for(byte b : imagex) {
					image[i.getAndIncrement()] = b;
				}
			});
			return new FlexBytes(image);
		} else {
			throw new UnsupportedOperationException(EMPTY_LOCAL_STACK);
		}
	}
	
	@Override
	public boolean hasNext() {
		if(i<this.stack.size()) {
			return true;
		}
		this.i = 0;
		return false;
	}

	@Override
	public FlexBytes next() {
		if(i!=this.stack.size()) {
			return this.stack.get(i++);
		} else {			
			throw new NoSuchElementException();
		}
	}
	
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Iterator<FlexBytes> iterator() {
		return this;
	}
	
}
