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

import com.sk89q.worldedit.antlr.ExpressionLexer;
import com.sk89q.worldedit.antlr.ExpressionParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

import java.util.List;
import javax.annotation.Nullable;

public class ExpressionHelper {

    public static void check(boolean condition, ParserRuleContext ctx, String message) {
        if (!condition) {
            throw evalException(ctx, message);
        }
    }

    public static void check(boolean condition, int positionInLine, String message) {
        if (!condition) {
            throw evalException(positionInLine, message);
        }
    }

    public static int getErrorPosition(Token token) {
        return token.getCharPositionInLine();
    }

    public static EvaluationException evalException(ParserRuleContext ctx, String message) {
        return evalException(ctx.start, message);
    }

    public static EvaluationException evalException(Token token, String message) {
        return evalException(getErrorPosition(token), message);
    }

    public static EvaluationException evalException(int positionInLine, String message) {
        return new EvaluationException(positionInLine, message);
    }

    public static void checkIterations(int iterations, int positionInLine) {
        check(iterations <= 256, positionInLine, "Loop exceeded 256 iterations");
    }

    public static Functions.Entry resolve(ExpressionParser.FunctionCallContext ctx) {
        String name = ctx.name.getText();
        List<Functions.Entry> candidates = Functions.lookup(name);
        check(!candidates.isEmpty(), ctx, "Unknown function '" + name + "'");

        int count = ctx.args.size();

        for (Functions.Entry candidate : candidates) {
            if (!candidate.isVarargs() && candidate.arity() != count) {
                continue;
            }

            if (!candidate.isVarargs()) {
                for (int i = 0; i < count; i++) {
                    if (candidate.kind(i) == Functions.Kind.VARIABLE) {
                        check(resolveId(ctx.args.get(i)) != null, ctx.args.get(i), "Function '" + name + "' requires a variable in parameter " + i);
                    }
                }
            }

            return candidate;
        }

        StringBuilder expected = new StringBuilder();
        for (Functions.Entry candidate : candidates) {
            if (expected.length() > 0) {
                expected.append('/');
            }
            expected.append(candidate.isVarargs() ? "0+" : String.valueOf(candidate.arity()));
        }

        throw evalException(ctx, "Incorrect number of arguments for function '" + name + "', expected " + expected + ", got " + count);
    }

    @Nullable
    public static String resolveId(ParserRuleContext arg) {
        ExpressionParser.WrappedExprContext wrapped = unwrap(arg);
        if (wrapped != null) {
            return resolveId(wrapped.expression());
        }

        Token token = arg.start;
        boolean isId = arg.start == arg.stop && token.getType() == ExpressionLexer.ID;
        return isId ? token.getText() : null;
    }

    @Nullable
    private static ExpressionParser.WrappedExprContext unwrap(ParserRuleContext ctx) {
        if (ctx instanceof ExpressionParser.WrappedExprContext wrapped) {
            return wrapped;
        }
        if (ctx.children == null || ctx.children.size() != 1) {
            return null;
        }

        List<ParserRuleContext> contexts = ctx.getRuleContexts(ParserRuleContext.class);
        if (contexts.size() != 1) {
            return null;
        }

        return unwrap(contexts.get(0));
    }

    private ExpressionHelper() {
    }
}
