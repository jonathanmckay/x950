package com.apps.quantum;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.datetimepicker.date.DatePickerDialog;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.twitter.Extractor;
import com.twitter.Extractor.Entity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class ActionListFragmentDSLV extends Fragment {
	private static final String TAG = "ActionListFragment";
	private static final String DROPBOX_DIALOG = "dropbox_dialog";
	private static final int REQUEST_LIST_REFRESH = 1;

	private Action mAction;
	private ActionLab mActionLab;
	private ActionReorderController mReordCtrl;
	private String mSubtaskTitle;
	private MenuItem mToggleMenuItem;
	private MenuItem mEditTitle;
	private View mScreenFooter;
	private View mKitkatFooter;
	private View mListFooter;
	private boolean mFirstTimeOpeningList;
	private boolean mListFooterAdded;

	public int mActionViewMode;
	private DragSortListView mListView;
	private ActionAdapter mAdapter;
	private Callbacks mCallbacks;
	private boolean mDetailVisible;

	private Extractor mTextExtractor;	
	private ImageButton mNewSubtaskButton;
	private EditText mSubtaskField;
	private EditText mTitleEdit;
    private ImageButton mDoneButton;
	private ImageButton fDoneButton;
	private TextView fDoneText;

	private Date d;
	private static final int REQUEST_DATE = 2;
    private static final int REQUEST_TIME = 3;
	private Action childAction;

	public interface Callbacks {
		void onActionSelected(Action a);

		void onDetailViewToggled(Action a);

		void updateOnScreenKeyboard(View v, int visibility);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mCallbacks = null;
	}

	public void updateUI() {
		mAdapter.notifyDataSetChanged();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mActionLab = ActionLab.get(getActivity());
		mReordCtrl = ActionReorderController.get(getActivity());
		mTextExtractor = new Extractor();
		mAction = mActionLab.getRoot();
		mActionViewMode = Action.TOP_FIVE_VIEW;
		mSubtaskTitle = null;

		setHasOptionsMenu(true);
		setRetainInstance(true);
		setTitle();
	}

	@SuppressLint("NewApi")
	private void refreshView() {
		mFirstTimeOpeningList = false;
		if (mListView == null) {
			mFirstTimeOpeningList = true;
			mListView = (DragSortListView) getActivity().findViewById(
					R.id.listview);
		}

		mActionLab.checkForPendingActions();
		mAdapter = new ActionAdapter(mAction.getActions(mActionViewMode));
		mAdapter.notifyDataSetChanged();
		mListView.setAdapter(mAdapter);

		if (mFirstTimeOpeningList) {
			setListeners();
		}

		mListView.post(new Runnable() {
			public void run() {

				int numItemsVisible = mListView.getLastVisiblePosition()
						- mListView.getFirstVisiblePosition();

				if ((mAdapter.getCount() - 1 > numItemsVisible)
						&& !mListFooterAdded) {
					mScreenFooter.setVisibility(View.GONE);

					mListView.addFooterView(mListFooter, 1, false);
					mListFooterAdded = true;

					// set your footer on the ListView
				} else if (mAdapter.getCount() - 1 <= numItemsVisible
						&& mListFooterAdded) {

					mListView.removeFooterView(mListFooter);
					mListFooterAdded = false;
					
					
					
					mScreenFooter.setVisibility(View.VISIBLE);

				}
			}
		});
        updateDoneButtonVisibility();
		updateFooter();
	}

    private void updateDoneButtonVisibility() {
        if(!mAction.hasActiveTasks()
            && !mAction.hasPendingTasks()
            && !mAction.isRoot()
        ) {
//            fDoneButton.setVisibility(View.VISIBLE);
            return;
        }

//        fDoneButton.setVisibility(View.INVISIBLE);
    }

	private void setListeners() {
		mListView.setDropListener(onDrop);
		mListView.setRightSwipeListener(onSwipe);
		mListView.setOnItemClickListener(onClick);
		DragSortController controller = new DragSortController(mListView,
				R.id.action_drag_handle, DragSortController.ON_DOWN,
				DragSortController.FLING_REMOVE, 0);
		controller.setDragHandleId(R.id.action_drag_handle);
		controller.setSwipeEnabled(true);
		controller.setSortEnabled(true);
		controller.setDragInitMode(1);
		mListView.setFloatViewManager(controller);
		mListView.setOnTouchListener(controller);
		mListView.setDragEnabled(true);
	}

	private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
		@Override
		public void drop(int from, int to) {
			mReordCtrl.moveWithinAdapter(mAdapter, from, to);
		}
	};

	private DragSortListView.RightSwipeListener onSwipe = new DragSortListView.RightSwipeListener() {
		@Override
		public void swipe(int position) {
			mAction.incrementCompleted();
			mReordCtrl.changeActionStatus(mAdapter, position, Action.COMPLETE);
			mAdapter.notifyDataSetChanged();
            updateDoneButtonVisibility();
            updateFooter();
		}
	};


	private AdapterView.OnItemClickListener onClick = new AdapterView.OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> adapterView, View v,
				int position, long id) {

			mAction = mAdapter.getItem(position);
			updateListToShowCurrentAction();
			return;
		}
	};

	private void updateListToShowCurrentAction() {
		mCallbacks.onActionSelected(mAction);
		// Log.d(TAG, mAction.getTitle() + " is now the focus");

		refreshView();
		getActivity().supportInvalidateOptionsMenu();
		setTitle();
		updateTitleEdit();
		updateNewItemHint(getView());
		// Log.d(TAG, " Set List mAdapter to " + mAction.getTitle());

		return;
	}

	@Override
	public void onResume() {
		super.onResume();
		refreshView();

	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_action_list, menu);

		mToggleMenuItem = menu.findItem(R.id.menu_item_detail_toggle);
		mEditTitle = menu.findItem(R.id.add);

		updateTitleEdit();

		if (mAction.equals(mActionLab.getRoot())) {
			mToggleMenuItem.setVisible(false);
			mEditTitle.setVisible(false);

		} else {
			mToggleMenuItem.setVisible(true);

			if (mAction.hasChildren()) {
				mDetailVisible = false;
			} else {
				mDetailVisible = true;
			}

			updateDetailsToggle();
		}

	}

	private void updateTitleEdit() {
		mTitleEdit = (EditText) mEditTitle.getActionView().findViewById(
				R.id.title);
		mTitleEdit.setText(mAction.getTitle());

		mTitleEdit.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence c, int start, int before,
					int count) {
				mActionLab.changeActionTitle(mAction, c.toString());

			}

			public void beforeTextChanged(CharSequence c, int start, int count,
					int after) {
				// This space intentionally left blank
			}

			public void afterTextChanged(Editable c) {

			}
		});
	}

	private void setTitle() {
		if (mAction.equals(mActionLab.getRoot())) {
			getActivity().setTitle(R.string.default_title);
		} else {
			getActivity().setTitle(mAction.getTitle());
		}
	}

	public void updateDetailsToggle() {
		if (mDetailVisible) {
			mToggleMenuItem.setIcon(R.drawable.ic_action_collapse);
			getActivity().setTitle(null);
			mEditTitle.setVisible(true);
		} else {
			mToggleMenuItem.setIcon(R.drawable.ic_action_expand);
			getActivity().setTitle(mAction.getTitle());
			mEditTitle.setVisible(false);
		}
	}

	@TargetApi(11)
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if (mSubtaskTitle != null && !mSubtaskTitle.equals("")) {
				saveNewSubtask();
			}
			mCallbacks.updateOnScreenKeyboard(getView(), View.INVISIBLE);

			navigateUp();
			refreshView();
			return true;

		case R.id.menu_item_detail_toggle:
			mCallbacks.onDetailViewToggled(mAction);
			mDetailVisible = !mDetailVisible;
			updateDetailsToggle();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	//select the view to show the main actions based on the settings drawer
	public void selectActionView(int menuPosition) {
		switch (menuPosition) {
		case 0:
			mActionViewMode = Action.TOP_FIVE_VIEW;
			break;
		case 1:
			mActionViewMode = Action.INCOMPLETE;
			break;
		case 2:
			mActionViewMode = Action.PENDING;
			break;
		case 3:
			mActionLab.deleteAllActions();
			break;
		case 4:
			FragmentManager fm = getActivity().getSupportFragmentManager();
			DropboxFragment dbx = new DropboxFragment();
			dbx.setTargetFragment(ActionListFragmentDSLV.this,
					REQUEST_LIST_REFRESH);
			dbx.show(fm, DROPBOX_DIALOG);
			break;
		default:
			break;
		}

		if (menuPosition >= 0 && menuPosition < 6) {
			refreshView();
		}
		return;

	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK)
			return;

		//Used by Date/Timepickers to pass date back to fragment
		if(d==null){
			Calendar calendarDate = Calendar.getInstance();

			int year = calendarDate.get(Calendar.YEAR);
			int month = calendarDate.get(Calendar.MONTH);
			int day = calendarDate.get(Calendar.DAY_OF_MONTH);

			int hour = ActionFragment.DEFAULT_HOUR;
			int minute = ActionFragment.DEFAULT_MINUTE;

			d = new GregorianCalendar(year,month,day,hour,minute).getTime();
		}

		switch (requestCode) {
		case REQUEST_LIST_REFRESH:
			mAction = mActionLab.getRoot();
			refreshView();
			break;
		case REQUEST_DATE:
			Date newDate = (Date)data.getSerializableExtra(DatePickerMaker.EXTRA_DATE);
			d = ActionFragment.combineDateAndTime(d, newDate);
			mActionLab.setStartDate(childAction, d);
			refreshView();
			break;

		case REQUEST_TIME:
			Date newTime = (Date)data.getSerializableExtra(DatePickerMaker.EXTRA_TIME);
			d = ActionFragment.combineDateAndTime(newTime, d);
			mActionLab.setStartDate(childAction, d);
			refreshView();
			break;
		default:
			break;
		}
	}

	// Returns to parent activity which controls the drawer button.
	public static final int ALREADY_AT_ROOT = 0;
	public static final int ARRIVED_AT_ROOT = 1;
	public static final int NOT_AT_ROOT = 2;

	public int navigateUp() {
		if (mAction.equals(mActionLab.getRoot())
				&& mActionViewMode == Action.TOP_FIVE_VIEW) {
			return ALREADY_AT_ROOT;
		} else if (mActionViewMode != Action.TOP_FIVE_VIEW) {
			mActionViewMode = Action.TOP_FIVE_VIEW;

		} else {
			mAction = mAction.getParent();
			if (mAction == null)
				mAction = mActionLab.getRoot();
		}

		updateListToShowCurrentAction();
		return (mAction.equals(mActionLab.getRoot())) ? ARRIVED_AT_ROOT
				: NOT_AT_ROOT;
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup parent,
			Bundle savedInstanceState) {
		View v = (inflater
				.inflate(R.layout.fragment_action_list, parent, false));

		initializeSubtaskField(v);

		fDoneButton = (ImageButton) v.findViewById(R.id.footer_clear_done_button);
		fDoneText = (TextView) v.findViewById(R.id.mark_as_done);

		//Don't show task done button with unfulfilled subtasks
		if (!(mAction.getChildren().get(0).isEmpty()) || mAction.isRoot()) {
			fDoneButton.setVisibility(View.INVISIBLE);
			fDoneText.setVisibility(View.INVISIBLE);
		}

		fDoneButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mAction.isRoot()) {
					Log.e("DSLV", "Tried to remove root");
					return;
				}

				Action toDelete = mAction;
				mAction = toDelete.getParent();
