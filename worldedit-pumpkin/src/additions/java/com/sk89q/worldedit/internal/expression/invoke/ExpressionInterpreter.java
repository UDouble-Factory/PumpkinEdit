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

package com.sk89q.worldedit.internal.expression.invoke;

import com.sk89q.worldedit.antlr.ExpressionBaseVisitor;
import com.sk89q.worldedit.antlr.ExpressionLexer;
import com.sk89q.worldedit.antlr.ExpressionParser;
import com.sk89q.worldedit.internal.expression.ExecutionData;
import com.sk89q.worldedit.internal.expression.ExpressionHelper;
import com.sk89q.worldedit.internal.expression.Functions;
import com.sk89q.worldedit.internal.expression.LocalSlot;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

final class ExpressionInterpreter extends ExpressionBaseVisitor<Object> {
    private static final Object NONE = new Object();
    private static final double[] FACTORIALS = new double[171];

    static {
        FACTORIALS[0] = 1;
        for (int i = 1; i < FACTORIALS.length; ++i) {
            FACTORIALS[i] = FACTORIALS[i - 1] * i;
        }
    }

    private final ExecutionData data;

    ExpressionInterpreter(ExecutionData data) {
        this.data = data;
    }

    Double value(ParseTree tree) {
        Object result = tree.accept(this);
        return result == NONE ? null : (Double) result;
    }

    private double require(ParserRuleContext ctx, String name) {
        Double result = value(ctx);
        if (result == null) {
            throw ExpressionHelper.evalException(ctx, "Invalid expression for " + name);
        }

        return result;
    }

    private double require(ParserRuleContext ctx) {
        return require(ctx, "a value");
    }

    private boolean bool(ParserRuleContext ctx) {
        return require(ctx, "a boolean") != 0;
    }

    private static double fromBool(boolean value) {
        return value ? 1 : 0;
    }

    private LocalSlot.Variable variable(Token token) {
        return variable(token, token.getText());
    }

    private LocalSlot.Variable variable(Token token, String name) {
        LocalSlot slot = data.slots().getSlot(name).orElseThrow(() -> ExpressionHelper.evalException(token, "'" + name + "' is not initialized yet"));
        if (!(slot instanceof LocalSlot.Variable result)) {
            throw ExpressionHelper.evalException(token, "'" + name + "' is not a variable");
        }

        return result;
    }

    private LocalSlot.Variable initVariable(Token token) {
        String name = token.getText();
        return data.slots().initVariable(name).orElseThrow(() -> ExpressionHelper.evalException(token, "Cannot overwrite non-variable '" + name + "'"));
    }

    @Override
    protected Object defaultResult() {
        return NONE;
    }

    @Override
    public Object visitChildren(RuleNode node) {
        Object result = NONE;
        int count = node.getChildCount();

        for (int i = 0; i < count; i++) {
            ParseTree child = node.getChild(i);
            if (child instanceof TerminalNode terminal) {
                if (terminal.getSymbol().getType() == Token.EOF) {
                    break;
                }
                continue;
            }

            Object childResult = child.accept(this);
            if (childResult != NONE) {
                result = childResult;
            }
        }

        return result;
    }

    private Double conditional(ParserRuleContext condition, ParserRuleContext trueBranch, ParserRuleContext falseBranch) {
        ParserRuleContext branch = bool(condition) ? trueBranch : falseBranch;
        return branch == null ? null : value(branch);
    }

    @Override
    public Object visitIfStatement(ExpressionParser.IfStatementContext ctx) {
        return conditional(ctx.condition, ctx.trueBranch, ctx.falseBranch);
    }

    @Override
    public Object visitTernaryExpr(ExpressionParser.TernaryExprContext ctx) {
        return conditional(ctx.condition, ctx.trueBranch, ctx.falseBranch);
    }

    private void beforeIteration(int iterations, ParserRuleContext body) {
        ExpressionHelper.checkIterations(iterations, body.start.getCharPositionInLine());
        data.checkDeadline();
    }

