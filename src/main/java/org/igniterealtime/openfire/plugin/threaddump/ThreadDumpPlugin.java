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

import org.igniterealtime.openfire.plugin.threaddump.evaluator.CoreThreadPoolsEvaluator;
import org.igniterealtime.openfire.plugin.threaddump.evaluator.DeadlockEvaluator;
import org.igniterealtime.openfire.plugin.threaddump.evaluator.Evaluator;
import org.igniterealtime.openfire.plugin.threaddump.evaluator.TaskEngineEvaluator;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.PropertyEventDispatcher;
import org.jivesoftware.util.PropertyEventListener;
import org.jivesoftware.util.TaskEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ThreadDumpPlugin implements Plugin, PropertyEventListener
{
    private static final Logger Log = LoggerFactory.getLogger( ThreadDumpPlugin.class );

    private DumpCheckTimerTask task;

    @Override
    public void initializePlugin( final PluginManager manager, final File pluginDirectory )
    {
        PropertyEventDispatcher.addListener( this );
        configMonitor();
    }

    @Override
    public void destroyPlugin()
    {
        PropertyEventDispatcher.removeListener( this );
        if ( task != null )
        {
            TaskEngine.getInstance().cancelScheduledTask( task );
            task = null;
        }
    }

    public synchronized void configMonitor()
    {
        if ( task != null )
        {
            TaskEngine.getInstance().cancelScheduledTask( task );
            task = null;
        }

        if ( isTaskEnabled() )
        {
            final long delayMS = getTaskDelay().toMillis();
            final long intervalMS = getTaskInterval().toMillis();
            final long backoffMS = getTaskBackoff().toMillis();
            final Set<Class<? extends Evaluator>> evaluatorClasses = getTaskEvaluatorClasses();
            final Set<Evaluator> evaluators = new HashSet<>();
            for ( final Class<? extends Evaluator> evaluatorClass : evaluatorClasses )
            {
                try
                {
                    final Evaluator evaluator = evaluatorClass.newInstance();
                    evaluators.add( evaluator );
                }
                catch ( Exception e )
                {
                    Log.warn( "Unable to instantiate evaluator {}", evaluatorClass, e );
                }
            }

            if ( !evaluators.isEmpty() )
            {
                final Duration backoff = Duration.of( backoffMS, ChronoUnit.MILLIS );
                Log.info( "Scheduling a check for thread dump necessity evaluation every {}ms (starting after a delay of {}ms. No more than one dump per {} will be generated.", new Object[] {intervalMS, delayMS, backoff} );
                evaluators.forEach( evaluator -> Log.info( "Enabled evaluator {}, running every {}", evaluator.getClass().getCanonicalName(), evaluator.getInterval() ) );
                task = new DumpCheckTimerTask( backoff );
                evaluators.forEach( evaluator -> task.add( evaluator ) );
                TaskEngine.getInstance().schedule( task, delayMS, intervalMS );
            }
        }
    }

    @Override
    public void propertySet( final String property, final Map<String, Object> params )
    {
        if ( property.startsWith( "threaddump.task." ) )
        {
            configMonitor();
        }
    }

    @Override
    public void propertyDeleted( final String property, final Map<String, Object> params )
    {
        if ( property.startsWith( "threaddump.task." ) )
        {
            configMonitor();
        }
    }

    @Override
    public void xmlPropertySet( final String property, final Map<String, Object> params )
    {}

    @Override
    public void xmlPropertyDeleted( final String property, final Map<String, Object> params )
    {}

    public boolean isTaskEnabled()
    {
        return JiveGlobals.getBooleanProperty( "threaddump.task.enable", true );
    }

    public void setTaskEnabled( boolean enabled )
    {
        JiveGlobals.setProperty( "threaddump.task.enable", Boolean.toString( enabled ) );
    }

    public Duration getTaskDelay()
    {
        return Duration.of( JiveGlobals.getLongProperty( "threaddump.task.delay", Duration.of( 60, ChronoUnit.SECONDS ).toMillis() ), ChronoUnit.MILLIS );
    }

    public void setTaskDelay( Duration duration )
    {
        JiveGlobals.setProperty( "threaddump.task.delay", String.valueOf( duration.toMillis() ) );
    }

    public Duration getTaskInterval()
    {
        return Duration.of( JiveGlobals.getLongProperty( "threaddump.task.interval", Duration.of( 5, ChronoUnit.SECONDS ).toMillis() ), ChronoUnit.MILLIS );
    }

    public void setTaskInterval( Duration duration )
    {
        JiveGlobals.setProperty( "threaddump.task.interval", String.valueOf( duration.toMillis() ) );
    }

    public Duration getTaskBackoff()
    {
        return Duration.of( JiveGlobals.getLongProperty( "threaddump.task.backoff", Duration.of( 5, ChronoUnit.MINUTES ).toMillis() ), ChronoUnit.MILLIS );
    }

    public void setTaskBackoff( Duration duration )
    {
        JiveGlobals.setProperty( "threaddump.task.backoff", String.valueOf( duration.toMillis() ) );
    }

    public Set<Class<? extends Evaluator>> getTaskEvaluatorClasses()
    {
        final Set<Class<? extends Evaluator>> result = new HashSet<>();
        final String evaluatorNames = JiveGlobals.getProperty( "threaddump.task.evaluators", CoreThreadPoolsEvaluator.class.getCanonicalName() + ", " + DeadlockEvaluator.class.getCanonicalName() + ", " + TaskEngineEvaluator.class.getCanonicalName() );
        if ( evaluatorNames != null ) {
            for ( final String evaluatorName : evaluatorNames.split( "\\s*,\\s*" ) )
            {
                if ( evaluatorName == null || evaluatorName.isEmpty() ) {
                    continue;
                }

                try
                {
                    final Class<? extends Evaluator> evaluator = (Class<? extends Evaluator>) Class.forName( evaluatorName );
                    result.add( evaluator );
                }
                catch ( Exception e )
                {
                    Log.warn( "Unable to load evaluator {} class.", evaluatorName, e );
                }
            }
        }
        return result;
    }

    public void setTaskEvaluatorClasses( Set<Class<? extends Evaluator>> evaluators )
    {
        final String propValue = evaluators.stream().map( Class::getCanonicalName ).collect( Collectors.joining( ", " ) );
        JiveGlobals.setProperty( "threaddump.task.evaluators", propValue );
    }

    public Instant getLastDumpInstant()
    {
        if ( task != null && task.getLastDumpInstant() != null && !task.getLastDumpInstant().equals( Instant.EPOCH ) )
        {
            return task.getLastDumpInstant();
        }

        return null;
    }
}
