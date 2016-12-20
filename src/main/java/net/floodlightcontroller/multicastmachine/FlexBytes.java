package net.floodlightcontroller.multicastmachine;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

public class FlexBytes implements Iterator<Byte>, Iterable<Byte> {
	
	private static final String EMPTY_ARRAY = "The empty byte array appeared on the input.";
	private static final String MISCONFIGURED_INDEX = "The start index is out of the bounds of the byte array.";
	private static final String MISCONFIGURED_LENGTH = "The wrong specification of the length appeared in the system.";
	private static final String WRONG_INDEX = "The input index is out of the bounds of the byte array.";
	private static final String SUBARRAY_SETTINGS = "The subarray parameters do not fit to the original array.";
	private static final String INVALID_RANGE = "The specified range does not correlate with the limit.";
	private static final String WRONG_FIT_SIZE = "The least possible size of the array is an one byte.";
	private static final String FLEX_COMPATIBILITY = "The flex arrays are not compatible with the bounds.";
	private static final String FIMPLE_FLEX_FAULT = "The defined start index and length do not fit to the flex array bounds.";
	
	private byte[] byteArray;
	private int startIndex;
	private int size;
	private int i = 0;
	
	public FlexBytes(byte[] array, int startIndex, int size) throws IllegalArgumentException {
		setArray(array);
		setStartIndex(startIndex);
		setSize(array, startIndex, size);		
	}
	
	public FlexBytes(byte[] array) throws IllegalArgumentException {
		setArray(array);
		setStartIndex(0);
		setSize(array, 0, array.length);
	}
	
	public FlexBytes(int value, int fitBytes) throws IllegalArgumentException {
		if(fitBytes>0) {
			byte[] bytes = new byte[fitBytes];
			IntStream.range(0, fitBytes).forEach(i -> {
				bytes[fitBytes-i-1] = (byte)(value>>>(i*8));
			});
			this.byteArray = bytes;
			this.startIndex = 0;
			this.size = fitBytes;
		} else {
			throw new IllegalArgumentException("'" + fitBytes + "':" + WRONG_FIT_SIZE);
		}		
	}
	
	public FlexBytes(long value, int fitBytes) throws IllegalArgumentException {
		if(fitBytes>0) {
			byte[] bytes = new byte[fitBytes];
			IntStream.range(0, fitBytes).forEach(i -> {
				bytes[fitBytes-i-1] = (byte)(value>>>(i*8));
			});
			this.byteArray = bytes;
			this.startIndex = 0;
			this.size = fitBytes;
		} else {
			throw new IllegalArgumentException("'" + fitBytes + "': " + WRONG_FIT_SIZE);
		}
	}
	
	private void setArray(byte[] array) throws IllegalArgumentException {
		if(array.length>0) {
			this.byteArray = array;
		} else {
			throw new IllegalArgumentException("'" + array + "': " + EMPTY_ARRAY);
		}
	}

	private void setStartIndex(int startIndex) throws IllegalArgumentException {
		if(startIndex>=0 && startIndex<byteArray.length) {
			this.startIndex = startIndex;
		} else {
			throw new IllegalArgumentException("'" + startIndex + "': " + MISCONFIGURED_INDEX);
		}			
	}
	
	private void setSize(byte[] array, int startIndex, int size) throws IllegalArgumentException {
		if(size>0 && size+startIndex<=array.length) {
			this.size = size;
		} else {
			throw new IllegalArgumentException("'" + size + "': " + MISCONFIGURED_LENGTH);
		}
	}
	
	public FlexBytes mirrorImage(int startIndex, int length) throws IllegalArgumentException {
		if(startIndex>=0 && startIndex<this.size && length>0 && length+startIndex<=this.size) {
			int imageStartIndex = this.startIndex+startIndex;
			byte[] image = new byte[length];
			IntStream.range(0, length).forEach(i -> {
				image[length-i-1] = this.byteArray[imageStartIndex+i];
			});
			return new FlexBytes(image,0,length);
		} else {
			throw new IllegalArgumentException("'" + startIndex + "," + length + "': " + FIMPLE_FLEX_FAULT);
		}
	}
	
