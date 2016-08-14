package simplepool.base;

import simplepool.base.abstracts.IPooledObj;

import java.util.concurrent.atomic.AtomicBoolean;

public class BasePooledObj<T> implements IPooledObj<T> {

	private volatile T _obj;
	
	private volatile long _createTime;
	private volatile long _lastBorrowTime;
	private volatile long _lastReturnTime;
	private volatile long _lastEvictionTestTime;

	private final AtomicBoolean _returned = new AtomicBoolean(true);
	
	public BasePooledObj(T obj) {
		_createTime = System.currentTimeMillis();
		_lastBorrowTime = _createTime;
		_lastReturnTime = _createTime;
		_lastEvictionTestTime = _createTime;
		
		_obj = obj;
	}
	
	@Override
	public long getCreateTime() {
		return _createTime;
	}

	@Override
	public long getLastBorrowTime() {
		return _lastBorrowTime;
	}

	@Override
	public void setLastBorrowTime(long time) {
		_lastBorrowTime = time;
	}
	
	@Override
	public long getLastReturnTime() {
		return _lastReturnTime;
	}

	@Override
	public void setLastReturnTime(long time) {
		_lastReturnTime = time;
	}

	@Override
	public long getLastEvictionTestTime() {
		return _lastEvictionTestTime;
	}

	@Override
	public void setLastEvictionTestTime(long time) {
		_lastEvictionTestTime = time;
	}

	@Override
	public T getObject() {
		return _obj;
	}

	@Override
	public void setObject(T obj) {
		_obj = obj;
	}

	@Override
	public boolean isReturned() {
		return _returned.get();
	}

	@Override
	public boolean setReturned(boolean newVal) {
		return _returned.compareAndSet(!newVal, newVal);
	}

	@Override
	public int compareTo(IPooledObj<T> other) {
        final long lastActiveDiff = this.getLastReturnTime() - other.getLastReturnTime();
        if (lastActiveDiff == 0) {
            // Make sure the natural ordering is broadly consistent with equals
            // although this will break down if distinct objects have the same
            // identity hash code.
            // see java.lang.Comparable Javadocs
            return System.identityHashCode(this) - System.identityHashCode(other);
        }
        // handle int overflow
        return (int)Math.min(Math.max(lastActiveDiff, Integer.MIN_VALUE), Integer.MAX_VALUE);
	}
}
