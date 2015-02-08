# ZenJCL
Zensquare Java Cron Library

ZenJCL is a lightweight scheduling library, it has been designed for those times
when you simply need some piece of code to run on a given schedule.

What ZenJCL does
---------------------
 - Parse Cron style expression strings
 - Runs code when you tell it to
 - Allows you to graphically build Cron expressions
 - Allows for super simple integration and with no dependencies
 - Display Cron style expressions in a human readable form

ZenJCL does not
----------------------
 - Have persistent storage of jobs (thats your job, and you are probably doing it somewhere already)
 - Support clustering or any of that fancy stuff libraries like Obsidian or Quartz have

Hello World!
----------------------
The program below prints Hello World to the console every minute. It's actually
included in the JobSchedule class so don't even have to type it.
```
public class HelloWorld {
    public static void main(String[] args){
        Logger.turnOnDebugging(); //Display extra debugging information
        JobScheduler scheduler = new JobScheduler(0); //The zero removes the default startup delay 
        Job job = new Job(){public void runJob(){System.out.println("Hello World");}};
        job.addSchedule("* * * * *");
        scheduler.addJob(job);
    }
}
```
ScheduleEditor
-----------------------
A basic Cron expression editor that can be used as a standalone program to build
Cron expressions or if you want use it directly in your application. If it
doesn't suit the look of your application it shouldn't be hard to tear it down
and reuse the components.

Human Readable
-----------------------
I'm not going to say it's beautiful - but for many less technical people it will
make understanding a Cron expression easier. Here are some examples of the output
of the Schedule class.
```
"0 11 * * 1-6/2 2015" - on the 1st minute of the hour at 11am every 2nd day from Monday to Saturday in 2015
"*/5 * * * 1-5 *" - every 5th minute from Monday to Friday
"1 1 2 * * *" - on the 2nd minute of the hour at 1am on the 2nd day of the month
```

HTML Markup
-----------------------
This is used by the ScheduleEditor to build the expression builder.
```
"0 11 * * 1-6/2 2015"
<pre>
<div class="cron_column"><div class="column_name">Minute filter</div><div class="rules"><div class="rule"><a href="remove:13" class="remove">[-] </a>on the <a href="edit:13" class="BasicFieldPart">2nd</a> minute of the hour</div><div class="rule"><a href="add:2" class="new">[+] Add new Rule</a></div></div></div><div class="cron_column"><div class="column_name">Hour filter</div><div class="rules"><div class="rule"><a href="remove:14" class="remove">[-] </a>at <a href="edit:14" class="BasicFieldPart">1am</a> </div><div class="rule"><a href="add:4" class="new">[+] Add new Rule</a></div></div></div><div class="cron_column"><div class="column_name">Day of Month filter</div><div class="rules"><div class="rule"><a href="remove:15" class="remove">[-] </a>on the <a href="edit:15" class="BasicFieldPart">2nd</a> day of the month</div><div class="rule"><a href="add:6" class="new">[+] Add new Rule</a></div></div></div><div class="cron_column"><div class="column_name">Month filter</div><div class="rules"><div class="rule"><a href="add:8" class="new">[+] Add new Rule</a></div></div></div><div class="cron_column"><div class="column_name">Day of Week filter</div><div class="rules"><div class="rule">This cannot be set while a day of month filter is set</div></div></div><div class="cron_column"><div class="column_name">Year filter</div><div class="rules"><div class="rule"><a href="add:12" class="new">[+] Add new Rule</a></div></div></div>
</pre>
```
Icons Credits
-----------------------
The ScheduleEditor uses two icon's from the silk icon set created by Mark James 
[www.famfamfam.com](www.famfamfam.com/lab/icons/silk/)