package com.nz.media.player;

import com.nz.media.backend.BackendCommand;
import com.nz.media.backend.VideoBackend;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Handles pending backend commands with TTL and coalescing logic.
 * Not thread-safe; must be accessed from a single thread (MediaThread).
 */
public class PlayerCommandQueue {

    private final Deque<PendingCommand> pending = new ArrayDeque<>();

    public void add(BackendCommand cmd, long ttlMs, Runnable action) {
        // "last command wins" per type: seeking/speed/volume should be coalesced.
        long now = System.nanoTime();
        long deadline = now + TimeUnit.MILLISECONDS.toNanos(Math.max(1, ttlMs));

        for (Iterator<PendingCommand> it = pending.iterator(); it.hasNext();) {
            PendingCommand p = it.next();
            if (p.cmd == cmd) {
                it.remove();
                break;
            }
        }

        pending.addLast(new PendingCommand(cmd, deadline, action));
    }

    public void flush(VideoBackend backend) {
        if (pending.isEmpty())
            return;

        long now = System.nanoTime();

        // drop expired first
        for (Iterator<PendingCommand> it = pending.iterator(); it.hasNext();) {
            PendingCommand p = it.next();
            if (now >= p.deadlineNs) {
                it.remove();
            }
        }

        if (pending.isEmpty())
            return;

        // Try to execute what we can, keep what we can't
        for (Iterator<PendingCommand> it = pending.iterator(); it.hasNext();) {
            PendingCommand p = it.next();
            if (backend.can(p.cmd)) {
                try {
                    p.action.run();
                } finally {
                    it.remove();
                }
            }
        }
    }

    public void clear() {
        pending.clear();
    }

    public boolean isEmpty() {
        return pending.isEmpty();
    }

    private static final class PendingCommand {
        final BackendCommand cmd;
        final long deadlineNs;
        final Runnable action;

        PendingCommand(BackendCommand cmd, long deadlineNs, Runnable action) {
            this.cmd = cmd;
            this.deadlineNs = deadlineNs;
            this.action = action;
        }
    }
}