	public void mirrorImageEdit(int startIndex, int length) throws IllegalArgumentException {
		if(startIndex>=0 && startIndex<this.size && length>0 && length+startIndex<=this.size) {
			int imageStartIndex = this.startIndex+startIndex;
			IntStream.range(0, length/2).forEach(i -> {
				byte cache = this.byteArray[i];
				this.byteArray[imageStartIndex+i] = this.byteArray[imageStartIndex+length-i-1];
				this.byteArray[imageStartIndex+length-i-1] = cache;
			});
		} else {
			throw new IllegalArgumentException("'" + startIndex + "," + length + "': " + FIMPLE_FLEX_FAULT);
		}
	}
	
	public int getSize() {
		return this.size;
	}
	
	public void alterByte(int index, byte setByte) {
		this.byteArray[this.startIndex+index] = setByte;
	}
	
	public void alterByteRange(int startIndex0, byte[] setByteRange, int startIndex1, int length) throws IllegalArgumentException {
		if(startIndex0>=0 && startIndex1>=0 && startIndex0<this.size && startIndex1<setByteRange.length && length>0 
				&& startIndex0+length<=this.size && startIndex1+length<=setByteRange.length) {
			IntStream.range(0, length).forEach(i -> {
				this.byteArray[this.startIndex+startIndex0+i] = setByteRange[startIndex1+i];
			});
		} else {
			throw new IllegalArgumentException("'" + startIndex0 + "," + startIndex1 + "," + length +  "': " + FLEX_COMPATIBILITY);
		}
	}
	
	public void alterByteRange(int startIndex0, FlexBytes setByteRange, int startIndex1, int length) throws IllegalArgumentException {
		if(startIndex0>=0 && startIndex1>=0 && startIndex0<this.size && startIndex1<setByteRange.getSize() && length>0 
				&& startIndex0+length<=this.size && startIndex1+length<=setByteRange.getSize()) {
			IntStream.range(0, length).forEach(i -> {
				this.byteArray[this.startIndex+startIndex0+i] = setByteRange.getByteByIndex(startIndex1+i);
			});
		} else {
			throw new IllegalArgumentException("'" + startIndex0 + "," + startIndex1 + "," + length +  "': " + FLEX_COMPATIBILITY);
		}
	}
	
	public byte getByteByIndex(int index) throws IllegalArgumentException {
		if(index>=0 && index<this.size) {
			return this.byteArray[index+this.startIndex];
		} else {
			throw new IllegalArgumentException("'" + index + "': " + WRONG_INDEX);
		}
	}
	
	public int getIntegerByIndex(int index) throws IllegalArgumentException {
		if(index>=0 && index<this.size) {
			return this.byteArray[index+this.startIndex]&0xff;
		} else {
			throw new IllegalArgumentException("'" + index + "': " + WRONG_INDEX);
		}
	}
	
	public byte[] createCopyOfByteArray() {
		byte[] copiedArray = new byte[this.size];
		IntStream.range(0, this.size).forEach(i -> {
			copiedArray[i] = this.byteArray[this.startIndex+i];
		});
		return copiedArray;
	}

	public FlexBytes createCopyOfFlexArray() throws IllegalArgumentException {
		byte[] copiedArray = new byte[this.size];
		IntStream.range(0, this.size).forEach(i -> {
			copiedArray[i] = this.byteArray[this.startIndex+i];
		});
		FlexBytes copiedFlexBytes = new FlexBytes(copiedArray, 0, this.size);		
		return copiedFlexBytes;
	}
	
	public byte[] createByteSubArray(int startIndex, int length) throws IllegalArgumentException {
		if(startIndex>=0 && startIndex<this.size && length>0 && length+startIndex<=this.size) {
			byte[] parsedByteArray = new byte[length];
			int nextStartIndex = this.startIndex + startIndex;
			IntStream.range(0, length).forEach(i -> {
				parsedByteArray[i] = this.byteArray[nextStartIndex + i];
			});
			return parsedByteArray;
		} else {
			throw new IllegalArgumentException("'" + startIndex + "," + length +  "': " + SUBARRAY_SETTINGS);
		}
	}
	
