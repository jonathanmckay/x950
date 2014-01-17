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
	            item.moveWithinList(item.getActionStatus(), from, to);
	            
	            adapter.remove(item);
	            adapter.insert(item, to); 
	            
	            adapter.notifyDataSetChanged();
	        }
	}
	public void moveToEnd(ActionAdapter adapter, int from){
		Action item = adapter.getItem(from);
		int end = item.getContainingList().size() - 1;
				
		if(from != end){				
			if(adapter.getCount() == item.getContainingList().size()){
				moveWithinAdapter(adapter, from, end);
			} else {
				adapter.remove(item);
				showNextAction(adapter);
			}
		}
		
		adapter.notifyDataSetChanged();
	}

	public void changeActionStatus(ActionAdapter adapter, int position, int newStatus){
		Action item = adapter.getItem(position);
		int itemOrigStatus = item.getActionStatus();
    	
    	if(item.hasActiveTasks()){
    		//Update the status of the subtask
    		Action subItem = mActionLab.preview(item);
    		
    		//This is the part that will change, throw down a switch
    		//Delete, Wishlist, skip, pin
    		mActionLab.changeActionStatus(subItem, newStatus);
    		
    		//If the parent action still has not changed, move it to the end
    		//Update the project location in the list
    		if(!item.isPinned() && (item.getActionStatus() == itemOrigStatus)){
    		moveToEnd(adapter, position);
    		} else if (item.getActionStatus() != itemOrigStatus){
    			showNextAction(adapter);
    		}
    		
    	} else {
    		adapter.remove(item);
    		mActionLab.changeActionStatus(item,  newStatus);
    		showNextAction(adapter);
    	}
    	
    	adapter.notifyDataSetChanged();
    	
    	
	}
	
	public void showNextAction(ActionAdapter adapter){
		int count = adapter.getCount();
		if(count == 0) return;
		
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