    @Override
    public Object visitWhileStatement(ExpressionParser.WhileStatementContext ctx) {
        Double result = null;
        int iterations = 0;

        while (bool(ctx.condition)) {
            beforeIteration(iterations++, ctx.body);

            try {
                result = value(ctx.body);
            } catch (BreakException e) {
                if (!e.doContinue) {
                    break;
                }
            }
        }

        return result;
    }

    @Override
    public Object visitDoStatement(ExpressionParser.DoStatementContext ctx) {
        Double result = null;
        int iterations = 0;

        do {
            beforeIteration(iterations++, ctx.body);

            try {
                result = value(ctx.body);
            } catch (BreakException e) {
                if (!e.doContinue) {
                    break;
                }
            }
        } while (bool(ctx.condition));

        return result;
    }

    @Override
    public Object visitForStatement(ExpressionParser.ForStatementContext ctx) {
        Double result = null;
        int iterations = 0;
        value(ctx.init);

        while (bool(ctx.condition)) {
            beforeIteration(iterations++, ctx.body);

            try {
                result = value(ctx.body);
            } catch (BreakException e) {
                if (!e.doContinue) {
                    break;
                }
            }

            value(ctx.update);
        }

        return result;
    }

    @Override
    public Object visitSimpleForStatement(ExpressionParser.SimpleForStatementContext ctx) {
        Double result = null;
        int iterations = 0;
        double first = require(ctx.first);
        double last = require(ctx.last);
        LocalSlot.Variable counter = initVariable(ctx.counter);

        for (double i = first; i <= last; i++) {
            beforeIteration(iterations++, ctx.body);
            counter.setValue(i);

            try {
                result = value(ctx.body);
            } catch (BreakException e) {
                if (!e.doContinue) {
                    break;
                }
            }
        }

        return result;
    }

    @Override
    public Object visitBreakStatement(ExpressionParser.BreakStatementContext ctx) {
        throw BreakException.BREAK;
    }

    @Override
    public Object visitContinueStatement(ExpressionParser.ContinueStatementContext ctx) {
        throw BreakException.CONTINUE;
    }

    @Override
    public Object visitReturnStatement(ExpressionParser.ReturnStatementContext ctx) {
        throw new ReturnException(value(ctx.value));
    }

    @Override
    public Object visitSwitchStatement(ExpressionParser.SwitchStatementContext ctx) {
        List<Double> keys = new ArrayList<>();
        List<ExpressionParser.StatementsContext> bodies = new ArrayList<>();
        ExpressionParser.StatementsContext defaultCase = null;

        for (int i = 0; i < ctx.labels.size(); i++) {
            ExpressionParser.SwitchLabelContext label = ctx.labels.get(i);
            ExpressionParser.StatementsContext body = ctx.bodies.get(i);

            if (label instanceof ExpressionParser.CaseContext caseContext) {
                double key = Double.parseDouble(caseContext.constant.getText());
                ExpressionHelper.check(!keys.contains(key), body, "Duplicate cases detected.");
                keys.add(key);
                bodies.add(body);
            } else {
                ExpressionHelper.check(defaultCase == null, body, "Duplicate default cases detected.");
                defaultCase = body;
            }
        }

        double target = require(ctx.target);
        boolean matched = false;
        boolean falling = false;
        Double evaluated = null;

        for (int i = 0; i < keys.size(); i++) {
            if (falling || keys.get(i) == target) {
                matched = true;

                try {
                    evaluated = value(bodies.get(i));
                    falling = true;
                } catch (BreakException e) {
                    ExpressionHelper.check(!e.doContinue, bodies.get(i).start.getCharPositionInLine(), "Cannot continue in a switch");
                    falling = false;
                    break;
                }
            }
        }

        if ((falling || !matched) && defaultCase != null) {
            try {
                evaluated = value(defaultCase);
            } catch (BreakException e) {
                ExpressionHelper.check(!e.doContinue, defaultCase.start.getCharPositionInLine(), "Cannot continue in a switch");
            }
        }

        return evaluated;
    }

    @Override
    public Object visitExpressionStatement(ExpressionParser.ExpressionStatementContext ctx) {
        return value(ctx.expression());
    }

