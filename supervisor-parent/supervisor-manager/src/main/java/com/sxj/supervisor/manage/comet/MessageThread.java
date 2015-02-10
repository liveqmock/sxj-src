package com.sxj.supervisor.manage.comet;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.comet4j.core.CometContext;
import org.comet4j.core.CometEngine;

import com.sxj.cache.manager.CacheLevel;
import com.sxj.cache.manager.HierarchicalCacheManager;
import com.sxj.util.logger.SxjLogger;

public class MessageThread extends Thread
{
    
    private AtomicInteger counter = new AtomicInteger(0);
    
    private CometEngine engine;
    
    private static int period = 4;
    
    private static int delay = 3;
    
    private static volatile MessageThread _self = null;
    
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private MessageThread(CometEngine engine)
    {
        this.engine = engine;
    }
    
    public static MessageThread newInstance(CometEngine engine)
    {
        if (_self == null)
        {
            _self = new MessageThread(engine);
            return _self;
        }
        return _self;
    }
    
    public static MessageThread getInstance()
    {
        if (_self == null)
            throw new RuntimeException("No instance exists!!!");
        return _self;
    }
    
    public void schedule()
    {
        scheduler.scheduleAtFixedRate(_self,
                getDelay(),
                getPeriod(),
                TimeUnit.SECONDS);
    }
    
    public void shutdown()
    {
        descrCounter();
        // if (getCounter().get() <= 0)
        // scheduler.shutdown();
    }
    
    public void descrCounter()
    {
        if (counter.get() > 0)
            counter.decrementAndGet();
    }
    
    public void incrCounter()
    {
        counter.incrementAndGet();
    }
    
    @Override
    public void run()
    {
        List<String> channels = CometContext.getInstance().getAppModules();
        SxjLogger.debug("Now Comet Connection Count: "
                + getEngine().getConnections().size(), getClass());
        SxjLogger.debug("Now Comet Channel Size: " + channels.size(),
                getClass());
        if (getCounter().get() > 0 && channels.size() > 0)
        {
            for (String channel : channels)
            {
                Object cache = HierarchicalCacheManager.get(CacheLevel.REDIS,
                        "comet_message",
                        channel);
                SxjLogger.debug("Sending Message to Comet Client:" + cache,
                        getClass());
                if (cache != null)
                {
                    send(channel, cache);
                }
                long sleep = (long) (Math.random() * 100);
                
                try
                {
                    Thread.currentThread().sleep(sleep);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }
        
    }
    
    public void send(String channel, Object message)
    {
        getEngine().sendToAll(channel, message);
    }
    
    public int getPeriod()
    {
        return period;
    }
    
    public void setPeriod(int period)
    {
        this.period = period;
    }
    
    public int getDelay()
    {
        return delay;
    }
    
    public void setDelay(int delay)
    {
        this.delay = delay;
    }
    
    public AtomicInteger getCounter()
    {
        return counter;
    }
    
    public static ScheduledExecutorService getScheduler()
    {
        return scheduler;
    }
    
    protected CometEngine getEngine()
    {
        return engine;
    }
    
    public static void main(String... strings)
    {
        
        Set<String> set = new HashSet<String>();
        set.add("1");
        set.add("1");
        System.out.println(set.size());
    }
}