//				mActionLab.changeActionStatus(toDelete, Action.COMPLETE);
//				toAncestor();
				removeAction(toDelete);
				updateListToShowCurrentAction();
				updateFooter();

				String toastText = "Task completed";
				Toast.makeText(getActivity(), toastText, Toast.LENGTH_SHORT).show();
			}
		});

		mListFooter = ((LayoutInflater) getActivity()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
				.inflate(R.layout.kitkat_spacer, null, false);

		//mListFooter = footerFrame.findViewById(R.id.footer_group);
		
		mScreenFooter = v.findViewById(R.id.footer_group);
		mKitkatFooter = v.findViewById(R.id.kitkat_footer);

		return v;
	}

	private void updateFooter() {
		TextView listFooterText = null;

		TextView screenFooterText = (TextView) mScreenFooter
				.findViewById(R.id.listview_footer);
		if (mListFooter != null) {
			listFooterText = (TextView) mListFooter
					.findViewById(R.id.listview_footer);
		}

		int listSize = mAction.countChildren();
		if (listSize == 0) {
			screenFooterText.setText(null);
			if (listFooterText != null)
				listFooterText.setText(null);
		} else {
			screenFooterText.setText(String.valueOf(listSize)
					+ " items in this list");
			if (listFooterText != null)
				listFooterText.setText(String.valueOf(listSize)
						+ " items in this list");
		}
		
		TextView completedToday = (TextView) mScreenFooter.findViewById(R.id.completed_today);
		TextView listCompletedToday = (TextView) mListFooter.findViewById(R.id.completed_today);
		
		completedToday.setText(String.valueOf(String.valueOf(mAction.getCompletedToday() + " completed today")));
		listCompletedToday.setText(String.valueOf(String.valueOf(mAction.getCompletedToday() + " completed today")));

//		Hide done button if this task has unfinished subtasks
		if (!(mAction.getChildren().get(0).isEmpty()) || mAction.isRoot() || mAction.hasPendingTasks() ) {
			fDoneButton.setVisibility(View.INVISIBLE);
			fDoneText.setVisibility(View.INVISIBLE);
		} else {
			fDoneButton.setVisibility(View.VISIBLE);
			fDoneText.setVisibility(View.VISIBLE);
		}
	}

	private void updateNewItemHint(View v) {
		TextView hint = (TextView) v
				.findViewById(R.id.new_action_hint_background);
		hint.setText((mAction.equals(mActionLab.getRoot())) ? "Add new item"
				: "Add new subtask");
	}

	private void initializeSubtaskField(View v) {

		mNewSubtaskButton = (ImageButton) v.findViewById(R.id.new_subtask_icon);

		mNewSubtaskButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mSubtaskTitle == null || mSubtaskTitle.equals("")) {
					mSubtaskField.requestFocus();
					mCallbacks.updateOnScreenKeyboard(mSubtaskField,
							View.VISIBLE);
				} else {
					saveNewSubtask();
				}
			}
		});

		updateNewItemHint(v);

		mSubtaskField = (EditText) v.findViewById(R.id.new_subtask);

		mSubtaskField.setText(null);
		mSubtaskField.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence c, int start, int before,
									  int count) {

				if (c.length() > 0) {
					// position the text type in the left top corner
					mSubtaskField.setBackgroundColor(Color.WHITE);
				} else {
					// no text entered. Center the hint text.
					mSubtaskField.setBackgroundColor(Color.TRANSPARENT);
				}
				try {
					mSubtaskTitle = c.toString();
					Log.d(TAG, c.toString() + " entered");
				} catch (Exception e) {
					// do nothing;
				}
				// mChangesMade = true;
			}

			public void beforeTextChanged(CharSequence c, int start, int count,
										  int after) {
				// This space intentionally left blank
			}

			public void afterTextChanged(Editable c) {
				// This also left blank
			}
		});

		mSubtaskField
				.setOnEditorActionListener(new EditText.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView v, int actionId,
												  KeyEvent event) {
						if (actionId == EditorInfo.IME_ACTION_SEARCH
								|| actionId == EditorInfo.IME_ACTION_DONE) {
							/*
							 * event.getAction() == KeyEvent.ACTION_DOWN &&
							 * event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
							 */
							if (mSubtaskTitle != null
									&& !mSubtaskTitle.equals("")) {
								saveNewSubtask();
							}
							mCallbacks.updateOnScreenKeyboard(getView(),
									View.INVISIBLE);

							return true;
						}
						return false;
					}
				});
		mSubtaskField.setOnKeyListener(new View.OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN)
						&& (keyCode == KeyEvent.KEYCODE_ENTER)) {
					if (mSubtaskTitle != null && !mSubtaskTitle.equals("")) {
						saveNewSubtask();
					}
					mCallbacks
							.updateOnScreenKeyboard(getView(), View.INVISIBLE);

				}
				// Returning false allows other listeners to react to the press.
				return false;
			}
		});

	}

	private void saveNewSubtask() {
        Action a = createActionFromInputString();

		// Reset new subtask field
		mSubtaskTitle = null;
		mSubtaskField.setText(null);

		// Toast
		String toastText = getResources().getString(R.string.save_toast);
		Toast.makeText(getActivity(), toastText, Toast.LENGTH_SHORT).show();

		//Update view without checking all pending actions and creating a new adapter
		if(mActionViewMode == Action.TOP_FIVE_VIEW){
         if(mAdapter.getCount() < 6) {
             mAdapter.add(a);
         } else {
             mAdapter.remove(mAdapter.getItem(5));
             mAdapter.add(a);
         }
		}		
		mAdapter.notifyDataSetChanged();
        updateDoneButtonVisibility();
		updateFooter();
	}

    private Action createActionFromInputString(){
        Action a = mActionLab.createActionIn(mAction);

        List<Entity> e = mTextExtractor.extractMentionedScreennamesWithIndices(mSubtaskTitle);

        if(e == null || e.isEmpty()) {
            a.setTitle(mSubtaskTitle);
            return a;
        }

        Entity context = e.get(0);
        a.setContextName(mSubtaskTitle.substring(context.getStart() + 1, context.getEnd()));

        StringBuilder hashlessTitle = new StringBuilder();
        if(context.getStart() > 0) hashlessTitle.append(mSubtaskTitle.substring(0, context.getStart() -1));
        if(context.getEnd() < mSubtaskTitle.length())
            hashlessTitle.append(mSubtaskTitle.substring(context.getEnd() + 1, mSubtaskTitle.length()-1));
        mSubtaskTitle = hashlessTitle.toString();

        return a;
    }

	protected class ActionAdapter extends ArrayAdapter<Action> {



		public ActionAdapter(ArrayList<Action> Actions) {
			super(getActivity(), 0, Actions);

		}

		public View getView(int position, View convertView, ViewGroup parent) {

			// Inflate new view if none exists
			if (convertView == null) {
				convertView = getActivity().getLayoutInflater().inflate(
						R.layout.list_item_action, null);
			}

			Action c = getItem(position);
			TextView titleTextView = (TextView) convertView
					.findViewById(R.id.action_list_item_titleTextView);
			TextView outcomeTextView = (TextView) convertView
					.findViewById(R.id.action_list_outcome);

            outcomeTextView.setText(c.getFirstSubtaskPath());

			if (c.getFirstSubAction() != null) {
				titleTextView.setText(mActionLab.preview(c).getTitle());
			} else {
				titleTextView.setText(c.getTitle());
			}

			int actionStatus = c.getActionStatus();

			TextView dateTextView = (TextView) convertView
					.findViewById(R.id.action_list_item_dateTextView);

			switch (actionStatus) {
			case Action.PENDING:
				titleTextView.setTextColor(Color.GRAY);
				titleTextView.setPaintFlags(titleTextView.getPaintFlags()
						& ~Paint.STRIKE_THRU_TEXT_FLAG);

				if (c.getStartDate() != null) {
					dateTextView.setText(new StringBuilder("Starts ")
							.append(android.text.format.DateFormat.format(
                                    "MMM dd", c.getStartDate()).toString()));
				} else {
					dateTextView.setText(null);
				}

				break;
			default:
				titleTextView.setPaintFlags(titleTextView.getPaintFlags()
						& ~Paint.STRIKE_THRU_TEXT_FLAG);
				titleTextView.setTextColor(Color.BLACK);

				if (c.getDueDate() != null) {
					dateTextView.setText(new StringBuilder("Due ")
							.append(android.text.format.DateFormat.format(
									"MMM dd", c.getDueDate()).toString()));
				} else {
					dateTextView.setText(null);
				}
				break;
			}

            if(c.getRepeatNumber() != 0) dateTextView.setText(
                    dateTextView.getText().toString().concat("↻")
            );


            TextView minutesToComplete = (TextView) convertView
					.findViewById(R.id.action_list_minutes_to_complete);

			int minutes = c.getMinutesExpected();
			minutesToComplete.setText((minutes == 0) ? null : String
					.valueOf(minutes));

			TextView context = (TextView) convertView
					.findViewById(R.id.action_list_context);

			if (mActionLab.preview(c).getContextName().equals("")
					|| c.getContextName() == null)
				context.setText(null);
			else
				context.setText("@" + mActionLab.preview(c).getContextName());

			TextView pinned = (TextView) convertView
					.findViewById(R.id.pinned_indicator);
			if (c.isPinned())
				pinned.setText("✪   ");
			else
				pinned.setText(null);

			initializeSwipeButtons(position, convertView, parent);

			return convertView;
		}

		public void initializeSwipeButtons(int position, View v,
				ViewGroup parent) {
			final int pos = position;

			ImageButton topButton = (ImageButton) v
					.findViewById(R.id.top_button);
			ImageButton bottomButton = (ImageButton) v
					.findViewById(R.id.bottom_button);
			ImageButton cancelButton = (ImageButton) v
					.findViewById(R.id.cancel_button);
			ImageButton delayButton = (ImageButton) v
					.findViewById(R.id.delay_button);

			topButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final int position = mListView.getPositionForView((View) v
							.getParent());
//					Action a = mAdapter.getItem(position);
					mReordCtrl.moveWithinAdapter(mAdapter, position, 0);
//                    a.setPinned(true);

					String toastText = "Moved to Top";
					Toast.makeText(getActivity(), toastText, Toast.LENGTH_SHORT).show();
				}
			});

			bottomButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					final int position = mListView.getPositionForView((View) v
							.getParent());
					mReordCtrl.moveToEnd(mAdapter, position);

					String toastText = "Moved to Bottom";
					Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
				}
			});

			//TODO: Verify implementation of cancel
			cancelButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					Log.d(TAG, "Click registered");

