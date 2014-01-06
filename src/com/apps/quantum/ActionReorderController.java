package com.apps.quantum;

import java.util.ArrayList;

import android.content.Context;

import com.apps.quantum.ActionListFragmentDSLV.ActionAdapter;

public class ActionReorderController {
	private ActionLab mActionLab;
	private static ActionReorderController sReorderController;
	
	
	private ActionReorderController(Context c){
		mActionLab = ActionLab.get(c);
	}
	public static ActionReorderController get(Context c){
		 if(sReorderController == null){
			 sReorderController = new ActionReorderController(c.getApplicationContext());
     }
     return sReorderController;
	}
	
	public void moveWithinAdapter(ActionAdapter adapter, int from, int to){
		 if (from != to)
	        {
	            Action item = adapter.getItem(from);
	            Action parent = item.getParent();
	            
	            parent.moveWithinList(Action.INCOMPLETE, from, to);
	            
	            adapter.remove(item);
	            adapter.insert(item, to); 
	            
	            adapter.notifyDataSetChanged();
	        }
	}
	public void moveToEnd(ActionAdapter adapter, int from){
		Action item = adapter.getItem(from);
		int end = item.getContainingList().size() - 1;
		
		moveWithinAdapter(adapter, from, end);
	}

	public void changeActionStatus(ActionAdapter adapter, int position, int newStatus){
		boolean actionMoved = false;
    	Action a = adapter.getItem(position);
    	
    	if(a.hasActiveTasks()){
    		//Update the status of the subtask
    		Action b = mActionLab.preview(a);
    		
    		//This is the part that will change, throw down a switch
    		//Delete, Wishlist, skip, pin
    		mActionLab.changeActionStatus(b, newStatus);
    		
    		//Update the project location in the list
    		if(!a.isPinned()){
    			a.getParent().moveToEnd(a.getActionStatus(), position);
    			adapter.remove(a);
    			actionMoved = true;
    		}
    		
    	} else {
    		adapter.remove(a);
    		mActionLab.changeActionStatus(a,  newStatus);
    		actionMoved = true;
    	}
    	
    	if(actionMoved) showNextAction(adapter);
    	    	
    	adapter.notifyDataSetChanged();
	}
	
	public void showNextAction(ActionAdapter adapter){
		int count = adapter.getCount();
		Action firstItem = adapter.getItem(0);
    	    	
    	ArrayList<Action> fullList = 
    			firstItem.getParent().getActions(firstItem.getActionStatus());
    	
    	if(count < fullList.size()){
    		adapter.insert(fullList.get(count), count);
    	}
	}
    	
    public void removeAction(ActionAdapter adapter, int position){
    	Action actionToDelete = adapter.getItem(position);
		
		mActionLab.deleteAction(actionToDelete);
		adapter.remove(actionToDelete);
		
		showNextAction(adapter);
    }

	
}
