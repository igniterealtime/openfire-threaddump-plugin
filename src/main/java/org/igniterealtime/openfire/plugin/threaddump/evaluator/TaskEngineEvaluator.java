package org.igniterealtime.openfire.plugin.threaddump.evaluator;

import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.TaskEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadPoolExecutor;

public class TaskEngineEvaluator implements Evaluator
{
    private static final Logger Log = LoggerFactory.getLogger( TaskEngineEvaluator.class );

    private int successiveHits = 0;

    @Override
    public Duration getInterval()
    {
        final long backoffMS = JiveGlobals.getLongProperty( "threaddump.evaluator.taskengine.interval", Duration.of( 5, ChronoUnit.SECONDS ).toMillis() );
        return Duration.of( backoffMS, ChronoUnit.MILLIS );
    }

    @Override
    public boolean shouldCreateThreadDump()
    {
        Log.trace( "Evaluating..." );
        if ( checkPool() )
        {
            Log.trace( "At least one statistic for the TaskEngine is at max. Increasing successive hit count." );
            successiveHits++;
            final int limit = JiveGlobals.getIntProperty( "threaddump.evaluator.taskengine.successive-hits", 2 );
            if ( successiveHits >= limit )
            {
                Log.trace( "Successive hit count ({}) has hit limit ({}).", successiveHits, limit );
                successiveHits = 0;
                Log.debug( "Do create a thread dump." );
                return true;
            }
        }
        else
        {
            successiveHits = 0;
        }

        Log.debug( "No need to create a thread dump." );
        return false;
    }

    protected boolean checkPool()
    {
        final int maxActiveCount = JiveGlobals.getIntProperty( "threaddump.evaluator.taskengine.max-threads", 300 );
        final int maxPoolsize = JiveGlobals.getIntProperty( "threaddump.evaluator.taskengine.max-poolsize", 500 );

        Field executorField = null;
        try {
            executorField = TaskEngine.class.getDeclaredField("executor"); //NoSuchFieldException
            executorField.setAccessible(true);
            final ThreadPoolExecutor executor = (ThreadPoolExecutor) executorField.get(TaskEngine.getInstance()); //IllegalAccessException
            final int activeCount = executor.getActiveCount();
            if (activeCount >= maxActiveCount) {
                return true;
            }

            final int poolSize = executor.getPoolSize();
            if (poolSize >= maxPoolsize) {
                return true;
            }
        } catch ( Throwable t ) {
            Log.warn("An exception occurred while trying to check the pool of TaskEngine.", t);
        } finally {
            if (executorField != null) {
                executorField.setAccessible(false);
            }
        }
        return false;
    }
}