    @Override
    public Object visitPostCrementExpr(ExpressionParser.PostCrementExprContext ctx) {
        LocalSlot.Variable target = variable(ctx.target);
        double previous = target.value();

        target.setValue(ctx.op.getType() == ExpressionLexer.INCREMENT ? previous + 1 : previous - 1);
        return previous;
    }

    @Override
    public Object visitPreCrementExpr(ExpressionParser.PreCrementExprContext ctx) {
        LocalSlot.Variable target = variable(ctx.target);
        double updated = ctx.op.getType() == ExpressionLexer.INCREMENT ? target.value() + 1 : target.value() - 1;

        target.setValue(updated);
        return updated;
    }

    @Override
    public Object visitPlusMinusExpr(ExpressionParser.PlusMinusExprContext ctx) {
        double operand = require(ctx.expr);

        return switch (ctx.op.getType()) {
            case ExpressionLexer.PLUS -> operand;
            case ExpressionLexer.MINUS -> -operand;
            default -> throw ExpressionHelper.evalException(ctx, "Invalid text for plus/minus expr: " + ctx.op.getText());
        };
    }

    @Override
    public Object visitNotExpr(ExpressionParser.NotExprContext ctx) {
        return fromBool(!bool(ctx.expr));
    }

    @Override
    public Object visitComplementExpr(ExpressionParser.ComplementExprContext ctx) {
        return (double) ~(long) require(ctx.expr);
    }

    @Override
    public Object visitConditionalAndExpr(ExpressionParser.ConditionalAndExprContext ctx) {
        if (!bool(ctx.left)) {
            return fromBool(false);
        }

        return require(ctx.right);
    }

    @Override
    public Object visitConditionalOrExpr(ExpressionParser.ConditionalOrExprContext ctx) {
        double left = require(ctx.left);
        if (left != 0) {
            return left;
        }

        return require(ctx.right);
    }

    @Override
    public Object visitPowerExpr(ExpressionParser.PowerExprContext ctx) {
        return Math.pow(require(ctx.left), require(ctx.right));
    }

    @Override
    public Object visitMultiplicativeExpr(ExpressionParser.MultiplicativeExprContext ctx) {
        double left = require(ctx.left);
        double right = require(ctx.right);

        return switch (ctx.op.getType()) {
            case ExpressionLexer.TIMES -> left * right;
            case ExpressionLexer.DIVIDE -> left / right;
            case ExpressionLexer.REMAINDER -> left % right;
            default -> throw ExpressionHelper.evalException(ctx, "Invalid text for multiplicative expr: " + ctx.op.getText());
        };
    }

    @Override
    public Object visitAddExpr(ExpressionParser.AddExprContext ctx) {
        double left = require(ctx.left);
        double right = require(ctx.right);

        return switch (ctx.op.getType()) {
            case ExpressionLexer.PLUS -> left + right;
            case ExpressionLexer.MINUS -> left - right;
            default -> throw ExpressionHelper.evalException(ctx, "Invalid text for additive expr: " + ctx.op.getText());
        };
    }

    @Override
    public Object visitShiftExpr(ExpressionParser.ShiftExprContext ctx) {
        double left = require(ctx.left);
        double right = require(ctx.right);

        return switch (ctx.op.getType()) {
            case ExpressionLexer.LEFT_SHIFT -> (double) ((long) left << (long) right);
            case ExpressionLexer.RIGHT_SHIFT -> (double) ((long) left >> (long) right);
            default -> throw ExpressionHelper.evalException(ctx, "Invalid text for shift expr: " + ctx.op.getText());
        };
    }

    @Override
    public Object visitRelationalExpr(ExpressionParser.RelationalExprContext ctx) {
        double left = require(ctx.left);
        double right = require(ctx.right);

        return switch (ctx.op.getType()) {
            case ExpressionLexer.LESS_THAN -> fromBool(left < right);
            case ExpressionLexer.LESS_THAN_OR_EQUAL -> fromBool(left <= right);
            case ExpressionLexer.GREATER_THAN -> fromBool(left > right);
            case ExpressionLexer.GREATER_THAN_OR_EQUAL -> fromBool(left >= right);
            default -> throw ExpressionHelper.evalException(ctx, "Invalid text for relational expr: " + ctx.op.getText());
        };
    }

