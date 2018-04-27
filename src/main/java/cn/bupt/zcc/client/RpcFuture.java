package cn.bupt.zcc.client;

import cn.bupt.zcc.common.RpcDecoder;
import cn.bupt.zcc.common.RpcRequest;
import cn.bupt.zcc.common.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedLongSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by 张城城 on 2018/4/26.
 */
public class RpcFuture  implements Future<Object>{

    private static final Logger LOGGER = LoggerFactory.getLogger(RpcFuture.class);

    private RpcRequest request;
    private RpcResponse response;
    private Sync sync;
    private long startTime;

    private long responseTimeThreshold = 5000;

    private ReentrantLock lock = new ReentrantLock();

    public RpcFuture(RpcRequest request){
        this.request =request;
        this.sync = new Sync();
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        sync.acquire(1);
        if (this.response!=null){
            return this.response.getResult();
        }
        return null;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireNanos(-1,unit.toNanos(timeout));
        if (success){
            if (this.response!=null){
                return this.response.getResult();
            }else {
                return null;
            }
        }else {
            throw new RuntimeException("Timeout exception. Request id: " + this.request.getRequestId()
                    + ". Request class name: " + this.request.getClassName()
                    + ". Request method: " + this.request.getMethodName());
        }
    }

    public void done(RpcResponse rpcResponse){
        this.response = rpcResponse;
        sync.release(1);

        long responseTime = System.currentTimeMillis() - startTime;
        if (responseTime > this.responseTimeThreshold) {
            LOGGER.warn("Service response time is too slow. Request id = " + rpcResponse.getRequestId() + ". Response Time = " + responseTime + "ms");
        }

    }
    static class Sync extends AbstractQueuedLongSynchronizer{
        private static final long serialVersionUID=1L;
        private final int done = 1;
        private final int pending = 0;

        protected boolean tryAcquire(int acquires){
            return getState() ==done ;
        }
        protected boolean tryRelease(int releases){
            if (getState()==pending){
                if (compareAndSetState(pending,done)){
                    return true;
                }
            }
            return false;
        }

        public boolean isDone(){
            getState();
            return getState() == done;
        }
    }
}
