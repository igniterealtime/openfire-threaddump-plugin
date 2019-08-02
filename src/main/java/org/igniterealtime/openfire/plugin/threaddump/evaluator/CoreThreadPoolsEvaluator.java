package org.igniterealtime.openfire.plugin.threaddump.evaluator;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.spi.ConnectionListener;
import org.jivesoftware.openfire.spi.ConnectionManagerImpl;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

public class CoreThreadPoolsEvaluator implements Evaluator
{
    private static final Logger Log = LoggerFactory.getLogger( CoreThreadPoolsEvaluator.class );

    private int successiveHits = 0;

    @Override
    public Duration getInterval()
    {
        final long backoffMS = JiveGlobals.getLongProperty( "threaddump.evaluator.threadpools.interval", Duration.of( 5, ChronoUnit.SECONDS ).toMillis() );
        return Duration.of( backoffMS, ChronoUnit.MILLIS );
    }

    @Override
    public boolean shouldCreateThreadDump()
    {
        Log.trace( "Evaluating..." );
        if ( checkPools() )
        {
            Log.trace( "At least one pool is at max. Increasing successive hit count." );
            successiveHits++;
            final int limit = JiveGlobals.getIntProperty( "threaddump.evaluator.threadpools.successive-hits", 2 );
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

    protected boolean checkPools()
    {
        final int busyPercentageLimit = JiveGlobals.getIntProperty( "threaddump.evaluator.threadpools.busy-percentage", 90 );
        final Set<ConnectionListener> listeners = ((ConnectionManagerImpl) XMPPServer.getInstance().getConnectionManager()).getListeners();
        for ( ConnectionListener listener : listeners )
        {
            final NioSocketAcceptor socketAcceptor = listener.getSocketAcceptor();
            if ( socketAcceptor == null )
            {
                continue;
            }

            final DefaultIoFilterChainBuilder filterChain = socketAcceptor.getFilterChain();
            if ( filterChain == null )
            {
                continue;
            }

            if ( filterChain.contains( ConnectionManagerImpl.EXECUTOR_FILTER_NAME ) )
            {
                final ExecutorFilter executorFilter = (ExecutorFilter) filterChain.get( ConnectionManagerImpl.EXECUTOR_FILTER_NAME );
                final int max = ((ThreadPoolExecutor) executorFilter.getExecutor()).getMaximumPoolSize();
                final int act = ((ThreadPoolExecutor) executorFilter.getExecutor()).getActiveCount();
                final int busyPercentage = (int) Math.round( act * 100.0 / max );

                Log.trace( "{}% ({}/{}) thread running in pool {} {}. Threshold: {}%", new Object[] { busyPercentage, act, max, listener.getType(), listener.getTLSPolicy(), busyPercentageLimit });
                if ( busyPercentage > busyPercentageLimit )
                {
                    return true;
                }
            }
        }

        return false;
    }
}
