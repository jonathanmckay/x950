package com.example.criminalintent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import android.content.Context;
import android.util.Log;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;

public class ActionLab {
	private static final String TAG = "ActionLab";
	private static final String FILENAME = "Actions.txt";
	private static final int COMPLETED = 1;
	private static final int NOT_COMPLETED = 0;
	
	private HashMap<String, Action> mActionHash;
	
	private static ActionLab sActionLab;
	private Context mAppContext;
	private Action mActionsRoot;
	private OutcomeSerializer mSerializer;
	
	private ActionLab(Context appContext){
		mAppContext = appContext;
		mSerializer = new OutcomeSerializer(mAppContext, FILENAME);
		
		mActionHash = new HashMap<String, Action>();
		
		initializeRoot();
		
		
		try {
			ArrayList<Action> actionList = mSerializer.loadActions();
			for(Action a: actionList){
				addToRoot(a);
			}
			
			System.out.println();
			
		}catch (Exception e) {
			Log.e(TAG, "Error loading Actions: ", e);
			Action visibleAction = new Action();
			visibleAction.setTitle("incompleteAction");
			visibleAction.setActionStatus(NOT_COMPLETED);
			
			Action nonVisibleAction = new Action();
			nonVisibleAction.setTitle("completedAction");
			nonVisibleAction.setActionStatus(COMPLETED);
			
			
			addToRoot(nonVisibleAction);
			addToRoot(visibleAction);
		}
	}
	
	private void initializeRoot(){
		mActionsRoot = new Action();
		mActionsRoot.makeRoot();
		mActionsRoot.setParent(mActionsRoot);
		mActionsRoot.setTitle("root");
		mActionHash.put(mActionsRoot.getTitle(), mActionsRoot);
		
		return;
	}
	public Action getRoot(){
		return mActionsRoot;
	}
	
	public void syncToDropBox(DbxAccountManager mDbxAcctMgr){
		try{
			
			DbxFileSystem dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
			DbxFile testAddFile = dbxFs.open(new DbxPath(DbxPath.ROOT, "read-from.txt"));
			
			try{
				String contents = testAddFile.readString();
				Log.d("Dropbox Test", "File contents: " + contents);
				addToActionsList(contents);
				
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
	
	public void addToActionsList(String contents){
		String[] lines = contents.split("\\n");
		for(String s: lines){
			Action a = new Action(s);
			addToRoot(a);
		}
		
		System.out.println();
	}
	
	public void changeActionTitle(Action a, String s){
		mActionHash.remove(a.getTitle());
		a.setTitle(s);
		mActionHash.put(a.getTitle(), a);
		return;
	}
	
	public boolean saveActions(){
		try{
			mSerializer.saveActions(mActionsRoot.toList());
			Log.d(TAG, "Actions saved to file");
			return true;
		} catch (Exception e){
			Log.e(TAG, "Error saving Actions: ", e);
			return false;
		}
	}
	public void addToRoot(Action a){
		if(a.getTitle() == null || a.getTitle() == ""){} //do nothing
		else if(this.hasAction(a)){
			//also do nothing
		}
		else{
			try{
				Action parent = findAction(UUID.fromString(a.getParentUUIDString()));
				parent.add(a);
				
			}catch(Exception e){
					String parentName = a.getOutcomeName();
					
					if(parentName == null || parentName.equals("")){
						mActionsRoot.add(a);
					}else{
						updateParentInfo(a, parentName);
					}
			}
			mActionHash.put(a.getTitle(),a);
		}
	}
	
	//Creates a new parent of the action based on the project name for a given action
	public void updateParentInfo(Action a, String parentName){
		if(parentName != null){
			if(mActionHash.containsKey(parentName)){
				Action parent = getActionFromTitle(parentName);
				parent.add(a);
			} else{
				createParent(a, parentName);
			}
		}
	}
	
	private void createParent(Action a, String parentName){
		Action parent = new Action();
		parent.setTitle(parentName);
		parent.add(a);
		parent.verifyActionIncomplete();
		mActionsRoot.add(parent);
		mActionHash.put(parentName, parent);
	}
	
	
	public void saveToDropbox(DbxAccountManager mDbxAcctMgr) throws DbxException{
		DbxFileSystem dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());
    	DbxFile testFile2 = dbxFs.open(new DbxPath(DbxPath.ROOT, "Readable.txt"));
		
		try{
			mSerializer.writeActionsToFile(mActionsRoot.toList(), testFile2);
			Log.d(TAG, "Saved successfully to dropbox");
        }catch(Exception e){
            Log.d(TAG, "Error saving to dropbox: ", e);
        }finally {
        	testFile2.close();
        }
		return;
	}
	
	public Action findAction(UUID id){
		return mActionsRoot.getAction(id);
	}
	
	//Singleton
	public static ActionLab get(Context c){
		if(sActionLab == null){
			sActionLab = new ActionLab(c.getApplicationContext());
		}
		return sActionLab;
	}
	public ArrayList<ArrayList<Action>> getAllActions(){
		return mActionsRoot.getChildren();
	}
	
	public ArrayList<Action> getActions(){
		return mActionsRoot.getChildren().get(NOT_COMPLETED);
	}
	
	public ArrayList<Action> getCompletedActions(){
		return mActionsRoot.getChildren().get(COMPLETED);
	}
	
	public ArrayList<Action> getActionsCopy(){
		return new ArrayList<Action>(mActionsRoot.getChildren().get(NOT_COMPLETED));
	}
	
	public ArrayList<Action> getCompletedActionsCopy(){
		return new ArrayList<Action>(mActionsRoot.getChildren().get(COMPLETED));
	}
	public void deleteAction(Action c){
		c.getParent().removeChild(c);
		mActionHash.remove(c.getTitle());
	}
	public void resetAction(Action c){		
		this.deleteAction(c);
		addToRoot(c);
	}
	
	public void deleteAllActions(){
		for(ArrayList<Action> list : mActionsRoot.getChildren()){
			list.clear();
		}
		mActionHash.clear();
		
		saveActions();
	}
	public boolean hasActionTitle(String actionTitle){
		return mActionHash.containsKey(actionTitle);
	}
	public Action getActionFromTitle(String actionTitle){
		return mActionHash.get(actionTitle);
	}
	
	public boolean hasAction(Action a){
		return hasActionTitle(a.getTitle());
	}
}
