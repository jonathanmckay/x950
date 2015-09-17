package com.apps.quantum;

import android.app.Activity;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.UUID;

public class ActionLab{
    private static final String TAG = "ActionLab";
    private static final String FILENAME = "Actions.txt";
    public static final String AUTOSAVE_FILENAME = "next5Autosave.txt";
    private static final String JSON_OUTPUT_FILENAME = "actions_json.txt";
    private static final int COMPLETED = 1;
    private static final int NOT_COMPLETED = 0;

    static final int REQUEST_LINK_TO_DBX = 0;

    private HashMap<UUID, Action> mActionHash;
    private HashMap<UUID, Action> mTempMap;
    private PriorityQueue<Action> mStartDateQueue;
    private DropboxAPI<AndroidAuthSession> mDBApi;
    private DropboxCorpusSync dbSync;

    public static final String DROPBOX_OPTIONS = "com.apps.quantum.dropboxfragment";

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

    private class StartDateComparator implements Comparator<Action>
    {
        @Override
        public int compare(Action a, Action b)
        {
           if(a.getStartDate() == null) return -1;
           if(b.getStartDate() == null) return 1;
           return (a.getStartDate().after(b.getStartDate())) ? 1 : -1;
        }
    }
        
    private static ActionLab sActionLab;
    private Activity mActivity;
    private Action mRoot;
    private OutcomeSerializer mSerializer;

