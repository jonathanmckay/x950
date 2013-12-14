package com.apps.quantum;

import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
	
	private DbxAccountManager mDbxAcctMgr;
	private boolean mAllActionsVisible;
	private DragSortListView mListView;
	private ActionAdapter mAdapter;
	private Callbacks mCallbacks;
	
	public interface Callbacks{
		void onActionSelected(Action a);
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
		mAllActionsVisible = false;
		
		setHasOptionsMenu(true);
		getActivity().setTitle(R.string.actions_title);
		setRetainInstance(true);
		
		//getActivity().setContentView(R.layout.activity_threepane);
		
		//updateAdapter();
	}
	
	private void updateAdapter(){
		getActivity().setContentView(R.layout.activity_threepane);
		mListView = (DragSortListView) getActivity().findViewById(R.id.listview);
		
		mAdapter = new ActionAdapter(mAction.getActions(mAllActionsVisible));
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
	private void refreshAdapter(){
		mAdapter = new ActionAdapter(new ArrayList<Action>(mAction.getActions(mAllActionsVisible)));
		mAdapter.notifyDataSetChanged();
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
	            mAdapter.notifyDataSetChanged();
	            /*
	             * mAdapter.remove(item);
	             * mAdapter.insert(item, to);
	             */
	            
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
		 }
	};
	
	private void updateListToShowCurrentAction(){
		mCallbacks.onActionSelected(mAction);
		
		Log.d(TAG, mAction.getTitle() + " is now the focus");
		
		updateAdapter();
		getActivity().setTitle(mAction.getTitle());
		if(mAllActionsVisible){
			getActivity().getActionBar().setSubtitle(R.string.subtitle);
		}
		
		Log.d(TAG, " Set List mAdapter to " + mAction.getTitle());
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
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		
		getActivity().getMenuInflater().inflate(R.menu.action_list_item_context, menu);
	}

	
	@TargetApi(11)
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()){
		case android.R.id.home:
			navigateUp();
			updateAdapter();
            return true;
            
		case R.id.menu_item_new_action:
			mAction = mActionLab.createActionIn(mAction);
			updateListToShowCurrentAction();
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
			mAllActionsVisible = !mAllActionsVisible;
			getActivity().getActionBar().setSubtitle((mAllActionsVisible)
					? "Showing all actions" : null);
			updateAdapter();
			
			Log.d(TAG, " View All Actions was Toggled");
			return true;
			
		case R.id.menu_item_backup:
			Intent intent = new Intent(getActivity(), DSLVActivity.class);
			startActivity(intent);		
			
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
		getActivity().setTitle(mAction.getParent().getTitle());
		mAction = mAction.getParent();
		mCallbacks.onActionSelected(mAction);
		updateAdapter();
		
	}
	
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState){
		View v = (inflater.inflate(R.layout.fragment_action_list, parent, false));
		getActivity().getActionBar().setDisplayHomeAsUpEnabled(true);
		
		return v;
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