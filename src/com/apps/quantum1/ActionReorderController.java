package com.apps.quantum1;

import android.app.Activity;

import com.apps.quantum1.ActionListFragmentDSLV.ActionAdapter;

import java.util.ArrayList;

public class ActionReorderController {
	private ActionLab mActionLab;
	private static ActionReorderController sReorderController;
	
	
	private ActionReorderController(Activity c){
		mActionLab = ActionLab.get(c);
	}
	public static ActionReorderController get(Activity c){
		 if(sReorderController == null){
			 sReorderController = new ActionReorderController(c);
     }
     return sReorderController;
	}
	
	public void moveWithinAdapter(ActionAdapter adapter, int from, int to){
        if (from == to) return;

        Action item = adapter.getItem(from);
        item.moveWithinList(from, to);
        adapter.remove(item);
        adapter.insert(item, to);
        adapter.notifyDataSetChanged();
	}
	public void moveToEnd(ActionAdapter adapter, int from){
		Action item = adapter.getItem(from);
		int end = item.getContainingList().size() - 1;
				
		if(from != end){				
			if(adapter.getCount() == item.getContainingList().size()){
				moveWithinAdapter(adapter, from, end);
			} else {
				item.moveWithinList(from, end);
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
    		
    		
    		mActionLab.changeActionStatus(subItem, newStatus);
    		
    		//If the parent action still has not changed, move it to the end
    		//Update the project location in the list
    		if(!item.isPinned() && (item.getActionStatus() == itemOrigStatus)){
    		//moveToEnd(adapter, position);
    		//showNextAction(adapter);
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
    	Action item = adapter.getItem(position);
    	if(item.hasActiveTasks()){
    		//Update the status of the subtask
    		Action toDelete = mActionLab.preview(item);
    		mActionLab.deleteAction(toDelete);
    		
    	} else {
    		mActionLab.deleteAction(item);
    		adapter.remove(item);
    		showNextAction(adapter);
    	}
    	
    	adapter.notifyDataSetChanged();
    }

	
}
