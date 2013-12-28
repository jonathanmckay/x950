package com.apps.quantum;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import android.content.Context;
import android.util.Log;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;

public class ActionLab{
        private static final String TAG = "ActionLab";
        private static final String FILENAME = "Actions.txt";
        private static final int COMPLETED = 1;
        private static final int NOT_COMPLETED = 0;
        
        private HashMap<UUID, Action> mActionHash;
        private HashMap<UUID, Action> mTempMap;
        
        private TitleMap mTitleHash;
        private class TitleMap extends HashMap<String, List<Action>> {
                   private static final long serialVersionUID = 1L;

                public void put(String key, Action a) {
                List<Action> current = super.get(key);
                if (current == null) {
                    current = new ArrayList<Action>();
                    super.put(key, current);
                }
                current.add(a);
            }
                
                public void remove(String key, Action a){
                        List<Action> current = super.get(key);
                        if(current != null && current.size() > 1){
                                for(int i = 0; i < current.size(); i++){
                                        if(current.get(i).equals(a)) current.remove(i);
                                }
                        }
                }
                
                //This may someday change to allow for conflict resolution
                public Action get(String key){
                        if(super.get(key) != null) return  super.get(key).get(0);
                        else return null;
                }
        }
        
        private static ActionLab sActionLab;
        private Context mAppContext;
        private Action mRoot;
        private OutcomeSerializer mSerializer;
        
        private ActionLab(Context appContext){
                mAppContext = appContext;
                mSerializer = new OutcomeSerializer(mAppContext, FILENAME);
                
                mActionHash = new HashMap<UUID, Action>();
                mTitleHash = new TitleMap();
                
                //This assumes that root is the first entry. and that root is the 
                // only self-referencing action
                
                try {
                        initializeRoot();
                        addAll(mSerializer.loadActions());
                }catch (Exception e) {
                        initializeRoot();
                        
                        Log.e(TAG, "Error loading Actions: ", e);
                        Action visibleAction = new Action();
                        visibleAction.setTitle("incompleteAction");
                        mRoot.adopt(visibleAction);
                        changeActionStatus(visibleAction, NOT_COMPLETED);
                        
                        
                        Action nonVisibleAction = new Action();
                        nonVisibleAction.setTitle("completedAction");
                        mRoot.adopt(nonVisibleAction);
                        changeActionStatus(nonVisibleAction, COMPLETED);
                }
        }
        //Singleton
        public static ActionLab get(Context c){
                if(sActionLab == null){
                        sActionLab = new ActionLab(c.getApplicationContext());
                }
                return sActionLab;
        }
        private void initializeRoot(){
                mRoot = new Action();
                mRoot.setParent(mRoot);
                mRoot.setTitle("root");
                mActionHash.put(mRoot.getId(), mRoot);
                mTitleHash.put(mRoot.getTitle(), mRoot);
                
                return;
        }
        public Action getRoot(){
                return mRoot;
        }
        
        public void syncDropBox(DbxAccountManager mDbxAcctMgr){
                try{
                        
                        DbxFileSystem dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
                        DbxFile testAddFile = dbxFs.open(new DbxPath(DbxPath.ROOT, "read-from.txt"));
                        
                        try{
                                String contents = testAddFile.readString();
                                Log.d("Dropbox Test", "File contents: " + contents);
                                addAll(parseActions(contents));
                                
                                Log.d(TAG, "Dropbox Load Successful");
                        } catch (Exception e){
                                Log.e(TAG, "Error adding Dropbox Actions: ", e);
                        } finally{
                                testAddFile.close();
                        }
                } catch (Exception e){
                        Log.e(TAG, "", e);
                }
                
                try{
                        saveToDropbox(mDbxAcctMgr);
                        Log.d(TAG, "Saved to Dropbox");
                }catch (Exception e){
                        Log.e(TAG, "Error Saving to Dropbox", e);
                }
                
                return;
        }
        
        public boolean saveActions(){
                return mSerializer.saveActions(toStringList(mRoot));
        }
        
        private ArrayList<String> toStringList(Action a){
        	Date timestamp = new Date();
        	
        	return toStringListHelper(a,timestamp);
        }
    	
