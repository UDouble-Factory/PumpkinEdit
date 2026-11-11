/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.internal.expression;

import com.google.common.primitives.Doubles;
import com.sk89q.worldedit.internal.expression.LocalSlot.Variable;
import com.sk89q.worldedit.math.Vector3;
import com.sk89q.worldedit.math.noise.PerlinNoise;
import com.sk89q.worldedit.math.noise.RidgedMultiFractalNoise;
import com.sk89q.worldedit.math.noise.VoronoiNoise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.DoubleUnaryOperator;

public final class Functions {

    public enum Kind {
        VALUE,
        VARIABLE,
        SLOT
    }

    public interface Invoker {
        double invoke(Functions functions, Object[] arguments);
    }

    public static final class Entry {
        private final int arity;
        private final Kind[] kinds;
        private final Invoker invoker;

        private Entry(int arity, Kind[] kinds, Invoker invoker) {
            this.arity = arity;
            this.kinds = kinds;
            this.invoker = invoker;
        }

        public boolean isVarargs() {
            return arity < 0;
        }

        public int arity() {
            return arity;
        }

        public Kind kind(int index) {
            return kinds.length == 0 ? Kind.VALUE : kinds[index];
        }

        public double invoke(Functions functions, Object[] arguments) {
            return invoker.invoke(functions, arguments);
        }
    }

    private static final Map<String, List<Entry>> TABLE = new HashMap<>();
    private static final Map<Integer, double[]> GLOBAL_MEGA_BUFFER = new HashMap<>();
    private static final Random RANDOM = new Random();

    private final Map<Integer, double[]> megaBuffer = new HashMap<>();
    private ExpressionEnvironment environment;

    static Functions create() {
        return new Functions();
    }

    private Functions() {
    }

    private static void add(String name, int arity, Invoker invoker, Kind... kinds) {
        TABLE.computeIfAbsent(name, key -> new ArrayList<>()).add(new Entry(arity, kinds, invoker));
    }

    private static void unary(String name, DoubleUnaryOperator operator) {
        add(name, 1, (functions, arguments) -> operator.applyAsDouble(value(arguments, 0)));
    }

    private static double value(Object[] arguments, int index) {
        return (Double) arguments[index];
    }

    private static double[] values(Object[] arguments) {
        double[] result = new double[arguments.length];

        for (int i = 0; i < arguments.length; i++) {
            result[i] = (Double) arguments[i];
        }

        return result;
    }

    static {
        unary("sin", Math::sin);
        unary("cos", Math::cos);
        unary("tan", Math::tan);
        unary("asin", Math::asin);
        unary("acos", Math::acos);
        unary("atan", Math::atan);
        unary("sinh", Math::sinh);
        unary("cosh", Math::cosh);
        unary("tanh", Math::tanh);
        unary("sqrt", Math::sqrt);
        unary("cbrt", Math::cbrt);
        unary("abs", Math::abs);
        unary("ceil", Math::ceil);
        unary("floor", Math::floor);
        unary("rint", Math::rint);
        unary("exp", Math::exp);
        unary("log", Math::log);
        unary("log10", Math::log10);
        unary("ln", Math::log);
        unary("round", input -> (double) Math.round(input));

        add("atan2", 2, (functions, arguments) -> Math.atan2(value(arguments, 0), value(arguments, 1)));
        add("min", -1, (functions, arguments) -> Doubles.min(values(arguments)));
        add("max", -1, (functions, arguments) -> Doubles.max(values(arguments)));

        add("rotate", 3, (functions, arguments) -> rotate((Variable) arguments[0], (Variable) arguments[1], value(arguments, 2)), Kind.VARIABLE, Kind.VARIABLE, Kind.VALUE);
        add("swap", 2, (functions, arguments) -> swap((Variable) arguments[0], (Variable) arguments[1]), Kind.VARIABLE, Kind.VARIABLE);

        add("gmegabuf", 1, (functions, arguments) -> getBufferItem(GLOBAL_MEGA_BUFFER, (int) value(arguments, 0)));
        add("gmegabuf", 2, (functions, arguments) -> setBufferItem(GLOBAL_MEGA_BUFFER, (int) value(arguments, 0), value(arguments, 1)));
        add("megabuf", 1, (functions, arguments) -> getBufferItem(functions.megaBuffer, (int) value(arguments, 0)));
        add("megabuf", 2, (functions, arguments) -> setBufferItem(functions.megaBuffer, (int) value(arguments, 0), value(arguments, 1)));
        add("closest", 6, (functions, arguments) -> findClosest(functions.megaBuffer, arguments));
        add("gclosest", 6, (functions, arguments) -> findClosest(GLOBAL_MEGA_BUFFER, arguments));

        add("random", 0, (functions, arguments) -> RANDOM.nextDouble());
        add("randint", 1, (functions, arguments) -> RANDOM.nextInt((int) Math.floor(value(arguments, 0))));
        add("perlin", 7, (functions, arguments) -> perlin(values(arguments)));
        add("voronoi", 5, (functions, arguments) -> voronoi(values(arguments)));
        add("ridgedmulti", 6, (functions, arguments) -> ridgedmulti(values(arguments)));

        add("query", 5, (functions, arguments) -> functions.query(arguments, 0), Kind.VALUE, Kind.VALUE, Kind.VALUE, Kind.SLOT, Kind.SLOT);
        add("queryAbs", 5, (functions, arguments) -> functions.query(arguments, 1), Kind.VALUE, Kind.VALUE, Kind.VALUE, Kind.SLOT, Kind.SLOT);
        add("queryRel", 5, (functions, arguments) -> functions.query(arguments, 2), Kind.VALUE, Kind.VALUE, Kind.VALUE, Kind.SLOT, Kind.SLOT);
    }

