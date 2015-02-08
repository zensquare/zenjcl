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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

/**
 * A Schedule calculates a set of points in time. It uses the CRON format to
 * configure the internal rules that can be used to iterate through the next
 * valid times.
 * 
 * Because there are varying implementations on the CRON format this class has
 * has been designed to allow for additional formats to be 'easily' adopted.
 * Currently the system only supports the UNIX and Quartz Scheduler formats.
 * 
 * The class supports outputting the schedule in a human("English") readable 
 * format.
 * 
 * @TODO: Actually handle different CRON formats rather then just lying about it =P
 * @author Nick Rechten
 */
public class Schedule {

    public static final Calendar CALENDAR = new GregorianCalendar();
    
    public static final int FIELD_MINUTES = 0;
    public static final int FIELD_HOURS = 1;
    public static final int FIELD_DAY_OF_MONTH = 2;
    public static final int FIELD_MONTH = 3;
    public static final int FIELD_DAY_OF_WEEK = 4;
    public static final int FIELD_YEAR = 5;
    
    public static final int FORMAT_QUARTZ = 1;
    public static final int FORMAT_UNIX = 0;
    
    private ScheduleField[] fields;
    public int format = FORMAT_QUARTZ;
    private HashMap<String, FieldPart> parts;
    public int ids = 0;

    /**
     * Creates a new schedule and parses the given CRON schedule string in the
     * in the given format
     * @param schedule The schedule for follow in CRON format
     * @param format the specific format to use to parse the CRON string
     */
    public Schedule(String schedule, int format) {
        this(format);
        parse(schedule);
    }

    /**
     * Creates a new schedule and parses the given CRON schedule string in the
     * in the default (UNIX) format
     * @param schedule The schedule for follow in CRON format
     */
    public Schedule(String schedule) {
        this(FORMAT_UNIX);
        parse(schedule);
    }

    /**
     * Creates a new blank schedule
     */
    public Schedule() {
        this(FORMAT_UNIX);
    }

    /**
     * Create a blank schedule with the given format
     * @param format the CRON string format type (FORMAT_QUARTZ or FORMAT_UNIX)
     */
    public Schedule(int format) {
        this.format = format;
        parts = new HashMap<String, FieldPart>();
        if (format == FORMAT_QUARTZ) {
            fields = new ScheduleField[]{
                new MinutesField("Minute", "1"),
                new HourField("Hour", "1"),
                new DayOfMonthField("Day of Month", "*"),
                new MonthField("Month", "*"),
                new DayOfWeekField("Day of Week", "*"),
                new YearField("Year", "*")
            };
        } else {
            fields = new ScheduleField[]{
                new MinutesField("Minute", "1"),
                new HourField("Hour", "1"),
                new DayOfMonthField("Day of Month", "*"),
                new MonthField("Month", "*"),
                new DayOfWeekField("Day of Week", "*"),
                new YearField("Year", "*")
            };
        }

    }

    /**
     * Adds a field part to the list of parts that make up the expression - it
     * will assign the part an ID and update the ID of the part with said ID.
     * @param fp the part to add
     */
    private void addFieldPart(FieldPart fp) {
        ids++;
        fp.id = ids;
        parts.put(String.valueOf(ids), fp);
    }

    /**
     * Parses the given CRON expression string - this should only be called once
     * if you need to call it again you should create a new Schedule object.
     * @param  schedule The CRON expression string
     */
    public final void parse(String schedule) {
        String[] cron = schedule.split(" ");
        for (int i = 0; i < cron.length && i < fields.length; i++) {
            fields[i].parse(cron[i]);
        }
    }

    @Override
    public String toString() {
        return toString(true);
    }
    
    public String toString(boolean newLines){
        String foo = "";
        String nl = newLines?"\n":" ";
        for (int i = 0; i < fields.length; i++) {
            String f = fields[i].toString().trim();
            if (!f.equals("")) {
                foo += f + nl;
            }
        }
        return foo.trim();
    }

    public String toMarkup() {
        String foo = "";
        for (int i = 0; i < fields.length; i++) {
            foo += fieldMarkup(fields[i]);
        }
        return foo;
    }

    public String toCron() {
        String foo = "";
        for (int i = 0; i < fields.length; i++) {
            if (i != 0) {
                foo += " ";
            }
            foo += fields[i].toCron();
        }
        return foo;
    }

