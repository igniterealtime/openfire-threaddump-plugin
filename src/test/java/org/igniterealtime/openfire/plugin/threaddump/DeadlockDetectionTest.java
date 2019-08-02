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

import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit tests that check the deadlock-detection functionality of {@link ThreadDump}
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public class DeadlockDetectionTest
{
    /**
     * Verifies that a deadlock is detected.
     */
    @Test
    public void testDeadlockDetection() throws Exception
    {
        // Setup test fixture.
        final Deadlock dl = new Deadlock();

        // Execute system under test
        ThreadDump td;
        final Instant stop = Instant.now().plus( 30, ChronoUnit.SECONDS );
        do {
            td = ThreadDump.getInstance();
            // It can take a few moments for the deadlock to occur. Retry until we hit one.
        } while ( Instant.now().isBefore( stop ) && td.getDeadLockedThreadIDs() == null );

        // Verify results;
        assertNotNull( td.getDeadLockedThreadIDs() );
    }

    /**
     * Verifies that no  deadlock is detected when none is present.
     */
    @Test
    public void testNoDeadlockDetection() throws Exception
    {
        // Setup test fixture.

        // Execute system under test
        final ThreadDump td = ThreadDump.getInstance();

        // Verify results;
        assertNull( td.getDeadLockedThreadIDs() );
    }
}
