package com.tarcrsd.letsgo.Module;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DateFormatterModule {

    public static String formatDate(Date date) {
        SimpleDateFormat fm = new SimpleDateFormat("dd-MMM-yyyy E");
        return fm.format(date);
    }

    public static Date getDate(String date) throws ParseException {
        SimpleDateFormat fm = new SimpleDateFormat("dd-MMM-yyyy E");
        return fm.parse(date);
    }

    public static String formatTime(Date time) {
        SimpleDateFormat fm = new SimpleDateFormat("hh:mm a");
        fm.setTimeZone(TimeZone.getDefault());
        return fm.format(time);
    }

    public static Date getTime(String time) throws ParseException {
        SimpleDateFormat fm = new SimpleDateFormat("hh:mm a");
        fm.setTimeZone(TimeZone.getDefault());
        return fm.parse(time);
    }

    public static String getAMOrPM(int AMorPM) {
        String str;

        if (AMorPM == 0) {
            str = "PM";
        } else {
            str = "AM";
        }
        return str;
    }
}
