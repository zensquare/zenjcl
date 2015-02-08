/*
 * Zensquare Java Cron Library (ZenJCL)
 * 
 * Copyright (C) 2015 Nick Rechten, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 *
*/
package org.zensquare.cron;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * The JobScheduler holds a list of jobs. The schedule that the jobs follow is
 * configured on the jobs them selves.
 * 
 * By design the JobScheduler will not create new threads for running jobs. If 
 * your application runs jobs that need to be run in parallel then you need to
 * manage this yourself.
 * 
 * The flow on affect of this is that if a job throws an exception it will kill
 * the main thread and the start() method will need to be called again to 
 * continue to run jobs. Either catch these exceptions in the jobs themselves or
 * modify the main run loop. * 
 * 
 * @author nrechten
 */
public class JobScheduler implements Runnable {

    /**
     * Helps to tune the scheduler including the startup delay and the amount
     * added to the job.last time before calculating the next run schedule. You
     * will need to adjust this if you want to have jobs run multiple times a
     * minute.
     */
    protected long SMALLEST_INTERVAL = 60000;
    /**
     * The limits the length of the sleep in the main scheduling thread. Acts as
     * a heartbeat and was a cheap way to allow jobs to be added to the schedule
     * without any extra logic. It does waste processing cycles and might be
     * removed at some point.
     */
    protected long DEFAULT_POLL_RATE = 3600000;
    /**
     * The scheduler does not run the job when job.next = now, it runs it when
     * job.next &lt; now. The window limits how late the job can run. If too
     * much time has elapsed the job will not run and a notice will be logged
     * that it missed it's scheduled window. 
     */
    protected long DEFAULT_WINDOW    = 1000 * 60 * 60 * 2; 
    
    private final List<Job> jobs = new ArrayList<>();
    private boolean run = true;
    private final Lock lock = new ReentrantLock();
    private Thread thread;
    private long   startDelay = SMALLEST_INTERVAL;
    private static long   FUZZ = 10;

    public JobScheduler() {
    }
    
    public JobScheduler(long startDelay){
        this.startDelay = startDelay;
    }
       
    
    
    
    /**
     * Start the JobScheduler - if it isn't already. If it has already started 
     * do nothing.
     */
    public synchronized void start(){
        if(thread == null){
            thread = new Thread(this);
            thread.start();
        }
    }
    
    /**
     * It's a loop with a fancy wait statement.
     * 
     * TODO: Document this mess
     */
    @Override
    public synchronized void run() {
        long next = System.currentTimeMillis() + DEFAULT_POLL_RATE;
        long wait = startDelay;
        
        //Kind of application specific - feel free to pull the next try statement
        if(wait > 0){
            try {
                Date d = new Date(next);
                Logger.debug("Scheduler Waiting " + wait + "ms  -  " + d.toString() + " Before starting main loop");
                this.wait(wait + FUZZ);
            } catch (InterruptedException ex) {
            }
        }
        
        
        //How long the first sleep should be
        lock.lock();
        try {
            for (Job job : jobs) {
                job.calcNext();
                next = Math.min(next, job.getNext());
            }
        } finally {
            lock.unlock();
        }
        
        
        while (run) {
            wait = next - System.currentTimeMillis();
            if (wait > 0) {
                try {
                    Date d = new Date(next);
                    Logger.debug("Scheduler Waiting " + wait + "ms  -  " + d.toString());
                    this.wait(wait + FUZZ);
                } catch (InterruptedException ex) {
                }
            }
            next = System.currentTimeMillis() + DEFAULT_POLL_RATE;
            lock.lock();
            try {
                for (Job job : jobs) {
                    long current = System.currentTimeMillis();
                    long jNext = job.getNext();
                    
                    if (jNext <= current) {
                        if( jNext > current - DEFAULT_WINDOW) {
                            Logger.debug("Running Job : " + job);
                            job.run();
                            Logger.debug("Next run : " + new Date(job.getNext()));
                        } else {
                            Logger.error(job.toString() + " : missed it's scheduled window");
                            job.last = job.getNext()+SMALLEST_INTERVAL;
                            job.calcNext();
                        }
                    }
                    next = Math.min(next, job.getNext());
                }
            } finally {
                lock.unlock();
            }
        }
        thread = null;
    }

    /**
     * Add a new job to the list of jobs to manage. This method will block until
     * the job is added that will only happen when the schedule thread is
     * waiting (or doesn't exist).
     * 
     * If this is the first job added to the scheduler main thread of the
     * scheduler.
     * @param job  the job to add to the scheduler - should already be configured
     * with a schedule.
     */
    public synchronized void addJob(Job job) {
        lock.lock();
        try {
            if(!jobs.contains(job)){
                jobs.add(job);
                job.calcNext();
                notifyAll();
            }
        } finally {
            lock.unlock();
        }
        start();
    }
    
    /**
     * Will remove the given job - as with the addJob method this will block
     * until the job can be 'safely' removed. This might mean that the job might
     * still be run after this method has been called to remove it.
     * 
     * @param job The job to remove
     */
    public void removeJob(Job job){
        lock.lock();
        try {
            jobs.remove(job);
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Remove all jobs, like the other methods for adding and removing jobs this 
     * will block until the main thread sleeps. This means that some jobs may
     * start after this method is called.
     */
    public synchronized void clear(){
        lock.lock();
        try {
            jobs.clear();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Does the main thread exist, it only checks if the thread exists - it
     * could be in the process of shutting down.
     * @return True if the thread exists.
     */
    public boolean isAlive(){
        return thread != null;
    }
    
    /**
     * This method will block until the jobs can be removed (see clear) and
     * shutdown the main thread.
     */
    public synchronized void shutdown(){
        clear();
        run = false;
        thread.notifyAll();
    }

    /**
     * Checks if the thread is alive or there are not jobs (so the thread
     * shouldn't be alive).
     * @return true if the status of the Scheduler is ok
     */
    public boolean isOK() {
        return jobs.isEmpty() || isAlive();
    }
    
    
    public static void main(String[] args){
        Logger.turnOnDebugging();
        JobScheduler scheduler = new JobScheduler(0);
        Job job = new Job(){public void runJob(){System.out.println("Hello world!");}};
        job.addSchedule("* * * * *");
        scheduler.addJob(job);
        System.out.println(job.getSchedules().get(0));
    }
}
