package com.apps.quantum;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.android.datetimepicker.date.DatePickerDialog;


public class ActionFragment extends Fragment {
	private Action mAction;
	private ActionLab mActionLab;
	private ActionReorderController mReordCtrl;
	
	private EditText mTitleField;
	private EditText mMinutesField;
	private EditText mContextField;
	private ImageButton mRepeatInterval;
	private ImageButton mPinnedButton;

	private Button mStartDateButton;
	private Button mDueDateButton;
	private int mDataFieldRequested;
	
	
	private ImageButton mDoneButton;
	private ImageButton mCancelButton;
	private ImageButton mSkipButton;
	private ImageButton mDemoteButton;
	private Date d;
	
	private String mOutcomeTempName;
	
	public static final String EXTRA_ACTION_ID = "com.apps.quantum.Action_id";
	
	private static final String DIALOG_DATE = "date";
	private static final String DIALOG_OR_DATE = "dialog_or_date";
	private static final String DIALOG_TIME = "time";
	private static final String DIALOG_REPEAT = "repeat";
	private static final int REQUEST_DATE = 2;
	private static final int REQUEST_DATE_OR_TIME = 1;
	private static final int REQUEST_TIME = 3;
	private static final int REQUEST_REPEAT_INFO = 4;
	private static final int DUE_DATE = 0;
	private static final int START_DATE = 1;
	

	
	private Callbacks mCallbacks;
	/*	Custom constructor that bundles an argument to a fragment,
		will be called by the code that creates this bundle. */
	public static ActionFragment newInstance(UUID ActionId){
		Bundle args = new Bundle();
		args.putSerializable(EXTRA_ACTION_ID, ActionId);
		
		ActionFragment fragment = new ActionFragment();
		fragment.setArguments(args);
		
		return fragment;
	}
	
	public interface Callbacks {
		void onActionUpdated();
		void navigateUp();
		//void updateOnScreenKeyboard(View v, int visibility);
	}
	
	@Override 
	public void onAttach(Activity activity){
		super.onAttach(activity);
		mCallbacks = (Callbacks)activity;
	}
	
	@Override 
	public void onDetach(){
		super.onDetach();
		mCallbacks = null;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

        mReordCtrl = ActionReorderController.get(getActivity());
		mActionLab = ActionLab.get(getActivity());
		
		if(getArguments() != null){
			UUID ActionId = (UUID)getArguments().getSerializable(EXTRA_ACTION_ID);
			mAction = mActionLab.getAction(ActionId);
		} else {
			mAction = mActionLab.getRoot();
		}
		mOutcomeTempName = null;
		
		
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, 
		Bundle savedInstanceState) {
		View v = (inflater.inflate(R.layout.fragment_action, parent, false));
		
		if(NavUtils.getParentActivityIntent(getActivity()) != null){
            getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		
		enableTextFields(v);
		enableButtons(v);
		
		displayActionDetails(mAction);
		//mCallbacks.updateOnScreenKeyboard(v, View.INVISIBLE);
				
		return v;
	}
	public void displayActionDetails(Action a){
		mAction = a;
		mMinutesField.setText(String.valueOf(mAction.getMinutesExpected()));
		
		mContextField.setText(mAction.getContextName());
		
		mStartDateButton.setText
				((mAction.getStartDate() == null)
						? "Set Start date"
						: (toButtonString(mAction.getStartDate())));
		
		if(mAction.getActionStatus() == Action.COMPLETE || mAction.getActionStatus() == Action.WISHLIST){
			mStartDateButton.setClickable(false);
			if(mAction.getStartDate() == null){
				mStartDateButton.setText(null);
				mStartDateButton.setTextColor(getResources().getColor(R.color.ltGrey));
			}
			
		}
		
		mDueDateButton.setText
		((mAction.getDueDate() == null)
		? "Set Due Date"
		:(toButtonString(mAction.getDueDate())));
	}

	private void enableTextFields(View v){
			
		mMinutesField = (EditText)v.findViewById(R.id.minutes_to_complete);
		if(mAction.getMinutesExpected() != 0){
			mMinutesField.setText(String.valueOf(mAction.getMinutesExpected()));
		}
		mMinutesField.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence c, int start, int before,
									  int count) {
				try {
					mAction.setMinutesExpected(Integer.parseInt(c.toString()));
				} catch (Exception e) {
					//do nothing;
				}
			}

			public void beforeTextChanged(CharSequence c, int start, int count, int after) {
				//This space intentionally left blank
			}

			public void afterTextChanged(Editable c) {
				//This also left blank
			}
		});
	
		mContextField = (EditText)v.findViewById(R.id.context_text_field);
		mContextField.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence c, int start, int before,
									  int count) {
				try {
					mAction.setContextName(c.toString());
				} catch (Exception e) {
					//do nothing;
				}

			}

			public void beforeTextChanged(CharSequence c, int start, int count, int after) {
				//This space intentionally left blank
			}

			public void afterTextChanged(Editable c) {
				//This also left blank
			}
		});
		
		
		
		return;
	}
	
	public void updatePinnedButton(){
		if(mAction.isPinned()){
			mPinnedButton.setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
		} else {
			mPinnedButton.clearColorFilter();
		}
	}
	
	private void enableButtons(View v){
		mPinnedButton = (ImageButton)v.findViewById(R.id.pinned_toggle);
		updatePinnedButton();
		mPinnedButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				mAction.setPinned(!mAction.isPinned());
				if (mAction.isPinned()) mAction.moveWithinList(mAction.getPriority(), 0);
				updatePinnedButton();
			}
		});
		
		mRepeatInterval = (ImageButton)v.findViewById(R.id.repeat_interval);
		updateRepeatIntervalButton(mAction.getRepeatInterval());
		mRepeatInterval.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				FragmentManager fm = getActivity().getSupportFragmentManager();

				RepeatPickerFragment repeatPicker = new RepeatPickerFragment();
