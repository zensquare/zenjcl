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
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The Job class should be extended with your own logic and added to a JobScheduler.
 * Two notes with extending this class - the JobScheduler WILL NOT spawn new
 * threads to run the job. When you extend this class you need to create your own
 * thread if the Job is non trivial. JobScheduler does not catch exceptions in
 * if there is potential that the job will throw an exception make sure you catch
 * it within the runJob method - if it escapes the JobScheduler will stop and will
 * need to be restarted.
 * 
 * @author Nikolai Rechten
 */
public abstract class Job {

    /**
     * Hardcoded limit on how often a job can run - set to every 60 Seconds
     */
    public static long MINIMUM_WAIT = 60000;
    
    protected List<Schedule> schedule = new ArrayList<>();

    /**
     * The next time (in UTC milliseconds from the epoch) this job needs to run
     */
    protected long next;
    
    
    /**
     * The last time (in UTC milliseconds from the epoch) this job was run
     */
    protected long last;
    
    /**
     * Get the scheduled run time for this job.
     * @return the next run time in UTC milliseconds from the epoch.
     */
    public long getNext(){
        return next;
    }
    
    /**
     * Calculate the next time that this job should be run, this will be called
     * automatically when the job is run to calculate the next run time. The job
     * Scheduler will call this when the job is first loaded.
     */
    public void calcNext(){
        long next_run = Long.MAX_VALUE;
        for (Schedule sched : schedule) {
            try {
                Calendar c = new GregorianCalendar();
                if(last != 0){
                    c.setTimeInMillis(last);
                }
                next_run = Math.min(sched.nextValid(c), next_run);
            } catch (Exception ex) {
                Logger.getLogger(Job.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        next = next_run;
    }
    
    /**
     * Add a run schedule to this job, make sure you run calcNext after adding
     * a schedule.
     * @param sched the new run schedule to the job
     */
    public void addSchedule(Schedule sched) {
        schedule.add(sched);
    }
    
    /**
     * Add a run schedule to this job, make sure you run calcNext after adding
     * a schedule.
     * @param sched the new run schedule in UNIX cron format
     */
    public void addSchedule(String sched) {
        schedule.add(new Schedule(sched));
    }
    
    /**
     * Run the job, update the last run time and calculate the next run. This is
     * called by the JobScheduler. If you are going to call this outside of the
     * JobScheduler you should think about the treading implications.
     */
    public final void run(){
        runJob();
        last = System.currentTimeMillis() + MINIMUM_WAIT;
        calcNext();
    }
    
    /**
     * Laziness prevails - you shouldn't be messing with the contents of this
     * list - or at the very least call calcNext after messing with it.
     * @return the Schedule list of this job - modify with care
     */
    public List<Schedule> getSchedules(){
        return schedule;
    }
    
    /**
     * Called by the run command when it's time for this job to run - override 
     * with your own business logic.
     */
    protected abstract void runJob();
    
   
}
