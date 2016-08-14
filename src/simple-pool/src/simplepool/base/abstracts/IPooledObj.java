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

    /**
     *
     * @return true: already returned to pool
     */
    boolean isReturned();

    /**
     * Updating returned flag with true must be atomic operation.
     * @return whether or not the new value is updated by this operation
     * <br>
     *      true:   new value is updated by this operation
     *      false:  already be equal with newVal when this operation is executed.
     */
	boolean setReturned(boolean newVal);
}
