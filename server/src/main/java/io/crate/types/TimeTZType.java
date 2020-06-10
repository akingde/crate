/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.types;

import io.crate.Streamer;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

import static io.crate.types.TimeTZParser.timeTZOf;
import static io.crate.types.TimeTZParser.exceptionForInvalidLiteral;

public final class TimeTZType extends DataType<TimeTZ> implements FixedWidthType, Streamer<TimeTZ> {

    public static final int ID = 20;
    public static final int TYPE_LEN = 12;
    public static final String NAME = "time with time zone";
    public static final TimeTZType INSTANCE = new TimeTZType();


    @Override
    public int id() {
        return ID;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Precedence precedence() {
        return Precedence.TIMETZ;
    }

    @Override
    public Streamer<TimeTZ> streamer() {
        return this;
    }

    @Override
    public int compare(TimeTZ val1, TimeTZ val2) {
        return val1.compareTo(val2);
    }

    @Override
    public TimeTZ readValueFrom(StreamInput in) throws IOException {
        if (in.readBoolean()) {
            return null;
        }
        return new TimeTZ(in.readLong(), in.readInt());
    }

    @Override
    public void writeValueTo(StreamOutput out, TimeTZ tz) throws IOException {
        out.writeBoolean(tz == null);
        if (tz != null) {
            out.writeLong(tz.getMicrosFromMidnight());
            out.writeInt(tz.getSecondsFromUTC());
        }
    }

    @Override
    public int fixedSize() {
        return TYPE_LEN;
    }

    @Override
    public TimeTZ value(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof TimeTZ) {
            return (TimeTZ) value;
        }
        if (value instanceof String) {
            try {
                return TimeTZParser.parse((String) value);
            } catch (IllegalArgumentException e0) {
                try {
                    return timeTZOf(
                        TimeTZType.class.getSimpleName(),
                        Long.valueOf((String) value));
                } catch (NumberFormatException e1) {
                    throw exceptionForInvalidLiteral(value);
                }
            }
        }
        throw exceptionForInvalidLiteral(value);
    }
}