    	private ArrayList<String> toStringListHelper(Action a, Date timestamp){
    		if(a == null || a.getSavedTimeStamp() == timestamp) return null;
    		
    		a.setSavedTimeStamp(timestamp);
    		
    		ArrayList<String> list = new ArrayList<String>();
    		
    		list.add(a.toFileTextLine());
    		list.trimToSize();
    		
    		
    		if(a.hasChildren()){
    			for(ArrayList<Action> subList : a.getChildren()){
    				for(int i = 0; i < subList.size(); i++){
    					ArrayList<String> subListActions = toStringListHelper(subList.get(i), timestamp);
    					if(subListActions != null) list.addAll(subListActions);
    				}
    			}
    		}
    		
    		return list;
    	}	
        
        private ArrayList<Action> parseActions(String contents){
                ArrayList<Action> actionList = new ArrayList<Action>();
                
                String[] lines = contents.split("\\n");
                for(String s: lines){
                        actionList.add(new Action(s));
                }
                
                return actionList;
        }
        
        public void changeActionTitle(Action a, String s){
                mTitleHash.remove(a.getTitle());
                a.setTitle(s);
                mTitleHash.put(a.getTitle(), a);
                return;
        }
    
    private void add(Action a){
            if(a.getTitle() == null || a.getTitle() == "" || a.getTitle().equals("root")){} //do nothing
            else {
                    if(this.hasAction(a)){
                            if(getAction(a.getId()).getModifiedDate().before(a.getModifiedDate())){
                                    //replace with newer version of the action, do nothing for now
                            }
                    }else if(mTempMap != null){
                            Action parent = null;
                            try{
                            parent = mTempMap.get(UUID.fromString(a.getParentUUIDString()));
                            } catch(Exception e){
                                    Log.e(TAG, "Error loading parent");
                            }
                            if(parent == null || parent.getTitle() == "" || parent.getTitle().equals("root")){
                                    mRoot.adopt(a);
                            } else {
                                    add(parent);
                                    parent.adopt(a);
                            }
                    } 
                            
                        mTitleHash.put(a.getTitle(),a);
                    mActionHash.put(a.getId(), a);
                }
    }
    private void addAll(ArrayList<Action> actionList){
            mTempMap = new HashMap<UUID, Action>();
            for(Action a : actionList){
                    mTempMap.put(a.getId(), a);
            }
            
        for(Action a : actionList){
            add(a);
        }
        
        mTempMap = null;
    }
        
        //Creates a new parent of the action based on the project name for a given action
        public void updateParentInfo(Action a, String parentName){
            if(a.equals(mRoot) 
            		|| a.getParent().getTitle().equals(parentName) 
            		|| parentName == null
            		|| parentName.equals("")) return; //do nothing.
    		
            Action parent = mTitleHash.get(parentName);
            if(parent != null){
               parent.adopt(a);
            } else{
               createParent(a, parentName);
            }
            
            return;
        }
        
        private void createParent(Action a, String parentName){
                Action parent = new Action();
                parent.setTitle(parentName);
                parent.adopt(a);
                parent.verifyStatusBasedOnChildren();
                mRoot.adopt(parent);
                mActionHash.put(parent.getId(), parent);
                mTitleHash.put(parent.getTitle(), parent);
        }
        
        
        public void changeActionStatus(Action a, int actionStatus){
                a.setActionStatus(actionStatus);
                a.getParent().adopt(a);
                
                if(actionStatus != Action.INCOMPLETE){
                        mTitleHash.remove(a.getTitle(), a);
                } else {
                        mTitleHash.put(a.getTitle(), a);
                }
        }
        
        public void deleteAction(Action a){
                Action parent = a.getParent();
                parent.removeChild(a);
                mTitleHash.remove(a.getTitle(), a);
                mActionHash.remove(a.getId());
        }
        
        public void deleteAllActions(){
                for(ArrayList<Action> list : mRoot.getChildren()){
                        list.clear();
                }
                mActionHash.clear();
                mTitleHash.clear();
                
                saveActions();
        }
        
        public boolean hasAction(Action a){
                return mActionHash.containsKey(a.getId());
        }
        
        
        public void saveToDropbox(DbxAccountManager mDbxAcctMgr) throws DbxException{
                DbxFileSystem dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
            DbxFile testFile2 = dbxFs.open(new DbxPath(DbxPath.ROOT, "Readable.txt"));
                
                try{
                        mSerializer.writeActionsToFile(toStringList(mRoot), testFile2);
                        Log.d(TAG, "Saved successfully to dropbox");
        }catch(Exception e){
            Log.d(TAG, "Error saving to dropbox: ", e);
        }finally {
                testFile2.close();
        }
                return;
        }
        public Action createActionIn(Action parent){
                Action action = new Action();
                mActionHash.put(action.getId(), action);
                parent.adopt(action);
                return action;
        }
        
