package simplepool.base.abstracts;

/**
 * @author XingGu_Liu
 * 
 * All public methods should be thread safe
 * 
 */
public interface IObjPool<T> {

	/**
	 * Take an object from idle queue.
	 * @return null if there is no idle object.
	 */
	T borrowObject();
	
	/**
	 * Return object to idle queue
	 * @param obj
	 */
	void returnObject(T obj);
	
	/**
	 * assigned object will be disposed, and a new one will be created as a replacement.
	 * @param obj
	 */
	void invalidateObject(T obj);
	
	/**
	 * Number of idle objects
	 * @return
	 */
	int getNumIdle();
	
	/**
	 * Number of active objects
	 * @return
	 */
	int getNumActive();
	
	/**
	 * After pool closed, all objects are disposed. Methods below will be thrown exception:
	 * borrowObject(), returnObject(obj), invalidateObject(obj)
	 */
	void close();
}
