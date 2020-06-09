/*
 * Licensed to Crate under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Crate licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial
 * agreement.
 */

package io.crate.metadata;

import io.crate.metadata.settings.SessionSettings;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class TransactionContext {

    public static TransactionContext of(SessionSettings sessionSettings) {
        return new TransactionContext(sessionSettings);
    }

    private static final AtomicReference<Supplier<Long>> MILLIS_PROVIDER = new AtomicReference<>(null);

    public static long getCurrentTimeMillis() {
        Supplier<Long> mp = MILLIS_PROVIDER.get();
        return mp != null ?  mp.get() : micros() / 1000L;
    }

    public static void setCurrentMillisFixed(long millis) {
        MILLIS_PROVIDER.set(() -> millis);
    }

    public static void setCurrentMillisSystem() {
        MILLIS_PROVIDER.set(null);
    }

    public static void setCurrentMillisProvider(Supplier<Long> provider) {
        MILLIS_PROVIDER.set(provider);
    }


    private final SessionSettings sessionSettings;
    private volatile long currentTimeMicros = -1;

    protected TransactionContext(SessionSettings sessionSettings) {
        this.sessionSettings = sessionSettings;
    }

    /**
     * @return current timestamp in ms.
     * Subsequent calls will always return the same value. (Not thread-safe)
     * Correlated with currentSystemUTCTime().
     */
    public long currentTimeMillis() {
        return currentTimeMicros() / 1000L;
    }

    /**
     * @return current time in the UTC time zone in microseconds.
     * Subsequent calls will always return the same value. (Not thread-safe)
     * Correlated with currentTimeMillis().
     */
    public long currentTimeMicros() {
        Supplier<Long> mp = MILLIS_PROVIDER.get();
        if (mp != null) {
            return mp.get() * 1000L;
        }
        // no synchronization because StmtCtx is mostly used during single-threaded analysis phase
        if (currentTimeMicros == -1) {
            currentTimeMicros = micros();
        }
        return currentTimeMicros;
    }

    private static long micros() {
        Instant i = Clock.systemUTC().instant();
        return (i.getEpochSecond() * 1000_000_000L + i.getNano()) / 1000L;
    }

    public SessionSettings sessionSettings() {
        return sessionSettings;
    }
}
