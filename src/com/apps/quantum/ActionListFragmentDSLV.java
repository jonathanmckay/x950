package com.apps.quantum;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.sync.android.DbxAccountManager;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;


public class ActionListFragmentDSLV extends Fragment {
	private static final String TAG = "ActionListFragment";
	static final int REQUEST_LINK_TO_DBX = 0;
	private Action mAction;
	private ActionLab mActionLab;
	private ActionReorderController mReordCtrl;
	private String mSubtaskTitle;
	private MenuItem mToggleMenuItem;
	private MenuItem mEditTitle;
	
	private DbxAccountManager mDbxAcctMgr;
	private int mActionViewMode;
	private DragSortListView mListView;
	private ActionAdapter mAdapter;
	private Callbacks mCallbacks;
	private boolean mDetailVisible;

	private EditText mSubtaskField;
	private EditText mTitleEdit;
	
	public interface Callbacks{
		void onActionSelected(Action a);
		void onDetailViewToggled(Action a);
		void closeOnScreenKeyboard(View v);
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
	
	public void updateUI(){
		mAdapter.notifyDataSetChanged();
	}
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mDbxAcctMgr = DbxAccountManager.getInstance(getActivity().getApplicationContext(),
				//App key --------- App secret
				"588rm6vl0oom62h", "3m69jjskzcfcssn");
		
		mActionLab = ActionLab.get(getActivity());
		mReordCtrl = ActionReorderController.get(getActivity());
		
		mAction = mActionLab.getRoot(); 
		mActionViewMode = Action.TOP_FIVE_VIEW;
		mSubtaskTitle = null;
		
		setHasOptionsMenu(true);
		setRetainInstance(true);
		
		
	}
	
	
	private void updateAdapter(){
		boolean firstTimeOpening = false;
		if(mListView == null){
			firstTimeOpening = true;
			mListView = (DragSortListView) getActivity().findViewById(R.id.listview);
		}
		
		mActionLab.checkForPendingActions(mAction);
		mAdapter = new ActionAdapter(mAction.getActions(mActionViewMode));
		mAdapter.notifyDataSetChanged();
		mListView.setAdapter(mAdapter);
		
	
		if(firstTimeOpening){
			setListeners();
		}
	}
	
	private void setListeners(){
		mListView.setDropListener(onDrop);
	    mListView.setSwipeListener(onSwipe);
	    mListView.setOnItemClickListener(onClick);
	    DragSortController controller = new DragSortController(mListView, R.id.action_drag_handle, 
	    		DragSortController.ON_DOWN, DragSortController.FLING_REMOVE, 0, R.id.back);
	    controller.setDragHandleId(R.id.action_drag_handle);
	    controller.setSwipeEnabled(true);
	    controller.setSortEnabled(true);
	    controller.setDragInitMode(1);
	    mListView.setFloatViewManager(controller);
	    mListView.setOnTouchListener(controller);
	    mListView.setDragEnabled(true);
	}
	
	private DragSortListView.DropListener onDrop = new DragSortListView.DropListener()
	{
	    @Override
	    public void drop(int from, int to)
	    {
	    	mReordCtrl.moveWithinAdapter(mAdapter, from, to);
	    }
	};
	
	private DragSortListView.SwipeListener onSwipe = new DragSortListView.SwipeListener()
	{
	    @Override
	    public void swipe(int position)
	    {
	    	mReordCtrl.changeActionStatus(mAdapter, position, Action.COMPLETE);
	    }
	};
	
	private AdapterView.OnItemClickListener onClick = new AdapterView.OnItemClickListener() 
	{
		 @Override
         public void onItemClick(AdapterView<?> arg0, View v, int position,
                 long arg3) {
             mAction = mAdapter.getItem(position);
     		 updateListToShowCurrentAction();   
     		 return;
		 }
	};
	
	private void updateListToShowCurrentAction(){
		mCallbacks.onActionSelected(mAction);
		//Log.d(TAG, mAction.getTitle() + " is now the focus");
		
		updateAdapter();
		
		getActivity().invalidateOptionsMenu();
		
		setTitle();
		updateTitleEdit();
		//Log.d(TAG, " Set List mAdapter to " + mAction.getTitle());
		
		return;
	}
	
	
	
	@Override
	public void onResume(){
		super.onResume();
		updateAdapter();
		
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.fragment_action_list, menu);
		
		mToggleMenuItem = menu.findItem(R.id.menu_item_detail_toggle);
		mEditTitle= menu.findItem(R.id.add);
		
		updateTitleEdit();
		
