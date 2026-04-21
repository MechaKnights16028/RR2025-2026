package org.firstinspires.ftc.teamcode.pedro.commands;

import com.pedropathing.ivy.Command;
import com.pedropathing.ivy.behaviors.BlockedBehavior;
import com.pedropathing.ivy.behaviors.ConflictBehavior;
import com.pedropathing.ivy.behaviors.EndCondition;
import com.pedropathing.ivy.behaviors.InterruptedBehavior;

import org.firstinspires.ftc.teamcode.pedro.AllianceColor;
import org.firstinspires.ftc.teamcode.pedro.subsystems.Indicator;

import java.util.Collections;
import java.util.Set;

/**
 * Default Indicator command. Paints the alliance color every tick.
 * Preempting commands (ShootClearCommand) temporarily override; when
 * they end, this resumes.
 */
public final class IndicatorDefaultCommand implements Command {

    private final Indicator indicator;
    private final AllianceColor alliance;

    public IndicatorDefaultCommand(Indicator indicator, AllianceColor alliance) {
        this.indicator = indicator;
        this.alliance = alliance;
    }

    @Override public void start() { }
    @Override public void execute() { indicator.paintAlliance(alliance); }
    @Override public boolean done() { return false; }
    @Override public void end(EndCondition endCondition) { }
    @Override public Set<Object> requirements() { return Collections.singleton((Object) indicator); }
    @Override public int priority() { return 0; }
    @Override public InterruptedBehavior interruptedBehavior() { return InterruptedBehavior.END; }
    @Override public BlockedBehavior blockedBehavior() { return BlockedBehavior.CANCEL; }
    @Override public ConflictBehavior conflictBehavior() { return ConflictBehavior.CANCEL; }
}
