package com.apps.quantum;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class ActionFragment extends Fragment {
	private Action mAction;
	private ActionLab mActionLab;
	private EditText mTitleField;
	private EditText mMinutesField;
	private EditText mOutcomeField;
	private EditText mContextField;
	private EditText mRepeatInterval;
	
	private Button mStartDateButton;
	private Button mDueDateButton;
	private int mDataFieldRequested;
	
	private boolean mChangesMade;
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
	private static final int REQUEST_DATE = 2;
	private static final int REQUEST_DATE_OR_TIME = 1;
	private static final int REQUEST_TIME = 3;
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
		
		mActionLab = ActionLab.get(getActivity());
		
		//This is sometimes returning null and causing x950 to crash
		if(getArguments() != null){
			UUID ActionId = (UUID)getArguments().getSerializable(EXTRA_ACTION_ID);
			mAction = mActionLab.getAction(ActionId);
		} else {
			mAction = mActionLab.getRoot();
		}
		mOutcomeTempName = null;
		mChangesMade = false;
		
		//used for the up button
		setHasOptionsMenu(true);
		
		
	}
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.activity_fragment, menu);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, 
		Bundle savedInstanceState) {
		View v = (inflater.inflate(R.layout.fragment_action, parent, false));
		
		if(NavUtils.getParentActivityIntent(getActivity()) != null){
			getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		
		/*
		InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
       	*/
		enableTextFields(v);
		enableButtons(v);
		
		displayActionDetails(mAction);
		//closeOnScreenKeyboard(v);
				
		return v;
	}
	public void displayActionDetails(Action a){
		mAction = a;
		mTitleField.setText(mAction.getTitle());
		mMinutesField.setText(String.valueOf(mAction.getMinutesExpected()));
		mOutcomeField.setText(mAction.getParent().getTitle());
		mContextField.setText(mAction.getContextName());
		
		mStartDateButton.setText
		((mAction.getStartDate() == null)
		? "Set Start date" 
		: (toButtonString(mAction.getStartDate())));
		mDueDateButton.setText
		((mAction.getDueDate() == null)
		? "Set Due Date"
		:(toButtonString(mAction.getDueDate())));
	}

	private void enableTextFields(View v){
		mTitleField = (EditText)v.findViewById(R.id.action_title);
		mTitleField.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence c, int start, int before, 
					int count) {
				mActionLab.changeActionTitle(mAction, c.toString());
				mChangesMade = true;
			}
			
			public void beforeTextChanged(CharSequence c, int start, int count, int after){
				//This space intentionally left blank
			}
			
			public void afterTextChanged(Editable c) {
				
			}
		});
		
		mMinutesField = (EditText)v.findViewById(R.id.minutes_to_complete);
		if(mAction.getMinutesExpected() != 0){
			mMinutesField.setText(String.valueOf(mAction.getMinutesExpected()));
		}
		mMinutesField.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence c, int start, int before, 
					int count) {
				try{
					mAction.setMinutesExpected(Integer.parseInt(c.toString()));
				}catch(Exception e){
					//do nothing;
				}
			}
			
			public void beforeTextChanged(CharSequence c, int start, int count, int after){
				//This space intentionally left blank
			}
			
			public void afterTextChanged(Editable c) {
				//This also left blank
			}
		});
	
		mOutcomeField = (EditText)v.findViewById(R.id.outcome_text_field);
		mOutcomeField.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence c, int start, int before, 
					int count) {
				mOutcomeTempName = c.toString();
				mChangesMade = true;
			}
			
			public void beforeTextChanged(CharSequence c, int start, int count, int after){
				//This space intentionally left blank
			}
			
			public void afterTextChanged(Editable c) {
				//Store the outcome in a temp field, will be saved when fragment paused.
				
			}
		});
	
		mContextField = (EditText)v.findViewById(R.id.context_text_field);
		mContextField.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence c, int start, int before, 
					int count) {
				try{
					mAction.setContextName(c.toString());
				}catch(Exception e){
					//do nothing;
				}
				mChangesMade = true;
			}
			
			public void beforeTextChanged(CharSequence c, int start, int count, int after){
				//This space intentionally left blank
			}
			
			public void afterTextChanged(Editable c) {
				//This also left blank
			}
		});
		
		
		mRepeatInterval = (EditText)v.findViewById(R.id.repeat_interval);
		mRepeatInterval.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence c, int start, int before, 
					int count) {
				try{
					int dayToMicrosec = 1000*60*60*24;
					long interval = Integer.valueOf(c.toString()) * dayToMicrosec;
					long end = interval * 3;
					long now = new Date().getTime();
					
					mAction.makeActionRepeatable(new Date(interval), new Date(now + end));
				}catch(Exception e){
					Log.e("ActionFragment", "Error reading repeat input", e);
				}
				mChangesMade = true;
			}
			
			public void beforeTextChanged(CharSequence c, int start, int count, int after){
				//This space intentionally left blank
			}
			
			public void afterTextChanged(Editable c) {
				//This also left blank
			}
		});
		
		
		return;
	}
	private void closeOnScreenKeyboard(View v){
		InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		if(imm != null && getActivity().getCurrentFocus() != null ){
			imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 
					InputMethodManager.HIDE_NOT_ALWAYS);
		}
	}
	private void enableButtons(View v){
		mStartDateButton = (Button)v.findViewById(R.id.action_start_date);
		
		mStartDateButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				FragmentManager fm = getActivity().getSupportFragmentManager();
				
				TimeOrDateFragment timeOrDate = new TimeOrDateFragment();
				timeOrDate.setTargetFragment(ActionFragment.this, REQUEST_DATE_OR_TIME);
				mDataFieldRequested = START_DATE;
				timeOrDate.show(fm, DIALOG_OR_DATE);
				
				mChangesMade = true;
			}
		});
		
		mDueDateButton = (Button)v.findViewById(R.id.action_due_date);
		
		mDueDateButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				FragmentManager fm = getActivity().getSupportFragmentManager();
				
				TimeOrDateFragment timeOrDate = new TimeOrDateFragment();
				timeOrDate.setTargetFragment(ActionFragment.this, REQUEST_DATE_OR_TIME);
				mDataFieldRequested = DUE_DATE;
				timeOrDate.show(fm, DIALOG_OR_DATE);
				mChangesMade = true;
			}
		});
		
		mDoneButton = (ImageButton)v.findViewById(R.id.done_button);
		mDoneButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mTitleField.setPaintFlags((mAction.getActionStatus() == Action.INCOMPLETE)
						? mTitleField.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
						: mTitleField.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
				
				int newActionStatus = (mAction.getActionStatus() == Action.INCOMPLETE)
						? Action.COMPLETE
						: Action.INCOMPLETE;
				
				mActionLab.changeActionStatus(mAction, newActionStatus);
				
				mChangesMade = true;
				mCallbacks.navigateUp();
			}
		});
		
		mCancelButton = (ImageButton)v.findViewById(R.id.cancel_button);
		mCancelButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				
				Action actionToDelete = mAction;
				
				mAction = mAction.getParent();
				
				mActionLab.deleteAction(actionToDelete);
				mOutcomeTempName = null;
				mCallbacks.navigateUp();
				
				mChangesMade = true;
				
			}
		});

		mSkipButton = (ImageButton)v.findViewById(R.id.skip_button);
		mSkipButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mAction.getParent().moveToEnd(mAction.getActionStatus(), mAction.getPriority());
				
				mCallbacks.navigateUp();
				mChangesMade = true;
			}
		});
		
		mDemoteButton = (ImageButton)v.findViewById(R.id.demote_button);
		mDemoteButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mActionLab.changeActionStatus(mAction, Action.WISHLIST);
				
				mCallbacks.navigateUp();
				mChangesMade = true;
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
		mActionLab.saveActions();
	}	
	
	
	
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(resultCode != Activity.RESULT_OK) return;
		
		if(mDataFieldRequested == DUE_DATE){
			d = mAction.getDueDate();
		} else {
			d = mAction.getStartDate();
		}
		d = (d == null) ? new Date() : d;
		
		switch(requestCode){
			case REQUEST_DATE_OR_TIME:
				FragmentManager fm = getActivity().getSupportFragmentManager();
				if((Integer)data.getSerializableExtra(TimeOrDateFragment.EXTRA_DATE_OR_TIME) == 2){
					DatePickerFragment dateDialog = DatePickerFragment.newInstance(d);
					dateDialog.setTargetFragment(ActionFragment.this, REQUEST_DATE);
					dateDialog.show(fm, DIALOG_DATE);
				}
				else if((Integer)data.getSerializableExtra(TimeOrDateFragment.EXTRA_DATE_OR_TIME) == 1){
					TimePickerFragment timeDialog = TimePickerFragment.newInstance(d);
					timeDialog.setTargetFragment(ActionFragment.this, REQUEST_TIME);
					timeDialog.show(fm, DIALOG_TIME);	
				}
				
				break;
			case REQUEST_DATE: 
				Date newDate = (Date)data.getSerializableExtra(DatePickerFragment.EXTRA_DATE);
				d = combineDateAndTime(d, newDate);
				updateTimeInfo(d);
				mChangesMade = true;
				mCallbacks.onActionUpdated();
				break;
				
			case REQUEST_TIME:
				Date newTime = (Date)data.getSerializableExtra(TimePickerFragment.EXTRA_TIME);
				d = combineDateAndTime(newTime, d);
				updateTimeInfo(d);
				mChangesMade = true;
				mCallbacks.onActionUpdated();
				break;
				
			default:
				break;
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
			mAction.setStartDate(d);
			mStartDateButton.setText(toButtonString(d));
		}		
	}
	private String toButtonString(Date d){
		return android.text.format.DateFormat.format("MM.dd HH:mm", d).toString();
	}
}
