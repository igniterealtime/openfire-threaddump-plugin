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
import org.jivesoftware.openfire.XMPPServer;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.management.LockInfo;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Formats a ThreadDump in a way that resembles, but is not a exact match,
 * of the standard (Oracle) thread dump format.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class DefaultThreadDumpFormatter implements ThreadDumpFormatter
{
    private Writer writer;

    @Override
    public String format( final ThreadDump threadDump )
    {
        try
        {
            writer = new StringWriter()
            {

                @Override
                public void write( String str )
                {
                    super.write( str + System.lineSeparator() );
                }
            };

            String ofVersion;
            try
            {
                ofVersion = XMPPServer.getInstance().getServerInfo().getVersion().toString();
            }
            catch ( Exception e )
            {
                ofVersion = "UNKOWN"; // happens during unit testing.
            }
            final String jvmVendor = System.getProperty( "java.vendor" );
            final String jvmVersion = System.getProperty( "java.version" );
            final String osArch = System.getProperty( "os.arch" );
            final String osName = System.getProperty( "os.name" );
            final String osVersion = System.getProperty( "os.version" );

            writer.write( new SimpleDateFormat( "yyyy-MM-dd HH:mm:ssZ" ).format( new Date( System.currentTimeMillis() ) ) );
            writer.write( "Full Java thread dump, Openfire " + ofVersion + ", Java " + jvmVersion + " " + jvmVendor + ", " + osName + " " + osArch + " " + osVersion );
            writer.write( "" );

            for ( final ThreadDump.Trace trace : threadDump.getTraces() )
            {
                printThreadInfo( trace );
                printLockedSynchronizers( trace.getThreadInfo().getLockedSynchronizers() );
            }
            writer.write( "" );
            printDeadlockInfo( threadDump );
            return writer.toString();
        }
        catch ( Exception e )
        {
            throw new IllegalStateException( "Unable to format thread dump.", e );
        }
    }

    private void printThreadInfo( ThreadDump.Trace trace ) throws IOException
    {
        // print thread information
        printThread( trace );

        // print stack trace with locks
        StackTraceElement[] stacktrace = trace.getThreadInfo().getStackTrace();
        MonitorInfo[] monitors = trace.getThreadInfo().getLockedMonitors();
        for ( int i = 0; i < stacktrace.length; i++ )
        {
            StackTraceElement ste = stacktrace[i];
            writer.write( "\tat " + ste.toString() );
            for ( MonitorInfo mi : monitors )
            {
                if ( mi.getLockedStackDepth() == i )
                {
                    writer.write( "\t- locked " + mi );
                }
            }
        }
        writer.write( "" );
    }

    private void printThread( ThreadDump.Trace trace ) throws IOException
    {
        // Section descriptions taken from https://dzone.com/articles/how-to-read-a-thread-dump.

        final StringBuilder sb = new StringBuilder();

        // section 'name'
        // Human-readable name of the thread. This name can be set by calling the setName method on a Threadobject and be obtained by calling getName on the object.
        sb.append( '"' ).append( trace.getThreadName() ).append( '"' );

        // section 'ID'
        // A unique ID associated with each Thread object. This number is generated, starting at 1, for all threads in the system. Each time a Thread object is created, the sequence number is incremented and then assigned to the newly
        // created Thread. This ID is read-only and can be obtained by calling getId on a Thread object.
        sb.append( " #" ).append( trace.getThreadId() );

        // Section 'Daemon status'
        // A tag denoting if the thread is a daemon thread. If the thread is a daemon, this tag will be present; if the thread is a non-daemon thread, no tag will be present. For example, Thread-0 is not a daemon thread and therefore has no
        // associated daemon tag in its summary: Thread-0" #12 prio=5....
        if ( trace.isThreadDeamon().isPresent() && trace.isThreadDeamon().get() )
        {
            sb.append( " daemon" );
        }

        // Section 'priority'
        // The numeric priority of the Java thread. Note that this does not necessarily correspond to the priority of the OS thread to with the Java thread is dispatched. The priority of a Thread object can be set using the setPriority
        // method and obtained using the getPriority method.
        if ( trace.threadPriority().isPresent() )
        {
            sb.append( " prio=" ).append( trace.threadPriority().getAsInt() );
        }

        // Section 'OS Thread Priority'
        // The OS thread priority. This priority can differ from the Java thread priority and corresponds to the OS thread on which the Java thread is dispatched.
        // TODO: sb.append( " os_prio=" ).append( );

        // Section 'Address'
        // The address of the Java thread. This address represents the pointer address of the Java Native Interface (JNI) native Thread object (the C++ Thread object that backs the Java thread through the JNI). This value is obtained by
        // converting the pointer to this (of the C++ object that backs the Java Thread object) to an integer on line 879 of hotspot/share/runtime/thread.cpp:
        //
        //    st->print("tid=" INTPTR_FORMAT " ", p2i(this));
        //
        // Although the key for this item (tid) may appear to be the thread ID, it is actually the address of the underlying JNI C++ Thread object and thus is not the ID returned when calling getId on a Java Thread object.
        // TODO: sb.append( " tid=" ).append( "");

        // Section 'OS Thread ID'
        // The unique ID of the OS thread to which the Java Thread is mapped. This value is printed on line 42 of hotspot/share/runtime/osThread.cpp:
        //
        //    st->print("nid=0x%x ", thread_id());
        // TODO: sb.append( "nid=" ).append( );

        // Section 'Status'
        // A human-readable string depicting the current status of the thread. This string provides supplementary information beyond the basic thread state (see below) and can be useful in discovering the intended actions of a thread (i.e.
        // was the thread trying to acquire a lock or waiting on a condition when it blocked).
        final ThreadInfo ti = trace.getThreadInfo();
        if ( ti.getLockName() != null )
        {
            sb.append( " waiting on lock" );
        }
        if ( trace.getThreadInfo().isSuspended() )
        {
            sb.append( " suspended" );
        }
        if ( trace.getThreadInfo().isInNative() )
        {
            sb.append( " running in native" );
        }

        // Section 'Last Known Java Stack Pointer'
        // The last known Stack Pointer (SP) for the stack associated with the thread. This value is supplied using native C++ code and is interlaced with the Java Thread class using the JNI. This value is obtained using the last_Java_sp()
        // native method and is formatted into the thread dump on line 2886 of hotspot/share/runtime/thread.cpp:
        //
        //    st->print_cr("[" INTPTR_FORMAT "]",
        //        (intptr_t)last_Java_sp() & ~right_n_bits(12));
        //
        // For simple thread dumps, this information may not be useful, but for more complex diagnostics, this SP value can be used to trace lock acquisition through a program.
        // TODO: sb.append( '[' ).append( ).append( ']' );

        // The second line represents the current state of the thread. The possible states for a thread are captured in the Thread.State enumeration:
        sb.append( System.lineSeparator() );
        sb.append( "   java.lang.Thread.State: " ).append( trace.getThreadState() );

        final LockInfo lockInfo = trace.getThreadInfo().getLockInfo();
        if ( lockInfo != null )
        {
            sb.append( System.lineSeparator() );
            switch ( trace.getThreadState() )
            {
                case BLOCKED:
                    sb.append( "\t- blocked on " ).append( lockInfo );
                    break;
                case WAITING:
                case TIMED_WAITING:
                    sb.append( "\t- waiting on " ).append( lockInfo );
                    break;
                default:
                    sb.append( "\t- " ).append( trace.getThreadState().toString().toLowerCase() ).append( " on " ).append( lockInfo );
            }
            if ( ti.getLockOwnerName() != null )
            {
                sb.append( " (owned by " ).append( ti.getLockOwnerName() ).append( " id=" ).append( ti.getLockOwnerId() ).append( ")" );
            }
        }

        writer.write( sb.toString() );
    }

    private void printLockedSynchronizers( LockInfo[] locks ) throws IOException
    {
        if ( locks != null && locks.length > 0 )
        {
            writer.write( "   Locked ownable synchronizers:" );
            for ( LockInfo li : locks )
            {
                writer.write( "     - " + li );
            }
            writer.write( "" );
        }
    }


    private void printDeadlockInfo( ThreadDump threadDump ) throws IOException
    {
        if ( threadDump.getDeadLockedThreadIDs() != null && threadDump.getDeadLockedThreadIDs().length != 0 )
        {
            writer.write( "Deadlock detected!" );
            for ( final long deadLockedThreadID : threadDump.getDeadLockedThreadIDs() )
            {
                StringBuilder sb = new StringBuilder();
                final String threadName = threadDump.getTraces().stream().filter( t -> t.getThreadId() == deadLockedThreadID ).findAny().get().getThreadName();
                final MonitorInfo[] lockedMonitors = threadDump.getTraces().stream().filter( t -> t.getThreadId() == deadLockedThreadID ).findAny().map( t -> t.getThreadInfo().getLockedMonitors() ).get();
                final LockInfo[] lockedSynchronizers = threadDump.getTraces().stream().filter( t -> t.getThreadId() == deadLockedThreadID ).findAny().map( t -> t.getThreadInfo().getLockedSynchronizers() ).get();
                final String waitName = threadDump.getTraces().stream().filter( t -> t.getThreadId() == deadLockedThreadID ).findAny().map( t -> t.getThreadInfo().getLockName() ).get();
                final String waitOwnerName = threadDump.getTraces().stream().filter( t -> t.getThreadId() == deadLockedThreadID ).findAny().map( t -> t.getThreadInfo().getLockOwnerName() ).get();
                final long waitOwnerId = threadDump.getTraces().stream().filter( t -> t.getThreadId() == deadLockedThreadID ).findAny().map( t -> t.getThreadInfo().getLockOwnerId() ).get();

                sb.append( " - #" ).append( deadLockedThreadID ).append( " (" ).append( threadName ).append( ") locked" );
                for ( final MonitorInfo lock : lockedMonitors )
                {
                    sb.append( " " ).append( lock );
                }
                for ( final LockInfo lock : lockedSynchronizers )
                {
                    sb.append( " " ).append( lock );
                }
                sb.append( ", waiting for " ).append( waitName ).append( " owned by #" ).append( waitOwnerId ).append( " (" ).append( waitOwnerName ).append( ")" );
                writer.write( sb.toString() );
            }
        }
        else
        {
            writer.write( "No deadlock detected." );
        }
    }
}
