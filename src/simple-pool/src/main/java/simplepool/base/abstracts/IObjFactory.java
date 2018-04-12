package simplepool.base.abstracts;

public interface IObjFactory<T> {

	T makeObject();
	
	void destroyObject(T obj);
	
	boolean validateObject(T obj);
	
	void activateObject(T obj);
	
	void passivateObject(T obj);
	
}
