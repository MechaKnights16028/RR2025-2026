package org.firstinspires.ftc.teamcode.pedro.util;

import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.Scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Polling-based replacement for WPILib's Trigger / .whileTrue() / .onTrue().
 *
 * Ivy doesn't ship a button-binding utility, so we roll our own. Register
 * bindings once during RobotContainer construction; then PedroTeleOp calls
 * {@link #poll()} exactly once per loop iteration (right before
 * {@link Scheduler#execute()}). Each binding compares this tick's boolean
 * against the previous tick's and reacts to the edge.
 *
 * TEACHING NOTE: This file is small on purpose. It is the one class that
 * translates raw gamepad state into Ivy command scheduling — everything
 * upstream of it should never read a gamepad directly, and everything
 * downstream of it should never call Scheduler.schedule() directly.
 * That one-way data flow is what makes the rebinding table in
 * RobotContainer readable.
 */
public final class GamepadTriggers {

    /** Lazily supplied to allow for parameterized commands (e.g. PathToPoseCommand with a dynamic target). */
    public interface CommandFactory extends Supplier<Command> { }

    private final List<Binding> bindings = new ArrayList<>();

    /**
     * Run the command while the condition is true. Schedules on false→true
     * edge, cancels on true→false edge. If the command completes on its own
     * (e.g. a one-shot that returns done()==true), the binding does not
     * re-schedule it until the condition goes false and then true again.
     */
    public void whileTrue(BooleanSupplier condition, CommandFactory factory) {
        bindings.add(new WhileBinding(condition, factory));
    }

    /**
     * Schedule the command once on a false→true edge. Never cancels.
     */
    public void onTrue(BooleanSupplier condition, CommandFactory factory) {
        bindings.add(new OnTrueBinding(condition, factory));
    }

    /** Call once per OpMode loop, before Scheduler.execute(). */
    public void poll() {
        for (Binding b : bindings) b.poll();
    }

    /** Implementation details below. */

    private static abstract class Binding {
        final BooleanSupplier cond;
        final CommandFactory factory;
        boolean prev = false;
        Binding(BooleanSupplier cond, CommandFactory factory) {
            this.cond = cond;
            this.factory = factory;
        }
        abstract void poll();
    }

    private static final class WhileBinding extends Binding {
        Command active;
        WhileBinding(BooleanSupplier cond, CommandFactory factory) { super(cond, factory); }
        @Override void poll() {
            boolean now = cond.getAsBoolean();
            if (now && !prev) {
                active = factory.get();
                Scheduler.schedule(active);
            } else if (!now && prev && active != null) {
                Scheduler.cancel(active);
                active = null;
            }
            prev = now;
        }
    }

    private static final class OnTrueBinding extends Binding {
        OnTrueBinding(BooleanSupplier cond, CommandFactory factory) { super(cond, factory); }
        @Override void poll() {
            boolean now = cond.getAsBoolean();
            if (now && !prev) Scheduler.schedule(factory.get());
            prev = now;
        }
    }
}
