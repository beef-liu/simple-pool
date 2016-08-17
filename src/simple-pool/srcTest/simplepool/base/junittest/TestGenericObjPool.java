package simplepool.base.junittest;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import simplepool.base.BasePoolConfig;
import simplepool.base.GenericObjPool;
import simplepool.base.abstracts.IObjFactory;

public class TestGenericObjPool {
	
	@Test
	public void test1() {
		BasePoolConfig poolConfig = new BasePoolConfig();
		poolConfig.setMaxTotal(128);
		poolConfig.setMaxIdle(64);
		poolConfig.setMinIdle(16);
		poolConfig.setTestWhileIdle(true);
		//10 seconds
		poolConfig.setTimeBetweenEvictionRunsMillis(10 * 1000L);
		
		final GenericObjPool<TestResource> pool = new GenericObjPool<TestResource>(
				poolConfig,
				new IObjFactory<TestResource>() {

					@Override
					public TestResource makeObject() {
						return new TestResource();
					}

					@Override
					public void destroyObject(TestResource obj) {
						obj.release();
					}

					@Override
					public boolean validateObject(TestResource obj) {
						return obj.isAlive();
					}

					@Override
					public void activateObject(TestResource obj) {
					}

					@Override
					public void passivateObject(TestResource obj) {
					}
				});
		
		try {
			//create test thread
			ExecutorService threadPool = Executors.newFixedThreadPool(32);
			
			long beginTime = System.currentTimeMillis();
			long testTime = 30 * 1000L;
			
			try {
				while (true) {
					if((System.currentTimeMillis() - beginTime) > testTime) {
						break;
					}
					
					for(int i = 0; i < 10000; i++) {
						threadPool.execute(new TestThread(pool));
					}
					
					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {
				//loop end
			}
			
			threadPool.shutdown();
			threadPool.awaitTermination(30 * 1000L, TimeUnit.MILLISECONDS);
			
			pool.close();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
	
	private static class TestThread implements Runnable {
		
		private final GenericObjPool<TestResource> _pool;
		
		public TestThread(GenericObjPool<TestResource> pool) {
			_pool = pool;
		}
		
		@Override
		public void run() {
			try {
				for (int i = 0; i < 1000; i++) {
					TestResource resource = _pool.borrowObject();
					if(resource != null) {
						try {
							//pseudo using resource through sleep
							Thread.sleep(0, 1000);
						} finally {
							_pool.returnObject(resource);
						}
					}
				}
			} catch (InterruptedException e) {
				//end
			}
		}
		
	}

	private static class TestResource {
		private static Random _rand = new Random(System.currentTimeMillis());
		
		private final long _birthTime = System.currentTimeMillis();
		private final long _lifeTime = (_rand.nextInt(10) + 5) * 1000L;
		
		private AtomicBoolean _released = new AtomicBoolean(false);
		
		public void release() {
			_released.set(true);
		}
		
		public boolean isAlive() {
			return (!_released.get()) 
					&& ((System.currentTimeMillis() - _birthTime) < _lifeTime);
		}
	}
	
	private final static SimpleDateFormat _dateFmtYmdHmsSSS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private static void logDebug(String msg) {
		System.out.println(_dateFmtYmdHmsSSS.format(new Date()).concat(" -> ").concat(msg));
	}
	
}
