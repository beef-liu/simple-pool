package simplepool.base;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import simplepool.base.abstracts.IObjFactory;
import simplepool.base.abstracts.IObjPool;
import simplepool.base.abstracts.IPooledObj;

public class GenericObjPool<T> implements IObjPool<T> {
	private final int _maxTotal;
	private final int _maxIdle;
	private final int _minIdle;
    private final boolean _testWhileIdle;
    private final long _timeBetweenEvictionRunsMillis;
    private final int _initialSize;
    
    
    private final String _logMsgPrefix;

    private final IObjFactory<T> _objFactory;

    private final AtomicInteger _idleCount = new AtomicInteger(0);
	//private final AtomicInteger _activeCount = new AtomicInteger(0);
    private final AtomicInteger _totalCount = new AtomicInteger(0);

    private final AtomicBoolean _closingFlg = new AtomicBoolean(false);
    private final AtomicBoolean _initFlg = new AtomicBoolean(false);

    private final Queue<IPooledObj<T>> _idleQueue = new LinkedTransferQueue<IPooledObj<T>>();
	private final Map<T, IPooledObj<T>> _allObjMap = new ConcurrentHashMap<T, IPooledObj<T>>();
	
	private TestThread _testThread = null;

    /**
     *
     * @param poolConfig only several fields are used, they are below:
     *                   maxTotal, maxIdle, minIdle
     * @param objFactory
     */
	public GenericObjPool(
			BasePoolConfig poolConfig,
			IObjFactory<T> objFactory
	) {
        _maxTotal = poolConfig.getMaxTotal();
        _maxIdle = poolConfig.getMaxIdle();
        _minIdle = poolConfig.getMinIdle();
        _testWhileIdle = poolConfig.isTestWhileIdle();
        _timeBetweenEvictionRunsMillis = poolConfig.getTimeBetweenEvictionRunsMillis();
        
        _initialSize = _minIdle;

        _objFactory = objFactory;
        
        _logMsgPrefix = GenericObjPool.class.getName() + "[factory:" + objFactory.getClass().getName() + "] ";

        //init pool
        initPool();
	}

	@Override
	public T borrowObject() {
		assertNotClosing();
		
		IPooledObj<T> t = dequeueOfIdle();
		if(t == null) {
            if(_totalCount.get() < _maxTotal) {
                //make new one
                t = makeNewObjButNotAddToIdle();
                if(t == null) {
                    return null;
                } else {
                    //will do operations below
                }
            } else {
                return null;
            }
		}

        //update state
        boolean stateUpdated = t.setReturned(false);
        if(stateUpdated) {
            t.setLastBorrowTime(System.currentTimeMillis());

            //_activeCount.incrementAndGet();
            return t.getObject();
        } else {
            System.err.println(_logMsgPrefix
                    + "Unexpected error occurred. State of object polled from idleQueue is (not returned)"
            );
            return null;
        }
	}

	@Override
	public void returnObject(T obj) {
		assertNotClosing();

		IPooledObj<T> t = _allObjMap.get(obj);
		if(t != null) {
			boolean stateUpdated = t.setReturned(true);
			if(stateUpdated) {
                t.setLastReturnTime(System.currentTimeMillis());
                enqueueOfIdle(t);
			}
		}
	}

	@Override
	public void invalidateObject(T obj) {
		assertNotClosing();

        removeAndDestroyObj(obj);
	}

	@Override
	public int getNumIdle() {
		return _idleCount.get();
	}

	@Override
	public int getNumActive() {
		//return _activeCount.get();
        return _totalCount.get() - _idleCount.get();
	}

	@Override
	public void close() {
        if(_closingFlg.compareAndSet(false, true)) {
            releaseAllObjs();
        }
	}

