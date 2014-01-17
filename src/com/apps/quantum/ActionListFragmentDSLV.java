package com.apps.quantum;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

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

	private ImageButton mNewSubtaskButton;
	private EditText mSubtaskField;
	private EditText mTitleEdit;

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

		mAction = mActionLab.getRoot();
		mActionViewMode = Action.TOP_FIVE_VIEW;
		mSubtaskTitle = null;

		setHasOptionsMenu(true);
		setRetainInstance(true);

	}

	@SuppressLint("NewApi")
	private void refreshView() {
		mFirstTimeOpeningList = false;
		if (mListView == null) {
			mFirstTimeOpeningList = true;
			mListView = (DragSortListView) getActivity().findViewById(
					R.id.listview);

		}

		mActionLab.checkForPendingActions(mAction);
		mAdapter = new ActionAdapter(mAction.getActions(mActionViewMode));
		mAdapter.notifyDataSetChanged();
		mListView.setAdapter(mAdapter);

		if (mFirstTimeOpeningList) {
			setListeners();
		}

		mListView.post(new Runnable() {
			public void run() {

				if (mFirstTimeOpeningList) {

					View footerFrame = ((LayoutInflater) getActivity()
							.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
							.inflate(R.layout.kitkat_spacer, null, false);

					mListFooter = footerFrame.findViewById(R.id.footer_group);
				}

				int numItemsVisible = mListView.getLastVisiblePosition()
						- mListView.getFirstVisiblePosition();

				Log.d(TAG,
						"First Visible " + mListView.getFirstVisiblePosition());
				Log.d(TAG, "Last Visible " + mListView.getLastVisiblePosition());
				Log.d(TAG, "Adapter Visible " + mAdapter.getCount());

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

		updateFooter();
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
			mReordCtrl.changeActionStatus(mAdapter, position, Action.COMPLETE);
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
		getActivity().invalidateOptionsMenu();
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
			getActivity().setTitle("What's Next");
		} else if (mAction.hasChildren()) {
			getActivity().setTitle(mAction.getTitle());
		} else {
			getActivity().setTitle(null);
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

	// menuPosition is based on the Array in Strings
	public void filterView(int menuPosition) {
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
			mActionViewMode = Action.WISHLIST;
			break;
		case 4:
			mActionViewMode = Action.COMPLETE;
			break;
		case 5:
			mActionLab.deleteAllActions();
			break;
		case 6:
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

		switch (requestCode) {
		case REQUEST_LIST_REFRESH:
			mAction = mActionLab.getRoot();
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

		mScreenFooter = v.findViewById(R.id.footer_group);
		mKitkatFooter = v.findViewById(R.id.kitkat_footer);

		updateFooter();

		return v;
	}

	private void updateFooter() {
		/*
		 * mScreenFooter.setVisibility(View.GONE);
		 * mKitkatFooter.setVisibility(View.GONE);
		 * 
		 * if(mActionViewMode != Action.TOP_FIVE_VIEW){
		 * mScreenFooter.setVisibility(View.GONE);
		 * mKitkatFooter.setVisibility(View.GONE); } else{
		 * 
		 * mScreenFooter.setVisibility(View.VISIBLE); int listSize =
		 * mAction.getChildren().get(Action.INCOMPLETE).size(); if(listSize ==
		 * 0) mScreenFooter.setText(null); else{
		 * mScreenFooter.setText(String.valueOf(listSize) +
		 * " items in this list"); } }
		 */
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

		// Create new subtask
		Action a = mActionLab.createActionIn(mAction);
		a.setTitle(mSubtaskTitle);

		// Reset new subtask field
		mSubtaskTitle = null;
		mSubtaskField.setText(null);

		// Toast
		String toastText = getResources().getString(R.string.save_toast);
		Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();

		mAdapter.notifyDataSetChanged();
		updateFooter();

		// Update view
		// refreshView();

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

			if (c.getFirstSubAction() != null) {
				outcomeTextView.setText(c.getTitle() + "  ↴");
				titleTextView.setText(mActionLab.preview(c).getTitle());
			} else {
				titleTextView.setText(c.getTitle());
				outcomeTextView.setText(null);
			}

			int actionStatus = c.getActionStatus();

			TextView dateTextView = (TextView) convertView
					.findViewById(R.id.action_list_item_dateTextView);

			switch (actionStatus) {
			case Action.COMPLETE:
				titleTextView.setPaintFlags(titleTextView.getPaintFlags()
						| Paint.STRIKE_THRU_TEXT_FLAG);
				titleTextView.setTextColor(Color.BLACK);

				if (c.getModifiedDate() != null) {
					dateTextView.setText(new StringBuilder("Finished ")
							.append(android.text.format.DateFormat.format(
									"MMM dd", c.getModifiedDate()).toString()));
				} else {
					dateTextView.setText(null);
				}

				break;
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
			ImageButton cancelButton = (ImageButton) v
					.findViewById(R.id.cancel_button);
			ImageButton skipButton = (ImageButton) v
					.findViewById(R.id.skip_button);
			ImageButton demoteButton = (ImageButton) v
					.findViewById(R.id.demote_button);
			ImageButton pinButton = (ImageButton) v
					.findViewById(R.id.pin_button);

			cancelButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					Log.d(TAG, "Click registered");

					final int position = mListView.getPositionForView((View) v
							.getParent());
					mReordCtrl.removeAction(mAdapter, position);
				}
			});

			skipButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					final int position = mListView.getPositionForView((View) v
							.getParent());
					mReordCtrl.moveToEnd(mAdapter, position);

				}
			});

			demoteButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					final int position = mListView.getPositionForView((View) v
							.getParent());
					mReordCtrl.changeActionStatus(mAdapter, position,
							Action.WISHLIST);
					updateFooter();
				}
			});

			pinButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final int position = mListView.getPositionForView((View) v
							.getParent());
					Action a = mAdapter.getItem(position);
					a.setPinned(true);
					mReordCtrl.moveWithinAdapter(mAdapter, position, 0);
				}
			});
		}
	}

	public void onPause() {
		super.onPause();
		mActionLab.saveActions();
		mActionLab.saveToDropbox(ActionLab.AUTOSAVE_FILENAME);
	}

}