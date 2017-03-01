package com.apps.quantum1;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by jonathanmckay on 12/14/14.
 */
public class RepeatDialog extends Dialog {
    private ArrayList<String> mRepeatIntervals;
    private ArrayList<String> mRepeatNumbers;
    private Context mContext;
    private Spinner mIntervalSpinner;
    private Spinner mNumberSpinner;
    private int dRepeatInterval;
    private int dRepeatNumber;

    public interface DialogListener {
        public void ready(int n, int m);
        public void cancelled();
    }

    private DialogListener mReadyListener;

    public RepeatDialog(Context context, DialogListener readyListener) {
        super(context);
        mReadyListener = readyListener;
        mContext = context;
        mRepeatIntervals = new ArrayList<String>();
        mRepeatNumbers = new ArrayList<String>();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources res = mContext.getResources();
        Collections.addAll(mRepeatIntervals, res.getStringArray(R.array.custom_repeat_intervals));
        Collections.addAll(mRepeatNumbers, res.getStringArray(R.array.custom_repeat_numbers));

        this.setTitle(R.string.repeat_dialogue_title);
        setContentView(R.layout.custom_repeat_dialog);
        mIntervalSpinner = (Spinner) findViewById (R.id.interval_spinner);
        ArrayAdapter<String> intervalAdapter = new ArrayAdapter<String> (mContext, android.R.layout.simple_spinner_dropdown_item, mRepeatIntervals);
        mIntervalSpinner.setAdapter(intervalAdapter);
        mIntervalSpinner.setSelection(dRepeatInterval); //default

        mNumberSpinner = (Spinner) findViewById (R.id.number_spinner);
        ArrayAdapter<String> numberAdapter = new ArrayAdapter<String> (mContext, android.R.layout.simple_spinner_dropdown_item, mRepeatNumbers);
        mNumberSpinner.setAdapter(numberAdapter);
        mNumberSpinner.setSelection(dRepeatNumber); //default

        Button buttonOK = (Button) findViewById(R.id.dialogOK);
        Button buttonCancel = (Button) findViewById(R.id.dialogCancel);
        buttonOK.setOnClickListener(new android.view.View.OnClickListener(){
            public void onClick(View v) {
                int n = mIntervalSpinner.getSelectedItemPosition();
                int m = mNumberSpinner.getSelectedItemPosition();
                mReadyListener.ready(n, m);
                RepeatDialog.this.dismiss();
            }
        });
        buttonCancel.setOnClickListener(new android.view.View.OnClickListener(){
            public void onClick(View v) {
                mReadyListener.cancelled();
                RepeatDialog.this.dismiss();
            }
        });
    }

    public void setDefaults(int repeat_interval, int repeat_number) {
        dRepeatInterval = repeat_interval;
        dRepeatNumber = repeat_number;
//        apply offset
        dRepeatInterval--;
        dRepeatNumber--;
    }

}