        public void checkForPendingActions(Action a){
    		ArrayList<Action> pendingList = a.getChildren().get(Action.PENDING);
    		Date now = new Date();
    		
    		for(Iterator<Action> it = pendingList.iterator(); it.hasNext();){
    			Action current = it.next();
    			if(current.getStartDate().before(now)){
    				
    				//If Action is a repeated Action
    				if(current.getRepeatInterval() != 0){
    					createRepeatedAction(current);
    				}
    				current.setActionStatus(Action.INCOMPLETE);
    				it.remove();
    				
    				ArrayList<Action> incompleteList = a.getIncomplete();
    				a.getIncomplete().add(getUnpinnedTop(incompleteList), current);
    			}
    		}
        }
        private int getUnpinnedTop(ArrayList<Action> list){
        	for(int i = 0; i < list.size(); i++){
        		if(!list.get(i).isPinned()) return i;
        	}
        	return list.size();
        }
        
    	
    	public void modifyRepeatInterval(int repeatInterval, Action a){
    		
    		if(repeatInterval < 0 || repeatInterval > 4){
    			Log.e("Action", "Invalid paramaters for a repeated action, aborting");
    			return;
    		}
    		
    		a.setRepeatInterval(repeatInterval);
    		
    		if (repeatInterval == 0){
    			removePendingRepeatedActions(a);
    		} else {
    			if(a.getStartDate() == null) a.setStartDate(a.getCreatedDate());
    			if(a.getStartDate() == null) a.setStartDate(new Date());
    			
    			if(a.getDueDate() == null){
    				a.setDueDate(nextRepeatTime(a.getStartDate(), a));
    			}
    			
    			Log.d("Action", a.getTitle() + " made repeatable");
    			
    			createRepeatedAction(a);
    		}
    	}
    	private void removePendingRepeatedActions(Action a){
    		ArrayList<Action> pendingList = a.getParent().getPending();
    		
    		for(Iterator<Action> it = pendingList.iterator(); it.hasNext();){
    			Action current = it.next();
    			if(current.getTitle().equals(a.getTitle())){
    				it.remove();
    				mActionHash.remove(current.getId());
    				mTitleHash.remove(current.getTitle(), current);
    			}
    		}
    	}
    	
    	private void createRepeatedAction(Action original){
    		Action nextRepeat = createActionIn(original.getParent());
    		
    		nextRepeat.setTitle(original.getTitle());
    		nextRepeat.setContextName(original.getContextName());
    		nextRepeat.setRepeatInterval(original.getRepeatInterval());
    		nextRepeat.setMinutesExpected(original.getMinutesExpected());	
    		nextRepeat.setParent(original.getParent());
    		
    		
    		Date nextStart = nextRepeatTime(original.getStartDate(), nextRepeat);
    		nextRepeat.setStartDate(nextStart);
    		nextRepeat.setDueDate(nextRepeatTime(nextStart, nextRepeat));
    		
    		//Keep creating repeated actions until there is only one that is pending. 
    		if(nextRepeat.getActionStatus() == Action.INCOMPLETE){
    			createRepeatedAction(nextRepeat);
    		}
    	}
    	

    	public static final int REPEAT_DAY = 1;
    	public static final int REPEAT_WEEK = 2;
    	public static final int REPEAT_MONTH = 3;
    	public static final int REPEAT_YEAR = 4;
    	
    	private Date nextRepeatTime(Date start, Action a){
    		Calendar calendar = Calendar.getInstance();
            calendar.setTime(start);
            
            switch(a.getRepeatInterval()){
    	        case REPEAT_DAY:
    	        	calendar.add(Calendar.DATE, 1);
    	        	break;
    	        case REPEAT_WEEK:
    	        	calendar.add(Calendar.DATE, 7);
    	        	break;
    	        case REPEAT_MONTH:
    	        	calendar.add(Calendar.MONTH, 1);
    	        	break;
    	        case REPEAT_YEAR:
    	        	calendar.add(Calendar.YEAR, 1);
    	        	break;
    	    }
            
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);
            return new GregorianCalendar
            		(year, month, day, hourOfDay, minute).getTime();

    	}
        
        
        public Action getAction(UUID id){
                return mActionHash.get(id);
        }
        public ArrayList<ArrayList<Action>> getAllActions(){
                return mRoot.getChildren();
        }
        
        public ArrayList<Action> getActions(){
                return mRoot.getChildren().get(NOT_COMPLETED);
        }
        
        public ArrayList<Action> getCompletedActions(){
                return mRoot.getChildren().get(COMPLETED);
        }
}