//					final int position = mListView.getPositionForView((View) v
//							.getParent());
//					mReordCtrl.removeAction(mAdapter, position);
					Action a = mAdapter.getItem(pos);
//					mActionLab.changeActionStatus(a, Action.COMPLETE);
//					toAncestor();


					if(a.isRoot()){
						Log.e("DSLV", "Tried to remove root");
						return;
					}
					//Don't remove tasks with unfulfilled subtasks
					if (!(a.getChildren().get(0).isEmpty()) ) {
//						mDoneButton.setVisibility(View.INVISIBLE);
						String toastText = "Unfinished subtasks!";
						Toast.makeText(getActivity(), toastText, Toast.LENGTH_SHORT).show();
						return;
					}

					removeAction(a);
					updateListToShowCurrentAction();

					String toastText = "Task completed";
					Toast.makeText(getActivity(), toastText, Toast.LENGTH_SHORT).show();
				}
			});

			delayButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					childAction = mAdapter.getItem(pos);
//					Show date-time picker
					String DIALOG_DATE = "date";
					FragmentManager fm = getActivity().getSupportFragmentManager();
					DatePickerDialog datePicker = DatePickerMaker.getDatePicker(ActionListFragmentDSLV.this);
					datePicker.show(fm, DIALOG_DATE);
					updateFooter();
				}
			});

		}
	}

	public void removeAction(Action toDelete) {
		ActionLab anActionLab = ActionLab.get(getActivity());
		anActionLab.changeActionStatus(toDelete, Action.COMPLETE);
//		If mAction goes pending, return to nearest non-pending ancestor
		while (mAction.isPending() && !mAction.isRoot()) {
			mAction = mAction.getParent();
			mAction.incrementCompleted();
		}

	}


//	public void toAncestor() {
////		mActionLab.checkForPendingActions();
//		refreshView();
////		If mAction goes pending, return to nearest non-pending ancestor
//		while (mAction.isPending() && !mAction.isRoot()) {
//			mAction = mAction.getParent();
//			mAction.incrementCompleted();
//		}
//	}

	public void onPause() {
		super.onPause();
		mActionLab.saveActions();
		if(mActionLab.dropboxLinked()) mActionLab.saveToDropbox(ActionLab.AUTOSAVE_FILENAME);
	}

}