package org.igniterealtime.openfire.plugin.threaddump.evaluator;

import java.time.Duration;

public interface Evaluator
{
    Duration getInterval();
    boolean shouldCreateThreadDump();
}
