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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * A representation of a full thread dump.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class ThreadDump
{
    public static ThreadDump getInstance()
    {
        final ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();

        final ThreadInfo[] threadInfos = mxBean.dumpAllThreads( mxBean.isObjectMonitorUsageSupported(), mxBean.isSynchronizerUsageSupported() );
        final Map<Long, Thread> threads = Thread.getAllStackTraces().keySet().stream().collect( Collectors.toMap( Thread::getId, thread -> thread ) );

        final List<Trace> traces = Arrays.stream( threadInfos ).map( threadInfo -> {
            final long threadId = threadInfo.getThreadId();
            final OptionalLong cpuTime = mxBean.isThreadCpuTimeSupported() && mxBean.isThreadCpuTimeEnabled() ? OptionalLong.of( mxBean.getThreadCpuTime( threadId ) ) : OptionalLong.empty();
            final OptionalLong userTime = mxBean.isThreadCpuTimeSupported() && mxBean.isThreadCpuTimeEnabled() ? OptionalLong.of( mxBean.getThreadUserTime( threadId ) ) : OptionalLong.empty();
            final Thread thread = threads.get( threadId );
            final OptionalInt priority = thread != null ? OptionalInt.of( thread.getPriority() ) : OptionalInt.empty();
            final Optional<Boolean> isDaemon = thread != null ? Optional.of( thread.isDaemon() ) : Optional.empty();

            return new Trace( threadId, threadInfo, priority, isDaemon, cpuTime, userTime );
        } ).collect( Collectors.toList() );

        final long[] deadlockedThreadIDs;
        if ( mxBean.isSynchronizerUsageSupported() )
        {
            deadlockedThreadIDs = mxBean.findDeadlockedThreads();
        }
        else
        {
            deadlockedThreadIDs = null;
        }

        return new ThreadDump( traces, deadlockedThreadIDs );
    }

    private final List<Trace> traces;
    private final long[] deadLockedThreadIDs;

    private ThreadDump( final List<Trace> traces, final long[] deadLockedThreadIDs )
    {
        this.traces = traces;
        this.deadLockedThreadIDs = deadLockedThreadIDs;
    }

    public List<Trace> getTraces()
    {
        return new CopyOnWriteArrayList<>( traces ); // defensive copy.
    }

    public long[] getDeadLockedThreadIDs()
    {
        return deadLockedThreadIDs == null ? null : deadLockedThreadIDs.clone();
    }

    public static class Trace
    {
        private final long threadId;
        private final ThreadInfo threadInfo;
        private final OptionalInt threadPriority;
        private final Optional<Boolean> isThreadDeamon;
        private final OptionalLong cpuTime;
        private final OptionalLong userTime;

        private Trace( final long threadId, final ThreadInfo threadInfo, final OptionalInt threadPriority, final Optional<Boolean> isThreadDeamon, final OptionalLong cpuTime, final OptionalLong userTime )
        {
            this.threadId = threadId;
            this.threadInfo = threadInfo;
            this.threadPriority = threadPriority;
            this.isThreadDeamon = isThreadDeamon;
            this.cpuTime = cpuTime;
            this.userTime = userTime;
        }

        public long getThreadId()
        {
            return threadId;
        }

        public ThreadInfo getThreadInfo()
        {
            return threadInfo;
        }

        public StackTraceElement[] getStack()
        {
            return threadInfo.getStackTrace();
        }

        public String getThreadName()
        {
            return threadInfo.getThreadName();
        }

        public OptionalInt threadPriority()
        {
            // Java 9: return getThreadInfo().getPriority();
            return threadPriority;
        }

        public Optional<Boolean> isThreadDeamon()
        {
            // Java 9: return threadInfo.isDaemon();
            return isThreadDeamon;
        }

        public Thread.State getThreadState()
        {
            return threadInfo.getThreadState();
        }

        public OptionalLong getThreadCpuTime()
        {
            return cpuTime;
        }

        public OptionalLong getThreadUserTime()
        {
            return userTime;
        }
    }
}
