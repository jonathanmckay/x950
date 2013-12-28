package com.apps.quantum;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;


public class RepeatPickerFragment extends DialogFragment {

	public static final String EXTRA_REPEAT_INFO = "com.apps.quantum.repeatinfo";
	private int mRepeatInterval;
	
	
	private void sendResult(int resultCode) {
		if(getTargetFragment() == null) return;
		
		Intent i = new Intent();
		i.putExtra(EXTRA_REPEAT_INFO, mRepeatInterval);
		getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, i);
	}
	
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		return new AlertDialog.Builder(getActivity()).setTitle(R.string.repeat_dialogue_title)
		.setItems(R.array.repeat_intervals, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int which) {
            	   mRepeatInterval = which;
            	   sendResult(Activity.RESULT_OK);
            	   return;
               }
		}).create();
	}
}