	public FlexBytes createFlexSubArray(int startIndex, int length) throws IllegalArgumentException {
		if(startIndex>=0 && startIndex<this.size && length>0 && length+startIndex<=this.size) {
			byte[] parsedByteArray = new byte[length];
			int nextStartIndex = this.startIndex + startIndex;
			IntStream.range(0, length).forEach(i -> {
				parsedByteArray[i] = this.byteArray[nextStartIndex + i];
			});
			FlexBytes parsedFlexArray = new FlexBytes(parsedByteArray, 0, length);
			return parsedFlexArray;
		} else {
			throw new IllegalArgumentException("'" + startIndex + "," + length +  "': " + SUBARRAY_SETTINGS);
		}
	}
	
	public FlexBytes linkFlexSubArray(int startIndex, int length) throws IllegalArgumentException {
		if(startIndex>=0 && startIndex<this.size && length>0 && length+startIndex<=this.size) {
			int nextStartIndex = this.startIndex + startIndex;
			FlexBytes parsedFlexArray = new FlexBytes(this.byteArray, nextStartIndex, length);
			return parsedFlexArray;
		} else {
			throw new IllegalArgumentException("'" + startIndex + "," + length +  "': " + SUBARRAY_SETTINGS);
		}
	}
	
	public FlexBytes linkFlexSubArray(int startIndex) throws IllegalArgumentException {
		if(startIndex>=0 && startIndex<this.size) {
			int nextStartIndex = this.startIndex + startIndex;
			FlexBytes parsedFlexArray = new FlexBytes(this.byteArray, nextStartIndex, this.size-startIndex);
			return parsedFlexArray;
		} else {
			throw new IllegalArgumentException("'" + startIndex + "': " + SUBARRAY_SETTINGS);
		}
	}
	
