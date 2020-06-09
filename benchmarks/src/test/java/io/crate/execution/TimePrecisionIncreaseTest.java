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

package io.crate.execution;

import io.crate.metadata.TransactionContext;
import io.crate.metadata.settings.SessionSettings;
import org.joda.time.DateTimeUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Timeout;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.TimeUnit;


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1)
@Measurement(iterations = 4)
@Timeout(time=30000, timeUnit = TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 10000, timeUnit = TimeUnit.MILLISECONDS)
public class TimePrecisionIncreaseTest {

    static abstract class TCtxBase extends TransactionContext {

        TCtxBase() {
            super(null);
        }

        @Override
        public long currentTimeMicros() {
            throw new UnsupportedOperationException();
        }

        @Override
        public SessionSettings sessionSettings() {
            throw new UnsupportedOperationException();
        }
    }

    static class TCtxOrig extends TCtxBase {
        @Override
        public long currentTimeMillis() {
            return DateTimeUtils.currentTimeMillis();
        }
    }

    static class TCtxNextGen extends TCtxBase {
        @Override
        public long currentTimeMillis() {
            Instant i = Clock.systemUTC().instant();
            return (i.getEpochSecond() * 1000_000_000L + i.getNano()) / 1000_000L;
        }
    }


    private TransactionContext ctxOriginal;
    private TransactionContext ctxNextGen;

    @Setup
    public void setup() {
        ctxOriginal = new TCtxOrig();
        ctxNextGen = new TCtxNextGen();
    }

    @Benchmark
    public void currentTimeMillisOriginal(Blackhole blackhole) {
        blackhole.consume(ctxOriginal.currentTimeMillis());
    }

    @Benchmark
    public void currentTimeMillisNextGen(Blackhole blackhole) {
        blackhole.consume(ctxNextGen.currentTimeMillis());
    }

    public static void main(String[] args) throws Exception {

        // Benchmark                                            Mode  Cnt  Score   Error  Units
        // TimeIncreasePrecisionTest.currentTimeMillisNextGen   avgt    4  0.039 ± 0.001  us/op
        // TimeIncreasePrecisionTest.currentTimeMillisOriginal  avgt    4  0.028 ± 0.001  us/op
        new Runner(
            new OptionsBuilder()
                .include(TimePrecisionIncreaseTest.class.getSimpleName())
                .build())
            .run();
    }
}
