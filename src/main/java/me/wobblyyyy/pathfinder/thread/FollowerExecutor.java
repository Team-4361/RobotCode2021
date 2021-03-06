/*
 *  ======================================================================
 *  || Copyright (c) 2020 Colin Robertson (wobblyyyy@gmail.com)         ||
 *  ||                                                                  ||
 *  || This file is part of the "Pathfinder" project, which is licensed ||
 *  || and distributed under the GPU General Public License V3.         ||
 *  ||                                                                  ||
 *  || Pathfinder is available on GitHub:                               ||
 *  || https://github.com/Wobblyyyy/Pathfinder                          ||
 *  ||                                                                  ||
 *  || Pathfinder's license is available:                               ||
 *  || https://www.gnu.org/licenses/gpl-3.0.en.html                     ||
 *  ||                                                                  ||
 *  || Re-distribution of this, or any other files, is allowed so long  ||
 *  || as this same copyright notice is included and made evident.      ||
 *  ======================================================================
 */

package me.wobblyyyy.pathfinder.thread;

import me.wobblyyyy.pathfinder.core.Follower;
import me.wobblyyyy.pathfinder.drive.Drive;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Execute a given follower among several different threads.
 *
 * <p>
 * In order to compensate for the computationally-expensive nature of complex
 * pathfinding and trajectory generation, important following code is offloaded
 * to a secondary thread. This ensures that the main thread stays freed enough
 * to allow a user to still have control over the program.
 * </p>
 *
 * <p>
 * As with any threaded things, however, they suck to conceptualize. The aim
 * of this class is to make it easier to follow along with how the pathfinder
 * actually generates and follows paths and trajectories.
 * </p>
 *
 * @author Colin Robertson
 */
public class FollowerExecutor {
    /**
     * Execution thread.
     */
    private final Thread executor;

    /**
     * A "bank" of all of the followers that need to be queued.
     */
    private final ArrayList<Follower> followerBank = new ArrayList<>();

    /**
     * The currently-executed follower(s?).
     */
    private final ArrayList<Follower> followers = new ArrayList<>();

    /**
     * A list of all of the followers that have run their calculation
     * method already.
     */
    private final ArrayList<Follower> hasCalculated = new ArrayList<>();

    /**
     * Actions to execute. MUST BE THREAD SAFE!
     */
    private final ArrayList<Runnable> actions = new ArrayList<>();

    /**
     * Should the thread continue its execution.
     */
    private boolean shouldRun = true;

    /**
     * The robot's drivetrain.
     *
     * <p>
     * This is used for enabling and disabling user control when the pathfinder
     * is enabled or disabled.
     * </p>
     */
    private final Drive drive;

    /**
     * Create a new follower executor and initialize the executor thread.
     *
     * <p>
     * PLEASE NOTE: Nothing's going to happen after you construct a new
     * instance of this executor. The thread must be enabled with the start
     * method prior to having any function. To disable the thread, you can
     * run the stop method.
     * </p>
     *
     * <p>
     * At this point, the thread hasn't been started yet. If you're
     * interfacing directly with this class rather than using a pathfinder
     * manager instance as a proxy, you'll need to remember to start the
     * threads before trying to follow paths.
     * </p>
     *
     * @param drive the robot's drivetrain.
     */
    public FollowerExecutor(Drive drive) {
        executor = new Thread(
                () -> {
                    /*
                     * The thread should almost always be active - if it isn't,
                     * the thread will stop, and, well, that wouldn't be cool.
                     */
                    while (shouldRun) {
                        /*
                         * Tell the CPU that this is a busy-wait and it doesn't
                         * exactly matter how much CPU time the execution of
                         * this has, so long as it's still executed.
                         *
                         * This should improve performance by freeing up some
                         * processing time.
                         */
                        Thread.onSpinWait();

                        /*
                         * Thread-safe way to add a list of actions.
                         */
                        addActions();

                        /*
                         * For each of the Runnable elements that need to be
                         * executed, we need to... well, actually run them.
                         *
                         * Note that any thrown exceptions here are ignored.
                         * If you need to debug this code, or code behind
                         * one of the generated Runnable elements, you need to
                         * make sure that you no longer ignore the exception.
                         */
                        for (Runnable r : getActions()) {
                            try {
                                r.run();
                            } catch (Exception ignored) {
                            }
                        }

                        /*
                         * Clear all of the actions - until next time!
                         */
                        clearActions();
                    }
                },
                "FollowerExecutor"
        );

        this.drive = drive;
    }

    /**
     * Start the follower executor's thread.
     *
     * <p>
     * Threads should not be started after they're already running.
     * </p>
     */
    public void start() {
        executor.start();
    }

    /**
     * Stop the follower executor's thread.
     *
     * <p>
     * Thread.stop() has been a deprecated method for a while - is there an
     * effective alternative we can use?
     * </p>
     *
     * <p>
     * THIS DOES NOT ACTUALLY STOP THE THREAD! Rather, it updates the flag
     * indicating whether or not the follower should be run as intended.
     * </p>
     */
    public void stop() {
        shouldRun = false;
    }