		if(mAction.equals(mActionLab.getRoot())) {
		   mToggleMenuItem.setVisible(false);
		   mEditTitle.setVisible(false);
		   
		} else {
			mToggleMenuItem.setVisible(true);
			
			
			
			if(mAction.hasChildren()){
				mDetailVisible = false;
			} else {
				mDetailVisible = true;
			}
		
			updateDetailsToggle();
		}
		
	}
	
	private void updateTitleEdit(){
		mTitleEdit = (EditText) mEditTitle.getActionView().findViewById(R.id.title);
		mTitleEdit.setText(mAction.getTitle());
		
		mTitleEdit.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence c, int start, int before, 
					int count) {
				mActionLab.changeActionTitle(mAction, c.toString());
				
			}
			
			public void beforeTextChanged(CharSequence c, int start, int count, int after){
				//This space intentionally left blank
			}
			
			public void afterTextChanged(Editable c) {
				
			}
		});
	}
	
	
	private void setTitle(){
		if(mAction.equals(mActionLab.getRoot())){
			getActivity().setTitle("What's Next");
		} else if (mAction.hasChildren()){
			getActivity().setTitle(mAction.getTitle());
		} else {
			getActivity().setTitle(null);
		}
	}
	
	public void updateDetailsToggle(){
		if(mDetailVisible){
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
		switch(item.getItemId()){
		case android.R.id.home:
			if(mSubtaskTitle != null && !mSubtaskTitle.equals("")) saveNewSubtask();
			navigateUp();
			updateAdapter();
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
	
	//menuPosition is based on the Array in Strings
	public void filterView(int menuPosition){
		switch (menuPosition){
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
			updateAdapter();
			break;
		case 6: 
			if(mDbxAcctMgr.hasLinkedAccount()){ 
				mActionLab.syncDropBox(mDbxAcctMgr);
				mAction = mActionLab.getRoot();
				Toast.makeText(getActivity(), "DBX Linked", Toast.LENGTH_LONG).show();
				updateAdapter();
			
			} else {
				mDbxAcctMgr.startLink(getActivity(), REQUEST_LINK_TO_DBX);
			}
			break;
		default:
			break;
		}
		
		if(menuPosition >= 0 && menuPosition < 5){
			updateAdapter();
		}
		return;
		
	}
		
	
	protected Action onBackPressed() {
			navigateUp();
			return mAction;
    }   
	
	private void navigateUp(){
		mAction = mAction.getParent();
		if(mAction == null) mAction = mActionLab.getRoot();
		updateListToShowCurrentAction();
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState){
		View v = (inflater.inflate(R.layout.fragment_action_list, parent, false));
		
		initializeSubtaskField(v);
		
		return v;
	}
	private void initializeSubtaskField(View v){
		mSubtaskField = (EditText)v.findViewById(R.id.new_subtask);
		
		mSubtaskField.setText(null);
		mSubtaskField.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence c, int start, int before, 
					int count) {
				
				 if (c.length() > 0){
		               // position the text type in the left top corner
		               mSubtaskField.setBackgroundColor(Color.WHITE);
		          }else{
		               // no text entered. Center the hint text.
		        	  mSubtaskField.setBackgroundColor(Color.TRANSPARENT);
		          }
				try{
					mSubtaskTitle = c.toString();
					Log.d(TAG, c.toString() + " entered");
				}catch(Exception e){
					//do nothing;
				}
				//mChangesMade = true;
			}
			
			public void beforeTextChanged(CharSequence c, int start, int count, int after){
				//This space intentionally left blank
			}
			
			public void afterTextChanged(Editable c) {
				//This also left blank
			}
		});
		mSubtaskField.setOnFocusChangeListener(new OnFocusChangeListener() {          

	        public void onFocusChange(View v, boolean hasFocus) {
	            if(!hasFocus && mSubtaskTitle != null && !mSubtaskTitle.equals("")){
	            	saveNewSubtask();
	            }
	        }
	    });
		
		mSubtaskField.setOnEditorActionListener(new EditText.OnEditorActionListener(){
			 @Override
			    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			        if (actionId == EditorInfo.IME_ACTION_SEARCH ||
			                actionId == EditorInfo.IME_ACTION_DONE){
			        	/*event.getAction() == KeyEvent.ACTION_DOWN &&
				                event.getKeyCode() == KeyEvent.KEYCODE_ENTER) */
				            saveNewSubtask();
				            return true;			                
			        }
			        return false;
			    }
			});
		mSubtaskField.setOnKeyListener(new View.OnKeyListener() {
			
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
				        (keyCode == KeyEvent.KEYCODE_ENTER))
				    {
				        saveNewSubtask();// Done pressed!  Do something here.
				    }
				    // Returning false allows other listeners to react to the press.
				    return false;
			}
		});
		
	}
	private void saveNewSubtask(){
		
		//Create new subtask
		Action a = mActionLab.createActionIn(mAction);
		a.setTitle(mSubtaskTitle);
		
		//Reset new subtask field
		mSubtaskTitle = null;
		mSubtaskField.setText(null);
		mCallbacks.closeOnScreenKeyboard(getView());
				
		//Toast
		String toastText = getResources().getString(R.string.save_toast);
		Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();

		//Update view
		updateAdapter();
		
	}
	
	protected class ActionAdapter extends ArrayAdapter<Action>{
		
		public ActionAdapter(ArrayList<Action> Actions){
			super(getActivity(),0,Actions);
			
		}
		
		public View getView(int position, View convertView, ViewGroup parent){
			
			//Inflate new view if none exists
			if(convertView == null){
				convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item_action, null);
			}
			
			/*View backView = convertView.findViewById(R.id.back);
			backView.setVisibility(View.INVISIBLE);
			*/
			Action c = getItem(position);
			TextView titleTextView = (TextView)convertView.findViewById(R.id.action_list_item_titleTextView);
			TextView outcomeTextView = (TextView)convertView.findViewById(R.id.action_list_outcome);
			
			if(c.peekStep() != null){
				outcomeTextView.setText(c.getTitle() + "  ↴");
				titleTextView.setText(mActionLab.preview(c).getTitle());
			}else{
				titleTextView.setText(c.getTitle());
				outcomeTextView.setText(null);
			}
			
			int actionStatus = c.getActionStatus();
			TextView dateTextView = (TextView)convertView.findViewById(R.id.action_list_item_dateTextView);
			
			switch(actionStatus){
				case Action.COMPLETE:
					titleTextView.setPaintFlags(
					titleTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
					titleTextView.setTextColor(Color.BLACK);
					
					if(c.getModifiedDate() != null){
					dateTextView.setText(new StringBuilder("Finished ")
						.append(android.text.format.DateFormat.format("MMM dd", c.getModifiedDate()).toString()));
					} else {
						dateTextView.setText(null);
					}
					
					
					break;
				case Action.PENDING:
					titleTextView.setTextColor(Color.GRAY);
					titleTextView.setPaintFlags(
							titleTextView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
					
					if(c.getStartDate() != null){
					dateTextView.setText(new StringBuilder("Starts ")
					.append(android.text.format.DateFormat.format("MMM dd", c.getStartDate()).toString()));
					} else {
						dateTextView.setText(null);
					}
					
					break;
				default:
					titleTextView.setPaintFlags(
					titleTextView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
					titleTextView.setTextColor(Color.BLACK);
					
					if(c.getDueDate() != null){
					dateTextView.setText(new StringBuilder("Due ")
						.append(android.text.format.DateFormat.format("MMM dd", c.getDueDate()).toString()));
					} else {
						dateTextView.setText(null);
					}
					break;
			}
						
			TextView minutesToComplete = (TextView)convertView.findViewById(R.id.action_list_minutes_to_complete);
			
			int minutes = c.getMinutesExpected();
			minutesToComplete.setText((minutes == 0) ? null : String.valueOf(minutes));
			
			TextView context = (TextView)convertView.findViewById(R.id.action_list_context);
			context.setText(c.getContextName());
			if(c.getContextName() == "" || c.getContextName() == null) context.setText(null);
			
			TextView pinned = (TextView)convertView.findViewById(R.id.pinned_indicator);
			if(c.isPinned()) pinned.setText("✪   "); 
			else pinned.setText(null);
			
			initializeSwipeButtons(position, convertView, parent);
			
			return convertView;
		}
		
		public void initializeSwipeButtons(int position, View v, ViewGroup parent){
			
	
			ImageButton cancelButton = (ImageButton)v.findViewById(R.id.cancel_button);
			ImageButton skipButton = (ImageButton)v.findViewById(R.id.skip_button);
			ImageButton demoteButton = (ImageButton)v.findViewById(R.id.demote_button);
			ImageButton pinButton = (ImageButton)v.findViewById(R.id.pin_button);
			
			cancelButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					final int position = mListView.getPositionForView((View) v.getParent());
					mReordCtrl.removeAction(mAdapter, position);
				}
			});
			
			skipButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					final int position = mListView.getPositionForView((View) v.getParent());
					mReordCtrl.moveToEnd(mAdapter, position);
					
				}
			});
			
			demoteButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					final int position = mListView.getPositionForView((View) v.getParent());
					mReordCtrl.changeActionStatus(mAdapter, position, Action.WISHLIST);
				}
			});
			
			pinButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					final int position = mListView.getPositionForView((View) v.getParent());
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
	}
    
	
}