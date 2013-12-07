package com.example.criminalintent;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;



public class TimeOrDateFragment extends DialogFragment {

	public static final String EXTRA_DATE_OR_TIME = "com.example.criminalintent.dateortime";
	private int mTimeOrDate;
	
	
	private void sendResult(int resultCode) {
		if(getTargetFragment() == null) return;
		
		Intent i = new Intent();
		i.putExtra(EXTRA_DATE_OR_TIME, mTimeOrDate);
		getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, i);
	}
	
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		
		return new AlertDialog.Builder(getActivity())
		.setTitle(R.string.date_or_time_title)
		.setPositiveButton(R.string.dialog_date_button, 
				new DialogInterface.OnClickListener() {
			
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mTimeOrDate = 2;
					sendResult(Activity.RESULT_OK);
				}
		})
		.setNegativeButton(R.string.dialog_time_button, 
				new DialogInterface.OnClickListener() {
			
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mTimeOrDate = 1;
					sendResult(Activity.RESULT_OK);
				}
		})
		.create();
	}
	
	
}