    @Override
    public Object visitEqualityExpr(ExpressionParser.EqualityExprContext ctx) {
        double left = require(ctx.left);
        double right = require(ctx.right);

        return switch (ctx.op.getType()) {
            case ExpressionLexer.EQUAL -> fromBool(left == right);
            case ExpressionLexer.NOT_EQUAL -> fromBool(left != right);
            case ExpressionLexer.NEAR -> fromBool(almostEqual(left, right));
            default -> throw ExpressionHelper.evalException(ctx, "Invalid text for equality expr: " + ctx.op.getText());
        };
    }

    private static boolean almostEqual(double a, double b) {
        long aLong = Double.doubleToRawLongBits(a);
        if (aLong < 0) {
            aLong = 0x8000000000000000L - aLong;
        }

        long bLong = Double.doubleToRawLongBits(b);
        if (bLong < 0) {
            bLong = 0x8000000000000000L - bLong;
        }

        return Math.abs(aLong - bLong) <= 450359963L;
    }

    @Override
    public Object visitPostfixExpr(ExpressionParser.PostfixExprContext ctx) {
        double operand = require(ctx.expr);
        if (ctx.op.getType() != ExpressionLexer.EXCLAMATION_MARK) {
            throw ExpressionHelper.evalException(ctx, "Invalid text for post-unary expr: " + ctx.op.getText());
        }

        int n = (int) operand;
        if (n < 0) {
            return 0.0;
        }
        if (n >= FACTORIALS.length) {
            return Double.POSITIVE_INFINITY;
        }

        return FACTORIALS[n];
    }

    @Override
    public Object visitAssignment(ExpressionParser.AssignmentContext ctx) {
        int type = ctx.assignmentOperator().getStart().getType();
        double argument = require(ctx.expression());
        LocalSlot.Variable target;
        double updated;

        if (type == ExpressionLexer.ASSIGN) {
            target = initVariable(ctx.target);
            updated = argument;
        } else {
            target = variable(ctx.target);
            updated = switch (type) {
                case ExpressionLexer.POWER_ASSIGN -> Math.pow(target.value(), argument);
                case ExpressionLexer.TIMES_ASSIGN -> target.value() * argument;
                case ExpressionLexer.DIVIDE_ASSIGN -> target.value() / argument;
                case ExpressionLexer.REMAINDER_ASSIGN -> target.value() % argument;
                case ExpressionLexer.PLUS_ASSIGN -> target.value() + argument;
                case ExpressionLexer.MINUS_ASSIGN -> target.value() - argument;
                default -> throw ExpressionHelper.evalException(ctx, "Invalid text for assign expr: " + ctx.assignmentOperator().getText());
            };
        }

        target.setValue(updated);
        return updated;
    }

    @Override
    public Object visitFunctionCall(ExpressionParser.FunctionCallContext ctx) {
        Functions.Entry function = ExpressionHelper.resolve(ctx);
        Object[] arguments = new Object[ctx.args.size()];

        for (int i = 0; i < arguments.length; i++) {
            ExpressionParser.ExpressionContext argument = ctx.args.get(i);
            Functions.Kind kind = function.isVarargs() ? Functions.Kind.VALUE : function.kind(i);

            if (kind == Functions.Kind.VALUE) {
                arguments[i] = require(argument);
                continue;
            }

            String id = ExpressionHelper.resolveId(argument);
            if (id != null) {
                arguments[i] = variable(argument.start, id);
            } else {
                arguments[i] = new LocalSlot.Constant(require(argument));
            }
        }

        return function.invoke(data.functions(), arguments);
    }

    @Override
    public Object visitConstantExpression(ExpressionParser.ConstantExpressionContext ctx) {
        return Double.parseDouble(ctx.getText());
    }

    @Override
    public Object visitIdExpr(ExpressionParser.IdExprContext ctx) {
        Token source = ctx.source;
        return data.slots().getSlotValue(source.getText()).orElseThrow(() -> ExpressionHelper.evalException(source, "'" + source.getText() + "' is not initialized yet"));
    }
}
