package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.behaviors.BlockedBehavior;
import com.pedropathing.ivy.behaviors.ConflictBehavior;
import com.pedropathing.ivy.behaviors.EndCondition;
import com.pedropathing.ivy.behaviors.InterruptedBehavior;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Runs a Runnable every tick, stops hardware via another Runnable on
 * release/interrupt. Used by every "hold a button" command on gamepad2.
 *
 * TEACHING NOTE: we use this instead of Ivy's Command.build() because
 * the builder (as documented) has no onEnd/setEnd hook — so a builder-
 * based command would leave hardware running when the button is
 * released. Explicit end() is the safe path.
 */
public final class HeldActionCommand implements Command {

    private final Runnable onRun;
    private final Runnable onStop;
    private final Set<Object> requirements;

    public HeldActionCommand(Runnable onRun, Runnable onStop, Object... reqs) {
        this.onRun = onRun;
        this.onStop = onStop;
        this.requirements = Collections.unmodifiableSet(
                new HashSet<>(Arrays.asList(reqs)));
    }

    @Override public void start() { }
    @Override public void execute() { onRun.run(); }
    @Override public boolean done() { return false; }
    @Override public void end(EndCondition endCondition) { onStop.run(); }
    @Override public Set<Object> requirements() { return requirements; }
    @Override public int priority() { return 0; }
    @Override public InterruptedBehavior interruptedBehavior() { return InterruptedBehavior.END; }
    @Override public BlockedBehavior blockedBehavior() { return BlockedBehavior.CANCEL; }
    @Override public ConflictBehavior conflictBehavior() { return ConflictBehavior.CANCEL; }
}
