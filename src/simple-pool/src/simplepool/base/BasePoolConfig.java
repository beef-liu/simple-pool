package simplepool.base;

public class BasePoolConfig {

	private String _poolName;

	private int _maxTotal;
	
	private int _maxIdle;
	
	private int _minIdle;
	
	/* deprecated for simplicity
    private boolean _testOnCreate = false;
    private boolean _testOnBorrow = false;
    private boolean _testOnReturn = false;
    */

    private boolean _testWhileIdle = true;
    
    private long _timeBetweenEvictionRunsMillis = 60 * 1000L;

    public String getPoolName() {
        return _poolName;
    }

    public void setPoolName(String poolName) {
        _poolName = poolName;
    }

    public int getMaxTotal() {
		return _maxTotal;
	}

	public void setMaxTotal(int maxTotal) {
		_maxTotal = maxTotal;
	}

	public int getMaxIdle() {
		return _maxIdle;
	}

	public void setMaxIdle(int maxIdle) {
		_maxIdle = maxIdle;
	}

	public int getMinIdle() {
		return _minIdle;
	}

	public void setMinIdle(int minIdle) {
		_minIdle = minIdle;
	}

	/*
	public boolean isTestOnCreate() {
		return _testOnCreate;
	}

	public void setTestOnCreate(boolean testOnCreate) {
		_testOnCreate = testOnCreate;
	}

	public boolean isTestOnBorrow() {
		return _testOnBorrow;
	}

	public void setTestOnBorrow(boolean testOnBorrow) {
		_testOnBorrow = testOnBorrow;
	}

	public boolean isTestOnReturn() {
		return _testOnReturn;
	}

	public void setTestOnReturn(boolean testOnReturn) {
		_testOnReturn = testOnReturn;
	}
	*/

	public boolean isTestWhileIdle() {
		return _testWhileIdle;
	}

	public void setTestWhileIdle(boolean testWhileIdle) {
		_testWhileIdle = testWhileIdle;
	}

	public long getTimeBetweenEvictionRunsMillis() {
		return _timeBetweenEvictionRunsMillis;
	}

	public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
		_timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
	}
    
}