    public static List<Entry> lookup(String name) {
        List<Entry> entries = TABLE.get(name);
        return entries == null ? List.of() : entries;
    }

    public ExpressionEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(ExpressionEnvironment environment) {
        this.environment = environment;
    }

    private static double rotate(Variable x, Variable y, double angle) {
        double cosF = Math.cos(angle);
        double sinF = Math.sin(angle);
        double xOld = x.value();
        double yOld = y.value();

        x.setValue(xOld * cosF - yOld * sinF);
        y.setValue(xOld * sinF + yOld * cosF);
        return 0.0;
    }

    private static double swap(Variable x, Variable y) {
        double tmp = x.value();

        x.setValue(y.value());
        y.setValue(tmp);
        return 0.0;
    }

    private static double[] getSubBuffer(Map<Integer, double[]> megabuf, int key) {
        return megabuf.computeIfAbsent(key, ignored -> new double[1024]);
    }

    private static double getBufferItem(Map<Integer, double[]> megabuf, int index) {
        return getSubBuffer(megabuf, index & ~1023)[index & 1023];
    }

    private static double setBufferItem(Map<Integer, double[]> megabuf, int index, double value) {
        getSubBuffer(megabuf, index & ~1023)[index & 1023] = value;
        return value;
    }

    private static double findClosest(Map<Integer, double[]> megabuf, Object[] arguments) {
        double x = value(arguments, 0);
        double y = value(arguments, 1);
        double z = value(arguments, 2);
        int index = (int) value(arguments, 3);
        int count = (int) value(arguments, 4);
        int stride = (int) value(arguments, 5);
        int closestIndex = -1;
        double minDistanceSquared = Double.MAX_VALUE;

        for (int i = 0; i < count; ++i) {
            double currentX = getBufferItem(megabuf, index) - x;
            double currentY = getBufferItem(megabuf, index + 1) - y;
            double currentZ = getBufferItem(megabuf, index + 2) - z;
            double currentDistanceSquared = currentX * currentX + currentY * currentY + currentZ * currentZ;

            if (currentDistanceSquared < minDistanceSquared) {
                minDistanceSquared = currentDistanceSquared;
                closestIndex = index;
            }

            index += stride;
        }

        return closestIndex;
    }

    private static double perlin(double[] arguments) {
        PerlinNoise perlin = new PerlinNoise();

        try {
            perlin.setSeed((int) arguments[0]);
            perlin.setFrequency(arguments[4]);
            perlin.setOctaveCount((int) arguments[5]);
            perlin.setPersistence(arguments[6]);
        } catch (IllegalArgumentException e) {
            throw new EvaluationException(0, "Perlin noise error: " + e.getMessage());
        }

        return perlin.noise(Vector3.at(arguments[1], arguments[2], arguments[3]));
    }

    private static double voronoi(double[] arguments) {
        VoronoiNoise voronoi = new VoronoiNoise();

        try {
            voronoi.setSeed((int) arguments[0]);
            voronoi.setFrequency(arguments[4]);
        } catch (IllegalArgumentException e) {
            throw new EvaluationException(0, "Voronoi error: " + e.getMessage());
        }

        return voronoi.noise(Vector3.at(arguments[1], arguments[2], arguments[3]));
    }

    private static double ridgedmulti(double[] arguments) {
        RidgedMultiFractalNoise ridgedMulti = new RidgedMultiFractalNoise();

        try {
            ridgedMulti.setSeed((int) arguments[0]);
            ridgedMulti.setFrequency(arguments[4]);
            ridgedMulti.setOctaveCount((int) arguments[5]);
        } catch (IllegalArgumentException e) {
            throw new EvaluationException(0, "Ridged multi error: " + e.getMessage());
        }

        return ridgedMulti.noise(Vector3.at(arguments[1], arguments[2], arguments[3]));
    }

    private double query(Object[] arguments, int mode) {
        double x = value(arguments, 0);
        double y = value(arguments, 1);
        double z = value(arguments, 2);
        LocalSlot type = (LocalSlot) arguments[3];
        LocalSlot data = (LocalSlot) arguments[4];
        double typeId;
        double dataValue;

        if (mode == 0) {
            typeId = environment.getBlockType(x, y, z);
            dataValue = environment.getBlockData(x, y, z);
        } else if (mode == 1) {
            typeId = environment.getBlockTypeAbs(x, y, z);
            dataValue = environment.getBlockDataAbs(x, y, z);
        } else {
            typeId = environment.getBlockTypeRel(x, y, z);
            dataValue = environment.getBlockDataRel(x, y, z);
        }

        double result = ((type.value() == -1 || typeId == type.value()) && (data.value() == -1 || dataValue == data.value())) ? 1.0 : 0.0;

        if (type instanceof Variable typeVar) {
            typeVar.setValue(typeId);
        }
        if (data instanceof Variable dataVar) {
            dataVar.setValue(dataValue);
        }

        return result;
    }
}