    private String fieldMarkup(ScheduleField value) {
        return "<div class=\"cron_column\"><div class=\"column_name\">" + value.name + " filter</div><div class=\"rules\">" + value.toMarkup() + "<div class=\"rule\">" + value.getAddText() + "</div></div></div>";
    }

    public void addToField(int id, String newPart) {
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].id == id) {
                fields[i].add(newPart);
                return;
            }
        }
        if (parts.containsKey(String.valueOf(id))) {
            FieldPart fp = parts.get(String.valueOf(id));
            fp.parent.replace(fp, fp.field.parseField(newPart, fp.parent));
        }
    }

    public void setField(int id, String newPart) {
        if (parts.containsKey(String.valueOf(id))) {
            FieldPart fp = parts.get(String.valueOf(id));
            fp.parent.replace(fp, fp.field.parseField(newPart, fp.parent));
        }
    }

    public void remove(int part) {
        if (parts.containsKey(String.valueOf(part))) {
            FieldPart fp = parts.get(String.valueOf(part));
            fp.parent.replace(fp, new WildcardFieldPart(fp.field));
        }
    }

    public String[][] getOptions(int part) {
        if (parts.containsKey(String.valueOf(part))) {
            FieldPart fp = parts.get(String.valueOf(part));
            ScheduleField sf = fp.field;
            String[][] options = new String[][]{new String[sf.upper - sf.lower + 1], new String[sf.upper - sf.lower + 1]};
            for (int i = 0; i <= sf.upper - sf.lower; i++) {
                options[0][i] = String.valueOf(i + sf.lower);
                options[1][i] = sf.getValue(i + sf.lower);
            }
            return options;
        }
        return new String[][]{new String[]{"0"}, new String[]{"No options"}};
    }

    public Object[] getFieldInfo(int part) {
        if (parts.containsKey(String.valueOf(part))) {
            FieldPart fp = parts.get(String.valueOf(part));
            ScheduleField sf = fp.field;
            Object[] fieldInfo = new Object[14];
            fieldInfo[0] = sf.id;
            fieldInfo[1] = sf.name;
            fieldInfo[2] = sf.lower;
            fieldInfo[3] = sf.upper;
            fieldInfo[4] = sf.in_on;
            fieldInfo[5] = sf.prefix;
            fieldInfo[6] = sf.quantifier;
            fieldInfo[7] = sf.upperQuantifier;
            fieldInfo[8] = fp.getClass().getSimpleName();
            fieldInfo[9] = fp.getIntValue();
            fieldInfo[10] = fp.toString();
            fieldInfo[11] = fp.getFullDescription();
            fieldInfo[12] = fp.parent.getClass().getSimpleName();
            fieldInfo[13] = fp.parent.relation(fp);

            return fieldInfo;
        }
        return null;
    }

    public long nextValid() throws Exception {
        Calendar c = new GregorianCalendar();
        return nextValid(c);
    }

    public long nextValid(Calendar c) throws Exception {

        boolean valid = false;
        while (!valid) {
            valid = true;
            for (int i = fields.length - 1; i >= 0; i--) {
                if (i == FIELD_YEAR && !fields[FIELD_YEAR].getNextValid(c)) { //If we dont have a valid year we have failed, there is no next valid time
                    return Long.MAX_VALUE;
                }
                if (!fields[i].getNextValid(c)) {

                    if (fields[i] instanceof MonthField) {
                        fields[i + 2].roll(c);
                    } else if (!(fields[i] instanceof DayOfWeekField)) {
                        fields[i + 1].roll(c);
                    } else {
                        
                    }
                    valid = false;
                    break;
                } else {
                }
//                Logger.debug(fields[i].getClass().getName() + ":" + c.getTime());
            }
        }
        return c.getTimeInMillis();
    }

    private interface PartParent {

        public void replace(FieldPart target, FieldPart replacement);

        public String relation(FieldPart part);
    }

    private abstract class ScheduleField implements PartParent {

        public String name;
        public FieldPart value;
        public int upper;
        public int lower;
        public String in_on = "in ";
        public String prefix = "the ";
        public int id;
        public String upperQuantifier = "";
        public String quantifier = "";

        public ScheduleField(String name, String quantifier, String upperQuantifier, String value, int lower, int upper) {
            this.name = name;
            this.upper = upper;
            this.lower = lower;
            this.value = parseField(value, this);
            this.quantifier = quantifier;
            this.upperQuantifier = upperQuantifier;
            this.id = ++ids;
        }

        public ScheduleField(String name, String quantifier, String upperQuantifier, String value, int lower, int upper, String in_on) {
            this(name, quantifier, upperQuantifier, value, lower, upper);
            this.id = ++ids;
        }

        public String getValue(int val) {
            val++;
            return addThing(val);
        }

        public String addThing(int val) {
            String thing = "th";
            if (val % 100 > 10 && val % 100 < 14) {
            } else if (val % 10 == 1) {
                thing = "st";
            } else if (val % 10 == 2) {
                thing = "nd";
            } else if (val % 10 == 3) {
                thing = "rd";
            }
            return val + thing;
        }

        @Override
        public String toString() {

            if (value instanceof CompoundFieldPart) {
                return value.toString();
            }
            if (value instanceof BasicFieldPart) {
                return in_on + prefix + value.toString() + " " + getQuantifier();
            }
            return value.toString() + "\n";
        }

        public String toMarkup() {
            if (value instanceof CompoundFieldPart) {
                return value.toMarkup();
            }
            if (value instanceof WildcardFieldPart) {
                return getWildcardText();
            }
            return formatRule(value);
        }

        public String toCron() {
            return value.toCron();
        }

        public void add(String part) {

            if (value instanceof CompoundFieldPart) {
                CompoundFieldPart cfp = (CompoundFieldPart) value;
                cfp.addPart(parseField(part, cfp));
            } else if (value instanceof WildcardFieldPart) {
                this.value.field = null;
                this.value = parseField(part, this);
            } else {
                CompoundFieldPart cfp = new CompoundFieldPart();
                cfp.field = this;
                cfp.parent = this;
                cfp.addPart(this.value);
                cfp.addPart(parseField(part, cfp));
                this.value = cfp;
            }
        }

        public void parse(String field) {
            this.value = parseField(field, this);
        }

        public FieldPart parseField(String field, PartParent pp) {

            String[] parts = field.split(",");
            if (parts.length > 0) {
                if (parts.length > 1) {
                    CompoundFieldPart cfp = new CompoundFieldPart();
                    cfp.field = this;
                    cfp.parent = pp;
                    for (int i = 0; i < parts.length; i++) {
                        cfp.addPart(parseField(parts[i], cfp));
                    }
                    return cfp;
                } else {
                    if (parts[0].contains("/")) {
                        IncrementPart ipart = new IncrementPart();
                        ipart.field = this;
                        ipart.parent = pp;
                        String[] subparts = parts[0].split("/", 2);
                        ipart.setRange(parseField(subparts[0], ipart));
                        ipart.setIncrement(parseField(subparts[1], ipart));
                        return ipart;
                    }
                    if (parts[0].contains("-")) {
                        RangePart rpart = new RangePart();
                        rpart.field = this;
                        rpart.parent = pp;
                        String[] subparts = parts[0].split("-", 2);
                        rpart.setLower(parseField(subparts[0], rpart));
                        rpart.setUpper(parseField(subparts[1], rpart));
                        return rpart;
                    }
                    if (parts[0].equals("*") || parts[0].equals("?")) {
                        WildcardFieldPart wfp = new WildcardFieldPart(this);
                        wfp.parent = pp;
                        return wfp;
                    }
                    BasicFieldPart bfp = new BasicFieldPart(bound(Integer.parseInt(parts[0])), this);
                    bfp.parent = pp;
                    return bfp;
                }
            }

            return null;
        }

        public int bound(int value) {
            if (value < lower) {
                return lower;
            }
            if (value > upper) {
                return upper;
            }
            return value;
        }

        public String formatBetween(RangePart range, boolean withMarkup) {
            if (withMarkup) {
                return "between " + prefix + range.lower.toMarkup() + " and " + range.upper.toMarkup() + " " + getQuantifier();
            } else {
                return "between " + prefix + range.lower.toString() + " and " + range.upper.toString() + " " + getQuantifier();
            }
        }

        public String formatIncrement(FieldPart a, boolean withMarkup) {
            if (a instanceof WildcardFieldPart) {
                return "";
            }

            String inc = "";
            if (a instanceof BasicFieldPart) {
                if (((BasicFieldPart) a).value == 1) {
                    inc += quantifier + " ";
                } else {
                    inc += addThing(((BasicFieldPart) a).value) + " " + quantifier + " ";
                }
            } else {
                if (withMarkup) {
                    inc += a.toMarkup();
                } else {
                    inc += a.toString();
                }

            }
            return " every " + (withMarkup ? a.markup(inc) : inc);
        }

        public String getWildcardText() {
            return "";
        }

        public String getAddText() {
            return "<a href=\"add:" + this.id + "\" class=\"new\">[+] Add new Rule</a>";
        }

        public String formatRule(FieldPart part) {

            if (part instanceof BasicFieldPart) {
                return "<div class=\"rule\"><a href=\"remove:" + part.id + "\" class=\"remove\">[-] </a>" + in_on + prefix + part.toMarkup() + " " + getQuantifier() + "</div>";
            } else {
                return "<div class=\"rule\"><a href=\"remove:" + part.id + "\" class=\"remove\">[-] </a>" + part.toMarkup() + "</div>";
            }

        }

        public void replace(FieldPart target, FieldPart replacement) {
            if (this.value == target) {
                this.value = replacement;
                replacement.parent = this;
                replacement.field = this;
            }
        }

        public String relation(FieldPart part) {
            if (this == part.parent) {
                return "child";
            }
            return null;
        }

        public String getQuantifier() {
            return quantifier + " of " + prefix + upperQuantifier;
        }

        public abstract boolean getNextValid(Calendar from);

        public abstract void roll(Calendar cal);
    }

    private class MinutesField extends ScheduleField {

        public MinutesField(String name, String value) {
            super(name, "minute", "hour", value, 0, 59);
            in_on = "on ";
            prefix = "the ";
        }

        @Override
        public String getWildcardText() {
            return "every minute";
        }

        @Override
        public String formatBetween(RangePart range, boolean withMarkup) {
            if (withMarkup) {
                return " between " + prefix + range.lower.toMarkup() + " and " + range.upper.toMarkup() + " " + getQuantifier();
            } else {
                return " between " + prefix + range.lower.toString() + " and " + range.upper.toString() + " " + getQuantifier();
            }
        }

        @Override
        public boolean getNextValid(Calendar from) {
            int minutes = from.get(Calendar.MINUTE);
            int next = value.nextValid(minutes);

            if (next < minutes) {
                return false;
            }

            if (next != minutes) {
                from.set(from.get(Calendar.YEAR), from.get(Calendar.MONTH), from.get(Calendar.DAY_OF_MONTH), from.get(Calendar.HOUR_OF_DAY), next, 0);
            }

            return true;
        }

        @Override
        public void roll(Calendar cal) {
            cal.add(Calendar.MINUTE, 1);
            cal.set(Calendar.SECOND, 0);
        }
    }

    private class HourField extends ScheduleField {

        public HourField(String name, String value) {
            super(name, "hour", "day", value, 0, 23);
            prefix = "";
            in_on = "at ";
        }

        @Override
        public String getValue(int val) {

            if (val == 0) {
                return "12am";
            } else if (val < 12) {
                return val + "am";
            }
            if (val == 12) {
                return "12pm";
            } else {
                return val - 12 + "pm";
            }
        }

        @Override
        public String getQuantifier() {
            return "";
        }

        @Override
        public boolean getNextValid(Calendar from) {
            int hour = from.get(Calendar.HOUR_OF_DAY);
            int next = value.nextValid(hour);

            if (next < hour) {
                return false;
            }


            if (next != hour) {
                from.set(from.get(Calendar.YEAR), from.get(Calendar.MONTH), from.get(Calendar.DAY_OF_MONTH), next, 0, 0);
            }
            return true;
        }

        @Override
        public void roll(Calendar cal) {
            cal.add(Calendar.HOUR_OF_DAY, 1);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MINUTE, 0);
        }
    }
    public static final String[] DAYS_OF_WEEK = new String[]{"Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

    public int dowStart() {
        //System.out.println(format);
        if (format == FORMAT_QUARTZ) {
            return 1;
        }
        return 0;
    }

    public int dowEnd() {
        if (format == FORMAT_QUARTZ) {
            return 7;
        }
        return 6;
    }

    private class DayOfWeekField extends ScheduleField {

        public DayOfWeekField(String name, String value) {
            super(name, "day", "week", value, dowStart(), dowEnd());

            in_on = "on ";
            prefix = "a ";
        }

        @Override
        public String getValue(int val) {
            return String.valueOf(DAYS_OF_WEEK[(val + (format == FORMAT_UNIX ? 1 : 0)) % 7]);
        } 

        @Override
        public String getQuantifier() {
            return "";
        }

        @Override
        public String formatBetween(RangePart range, boolean withMarkup) {
            if (withMarkup) {
                return "from " + range.lower.toMarkup() + " to " + range.upper.toMarkup() + " " + getQuantifier();
            } else {
                return "from " + range.lower.toString() + " to " + range.upper.toString() + " " + getQuantifier();
            }
        }

        @Override
        public String getAddText() {
            if (fields[2].value instanceof WildcardFieldPart) {
                return "<a href=\"add:" + this.id + "\" class=\"new\">[+] Add new Rule</a>";
            } else {
                return "This cannot be set while a day of month filter is set";
            }
        }

        @Override
        public boolean getNextValid(Calendar from) {
            int dow = from.get(Calendar.DAY_OF_WEEK);
            int next = value.nextValid(dow);

            if (next != dow) {
                int month = from.get(Calendar.MONTH);
                from.set(Calendar.SECOND, 0);
                from.set(Calendar.MINUTE, 0);
                from.set(Calendar.HOUR_OF_DAY, 0);
                from.add(Calendar.DAY_OF_MONTH, (next < 0 ? 7 - dow - next : next - dow));
                return month == from.get(Calendar.MONTH);
            }

            return true;
        }

        @Override
        public void roll(Calendar cal) {
//            cal.add(Calendar.HOUR_OF_DAY, 1);
        }
    }

    private class DayOfMonthField extends ScheduleField {

        public DayOfMonthField(String name, String value) {
            super(name, "day", "month", value, 1, 31);
            in_on = "on ";
        }

        @Override
        public String getValue(int val) {
            return super.getValue(val - 1);
        }

        @Override
        public String getAddText() {
            if (fields[4].value instanceof WildcardFieldPart) {
                return "<a href=\"add:" + this.id + "\" class=\"new\">[+] Add new Rule</a>";
            } else {
                return "This cannot be set while a day of week filter is set";
            }
        }

        @Override
        public boolean getNextValid(Calendar from) {
            int date = from.get(Calendar.DAY_OF_MONTH);
            int next = value.nextValid(date);

            if (next < date) {
                return false;
            }

            if (next != date) {
                from.set(from.get(Calendar.YEAR), from.get(Calendar.MONTH), next, 0, 0, 0);
            }
            return true;
        }

        @Override
        public void roll(Calendar cal) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.HOUR_OF_DAY, 0);
        }
    }
    public static final String[] MONTHS = new String[]{"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

    private class MonthField extends ScheduleField {

        public MonthField(String name, String value) {
            super(name, "month", "year", value, 1, 12);
            in_on = "in ";
            prefix = "";
        }

        @Override
        public String getValue(int val) {
            return String.valueOf(MONTHS[Math.max(0, val - 1) % 12]);
        }

        @Override
        public String getQuantifier() {
            return "";
        }

        @Override
        public boolean getNextValid(Calendar from) {
            int month = from.get(Calendar.MONTH);
            int next = value.nextValid(month+1)-1;
            
            if (next < month) {
                return false;
            }

            if (next != month) {
                from.set(from.get(Calendar.YEAR), next, 1, 0, 0, 0);
            }

            return true;
        }

        @Override
        public void roll(Calendar cal) {
            cal.add(Calendar.MONTH, 1);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private class YearField extends ScheduleField {

        public YearField(String name, String value) {
            super(name, "year", "year", value, (Calendar.getInstance().get(Calendar.YEAR)), (Calendar.getInstance().get(Calendar.YEAR)) + 100);
            in_on = "in ";
            prefix = "";
        }

        @Override
        public String getValue(int val) {
            return super.getValue(val - 1);
        }

        @Override
        public String addThing(int val) {
            return String.valueOf(val);
        }

        @Override
        public String getQuantifier() {
            return "";
        }

        @Override
        public boolean getNextValid(Calendar from) {

            int year = from.get(Calendar.YEAR);
            int next = value.nextValid(year);

            if (next < year) {
                return false;
            }

            if (next != year) {
                from.set(next, Calendar.JANUARY, 1, 1, 0, 0);
            }

            return true;
        }

        public int bound(int value){
            return value>0?value:0;
        }
        
        @Override
        public void roll(Calendar cal) {
            cal.set(cal.get(Calendar.YEAR)+1, 1, 1, 0, 0, 0);
        }
    }

    private abstract class FieldPart {

        public ScheduleField field;
        public int id;
        public PartParent parent;

        public FieldPart() {
            addFieldPart(this);
        }

        public abstract String toCron();

        public abstract String toMarkup();

        public String getQuantifier() {
            return field.quantifier + " of the " + field.upperQuantifier;
        }

        public String getFullDescription() {
            return field.in_on + field.prefix + this.toString() + " " + field.quantifier;
        }

        public int getIntValue() {
            return 0;
        }

        public String relation(FieldPart part) {
            if (part == this) {
                return "this";
            }
            if (this == part.parent) {
                return "child";
            }
            return null;
        }

        public String markup(String value) {
            return "<a href=\"edit:" + id + "\" class=\"" + this.getClass().getSimpleName() + "\">" + value + "</a>";
        }

        @Override
        public String toString() {
            return field.getValue(getIntValue());
        }

        public abstract int nextValid(int start);
    }

    private class BasicFieldPart extends FieldPart {

        public int value;

        public BasicFieldPart(int value, ScheduleField field) {
            this.field = field;
            this.value = value;
        }

        @Override
        public String toString() {
            return field.getValue(value);
        }

        public String toMarkup() {
            return markup(field.getValue(value));
        }

        @Override
        public String getFullDescription() {
            return field.in_on + field.prefix + this.toString() + " " + getQuantifier();
        }

        @Override
        public int getIntValue() {
            return value;
        }

        @Override
        public String toCron() {
            return String.valueOf(value);
        }

        @Override
        public int nextValid(int start) {
            return start > value ? -value : value;
        }
    }

    private class CompoundFieldPart extends FieldPart implements PartParent {

        public ArrayList<FieldPart> parts = new ArrayList<FieldPart>();

        @Override
        public String toString() {
            String ret = "";
            for (int i = 0; i < parts.size(); i++) {
                if (i != 0) {
                    if (i < parts.size() - 1) {
                        ret += ", ";
                    } else {
                        ret += " and ";
                    }
                }
                if (parts.get(i) instanceof BasicFieldPart) {
                    ret += field.in_on + field.prefix + parts.get(i).toString() + " " + field.getQuantifier();
                } else {
                    ret += parts.get(i).toString();
                }
            }
            return ret;
        }

        @Override
        public String toMarkup() {
            String ret = "";
            for (int i = 0; i < parts.size(); i++) {
                ret += field.formatRule(parts.get(i));
            }
            return ret;
        }

        @Override
        public String toCron() {
            String ret = "";
            for (int i = 0; i < parts.size(); i++) {
                if (i != 0) {
                    ret += ",";
                }
                ret += parts.get(i).toCron();
            }
            return ret;
        }

        public void addPart(FieldPart part) {
            parts.add(part);
            part.parent = this;
            part.field = this.field;
        }

        public void removeField(FieldPart part) {
            if (parts.remove(part)) {
                part.parent = null;
                part.field = null;
            }
        }

        public void replace(FieldPart target, FieldPart replacement) {
            removeField(target);
            if (replacement instanceof WildcardFieldPart) {
                if (parts.size() == 1) {
                    parent.replace(this, parts.get(0));
                } else if (parts.isEmpty()) {
                    parent.replace(this, replacement);
                }
            } else {
                parts.add(replacement);
            }
        }

        @Override
        public int nextValid(int start) {
            int next = -1, t = 0;
            for (FieldPart fieldPart : parts) {
                t = fieldPart.nextValid(start);
                if (t < 0) {
                    continue;
                }
                if (next == -1) {
                    next = t;
                } else {
                    next = Math.min(next, t);
                }
            }
            return next;
        }
    }

    private class RangePart extends FieldPart implements PartParent {

        public FieldPart lower, upper;

        @Override
        public String toCron() {
            return lower.toCron() + "-" + upper.toCron();
        }

        @Override
        public String toString() {
            return field.formatBetween(this, false);
        }

        public String toMarkup() {
            return field.formatBetween(this, true);
        }

        public void setUpper(FieldPart upper) {
            if (this.upper != null) {
                this.upper.parent = null;
                this.upper.field = null;
            }
            this.upper = upper;
            upper.parent = this;
            upper.field = this.field;
        }

        public void setLower(FieldPart lower) {
            if (this.lower != null) {
                this.lower.parent = null;
                this.lower.field = null;
            }
            this.lower = lower;
            lower.parent = this;
            lower.field = this.field;
        }

        public void replace(FieldPart target, FieldPart replacement) {
            if (target == lower) {
                setLower(replacement);
            } else if (target == upper) {
                setUpper(replacement);
            }
        }

        @Override
        public String relation(FieldPart part) {
            String relation = super.relation(part);
            if (relation.equals("child")) {
                if (part == lower) {
                    return "lower";
                }
                if (part == upper) {
                    return "upper";
                }
            }
            return relation;
        }

        @Override
        public int nextValid(int start) {
            if (start < lower.getIntValue()) {
                return lower.getIntValue();
            }
            if (start > upper.getIntValue()) {
                return -lower.getIntValue();
            }
            return start;
        }
    }

    private class WildcardFieldPart extends RangePart {

        public WildcardFieldPart(ScheduleField field) {
            this.field = field;
        }

        @Override
        public String toString() {
            return field.getWildcardText();
        }

        @Override
        public String toMarkup() {
            return markup("*");
        }

        @Override
        public String getFullDescription() {
            return "";
        }

        @Override
        public String toCron() {
            if ((field instanceof DayOfWeekField && this == field.value) || (field instanceof DayOfMonthField && !(fields[FIELD_DAY_OF_WEEK].value instanceof WildcardFieldPart))) {
                return "?";
            }
            return "*";
        }

        @Override
        public void replace(FieldPart target, FieldPart replacement) {
        }

        @Override
        public int nextValid(int start) {
            return start;
        }
    }

    private class IncrementPart extends FieldPart implements PartParent {

        public FieldPart range;
        public FieldPart increment;

        public String toString() {
            if (range instanceof RangePart) {
                return field.formatIncrement(increment, false) + (range instanceof WildcardFieldPart ? "" : range.toString());
            } else {
                return "Doesn't make sense";
            }
        }

        public String toMarkup() {
            if (range instanceof RangePart) {
                return field.formatIncrement(increment, true) + range.toMarkup();
            } else {
                return "Doesn't make sense";
            }
        }

        @Override
        public String getFullDescription() {
            return this.toString();
        }

        @Override
        public String toCron() {
            return range.toCron() + "/" + increment.toCron();
        }

        public void setRange(FieldPart range) {
            if (this.range != null) {
                this.range.parent = null;
                this.range.field = null;
            }
            this.range = range;
            range.parent = this;
            range.field = this.field;
        }

        public void setIncrement(FieldPart increment) {
            if (this.increment != null) {
                this.increment.parent = null;
                this.increment.field = null;
            }
            this.increment = increment;
            increment.parent = this;
            increment.field = this.field;
        }

        public void replace(FieldPart target, FieldPart replacement) {
            if (target == range) {
                setRange(replacement);
            } else if (target == increment) {
                setIncrement(replacement);
            }
        }

        @Override
        public String relation(FieldPart part) {
            String relation = super.relation(part);
            if (relation.equals("child")) {
                if (part == range) {
                    return "range";
                }
                if (part == increment) {
                    return "increment";
                }
            }
            return relation;
        }

        @Override
        public int nextValid(int start) {
            int s = range.nextValid(start);
            int t = range.nextValid(0);

            if (s < start) {
                return -t;
            }

            if (s == t) {
                return s;
            }

            s = (int) Math.ceil((s - t) / (float) increment.getIntValue()) * increment.getIntValue();

            return t + s;
        }
    }

    public static void main(String[] args) {
        Schedule s = new Schedule("1 1 2 * * *");//10-20/5 9-17/2 1,2 */3 2");
        System.out.println(s.toString(false));
        System.out.println(s.toMarkup());
        try {
            System.out.println("will next run : " + new Date(s.nextValid()));
        } catch (Exception ex) {
        }
    }
}
