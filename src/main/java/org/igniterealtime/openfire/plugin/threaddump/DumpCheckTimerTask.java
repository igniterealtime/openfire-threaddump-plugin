/*
 * Copyright (C) 2019 Ignite Realtime Foundation. All rights reserved.
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
package org.igniterealtime.openfire.plugin.threaddump;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.igniterealtime.openfire.plugin.threaddump.evaluator.Evaluator;
import org.igniterealtime.openfire.plugin.threaddump.formatter.DefaultThreadDumpFormatter;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.spi.ConnectionListener;
import org.jivesoftware.openfire.spi.ConnectionManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * A timer task that, conditionally, generates a thread dump.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class DumpCheckTimerTask extends TimerTask
{
    private static final Logger Log = LoggerFactory.getLogger( DumpCheckTimerTask.class );

    private final Duration backoff;

    private final ConcurrentMap<Evaluator, Instant> evaluators = new ConcurrentHashMap<>();

    private Instant lastDump = Instant.EPOCH;

    public DumpCheckTimerTask( Duration backoff )
    {
        this.backoff = backoff;
    }

    public void add( Evaluator evaluator )
    {
        evaluators.putIfAbsent( evaluator, Instant.EPOCH );
    }

    public void remove( Evaluator evaluator )
    {
        evaluators.remove( evaluator );
    }

    @Override
    public void run()
    {
        if ( evaluators.isEmpty() )
        {
            Log.trace( "Aborting this run: No evaluators are configured." );
            return;
        }

        if ( Instant.now().isBefore( lastDump.plus( backoff ) ) )
        {
            Log.trace( "Aborting this run: configuration disallows for more than 1 thread dump per {}. Last thread dump was created at {}.", backoff, lastDump );
            return;
        }

        boolean eligible = false;

        // Find evaluators that are due to run.
        final Set<Evaluator> due = evaluators.entrySet().stream()
            .filter( entry -> entry.getValue().plus( entry.getKey().getInterval() ).isBefore( Instant.now() ) )
            .map( Map.Entry::getKey )
            .collect( Collectors.toSet() );

        if ( due.isEmpty() )
        {
            Log.trace( "Aborting this run: No evaluators are due to run." );
            return;
        }

        // Check if at least one evaluator thinks that it's a good idead to create a thread dump.
        for ( final Evaluator evaluator : due )
        {
            Log.trace( "Running evaluator {}", evaluator.getClass().getSimpleName() );
            try
            {
                final boolean result = evaluator.shouldCreateThreadDump();
                evaluators.computeIfPresent( evaluator, ( e, i ) -> Instant.now() );
                if ( result )
                {
                    Log.info( "Evaluator {} requested a thread dump to be created.", evaluator.getClass().getSimpleName() );
                    eligible = true;
                    break;
                }
            } catch ( Exception e ) {
                Log.warn( "An unexpected exception occurred while determining of a thread dump should be created, using evaluator {}.", evaluator, e );
            }
        }

        Log.debug( "Run result: a thread dump {} be created.", eligible ? "should" : "should not" );
        if ( eligible )
        {
            dump();
            lastDump = Instant.now();
        }
    }

    private static void dump()
    {
        Log.info( "Creating thread dump." );
        final ThreadDump td = ThreadDump.getInstance();
        final String dump = new DefaultThreadDumpFormatter().format( td );
        LoggerFactory.getLogger( "threaddump" ).info( dump );
    }

    public Instant getLastDumpInstant()
    {
        return lastDump;
    }
}