    private ActionLab(Activity a){
        mActivity = a;
        mSerializer = new OutcomeSerializer(mActivity.getApplicationContext(), FILENAME, JSON_OUTPUT_FILENAME);

        mActionHash = new HashMap<UUID, Action>();
        mTitleHash = new TitleMap();
        Comparator<Action> comparator = new StartDateComparator();
        mStartDateQueue = new PriorityQueue<Action>(50, comparator);

        dbSync = DropboxCorpusSync.get(a);
        mDBApi = dbSync.getDropboxAPI();

        //This assumes that root is the first entry. and that root is the
        // only self-referencing action
        try {
            Log.d("add all from initialize", "");
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
        dbSync.launchCorpusSync();
    }
    //Singleton
    public static ActionLab get(Activity c){
        sActionLab = (sActionLab == null) ? new ActionLab(c) : sActionLab;
        return sActionLab;
    }

    private void initializeRoot(){
        mRoot = new Action();
        mRoot.setParent(mRoot);
        mRoot.setTitle("root");
        addToLab(mRoot);
    }
    public Action getRoot(){
            return mRoot;
    }

//    TODO: Return success code
    public void importDbxFile(String filename) throws IllegalArgumentException {
        if(!filename.endsWith(".txt")) throw new IllegalArgumentException();
        try {
            File file = new File(mActivity.getApplicationContext().getFilesDir(), filename);
            if (!file.exists()) {
                final String fname = filename;
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        dbSync.getRemoteFile(fname);
                    }
                });
                t.start();
                t.join(); //wait for download to finish before reading file
                file = new File(mActivity.getApplicationContext().getFilesDir(), filename);
            }
            String contents = readAsString(file);
            addAll(parseActions(contents));
            Log.d(TAG, "Dropbox Load Successful");
        } catch (Exception e) {
            Log.e(TAG, "Error adding Dropbox Actions: ", e);
        }
    }

    public String readAsString(File file) throws java.io.FileNotFoundException, java.io.IOException {
        StringBuffer contents = new StringBuffer();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            contents.append(line);
            contents.append("\n");
        }
        reader.close();
        return new String(contents);
    }

    public boolean saveActions(){
            return mSerializer.saveActions(toStringList(mRoot));
    }

    public ArrayList<String> getTaskNames() {
        //return all task names
        ArrayList<String> sts = toStringList(mRoot);
        ArrayList<String> out = new ArrayList<>();
        for (String st : sts ) {
            String name = st.split("\t")[0];
            out.add(name);
        }
        return out;
    }

    private ArrayList<String> toStringList(Action a){
        Date timestamp = new Date();

        return toStringListHelper(a, timestamp);
    }

    private ArrayList<String> toStringListHelper(Action a, Date timestamp){
        if(a == null || a.getSavedTimeStamp() == timestamp) return null;

        a.setSavedTimeStamp(timestamp);

        ArrayList<String> list = new ArrayList<String>();
        if(a.getActionStatus() == Action.COMPLETE){
            return list;
        }


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
    
    private void addValid(Action a){
        if(a.getTitle() == null || a.getTitle() == "") return;
        if (a.getTitle().equals("root") && UUID.fromString(a.getParentUUIDString()).equals(a.getId())){
            //Overwrite the root
            a.setParent(a);

            removeFromLab(mRoot);
            mRoot = a;
            Log.d("add root on startup", "");
            addToLab(mRoot);
            return;
        }

        if(mTempMap != null && !this.hasAction(a)){
            Action parent = null;
            try{
                parent = mTempMap.get(UUID.fromString(a.getParentUUIDString()));
            } catch(Exception e){
                Log.e(TAG, "Error loading parent");
            }
            if(parent == null || parent.getTitle() == "" || parent.getTitle().equals("root") || parent.equals(a)){
                mRoot.adopt(a);
            } else {
                addValid(parent);
                parent.adopt(a);
            }
        }
        Log.d("Add normal action to lab on startup", "");
        addToLab(a);
    }

    
    public Action preview(Action parent){
		Action a = new Action();
		a = parent;
		
		while(a.hasActiveTasks()){
			a = a.getFirstSubAction();
    	}
		return a;
	}
    
    private void addAll(ArrayList<Action> actionList){
        mTempMap = new HashMap<UUID, Action>();
        for(Action a : actionList){
                mTempMap.put(a.getId(), a);
        }
        for(Action a : actionList){
            addValid(a);
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

        addToLab(parent);
    }


    public void changeActionStatus(Action a, int actionStatus){
        if(actionStatus == Action.COMPLETE){
            deleteAction(a);
            return;
        }
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
        removeFromLab(a);
        deactivateOnComplete(parent);
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

    public boolean dropboxLinked(){
        return mDBApi.getSession().isLinked();
    }

    public void saveToDropbox(String filename){
        if(dropboxLinked()) saveToDropboxHelper(filename);
        else {
            dbSync.authDropbox();
        }
    }

    //TODO: should this overwrite existing file?
    private void saveToDropboxHelper(String filename){
        try {
            File saveFile = new File(mActivity.getApplicationContext().getFilesDir(), filename);
            mSerializer.writeActionsToFile(toStringList(mRoot), saveFile);
            dbSync.launchPostFileOverwrite(filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public Action createActionIn(Action parent){
        Action action = new Action();
        Log.d("create action in ", "");
        addToLab(action);
        parent.adopt(action);

        return action;
    }
        
    public void checkForPendingActions(){
        while(!mStartDateQueue.isEmpty()
            && mStartDateQueue.peek().getStartDate().before(new Date())){
            Action a = mStartDateQueue.poll();
            activate(a);
            if(a.isRepeat()) {
                createRepeatedAction(a, a);
            }
        }
    }

    public void setStartDate(Action a, Date startDate) {
        if(startDate == null) return;
        if (a.isPending() && a.isRepeat()) {
            unrepeatPending(a);
        }
        modifyStartDate(a, startDate);
        if (startDate != null && startDate.after(new Date()) && a.isIncomplete()){
            deactivate(a);
        }
        if (startDate.before(new Date()) && a.isPending()) activate(a);
    }

    private void unrepeatPending(Action a) {
        //Create the next pending repeated action, remove repeat interval from current action
        createRepeatedAction(a, a);
        a.setRepeatInfo(0,0);
    }

    public void addToStartDateQueue(Action a) {
        if((a.getStartDate() != null && a.getStartDate().after(new Date()))
            || a.getActionStatus() == Action.PENDING)
            mStartDateQueue.add(a);
    }

    private void modifyStartDate(Action a, Date startDate) {
        mStartDateQueue.remove(a);
        a.setStartDateRaw(startDate);
        addToStartDateQueue(a);
    }

    // Mark an action as active and change the status of any ancestors as necessary
    private void activate(Action a){
        a.setActionStatus(Action.INCOMPLETE);
        a.moveToUnpinnedTop();
        a = a.getParent();

        while(a != null && !a.isRoot() && !a.isIncomplete()){
            if(a.isPending()) {
                a.setActionStatus(Action.INCOMPLETE);
                a.moveToUnpinnedTop();
            }
            a = a.getParent();
        }
    }

    // Mark an action as pending, and change the status of children and ancestors as necessary
    private void deactivate(Action a){
        a.setActionStatus(Action.PENDING);
        if(a.hasActiveTasks()) deactivateChildren(a);
        if(a.getParent() != null && !a.isRoot() && a.getParent().isIncomplete())
            deactivateParents(a);
    }

    private void deactivateParents(Action a) {
        Date currentStartDate = a.getStartDate();
        a = a.getParent();
        while(a != null && !a.isRoot() && a.isIncomplete()){
            // Only deactivate the parent if there are no other active tasks
            if(!a.hasActiveTasks() && a.isIncomplete()) {
                modifyStartDate(a, currentStartDate);
                a.setActionStatus(Action.PENDING);
            }
            if (a.getStartDate() != null
                && a.getStartDate().after(currentStartDate)
                && a.isPending())
                modifyStartDate(a, currentStartDate);
            a = a.getParent();
        }
    }

    //Call on parent action to deactivate parents up the tree
    public void deactivateOnComplete(Action a){
        while(a != null && !a.isRoot()
            && a.isIncomplete()
            && !a.hasActiveTasks()
            && a.hasPendingTasks()){
            Date earliestChildStart = new Date(Long.MAX_VALUE);
            for(Action pendingSub : a.getPending()){
                Date currentStart = pendingSub.getStartDate();
                if(currentStart.before(earliestChildStart)) earliestChildStart= currentStart;
            }
            modifyStartDate(a, earliestChildStart);
            a.setActionStatus(Action.PENDING);
            a = a.getParent();
        }

    }

    private void deactivateChildren(Action a){
        // If the parent is pending, all children must be pending as well.
        if (a.hasActiveTasks()) {
            ArrayList<Action> newPendingSubs = new ArrayList<Action>();
            for (Iterator<Action> it = a.getIncomplete().iterator(); it.hasNext();) {
                newPendingSubs.add(it.next());
                it.remove();
            }
            for (Action sub : newPendingSubs) {
                sub.setActionStatus(Action.PENDING);
                modifyStartDate(sub, a.getStartDate());
                deactivate(sub);
            }
        }

        // If there are pending actions, make sure they do not activate before the
        // master task.
        for(Action sub : a.getPending()){
            if(sub.getStartDate().after(a.getStartDate()))
                modifyStartDate(sub, a.getStartDate());
        }
    }

    public void modifyRepeatInterval(int repeatInterval, int repeatNumber, Action a){
        if(repeatInterval < 0 || repeatInterval > 5){
            Log.e("Action", "Invalid paramaters for a repeated action, aborting");
            return;
        }

        a.setRepeatInfo(repeatInterval, repeatNumber);

        if (repeatInterval == 0) {
            removePendingRepeatedActions(a);
            return;
        }

        if(a.getStartDate() == null) a.setStartDateRaw(a.getCreatedDate());
        if(a.getStartDate() == null) a.setStartDateRaw(new Date());
        setStartDate(a, a.getStartDate());

        if(a.getDueDate() == null) a.setRepeatDue();

        if(!a.isPending()) createRepeatedAction(a, a);
    }

    private void removePendingRepeatedActions(Action a){
        ArrayList<Action> pendingList = a.getParent().getPending();

        for(Iterator<Action> it = pendingList.iterator(); it.hasNext();){
            Action current = it.next();
            if(current.getTitle().equals(a.getTitle())){
                it.remove();
                removeFromLab(current);
            }
        }
    }

    private Action createRepeatedAction(Action original, Action repeatOriginal){
        Action nextRepeat = original.createNextRepeat(repeatOriginal);
        original.getParent().adopt(nextRepeat);

        Log.d("ACTIONLAB", "createRepeatedaction");
        addToLab(nextRepeat);

        //Copy any incomplete subtasks that are a part of the repetiton
        for(Action sub : original.getAllAsArrayList()) {
            copySubActions(sub, nextRepeat);
        }

        if(nextRepeat.isIncomplete()){
            createRepeatedAction(nextRepeat, nextRepeat);
        }

        return nextRepeat;
    }

    private void copySubActions(Action original, Action repeatParent){
        Action nextRepeat = original.createNextRepeatSub(repeatParent);
        repeatParent.adopt(nextRepeat);
        addToLab(nextRepeat);

        for(Action sub : original.getAllAsArrayList()){
            copySubActions(sub, nextRepeat);
        }
    }


    public Action getAction(UUID id){
        Log.d("ACTIONLAB", mActionHash.get(id).toString());
        return mActionHash.get(id);
    }

    public void addToLab(Action a){
        mActionHash.put(a.getId(), a);
        Log.d("ACTIONLAB", "added " + a.toString() + " to lab");
        mTitleHash.put(a.getTitle(), a);
        addToStartDateQueue(a);
    }

    public void removeFromLab(Action a){
        mActionHash.remove(a.getId());
        mTitleHash.remove(a.getTitle());
        if(mStartDateQueue.contains(a)) mStartDateQueue.remove(a);
    }

}