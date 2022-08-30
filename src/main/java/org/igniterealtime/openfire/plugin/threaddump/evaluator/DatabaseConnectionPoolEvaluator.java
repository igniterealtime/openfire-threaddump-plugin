/*
 * Copyright (C) Ignite Realtime Foundation. All rights reserved.
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

import org.jivesoftware.database.ConnectionProvider;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.database.DefaultConnectionProvider;
import org.jivesoftware.database.EmbeddedConnectionProvider;
import org.jivesoftware.util.JiveGlobals;
import org.jivesoftware.util.TaskEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadPoolExecutor;

public class DatabaseConnectionPoolEvaluator implements Evaluator
{
    private static final Logger Log = LoggerFactory.getLogger(DatabaseConnectionPoolEvaluator.class);

    private int successiveHits = 0;

    @Override
    public Duration getInterval()
    {
        final long backoffMS = JiveGlobals.getLongProperty( "threaddump.evaluator.dbpool.interval", Duration.of( 5, ChronoUnit.SECONDS ).toMillis() );
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
            final int limit = JiveGlobals.getIntProperty( "threaddump.evaluator.dbpool.successive-hits", 2 );
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
        final int busyPercentageLimit = JiveGlobals.getIntProperty( "threaddump.evaluator.dbpool.busy-percentage-max", 90 );

        final ConnectionProvider connectionProvider = DbConnectionManager.getConnectionProvider();
        if (isSupported()) {
            final DefaultConnectionProvider defaultConnectionProvider = (DefaultConnectionProvider) connectionProvider;
            final int activeConnections = defaultConnectionProvider.getActiveConnections();
            final int maxConnections = defaultConnectionProvider.getMaxConnections();
            final int busyPercentage = 100 * activeConnections / maxConnections;
            Log.trace( "{}% ({}/{}) connections of database pool are currently active. Threshold: {}%", new Object[] { busyPercentage, activeConnections, maxConnections, busyPercentageLimit });
            return busyPercentage > busyPercentageLimit;
        } else {
            Log.debug("This server is using {} as the database connection provider for Openfire, which is not supported by this evaluator.", connectionProvider.getClass().getSimpleName());
            return false;
        }
    }

    @Override
    public boolean isSupported() {
        return DbConnectionManager.getConnectionProvider() instanceof DefaultConnectionProvider && DbConnectionManager.getConnectionProvider().isPooled();
    }
}