    /**
     * Thread-safe way to get all of the actions.
     *
     * @return actions to be executed.
     */
    public synchronized ArrayList<Runnable> getActions() {
        return actions;
    }

    /**
     * Thread-safe method to generate actions.
     */
    public synchronized void addActions() {
        ArrayList<Runnable> actions = generateRunnables();
        this.actions.addAll(actions);
    }

    /**
     * Thread-safe method to clear all of the actions.
     */
    public synchronized void clearActions() {
        this.actions.clear();
    }

    /**
     * Clear all of the followers and runnables, essentially resetting the
     * FollowerExecutor instance.
     */
    public synchronized void clear() {
        followerBank.clear();
        followers.clear();
    }

    /**
     * Generate a list of Runnable elements to be executed by the execution
     * thread.
     *
     * <p>
     * If a follower has not finished its execution yet, we disable user
     * control for the robot's drivetrain as a precautionary measure. If the
     * follower HAS finished its execution, we can then enable user control
     * once again.
     * </p>
     *
     * @return to-be-executed Runnable elements.
     */
    public ArrayList<Runnable> generateRunnables() {
        ArrayList<Runnable> runnables = new ArrayList<>();

        for (Follower f : followers) {
            if (!f.isDone()) {
                /*
                 * If the follower hasn't had its calculations executed yet,
                 * we need to make sure it does that.
                 *
                 * Add the follower to a list of followers that have already
                 * had their values calculated so we don't need to worry about
                 * it any longer.
                 */
                if (!hasCalculated.contains(f)) {
                    runnables.add(
                            f::calculate
                    );
                    hasCalculated.add(f);
                }

                /*
                 * Make sure to disable the user's control over the robot so
                 * they don't cause any issues with the motors.
                 */
                runnables.add(
                        drive::disableUserControl
                );

                /*
                 * Update the follower's power values.
                 */
                runnables.add(
                        f::update
                );

                /*
                 * Make the follower drive around. Yay!
                 *
                 * If user control hasn't been disabled by this point, we may
                 * see some issues with the motors having seemingly random
                 * spasms.
                 */
                runnables.add(
                        f::drive
                );
            } else {
                /*
                 * If the follower has finished its execution, we resume to
                 * normal.
                 *
                 * First and foremost, the user needs to re-gain control over
                 * their drivetrain.
                 *
                 * After giving the user their control back, we can focus on
                 * moving to the next follower.
                 *
                 * The user might only have control for a fraction of a second
                 * before any other queued followers are activated, but who
                 * cares - sucks to suck I guess.
                 */
                runnables.add(
                        drive::enableUserControl
                );

                runnables.add(
                        moveUp(f)
                );
            }
        }

        return runnables;
    }

    /**
     * Generate a Runnable that can be executed to progress past the current
     * follower and onto the next one.
     *
     * @param f the current follower.
     * @return a Runnable to move upwards in the execution order.
     */
    private synchronized Runnable moveUp(Follower f) {
        return () -> {
            try {
                followers.remove(f);
                followerBank.remove(f);
                followers.add(followerBank.get(0));
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    /**
     * Queue a single follower element.
     *
     * <p>
     * Queuing followers isn't a complicated process. Note, however, that if
     * the thread that executes the followers IS NOT active, nothing will
     * actually happen to the followers.
     * </p>
     *
     * <p>
     * After a follower has been queued, it won't take effect immediately.
     * After the current bank of followers has been exhausted, it'll move on
     * to newly-queued followers, such as the follower that you may have just
     * provided. How cool!
     * </p>
     *
     * @param f the follower to queue.
     */
    public synchronized void queueFollower(Follower f) {
        f.calculate();
        followerBank.add(f);
        followers.add(f);
    }

    /**
     * Queue a collection of follower elements.
     *
     * <p>
     * Queuing followers isn't a complicated process. Note, however, that if
     * the thread that executes the followers IS NOT active, nothing will
     * actually happen to the followers.
     * </p>
     *
     * <p>
     * After a follower has been queued, it won't take effect immediately.
     * After the current bank of followers has been exhausted, it'll move on
     * to newly-queued followers, such as the follower that you may have just
     * provided. How cool!
     * </p>
     *
     * @param followers the follower elements to queue.
     */
    public synchronized void queueFollowers(Collection<Follower> followers) {
        for (Follower f : followers) {
            queueFollower(f);
        }
    }

    /**
     * Check whether or not both of the follower array lists are empty,
     * indicating that there's nothing for the pathfinder to do.
     *
     * @return whether or not the pathfinder is idle.
     */
    public synchronized boolean isEmpty() {
        return followerBank.isEmpty() && followers.isEmpty();
    }

    /**
     * Lock the current thread until the pathfinder has finished its
     * execution.
     *
     * <p>
     * Unfortunately, we can't use a Thread.join() like method because of the
     * way Pathfinder handles multithreading, especially path following. This
     * should be (roughly) the same.
     * </p>
     *
     * <p>
     * As a locking operation, the calling thread will not be able to progress
     * until the execution of this thread has finished. If you'd like to
     * queue another follower while the system is active, you can do so. Yay!
     * </p>
     */
    public void lock() {
        do {
            Thread.onSpinWait();
        } while (!isEmpty());
    }
}
