package com.apps.quantum1;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;

import com.android.datetimepicker.date.DatePickerDialog;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by user on 8/7/15.
 */
public class DatePickerMaker {
    public static final int REQUEST_DATE = 2;
    public static final String EXTRA_DATE = "com.apps.quantum.date";
    public static final String EXTRA_TIME = "com.apps.quantum.time";

    public static DatePickerDialog getDatePicker(final Fragment frag) {
        //Default date is now
        Calendar calendarDate = Calendar.getInstance();
        int year = calendarDate.get(Calendar.YEAR);
        int month = calendarDate.get(Calendar.MONTH);
        int day = calendarDate.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePicker = DatePickerDialog.newInstance( new DatePickerDialog.OnDateSetListener() {
//          This callback stores the date when the user presses "Done" or "Time"
//          There is a similar callback defined in DatePickerDialog.java to store the time
            @Override
            public void onDateSet(DatePickerDialog dialog, int year, int monthOfYear, int dayOfMonth) {
//				Pass date to action fragment
                if(dialog.getTargetFragment() == null) {
                    dialog.setTargetFragment(frag, REQUEST_DATE);
                }
                Intent i = new Intent();

                int hour = ActionFragment.DEFAULT_HOUR;
                int minute = ActionFragment.DEFAULT_MINUTE;
                Date mDate = new GregorianCalendar(year, monthOfYear, dayOfMonth, hour, minute).getTime();
                i.putExtra(EXTRA_DATE, mDate);
                dialog.getTargetFragment().onActivityResult(REQUEST_DATE, Activity.RESULT_OK, i);
            }

        }, year, month, day);

        datePicker.setTargetFragment(frag, REQUEST_DATE);
        return datePicker;
    }
}
