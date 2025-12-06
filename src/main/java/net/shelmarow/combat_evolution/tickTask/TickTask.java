package net.shelmarow.combat_evolution.tickTask;

import java.util.UUID;

public abstract class TickTask {
    protected UUID taskID;
    protected int maxTime;
    protected int tickTimer;

    public TickTask(int durationTicks) {
        this.maxTime = durationTicks;
        this.tickTimer = 0;
    }

    public abstract void onStart();

    public abstract void onTick();

    public abstract void onFinish();

    public UUID getTaskID() {
        return taskID;
    }

    public boolean isFinished() {
        return tickTimer >= maxTime;
    }

    public void tick() {
        tickTimer++;
        if (!isFinished()) {
            onTick();
        }
        else{
            onFinish();
        }

    }

}

