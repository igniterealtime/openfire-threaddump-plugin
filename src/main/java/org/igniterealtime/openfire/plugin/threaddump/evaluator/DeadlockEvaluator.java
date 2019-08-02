package org.igniterealtime.openfire.plugin.threaddump.evaluator;

import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class DeadlockEvaluator implements Evaluator
{
    private static final Logger Log = LoggerFactory.getLogger( DeadlockEvaluator.class );

    @Override
    public Duration getInterval()
    {
        final long backoffMS = JiveGlobals.getLongProperty( "threaddump.evaluator.deadlock.interval", Duration.of( 5, ChronoUnit.MINUTES ).toMillis() );
        return Duration.of( backoffMS, ChronoUnit.MILLIS );
    }

    @Override
    public boolean shouldCreateThreadDump()
    {
        Log.trace( "Evaluating..." );
        final boolean result = detectDeadlock();
        Log.debug( "Deadlock {}", result ? "detected!" : "not detected." );
        return result;
    }

    protected boolean detectDeadlock()
    {
        final long[] deadlockedThreads = ManagementFactory.getThreadMXBean().findDeadlockedThreads();
        return deadlockedThreads != null && deadlockedThreads.length > 0;
    }
}
