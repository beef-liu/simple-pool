package simplepool.base.abstracts;

public interface IPooledObj<T> extends Comparable<IPooledObj<T>> {

	long getCreateTime();
	
	long getLastBorrowTime();
	void setLastBorrowTime(long time);
	
	long getLastReturnTime();
	void setLastReturnTime(long time);
	
	long getLastEvictionTestTime();
	void setLastEvictionTestTime(long time);
	
	T getObject();
	
	/**
	 * Caller should destroy the old object before it is replaced by new one.
	 * @param obj
	 */
	void setObject(T obj);
	
}
