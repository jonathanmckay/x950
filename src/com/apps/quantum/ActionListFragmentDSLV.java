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
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
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
	private String mSubtaskTitle;
	
	private DbxAccountManager mDbxAcctMgr;
	private int mActionViewMode;
	private DragSortListView mListView;
	private ActionAdapter mAdapter;
	private Callbacks mCallbacks;

	private EditText mSubtaskField;
	
	public interface Callbacks{
		void onActionSelected(Action a);
		void onDetailViewToggled(Action a);
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
		mAction = mActionLab.getRoot(); 
		mActionViewMode = Action.TOP_FIVE_ACTIONS_VIEW;
		mSubtaskTitle = null;
		setHasOptionsMenu(true);
		setRetainInstance(true);
		
		
	}
	
	private void updateAdapter(){
		mListView = (DragSortListView) getActivity().findViewById(R.id.listview);
		
		mAdapter = new ActionAdapter(mAction.getActions(mActionViewMode));
		mAdapter.notifyDataSetChanged();
		mListView.setAdapter(mAdapter);
				
		mListView.setDropListener(onDrop);
	    mListView.setRemoveListener(onRemove);
	    mListView.setOnItemClickListener(onClick);

	    DragSortController controller = new DragSortController(mListView);
	    controller.setDragHandleId(R.id.action_drag_handle);
	    controller.setRemoveEnabled(true);
	    controller.setSortEnabled(true);
	    controller.setDragInitMode(1);
	    mListView.setFloatViewManager(controller);
	    mListView.setOnTouchListener(controller);
	    mListView.setDragEnabled(true);
		
	}
	private void updateAdapterInfo(){
		
	}
	
	private DragSortListView.DropListener onDrop = new DragSortListView.DropListener()
	{
	    @Override
	    public void drop(int from, int to)
	    {
	        if (from != to)
	        {
	            Action item = mAdapter.getItem(from);
	            mAction.moveWithinList(Action.INCOMPLETE, from, to);
	            
	            mAdapter.remove(item);
	            mAdapter.insert(item, to); 
	            
	            mAdapter.notifyDataSetChanged();
	            
	        }
	    }
	};
	
	private DragSortListView.RemoveListener onRemove = new DragSortListView.RemoveListener()
	{
	    @Override
	    public void remove(int position)
	    {
	    	Action a = mAdapter.getItem(position);
	    	while(a.hasActiveTasks()){
	    		a = a.peekStep();
	    	}
	    	
	    	mActionLab.changeActionStatus(a, Action.COMPLETE);
	    	mAdapter = new ActionAdapter(mAction.getActions(mActionViewMode));
	    	mAdapter.notifyDataSetChanged();
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
		setSubtitle();
		//Log.d(TAG, " Set List mAdapter to " + mAction.getTitle());
		
		updateHomeButton();
		return;
	}
	private void setTitle(){
		if(mAction.equals(mActionLab.getRoot())){
			if(mActionViewMode == Action.TOP_FIVE_ACTIONS_VIEW){
				getActivity().setTitle(R.string.app_name);
			} else {
				getActivity().setTitle("What's Next");
			}
		} else {
			getActivity().setTitle(mAction.getTitle());
		}
	}
	private void setSubtitle(){
		if(mActionViewMode == Action.ALL_ACTIONS_VIEW){
			getActivity().getActionBar().setSubtitle(R.string.subtitle);
		} else {
			getActivity().getActionBar().setSubtitle(null);
		}
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
		
		if(mAction.equals(mActionLab.getRoot())) {
		    menu.findItem(R.id.menu_item_detail_toggle).setVisible(false);
		} else {
		   menu.findItem(R.id.menu_item_detail_toggle).setVisible(true);
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
            
		case R.id.menu_item_dropbox:
			if(mDbxAcctMgr.hasLinkedAccount()){ 
				mActionLab.syncDropBox(mDbxAcctMgr);
				mAction = mActionLab.getRoot();
				Toast.makeText(getActivity(), "DBX Linked", Toast.LENGTH_LONG).show();
				updateAdapter();
			
			} else {
				mDbxAcctMgr.startLink(getActivity(), REQUEST_LINK_TO_DBX);
			}
			return true;
			
		case R.id.menu_item_remove_all:
			mActionLab.deleteAllActions();
			updateAdapter();
			return true;
			
		case R.id.menu_item_toggle_completed:
			mActionViewMode = (mActionViewMode != Action.ALL_ACTIONS_VIEW) 
				? Action.ALL_ACTIONS_VIEW
				: Action.INCOMPLETE_ACTIONS_VIEW;
			
			getActivity().getActionBar().setSubtitle((mActionViewMode == Action.ALL_ACTIONS_VIEW)
					? "Showing all actions" : null);
			updateAdapter();
			
			Log.d(TAG, " View All Actions was Toggled");
			return true;
			
		case R.id.menu_item_detail_toggle:
			mCallbacks.onDetailViewToggled(mAction);
			return true;
		
		case R.id.menu_item_next_five:
			mActionViewMode = Action.TOP_FIVE_ACTIONS_VIEW;
			updateAdapter();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	//Returns whether at root or not. 
	protected int onBackPressed() {
		if(mAction == mActionLab.getRoot()){
			return -1;
		} else {
			navigateUp();
			return 0;
		}
    }   
	
	private void navigateUp(){
		mAction = mAction.getParent();
		if(mAction == null) mAction = mActionLab.getRoot();
		updateListToShowCurrentAction();
	}
	
	public void updateHomeButton(){
		if(!mAction.equals(mActionLab.getRoot())){
			getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
		}
		else{
			getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
		}
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState){
		View v = (inflater.inflate(R.layout.fragment_action_list, parent, false));
		
		updateHomeButton();
		initializeSubtaskField(v);
		
		return v;
	}
	private void initializeSubtaskField(View v){
		mSubtaskField = (EditText)v.findViewById(R.id.new_subtask);
		
		mSubtaskField.setText(null);
		mSubtaskField.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence c, int start, int before, 
					int count) {
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
		Action a = mActionLab.createActionIn(mAction);
		a.setTitle(mSubtaskTitle);
		mSubtaskTitle = null;
		mSubtaskField.setText(null);
	}
	
	private class ActionAdapter extends ArrayAdapter<Action>{
		
		public ActionAdapter(ArrayList<Action> Actions){
			super(getActivity(),0,Actions);
			
		}
		
		public View getView(int position, View convertView, ViewGroup parent){
			
			//Inflate new view if none exists
			if(convertView == null){
				convertView = getActivity().getLayoutInflater().inflate(R.layout.list_item_action, null);
			}
			
			Action c = getItem(position);
			TextView titleTextView = (TextView)convertView.findViewById(R.id.action_list_item_titleTextView);
			
			if(c.peekStep() != null){
				titleTextView.setText(c.getTitle() + " -> " + c.peekStep().getTitle());
			}else{
				titleTextView.setText(c.getTitle());
			}
			int actionStatus = c.getActionStatus();
			switch(actionStatus){
				case Action.COMPLETE:
					titleTextView.setPaintFlags(
					titleTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
					titleTextView.setTextColor(Color.BLACK);
					
					break;
				case Action.PENDING:
					titleTextView.setTextColor(Color.GRAY);
					titleTextView.setPaintFlags(
							titleTextView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
					break;
				default:
					titleTextView.setPaintFlags(
					titleTextView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
					titleTextView.setTextColor(Color.BLACK);
						break;
			}
			
			TextView dateTextView = (TextView)convertView.findViewById(R.id.action_list_item_dateTextView);
			dateTextView.setText(android.text.format.DateFormat.format("MM.dd", c.getCreatedDate()).toString());
			
			TextView minutesToComplete = (TextView)convertView.findViewById(R.id.action_list_minutes_to_complete);
			minutesToComplete.setText(String.valueOf(c.getMinutesExpected()));
			
			TextView context = (TextView)convertView.findViewById(R.id.action_list_context);
			context.setText(c.getContextName());
			
			return convertView;
		}
		
	}
	
	public void onPause() {
		super.onPause();
		mActionLab.saveActions();
	}
    
	
}