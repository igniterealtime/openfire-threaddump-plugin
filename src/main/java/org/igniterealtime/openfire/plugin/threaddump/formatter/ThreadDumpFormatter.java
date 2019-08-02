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

/**
 * Formats a ThreadDump.
 *
 * Implementations cannot be expected to be thread safe, unless explicitly
 * defined.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 */
public interface ThreadDumpFormatter
{
    String format( ThreadDump threadDump );
}