    private void releaseAllObjs() {
        //stop test thread
    	try {
        	stopObjTestThread();
    	} catch (Throwable e) {
    		e.printStackTrace();
    	}

        //clear idle queue
    	try {
            _idleQueue.clear();
    	} catch (Throwable e) {
    		e.printStackTrace();
    	}

    	//destroy all
    	try {
            Collection<IPooledObj<T>> pooledObjList = _allObjMap.values();
            for (IPooledObj<T> pooledObj : pooledObjList) {
                try {
                    _objFactory.destroyObject(pooledObj.getObject());
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
    	} catch (Throwable e) {
    		e.printStackTrace();
    	}
        
    	//clear all obj
        _allObjMap.clear();
    }

    private void initPool() {
    	if(!_initFlg.compareAndSet(false, true)) {
    		return;
    	}
    	
    	//create test thread
    	if(_testWhileIdle) {
    		startObjTestThread();
    	}
    	
    	//make objects of initial size
    	for(int i = 0; i < _initialSize; i++) {
    		try {
                makeNewObjAndAddToIdle();
    		} catch (Throwable e) {
    			e.printStackTrace();
    		}
    	}
    }

    private void makeNewObjAndAddToIdle() {
        IPooledObj<T> t = makeNewObjButNotAddToIdle();
        if(t != null) {
            enqueueOfIdle(t);
        }
    }

    private IPooledObj<T> makeNewObjButNotAddToIdle() {
        T obj = _objFactory.makeObject();
        IPooledObj<T> t = makePooledObj(obj);

        if(_allObjMap.put(obj, t) == null) {
            _totalCount.incrementAndGet();

            return t;
        } else {
            System.err.println(_logMsgPrefix
                    + "Unexpected error occurred. _objFactory.makeObject() should never make new one same as the old one."
            );
            return null;
        }
    }

    private void removeAndDestroyObj(T obj) {
        //remove from allObjMap
        if(_allObjMap.remove(obj) != null) {
            //_activeCount.decrementAndGet();
            _totalCount.decrementAndGet();
        }

        //destroy obj
        if(obj != null) {
            try {
                _objFactory.destroyObject(obj);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private void assertNotClosing() {
        if(_closingFlg.get()) {
            throw new RuntimeException("Pool is closed!");
        }
    }

    private IPooledObj<T> dequeueOfIdle() {
        IPooledObj<T> t = _idleQueue.poll();
        if(t != null) {
            _idleCount.decrementAndGet();
        }

        return t;
    }

    private void enqueueOfIdle(IPooledObj<T> t) {
        _idleQueue.add(t);
        _idleCount.incrementAndGet();
    }

    private void startObjTestThread() {
    	_testThread = new TestThread();
    	_testThread.start();
    }
    
    private void stopObjTestThread() {
    	if(_testThread != null) {
    		_testThread.shutdown();
    	}
    }
    
    private IPooledObj<T> makePooledObj(T obj) {
    	return new BasePooledObj<T>(obj);
    }

    private class TestThread extends Thread {
        private final float _maxRatioInEviction = 0.25f;

        private volatile boolean _stopFlg = false;
    	private final LinkedTransferQueue<IPooledObj<T>> _testQueue = new LinkedTransferQueue<>();

    	public void shutdown() {
            _stopFlg = true;
			this.interrupt();
    	}
    	
    	public void addObj(IPooledObj<T> t) {
    		_testQueue.add(t);
    	}

    	@Override
    	public void run() {
    		IPooledObj<T> t;
    		boolean isValid;
    		
    		try {
        		while(!_stopFlg) {
        			t = _testQueue.poll(_timeBetweenEvictionRunsMillis, TimeUnit.MILLISECONDS);
                    if(t == null) {
                        checkIdleObjsForEviction();
                        continue;
                    }
        			
        			try {
                        if (_totalCount.get() >= _maxTotal) {
                            //destroy it without validating
                            removeAndDestroyObj(t.getObject());
                        } else {
                            isValid = _objFactory.validateObject(t.getObject());
                            if(!isValid) {
                                removeAndDestroyObj(t.getObject());

                                final int curIdleCount = _idleCount.get();
                                if(curIdleCount >= _maxIdle) {
                                    //do not make new obj, release the very idle one.
                                } else {
                                    //make new obj
                                    makeNewObjAndAddToIdle();
                                }
                            } else {
                                //return valid obj to idle queue
                                t.setLastEvictionTestTime(System.currentTimeMillis());
                                _idleQueue.add(t);
                            }
                        }
        			} catch (Throwable e) {
        				e.printStackTrace();
        			}
        		}
    		} catch (InterruptedException e) {
    			System.out.println(_logMsgPrefix + "TestThread loop end. _testQueue is cleared.");
    		} catch (Throwable e) {
                e.printStackTrace();
            }

            //test loop end
            _testQueue.clear();
    	}

        private void checkIdleObjsForEviction() {
            final int evictionMax = (int) (_idleCount.get() * _maxRatioInEviction);
            int evictionAddCount = 0;

            IPooledObj<T> t;
            while(true) {
                t = _idleQueue.poll();
                if(t == null) {
                    break;
                }

                if(isNeedEvictionTest(t)) {
                    addObj(t);

                    evictionAddCount ++;
                    if(evictionAddCount >= evictionMax) {
                        break;
                    }
                } else {
                    break;
                }
            }

            //check _minIdle
            if(_idleCount.get() < _minIdle) {
                final int 
            }
        }

        private boolean isNeedEvictionTest(IPooledObj<T> t) {
            long elapsedTime = System.currentTimeMillis() - t.getLastEvictionTestTime();
            if(elapsedTime >= _timeBetweenEvictionRunsMillis) {
                return true;
            } else {
                return false;
            }
        }

    }
}
