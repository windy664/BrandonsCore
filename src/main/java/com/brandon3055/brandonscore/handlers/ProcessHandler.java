package com.brandon3055.brandonscore.handlers;

import net.covers1624.quack.util.CrashLock;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is a class used to run processes that implement IProcess.
 * Processes are similar to tile entities except that they are not bound to anything and they are not currently
 * persistent (they will be deleted when the world closes)
 * <p/>
 * Created by brandon3055 on 12/8/2015.
 */

public class ProcessHandler {
    private static final CrashLock LOCK = new CrashLock("Already Initialized.");

    private static List<IProcess> processes = new ArrayList<IProcess>();
    private static List<IProcess> newProcesses = new ArrayList<IProcess>();

    public static void init() {
        LOCK.lock();

        NeoForge.EVENT_BUS.addListener(ProcessHandler::onServerTick);
        NeoForge.EVENT_BUS.addListener(ProcessHandler::onServerStop);
    }

    public static void onServerTick(ServerTickEvent.Pre event) {
        Iterator<IProcess> i = processes.iterator();

        while (i.hasNext()) {
            IProcess process = i.next();
            if (process.isDead()) {
                i.remove();
            } else {
                process.updateProcess();
            }
        }

        if (!newProcesses.isEmpty()) {
            processes.addAll(newProcesses);
            newProcesses.clear();
        }
    }

    public static void clearHandler() {
        processes.clear();
        newProcesses.clear();
    }

    public static void addProcess(IProcess process) {
        newProcesses.add(process);
    }

    public static void onServerStop(ServerStoppedEvent event) {
        clearHandler();
    }
}
