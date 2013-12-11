package com.apps.quantum;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;
import com.apps.quantum.R;

public class TimePickerFragment extends DialogFragment {
	public static final String EXTRA_TIME = "com.apps.quantum.time";
	private Date mTime;
	
	public static TimePickerFragment newInstance(Date time){
		Bundle args = new Bundle();
		
		args.putSerializable(EXTRA_TIME, time);
		
		TimePickerFragment fragment = new TimePickerFragment();
		fragment.setArguments(args);
		
		return fragment;
	}
	
	private void sendResult(int resultCode) {
		if(getTargetFragment() == null) return;
		
		Intent i = new Intent();
		i.putExtra(EXTRA_TIME, mTime);
		getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, i);
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		mTime = (Date)getArguments().getSerializable(EXTRA_TIME);
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(mTime);
		
		int hour = calendar.get(Calendar.HOUR);
		int minute = calendar.get(Calendar.MINUTE);
		
		
		View v = getActivity().getLayoutInflater().inflate(R.layout.dialog_time, null);
		
		TimePicker timePicker = (TimePicker)v.findViewById(R.id.dialog_time_timePicker);
		timePicker.setCurrentHour(hour);
		timePicker.setCurrentMinute(minute);
		
		timePicker.setOnTimeChangedListener(new OnTimeChangedListener(){
			public void onTimeChanged(TimePicker view, int hour, int minute){
							
				mTime = new GregorianCalendar(0,0,0,hour,minute).getTime();
				getArguments().putSerializable(EXTRA_TIME, mTime);
			}
		});
		
		
		
		return new AlertDialog.Builder(getActivity())
		.setView(v)
		.setTitle(R.string.time_picker_title)
		.setPositiveButton(android.R.string.ok, 
			new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					sendResult(Activity.RESULT_OK);
				}
			})
		.create();
	}
	
	
}
