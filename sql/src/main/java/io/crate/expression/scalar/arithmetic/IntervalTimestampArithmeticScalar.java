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

package io.crate.expression.scalar.arithmetic;

import io.crate.data.Input;
import io.crate.expression.scalar.ScalarFunctionModule;
import io.crate.metadata.FunctionIdent;
import io.crate.metadata.FunctionInfo;
import io.crate.metadata.Scalar;
import io.crate.metadata.TransactionContext;
import io.crate.metadata.functions.Signature;
import io.crate.types.DataType;
import io.crate.types.DataTypes;
import io.crate.types.IntervalType;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiFunction;

public class IntervalTimestampArithmeticScalar extends Scalar<Long, Object> implements BiFunction<Long, Period, Long> {

    public static void register(ScalarFunctionModule module) {
        for (var timestampType : List.of(DataTypes.TIMESTAMP, DataTypes.TIMESTAMPZ)) {
            module.register(
                Signature.scalar(
                    ArithmeticFunctions.Names.ADD,
                    DataTypes.INTERVAL.getTypeSignature(),
                    timestampType.getTypeSignature(),
                    timestampType.getTypeSignature()
                ).withForbiddenCoercion(),
                (signature, args) ->
                    new IntervalTimestampArithmeticScalar(
                        "+",
                        ArithmeticFunctions.Names.ADD,
                        args,
                        args.get(1),
                        signature
                    )
            );
            module.register(
                signatureFor(timestampType, ArithmeticFunctions.Names.ADD),
                (signature, args) ->
                    new IntervalTimestampArithmeticScalar(
                        "+",
                        ArithmeticFunctions.Names.ADD,
                        args,
                        args.get(0),
                        signature
                    )
            );

            module.register(
                signatureFor(timestampType, ArithmeticFunctions.Names.SUBTRACT),
                (signature, args) ->
                    new IntervalTimestampArithmeticScalar(
                        "-",
                        ArithmeticFunctions.Names.SUBTRACT,
                        args,
                        args.get(0),
                        signature
                    )
            );
        }
    }

    public static Signature signatureFor(DataType<?> timestampType, String name) {
        return Signature.scalar(
            name,
            timestampType.getTypeSignature(),
            DataTypes.INTERVAL.getTypeSignature(),
            timestampType.getTypeSignature()
        ).withForbiddenCoercion();
    }

    private final BiFunction<DateTime, Period, DateTime> operation;
    private final FunctionInfo info;
    private final Signature signature;
    private final int periodIdx;
    private final int timestampIdx;

    public IntervalTimestampArithmeticScalar(String operator,
                                             String name,
                                             List<DataType> argTypes,
                                             DataType<?> returnType,
                                             Signature signature) {
        this.info = new FunctionInfo(new FunctionIdent(name, argTypes), returnType);
        this.signature = signature;
        var firstArgType = argTypes.get(0);
        if (firstArgType.id() == IntervalType.ID) {
            periodIdx = 0;
            timestampIdx = 1;
        } else {
            periodIdx = 1;
            timestampIdx = 0;
        }

        switch (operator) {
            case "+":
                operation = DateTime::plus;
                break;
            case "-":
                if (firstArgType.id() == IntervalType.ID) {
                    throw new IllegalArgumentException("Unsupported operator for interval " + operator);
                }
                operation = DateTime::minus;
                break;
            default:
                operation = (a, b) -> {
                    throw new IllegalArgumentException("Unsupported operator for interval " + operator);
                };
        }
    }

    @Override
    public FunctionInfo info() {
        return this.info;
    }

    @Nullable
    @Override
    public Signature signature() {
        return signature;
    }

    @Override
    public Long evaluate(TransactionContext txnCtx, Input<Object>[] args) {
        final Long timestamp = (Long) args[timestampIdx].value();
        final Period period = (Period) args[periodIdx].value();
        return apply(timestamp, period);
    }

    @Override
    public Long apply(Long timestamp, Period period) {
        if (period == null || timestamp == null) {
            return null;
        }
        return operation.apply(new DateTime(timestamp, DateTimeZone.UTC), period).toInstant().getMillis();
    }
}