//				Pass defaults to RepeatPickerFragment
				Bundle bundle = new Bundle();
				bundle.putInt("DEFAULT_REPEAT_INTERVAL", mAction.getRepeatInterval());
				bundle.putInt("DEFAULT_REPEAT_NUMBER", mAction.getRepeatNumber());
				repeatPicker.setArguments(bundle);

				repeatPicker.setTargetFragment(ActionFragment.this, REQUEST_REPEAT_INFO);
				mDataFieldRequested = DUE_DATE;
				repeatPicker.show(fm, DIALOG_REPEAT);
			}
		});
		
		
		mStartDateButton = (Button)v.findViewById(R.id.action_start_date);
		
		mStartDateButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mDataFieldRequested = START_DATE;
				FragmentManager fm = getActivity().getSupportFragmentManager();
				DatePickerDialog datePicker = DatePickerMaker.getDatePicker(ActionFragment.this);
				datePicker.show(fm, DIALOG_DATE);
			}
		});
		
		mDueDateButton = (Button)v.findViewById(R.id.action_due_date);
		
		mDueDateButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mDataFieldRequested = DUE_DATE;
				FragmentManager fm = getActivity().getSupportFragmentManager();
				DatePickerDialog datePicker = DatePickerMaker.getDatePicker(ActionFragment.this);
				datePicker.show(fm, DIALOG_DATE);
			}
		});

	}
	
	
	
	@Override
	public void onPause() {
		super.onPause();
		if(mOutcomeTempName != null && !mOutcomeTempName.equals("") &&
				!mOutcomeTempName.equals(mAction.getParent().getTitle())){
			mActionLab.updateParentInfo(mAction, mOutcomeTempName);
		}
	}	
	
	public static int DEFAULT_HOUR = 4;
	public static int DEFAULT_MINUTE = 0;

	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(resultCode != Activity.RESULT_OK) return;
		
		if(mDataFieldRequested == DUE_DATE){
			d = mAction.getDueDate();
		} else if (mDataFieldRequested == START_DATE){
			d = mAction.getStartDate();
		}
		
		if(d==null){
			Calendar calendarDate = Calendar.getInstance();
			
			int year = calendarDate.get(Calendar.YEAR);
			int month = calendarDate.get(Calendar.MONTH);
			int day = calendarDate.get(Calendar.DAY_OF_MONTH);
			
			
			int hour = DEFAULT_HOUR;
			int minute = DEFAULT_MINUTE;
			
			d = new GregorianCalendar(year,month,day,hour,minute).getTime();
		}
		d = (d == null) ? new Date() : d;
		
		switch(requestCode){
			case REQUEST_DATE:
				Date newDate = (Date)data.getSerializableExtra(DatePickerMaker.EXTRA_DATE);
				d = combineDateAndTime(d, newDate);
				updateTimeInfo(d);
			
				mCallbacks.onActionUpdated();
				break;
				
			case REQUEST_TIME:
				Date newTime = (Date)data.getSerializableExtra(DatePickerMaker.EXTRA_TIME);
				d = combineDateAndTime(newTime, d);
				updateTimeInfo(d);
				
				mCallbacks.onActionUpdated();
				break;
				
			case REQUEST_REPEAT_INFO:
				int repeatInterval = (Integer)data.getSerializableExtra(RepeatPickerFragment.EXTRA_REPEAT_INTERVAL);
                int repeatNumber = (Integer)data.getSerializableExtra(RepeatPickerFragment.EXTRA_REPEAT_NUMBER);
				mActionLab.modifyRepeatInterval(repeatInterval, repeatNumber, mAction);
				
				//Change the color of the button
				updateRepeatIntervalButton(repeatInterval);
			default:
				break;
		}
	}	
	
	private void updateRepeatIntervalButton(int repeatInterval){
		if(repeatInterval != 0){
			mRepeatInterval.setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
		} else {
			mRepeatInterval.clearColorFilter();
		}
	}
	
	private Date combineDateAndTime(Date time, Date date){
		
		Calendar calendarDate = Calendar.getInstance();
		Calendar calendarTime = Calendar.getInstance();
		calendarDate.setTime(date);
		calendarTime.setTime(time);
		
		int hour = calendarTime.get(Calendar.HOUR_OF_DAY);
		int minute = calendarTime.get(Calendar.MINUTE);
		int year = calendarDate.get(Calendar.YEAR);
		int month = calendarDate.get(Calendar.MONTH);
		int day = calendarDate.get(Calendar.DAY_OF_MONTH);
		
		return new GregorianCalendar(year,month,day,hour,minute).getTime();
	}
	private void updateTimeInfo(Date d){

		if(mDataFieldRequested == DUE_DATE){
			mAction.setDueDate(d);
			mDueDateButton.setText(toButtonString(d));
		} else {
			mActionLab.setStartDate(mAction, d);
			mStartDateButton.setText(toButtonString(d));
		}		
	}
	private String toButtonString(Date d){
		Calendar calendarDate = Calendar.getInstance();
		calendarDate.setTime(d);
		
		//If it is the default time, don't display the time of day info, only display day
		if(calendarDate.get(Calendar.HOUR_OF_DAY) == 4 && calendarDate.get(Calendar.MINUTE) == 0){
			return android.text.format.DateFormat.format("MMM dd", d).toString();	
		} else {
			return android.text.format.DateFormat.format("MMM dd hh:mm aaa", d).toString();
		}
	}
}