	public char[] deriveHexArray() {
		char[] output = new char[this.size*2];
		char[] hexOptions = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};		
		IntStream.range(0, this.size).forEach(i -> {
			int intValue = this.byteArray[this.startIndex+i]&0xFF;
			output[i*2] = hexOptions[intValue>>>4];
			output[i*2+1] = hexOptions[intValue&0x0F];
		});
		return output;
	}
	
	public boolean[] deriveBitArrayHigh() {
		boolean[] boolArray = new boolean[this.size*8];
		AtomicInteger boolCounter = new AtomicInteger(0);
		IntStream.range(0, this.size).forEach(i -> {
			IntStream.range(0,8).forEach(j -> {
				int actBoolCounter = boolCounter.incrementAndGet();
				if(((this.byteArray[this.startIndex+i]>>(7-j))&0x01)==1) {
					boolArray[actBoolCounter] = true;
				} else {
					boolArray[actBoolCounter] = false;
				}
			});
			boolCounter.set(0);
		});	
		return boolArray;
	}
	
	public boolean[] deriveBitArrayLow() {
		boolean[] boolArray = new boolean[this.size*8];
		AtomicInteger boolCounter = new AtomicInteger(0);
		IntStream.range(0, this.size).forEach(i -> {
			IntStream.range(0,8).forEach(j -> {
				int actBoolCounter = boolCounter.incrementAndGet();
				if(((this.byteArray[this.startIndex+i]>>j)&0x01)==1) {
					boolArray[actBoolCounter] = true;
				} else {
					boolArray[actBoolCounter] = false;
				}
			});
			boolCounter.set(0);
		});	
		return boolArray;
	}
	
	public int getIntegerFromRange(int startIndex, int length) throws IllegalArgumentException {
		if(startIndex>=0 && length>=1) {
			AtomicInteger value = new AtomicInteger(0);
			IntStream.range(startIndex,startIndex+length).forEach(i -> {
				value.set((value.get()<<8)+(byteArray[this.startIndex+i]&0xff));
			});
			return value.get();
		} else {
			throw new IllegalArgumentException("'" + startIndex + "," + length +  "': " + INVALID_RANGE);
		}
	}
	
	public long getLongFromRange(int startIndex, int length) throws IllegalArgumentException {
		if(startIndex>=0 && length>=1) {
			AtomicLong value = new AtomicLong(0);
			IntStream.range(startIndex,startIndex+length).forEach(i -> {
				value.set((value.get()<<8)+(byteArray[this.startIndex+i]&0xff));
			});
			return value.get();
		} else {
			throw new IllegalArgumentException("'" + startIndex + "," + length +  "': " + INVALID_RANGE);
		}
	}
	
	public FlexBytes logicalAnd(int startIndex0, FlexBytes flex, int startIndex1, int length) throws IllegalArgumentException {
		if(startIndex0>=0 && startIndex1>=0 && startIndex0<this.size && startIndex1<flex.size && length>0 
				&& startIndex0+length<=this.size && startIndex1+length<=flex.size) {
			byte[] computedArray = new byte[length];
			IntStream.range(0,length).forEach(i -> {
				computedArray[i] = (byte)(this.byteArray[this.startIndex+startIndex0+i]&flex.byteArray[flex.startIndex+startIndex1+i]);
			});
			FlexBytes computedFlex = new FlexBytes(computedArray, 0, length);
			return computedFlex;
		} else {
			throw new IllegalArgumentException("'" + startIndex0 + "," + startIndex1 + "," + length +  "': " + FLEX_COMPATIBILITY);
		}
	}
	
	public void logicalAndEdit(int startIndex0, FlexBytes flex, int startIndex1, int length) throws IllegalArgumentException {
		if(startIndex0>=0 && startIndex1>=0 && startIndex0<this.size && startIndex1<flex.size && length>0 
				&& startIndex0+length<=this.size && startIndex1+length<=flex.size) {
			IntStream.range(0,length).forEach(i -> {
				this.byteArray[this.startIndex+startIndex0+i] = (byte)(this.byteArray[this.startIndex+startIndex0+i]
						&flex.byteArray[flex.startIndex+startIndex1+i]);
			});
		} else {
			throw new IllegalArgumentException("'" + startIndex0 + "," + startIndex1 + "," + length +  "': " + FLEX_COMPATIBILITY);
		}
	}
	
	public FlexBytes logicalOr(int startIndex0, FlexBytes flex, int startIndex1, int length) throws IllegalArgumentException {
		if(startIndex0>=0 && startIndex1>=0 && startIndex0<this.size && startIndex1<flex.size && length>0 
				&& startIndex0+length<=this.size && startIndex1+length<=flex.size) {
			byte[] computedArray = new byte[length];
			IntStream.range(0,length).forEach(i -> {
				computedArray[i] = (byte)(this.byteArray[this.startIndex+startIndex0+i]|flex.byteArray[flex.startIndex+startIndex1+i]);
			});
			FlexBytes computedFlex = new FlexBytes(computedArray, 0, length);
			return computedFlex;
		} else {
			throw new IllegalArgumentException("'" + startIndex0 + "," + startIndex1 + "," + length +  "': " + FLEX_COMPATIBILITY);
		}
	}
	
	public void logicalOrEdit(int startIndex0, FlexBytes flex, int startIndex1, int length) throws IllegalArgumentException {
		if(startIndex0>=0 && startIndex1>=0 && startIndex0<this.size && startIndex1<flex.size && length>0 
				&& startIndex0+length<=this.size && startIndex1+length<=flex.size) {
			IntStream.range(0,length).forEach(i -> {
				this.byteArray[this.startIndex+startIndex0+i] = (byte)(this.byteArray[this.startIndex+startIndex0+i]
						|flex.byteArray[flex.startIndex+startIndex1+i]);
			});
		} else {
			throw new IllegalArgumentException("'" + startIndex0 + "," + startIndex1 + "," + length +  "': " + FLEX_COMPATIBILITY);
		}
	}
	
	public FlexBytes logicalXOr(int startIndex0, FlexBytes flex, int startIndex1, int length) throws IllegalArgumentException {
		if(startIndex0>=0 && startIndex1>=0 && startIndex0<this.size && startIndex1<flex.size && length>0 
				&& startIndex0+length<=this.size && startIndex1+length<=flex.size) {
			byte[] computedArray = new byte[length];
			IntStream.range(0,length).forEach(i -> {
				computedArray[i] = (byte)(this.byteArray[this.startIndex+startIndex0+i]^flex.byteArray[flex.startIndex+startIndex1+i]);
			});
			FlexBytes computedFlex = new FlexBytes(computedArray, 0, length);
			return computedFlex;
		} else {
			throw new IllegalArgumentException("'" + startIndex0 + "," + startIndex1 + "," + length +  "': " + FLEX_COMPATIBILITY);
		}
	}
	
	public void logicalXOrEdit(int startIndex0, FlexBytes flex, int startIndex1, int length) throws IllegalArgumentException {
		if(startIndex0>=0 && startIndex1>=0 && startIndex0<this.size && startIndex1<flex.size && length>0 
				&& startIndex0+length<=this.size && startIndex1+length<=flex.size) {
			IntStream.range(0,length).forEach(i -> {
				this.byteArray[this.startIndex+startIndex0+i] = (byte)(this.byteArray[this.startIndex+startIndex0+i]
						^flex.byteArray[flex.startIndex+startIndex1+i]);
			});
		} else {
			throw new IllegalArgumentException("'" + startIndex0 + "," + startIndex1 + "," + length +  "': " + FLEX_COMPATIBILITY);
		}
	}
	
	public FlexBytes logicalNot(int startIndex, int length) throws IllegalArgumentException {
		if(startIndex>=0 && startIndex<this.size && length>0 && startIndex+length<=this.size) {
			byte[] computedArray = new byte[length];
			IntStream.range(0,length).forEach(i -> {
				computedArray[i] = (byte)(~this.byteArray[this.startIndex+startIndex+i]);
			});
			return new FlexBytes(computedArray, startIndex, length);
		} else {
			throw new IllegalArgumentException("'" + startIndex + "," + length +  "': " + FIMPLE_FLEX_FAULT);
		}
	}
	
	public void logicalNotEdit(int startIndex, int length) throws IllegalArgumentException {
		if(startIndex>=0 && startIndex<this.size && length>0 && startIndex+length<=this.size) {
			IntStream.range(0,length).forEach(i -> {
				this.byteArray[this.startIndex+startIndex+i] = (byte)(~this.byteArray[this.startIndex+i]);
			});
		} else {
			throw new IllegalArgumentException("'" + startIndex + "," + length +  "': " + FIMPLE_FLEX_FAULT);
		}
	}
	
	private static boolean compareFlexArrays(FlexBytes flex0, FlexBytes flex1) {
		boolean statement = true;
		if(flex0.size != flex1.size) {
			statement = false;
		} else {
			for(int i=0; i<flex0.size; i++) {
				if(flex0.byteArray[flex0.startIndex+i]!=flex1.byteArray[flex1.startIndex+i]) {
					statement = false;
					break;
				}
			}
		}
		return statement;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != FlexBytes.class) {
			return false;
	    }
		FlexBytes other = (FlexBytes) obj;
	    if (!compareFlexArrays(this, other)) {
	    	return false;
	    }
	    return true;
	}

	@Override
	public int hashCode() {
		AtomicInteger result = new AtomicInteger(0);
		IntStream.range(startIndex,startIndex+size).forEach(i -> {
			result.set(result.get()^byteArray[i]);
		});
		return result.get();
	}

	@Override
	public boolean hasNext() {
		if(i<this.size) {
			return true;
		} else {
			this.i = 0;
			return false;
		}
	}

	@Override
	public Byte next() {
		if(i!=this.size) {
			return this.byteArray[this.startIndex+i++];
		} else {
			throw new NoSuchElementException();
		}
	}
	
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Iterator<Byte> iterator() {
		return this;
	}
	
}
