package net.shelmarow.combat_evolution.tickTask;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class TickTaskManager {
    private static final Map<UUID,TickTask> taskMap = new HashMap<>();

    public static void addTask(UUID uuid, TickTask task) {
        task.taskID = uuid;
        taskMap.putIfAbsent(uuid,task);
        task.onStart();
    }

    public static void tickAll() {
        Iterator<TickTask> iterator = taskMap.values().iterator();
        while (iterator.hasNext()) {
            TickTask task = iterator.next();
            task.tick();
            if (task.isFinished()) {
                iterator.remove();
            }
        }
    }
}

