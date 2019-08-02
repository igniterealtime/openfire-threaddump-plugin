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

package org.igniterealtime.openfire.plugin.threaddump.formatter;

import org.igniterealtime.openfire.plugin.threaddump.ThreadDump;

import java.lang.management.ThreadInfo;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Formats a ThreadDump in a quick and dirty manner.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class SimpleFormatter implements ThreadDumpFormatter
{
    public static final String NEWLINE = System.lineSeparator();

    @Override
    public String format( final ThreadDump threadDump )
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "Dump of " + threadDump.getTraces().size() + " threads at "
                       + new SimpleDateFormat( "yyyy/MM/dd HH:mm:ss z" ).format( new Date( System.currentTimeMillis() ) )
                       + NEWLINE + NEWLINE );
        for ( final ThreadDump.Trace trace : threadDump.getTraces() )
        {
            sb.append( "\"" + trace.getThreadName() + "\"" );
            if ( trace.threadPriority().isPresent() )
            {
                sb.append( " prio=" ).append( trace.threadPriority().getAsInt() );
            }

            sb.append( " tid=" ).append( trace.getThreadId() );
            sb.append( " " ).append( trace.getThreadState() );

            if ( trace.isThreadDeamon().isPresent() )
            {
                sb.append( " " ).append( trace.isThreadDeamon().get() ? "daemon" : "worker" );
            }
            sb.append( NEWLINE );

            ThreadInfo threadInfo = trace.getThreadInfo();
            if ( threadInfo != null )
            {
                sb.append( "    native=" + threadInfo.isInNative() + ", suspended=" + threadInfo.isSuspended()
                               + ", block=" + threadInfo.getBlockedCount() + ", wait=" + threadInfo.getWaitedCount()
                               + NEWLINE );
                sb.append( "    lock="
                               + threadInfo.getLockName()
                               + " owned by "
                               + threadInfo.getLockOwnerName()
                               + " ("
                               + threadInfo.getLockOwnerId()
                               + "), cpu="
                               + trace.getThreadCpuTime()
                               + ", user="
                               + trace.getThreadUserTime()
                               + NEWLINE );
            }
            for ( StackTraceElement element : trace.getStack() )
            {
                sb.append( "    " );
                String eleStr = element.toString();
                sb.append( "    " );
                sb.append( eleStr );
                sb.append( NEWLINE );
            }
            sb.append( NEWLINE );
        }

        if ( threadDump.getDeadLockedThreadIDs() != null && threadDump.getDeadLockedThreadIDs().length != 0 )
        {
            sb.append( "Deadlock detected!" );
            sb.append( "Deadlocked threads: " );
            for ( final long deadLockedThreadID : threadDump.getDeadLockedThreadIDs() )
            {
                sb.append( " - " ).append( deadLockedThreadID );
            }
        }
        else
        {
            sb.append( "No deadlock detected." );
        }
        return sb.toString();
    }
}
