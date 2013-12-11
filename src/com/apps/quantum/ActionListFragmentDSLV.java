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
		adapter.notifyDataSetChanged();
	}

	DragSortListView listView;
	ActionAdapter adapter;
	
	

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
		
		initializeAdapter();
	}
	
	private void initializeAdapter(){
	    listView = (DragSortListView) getActivity().findViewById(R.id.listview);
	    
	    adapter = new ActionAdapter(mAction.getActions(mAllActionsVisible));
	    
	    listView.setAdapter(adapter);
	    listView.setDropListener(onDrop);
	    listView.setRemoveListener(onRemove);
	    listView.setOnItemClickListener(onClick);

	    DragSortController controller = new DragSortController(listView);
	    controller.setDragHandleId(R.id.action_drag_handle);
	    controller.setRemoveEnabled(true);
	    controller.setSortEnabled(true);
	    controller.setDragInitMode(1);
	    listView.setFloatViewManager(controller);
	    listView.setOnTouchListener(controller);
	    listView.setDragEnabled(true);
	}
	
	private void updateAdapter(){
		listView = (DragSortListView) getActivity().findViewById(R.id.listview);
		
		adapter = new ActionAdapter(mAction.getActions(mAllActionsVisible));
		
		listView.setAdapter(adapter);
		
		listView.setDropListener(onDrop);
	    listView.setRemoveListener(onRemove);
	    listView.setOnItemClickListener(onClick);

	    DragSortController controller = new DragSortController(listView);
	    controller.setDragHandleId(R.id.action_drag_handle);
	    controller.setRemoveEnabled(true);
	    controller.setSortEnabled(true);
	    controller.setDragInitMode(1);
	    listView.setFloatViewManager(controller);
	    listView.setOnTouchListener(controller);
	    listView.setDragEnabled(true);
		
	}
	
	private DragSortListView.DropListener onDrop = new DragSortListView.DropListener()
	{
	    @Override
	    public void drop(int from, int to)
	    {
	        if (from != to)
	        {
	            Action item = adapter.getItem(from);
	            adapter.remove(item);
	            adapter.insert(item, to);
	        }
	    }
	};
	
	private DragSortListView.RemoveListener onRemove = new DragSortListView.RemoveListener()
	{
	    @Override
	    public void remove(int position)
	    {
	    	Action a = adapter.getItem(position);
	    	while(a.hasActiveTasks()){
	    		a = a.peekStep();
	    	}
	    	
	    	mActionLab.changeActionStatus(a, Action.COMPLETE);
	        //adapter.remove(adapter.getItem(which));
	    	adapter.notifyDataSetChanged();
	    }
	};
	
	private AdapterView.OnItemClickListener onClick = new AdapterView.OnItemClickListener() 
	{
		 @Override
         public void onItemClick(AdapterView<?> arg0, View v, int position,
                 long arg3) {
             mAction = adapter.getItem(position);
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
		
		Log.d(TAG, " Set List adapter to " + mAction.getTitle());
	}


	@Override
	public void onResume(){
		super.onResume();
		getActivity().setContentView(R.layout.activity_threepane);
		initializeAdapter();
		
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
			updateUI();
            return true;
            
		case R.id.menu_item_new_action:
			Action action = new Action();
			mAction.adopt(action);
			mAction = action;
			
			updateListToShowCurrentAction();
			return true; 
		
		case R.id.menu_item_dropbox:
			if(mDbxAcctMgr.hasLinkedAccount()){ 
				ActionLab.get(getActivity()).syncDropBox(mDbxAcctMgr);
				Toast.makeText(getActivity(), "DBX Linked", Toast.LENGTH_LONG).show();
				updateAdapter();
			
			} else {
				mDbxAcctMgr.startLink(getActivity(), REQUEST_LINK_TO_DBX);
			}
			return true;
		case R.id.menu_item_remove_all:
			ActionLab.get(getActivity()).deleteAllActions();
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
		/*ListView listView = (ListView)v.findViewById(android.R.id.list);
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		listView.setMultiChoiceModeListener(new MultiChoiceModeListener(){
			public void onItemCheckedStateChanged(ActionMode mode, int position, 
					long id, boolean checked){
				//Reqd but not used here
			}
			public boolean onCreateActionMode(ActionMode mode, Menu menu){
				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.action_list_item_context, menu);
				return true;
			}
			public boolean onPrepareActionMode(ActionMode mode, Menu menu){
				return false;
			}
			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch (item.getItemId()) {
				case R.id.menu_item_delete_action:
					ActionAdapter adapter = (ActionAdapter)getListAdapter();
					ActionLab actionLab = ActionLab.get(getActivity());
					for(int i = adapter.getCount() - 1; i >= 0; i--){
						if(getListView().isItemChecked(i)) {
							actionLab.deleteAction(adapter.getItem(i));
						}
					}
					mode.finish();
					adapter.notifyDataSetChanged();
					return true;
				default:
					return false;	
				}
			}
			@Override
			public void onDestroyActionMode(ActionMode arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		        
		*/
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