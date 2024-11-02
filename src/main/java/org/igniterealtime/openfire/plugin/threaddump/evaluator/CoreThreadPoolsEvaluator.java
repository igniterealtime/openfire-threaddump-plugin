/*
 * Copyright (C) 2019-2024 Ignite Realtime Foundation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.igniterealtime.openfire.plugin.threaddump.evaluator;

import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.spi.ConnectionAcceptor;
import org.jivesoftware.openfire.spi.ConnectionListener;
import org.jivesoftware.openfire.spi.ConnectionManagerImpl;
import org.jivesoftware.openfire.spi.NettyConnectionAcceptor;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;

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
        final int busyPercentageLimit = JiveGlobals.getIntProperty( "threaddump.evaluator.threadpools.busy-percentage-max", 90 );
        final Set<ConnectionListener> listeners = ((ConnectionManagerImpl) XMPPServer.getInstance().getConnectionManager()).getListeners();
        for ( ConnectionListener listener : listeners )
        {
            final ConnectionAcceptor socketAcceptor = listener.getConnectionAcceptor();
            if ( socketAcceptor == null )
            {
                continue;
            }

            if (!(socketAcceptor instanceof NettyConnectionAcceptor))
            {
                continue;
            }

// FIXME: Replace the MINA-based implementation with one that is compatible with Netty. See https://github.com/igniterealtime/openfire-threaddump-plugin/issues/16
//            final EventLoopGroup childEventLoopGroup = ((NettyConnectionAcceptor) socketAcceptor)..getChildEventLoopGroup();
//            if (childEventLoopGroup == null)
//            {
//                continue;
//            }
//
//            childEventLoopGroup.
//            if ( filterChain.contains( ConnectionManagerImpl.EXECUTOR_FILTER_NAME ) )
//            {
//                final ExecutorFilter executorFilter = (ExecutorFilter) filterChain.get( ConnectionManagerImpl.EXECUTOR_FILTER_NAME );
//                final int max = ((ThreadPoolExecutor) executorFilter.getExecutor()).getMaximumPoolSize();
//                final int act = ((ThreadPoolExecutor) executorFilter.getExecutor()).getActiveCount();
//                final int busyPercentage = (int) Math.round( act * 100.0 / max );
//
//                Log.trace( "{}% ({}/{}) thread running in pool {} {}. Threshold: {}%", busyPercentage, act, max, listener.getType(), listener.getTLSPolicy(), busyPercentageLimit);
//                if ( busyPercentage > busyPercentageLimit )
//                {
//                    return true;
//                }
//            }
        }

        return false;
    }
}
