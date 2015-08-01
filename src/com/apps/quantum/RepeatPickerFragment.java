package com.apps.quantum;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;


public class RepeatPickerFragment extends DialogFragment {

	public static final String EXTRA_REPEAT_INTERVAL = "com.apps.quantum.repeatinterval";
    public static final String EXTRA_REPEAT_NUMBER = "comp.apps.quantum.repeatnumber";
	private int mRepeatInterval;
    private int mRepeatNumber;
	
	
	private void sendResult(int resultCode) {
		if(getTargetFragment() == null) return;
		
		Intent i = new Intent();
		i.putExtra(EXTRA_REPEAT_INTERVAL, mRepeatInterval);
        i.putExtra(EXTRA_REPEAT_NUMBER, mRepeatNumber);
		getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, i);
	}

	public Dialog onCreateDialog(Bundle savedInstanceState) {
        //Acquire default values from ActionFragment's bundle
        Bundle bundle = this.getArguments();
        int dRepeatInterval = 0;
        int dRepeatNumber = 0;
        if (bundle != null) {
            dRepeatInterval = bundle.getInt("DEFAULT_REPEAT_INTERVAL");
            dRepeatNumber = bundle.getInt("DEFAULT_REPEAT_NUMBER");
        }

        RepeatDialog mSpinnerDialog = new RepeatDialog(getActivity(), new RepeatDialog.DialogListener() {
            public void cancelled() {
                System.out.println("Cancelled spinner dialog");
                mRepeatNumber = 0;
                mRepeatInterval = 0;
                sendResult(Activity.RESULT_OK);
                return;
            }
            public void ready(int interval, int number) {
                System.out.println("Ready spinner dialog");
                //Offset by one so that this represents the actual number shown
                mRepeatInterval = interval + 1;
                mRepeatNumber = number + 1;
                sendResult(Activity.RESULT_OK);
                return;
            }
        });

        mSpinnerDialog.setDefaults(dRepeatInterval, dRepeatNumber);


        /*
		return new AlertDialog.Builder(getActivity()).setTitle(R.string.repeat_dialogue_title)
		.setItems(R.array.repeat_intervals, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int which) {
            	   mRepeatInterval = which;
            	   sendResult(Activity.RESULT_OK);
            	   return;
               }
		}).create();
		*/
        return mSpinnerDialog;
	}


}
