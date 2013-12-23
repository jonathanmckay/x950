package com.apps.quantum;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

import android.util.Log;

public class Action {
	//Three types of outcome: complete, incomplete, pending
	private static final int OUTCOME_CATEGORIES = 4;
	public static final int INCOMPLETE = 0;
	public static final int COMPLETE = 1;
	public static final int PENDING = 2;
	public static final int WISHLIST = 3;
	
	
	private UUID mId;
	private String mTitle;
	private String mOutcomeName;
	private String mContextName;
	private String mParentUUIDString;
	
	private Date mCreatedDate;
	private Date mModifiedDate;
	private Date mStartDate;
	private Date mDueDate;
	private Date mRepeatInterval;
	private Date mEndRepeat;
	
	
	private int mMinutesExpected;
	private int mMinutesActual;
	private int mActionStatus;
	private int mPriority;
	private boolean mPinned;
	
	
	
	private Action mParent;
	private ArrayList<ArrayList<Action>> mChildren;
	
	private Date mSavedTimeStamp;
	
	protected Action(){
		mId = UUID.randomUUID();
		mOutcomeName = "";
		mContextName = ""; // action only
		mTitle = "";
		
		mCreatedDate = new Date();
		mModifiedDate = new Date();
		mStartDate = null;
		mDueDate = null;
		
		mMinutesExpected = 0;
		mMinutesActual = 0;
		mActionStatus = 0;
		
		mChildren = initializeChildren();
		mParent = null; // This will cause a null pointer exception if not handled!!
		mParentUUIDString = "";
		mPriority = -1;
		mRepeatInterval = null;
		mEndRepeat = null;
		
		mPinned = false;
	}
	
	public Action(String line){
		String[] tokens = line.split("\\t");
		
		this.mTitle = tokens[0].toLowerCase(Locale.US);
		this.mOutcomeName = tokens[1].toLowerCase(Locale.US);
		this.mContextName = tokens[2].toLowerCase(Locale.US);
		try{
			this.mActionStatus = Integer.parseInt(tokens[3]);
			if(mActionStatus > 3) throw new IllegalArgumentException();
		} catch (Exception e){
			Log.d("Parser", "Invalid actionStatus: " + tokens[3]);
			mActionStatus = 0;
		}
		
		this.mCreatedDate = toJavaDate(tokens[4]);
		this.mModifiedDate = toJavaDate(tokens[5]);
		
		try{
			this.mStartDate = toJavaDate(tokens[6]);
		}catch(Exception e){
			this.mStartDate = null;
		}
		
		try{
			this.mMinutesActual = Integer.parseInt(tokens[7]);
		}catch (Exception e){
			mMinutesActual = 0;
		}
		
		mPinned = tokens[8].equals("pinned") ? true : false;
		
		try{
			this.mMinutesExpected = Integer.parseInt(tokens[9]);
		}catch (Exception e){
			mMinutesExpected = 0;
		}
		try{
			this.mId = UUID.fromString(tokens[10]);
		}catch (Exception e){
			this.mId = UUID.randomUUID();
		}
		if(tokens.length > 11){
			this.mParentUUIDString = (tokens[11]); //don't handle yet, handle in Actionlab
		}
		
		try{
			this.mPriority = Integer.parseInt(tokens[12]);
		} catch (Exception e){
			mPriority = -1;
		}
		try {
			this.mDueDate = toJavaDate(tokens[13]);
		} catch (Exception e){
			this.mDueDate = null;
		}try{
			this.mRepeatInterval = toJavaDate(tokens[13]);
			this.mEndRepeat = toJavaDate(tokens[14]);
		} catch (Exception e){
			this.mRepeatInterval = null;
			this.mEndRepeat = null;
		}
		
		mChildren = initializeChildren();
		
	}
	
	public void verifyStatusBasedOnChildren(){
		if(this.hasActiveTasks()){
			mActionStatus = INCOMPLETE;
		}else if(this.getPending().size() != 0){
			mActionStatus = PENDING;
		}else{
			//Do nothing for now, force manual delete
		}
	return;
	}
	
	private ArrayList<ArrayList<Action>> initializeChildren(){
		ArrayList<ArrayList<Action>> children = new ArrayList<ArrayList<Action>>(OUTCOME_CATEGORIES);
		
		for(int i = 0; i < OUTCOME_CATEGORIES; i++){
			children.add(new ArrayList<Action>());
		}
		
		children.trimToSize();
		return children;
	}
	

	public boolean hasActiveTasks(){
		return !(mChildren.get(0).isEmpty());
	}
	
	public void adopt(Action a){
		Action oldParent = a.getParent();
		if(oldParent != null) oldParent.removeChild(a);
		
		a.setParent(this);
		
		
		ArrayList<Action> currentList = mChildren.get(a.getActionStatus());
		a.setPriority(currentList.size());
		
		
		//Log.d("ACTION", "Priority of " + String.valueOf(a.getPriority()));
		
		currentList.add(a);
		verifyStatusBasedOnChildren();
	}
	
	public void removeChild(Action a){
		int index = -1;
		int listIndex = -1;
		for(int j = 0; j < mChildren.size(); j++){
			for(Iterator<Action> it = mChildren.get(j).iterator(); it.hasNext();){
				Action b = it.next();

				if(a.getId().equals(b.getId())){
					index = mChildren.get(j).indexOf(b);
					Log.d("RemoveChild at index ", String.valueOf(index) + " of " + String.valueOf(j));
					listIndex = j;
					it.remove();
				}
			}
		}
		
		if(index > -1){
			//mChildren.get(listIndex).remove(index);
			for(int i = index; i < mChildren.get(listIndex).size() - 1; i++){
				mChildren.get(listIndex).get(i).setPriority(i);
			}
		}
		
		verifyStatusBasedOnChildren();
	}
	
	public void makeActionRepeatable(Date repeatInterval, Date endRepeat){
		
		if(repeatInterval.getTime() < (1000*60*60*24) || endRepeat.before(new Date())){
			Log.e("Action", "Invalid paramaters for a repeated action, aborting");
			return;
		}
		
		this.mRepeatInterval = repeatInterval;
		this.mEndRepeat = endRepeat;
		
		if(mStartDate == null) mStartDate = new Date();
		if(mDueDate == null){
			mDueDate = new Date(mRepeatInterval.getTime() + mStartDate.getTime());
		}
		
		Log.d("Action", this.getTitle() + " made repeatable");
		
		this.createRepeatedAction();
	}
	
	private void createRepeatedAction(){
		Action nextRepeat = new Action();
		
		nextRepeat.setTitle(this.getTitle());
		nextRepeat.setContextName(this.getContextName());

		nextRepeat.setRepeatInterval(this.getRepeatInterval());
		nextRepeat.setEndRepeat(this.getEndRepeat());
		
		nextRepeat.setMinutesExpected(this.getMinutesExpected());	
		

		nextRepeat.setParent(this.getParent());
		
		long newStart = getStartDate().getTime() + getRepeatInterval().getTime();
		nextRepeat.setStartDate(new Date(newStart));
		long newDue = getDueDate().getTime() + getRepeatInterval().getTime();
		nextRepeat.setDueDate(new Date(newDue));
				
		
		//Keep creating repeated actions until there is only one that is pending. 
		if(nextRepeat.getActionStatus() == INCOMPLETE){
			nextRepeat.createRepeatedAction();
		}
	}
	
	public void cancelRepeat(){
	// to be implemented	
	}
	
	public boolean equals(Action a){
		return (this.getId().equals(a.getId())) ? true : false;
	}
	public Action peekStep(){
		if(this.hasActiveTasks()){
			return mChildren.get(0).get(0);
		}
		else return null;
	}
	
	
	
	
	public static final int INCOMPLETE_ACTIONS_VIEW = 0;
	public static final int ALL_ACTIONS_VIEW = 1;
	public static final int TOP_FIVE_ACTIONS_VIEW = 2;
	
	public ArrayList<Action> getActions(int viewSetting){
		if(viewSetting == ALL_ACTIONS_VIEW){
			ArrayList<Action> output = new ArrayList<Action>();
			for(ArrayList<Action> list : getChildren()){
				output.addAll(list);
			}
			return output;
			
		} else if(viewSetting == INCOMPLETE_ACTIONS_VIEW){
			checkForPendingActions();
			return getIncomplete();
			
		} else if(viewSetting == TOP_FIVE_ACTIONS_VIEW){
			checkForPendingActions();
			ArrayList<Action> output = new ArrayList<Action>();
			ArrayList<Action> fullList = getIncomplete();
			for(int i = 0; i < 5 && i < fullList.size() ; i++){
				output.add(fullList.get(i));
			}
			return output;
			
		} else {
			Log.e("getActions", "getActions has been passed an illegal argument");
			return null;
		}
	}
	
	public void checkForPendingActions(){
		ArrayList<Action> pendingList = mChildren.get(PENDING);
		Date now = new Date();
		
		for(Iterator<Action> it = pendingList.iterator(); it.hasNext();){
			Action a = it.next();
			if(a.getStartDate().before(now)){
				
				//If Action is a repeated Action
				if(a.getRepeatInterval() != null && a.getEndRepeat() != null){
					Date nextRepeat = new Date(a.getStartDate().getTime() + a.getRepeatInterval().getTime());
					if(a.getEndRepeat().after(nextRepeat));
					a.createRepeatedAction();
				}
				a.setActionStatus(INCOMPLETE);
				it.remove();
				this.getIncomplete().add(a);
			}
		}
	}	
	
	//I don't like this function, should rewrite
	public void setStartDate(Date startDate) {
		mStartDate = startDate;
		if(mStartDate.after(new Date()) && mActionStatus == INCOMPLETE){
			mActionStatus = PENDING;
		} else if(mStartDate.before(new Date()) && mActionStatus == PENDING){
			mActionStatus = INCOMPLETE;
		}
			
		getParent().adopt(this);
	}
	
	public void moveToEnd(int list, int from){
		moveWithinList(list, from, this.mChildren.get(list).size() - 1);
	}
	
	public void moveToFront(int list, int from){
		moveWithinList(list, from, 0);
	}
	
	public void moveWithinList(int list, int from, int to){
		ArrayList<Action> currentList = this.mChildren.get(list);
		
		if(to < from){
			for(int i = to; i < from; i++){
				currentList.get(i).mPriority ++;
			}

		} else {
			for(int i = from + 1; i <= to; i++){
				currentList.get(i).mPriority --;
			}
		}
		
		currentList.get(from).setPriority(to);
		currentList.add(to, mChildren.get(list).remove(from));
		
	}
	
	
	private Date toJavaDate(String excelDate){
		if(excelDate == null || excelDate == "") return null;
		
		Date output = new Date();
		
		try{
			output = new SimpleDateFormat("yyyy.MM.dd hh:mm", Locale.US).parse(excelDate);
		} catch (ParseException e){
			output = new Date();
		}
		return output;
	}

	public String toFileTextLine(){
		StringBuilder sb = new StringBuilder();
		sb.append(mTitle);
		sb.append("\t");
		sb.append(mOutcomeName);
		sb.append("\t");
		sb.append(mContextName);
		sb.append("\t");
		sb.append(String.valueOf(mActionStatus));
		sb.append("\t");
		sb.append(android.text.format.DateFormat.format("yyyy.MM.dd HH:mm", mCreatedDate));
		sb.append("\t");
		sb.append(android.text.format.DateFormat.format("yyyy.MM.dd HH:mm", mModifiedDate));
		sb.append("\t");
		if(mStartDate != null) sb.append(android.text.format.DateFormat.format("yyyy.MM.dd HH:mm", mStartDate));
		sb.append("\t");
		sb.append(String.valueOf(mMinutesActual));
		sb.append("\t");
		sb.append((mPinned) ? "pinned" : "");
		sb.append("\t");
		sb.append(String.valueOf(mMinutesExpected));
		sb.append("\t");
		sb.append(mId.toString());
		sb.append("\t");
		if(mParent != null) sb.append(mParent.getId().toString());
		sb.append("\t");
		sb.append(String.valueOf(mPriority));
		sb.append("\t");
		if(mDueDate != null) sb.append(android.text.format.DateFormat.format("yyyy.MM.dd HH:mm", mDueDate));
		sb.append("\t");
		if(mRepeatInterval != null) sb.append(android.text.format.DateFormat.format("yyyy.MM.dd HH:mm", mRepeatInterval));
		sb.append("\t");
		if(mEndRepeat != null) sb.append(android.text.format.DateFormat.format("yyyy.MM.dd HH:mm", mEndRepeat));
		sb.append("\n");
		
		return sb.toString();
	}
	
	public String getOutcomeName() {
		return mOutcomeName;
	}

	public void setOutcomeName(String parentName) {
		mOutcomeName = parentName;
	}

	public String getContextName() {
		return mContextName;
	}

	public void setContextName(String contextName) {
		mContextName = contextName;
	}

	public int getActionStatus() {
		return mActionStatus;
	}

	protected void setActionStatus(int actionStatus) {
		mActionStatus = actionStatus;
	}

	public Date getModifiedDate() {
		return mModifiedDate;
	}

	public void setModifiedDate(Date modifiedDate) {
		mModifiedDate = modifiedDate;
	}

	public int getMinutesExpected() {
		return mMinutesExpected;
	}

	public void setMinutesExpected(int minutesExpected) {
		mMinutesExpected = minutesExpected;
	}
	public int getMinutesActual() {
		return mMinutesActual;
	}

	public void setMinutesActual(int minutesActual) {
		mMinutesActual = minutesActual;
	}

	public void setId(UUID id) {
		mId = id;
	}
	
	public Date getCreatedDate() {
		return mCreatedDate;
	}

	public void setCreatedDate(Date date) {
		mCreatedDate = date;
	}

	public UUID getId() {
		return mId;
	}

	public String getTitle() {
		return mTitle;
	}

	public void setTitle(String title) {
		mTitle = title;
	}
	
	@Override
	public String toString(){
		return mTitle;
	}

	public int getPriority() {
		return mPriority;
	}

	public void setPriority(int priority) {
		mPriority = priority;
	}

	public Action getParent() {
		return mParent;
	}

	public void setParent(Action parent) {
		mParent = parent;
		mParentUUIDString = mParent.getId().toString();
	}

	public ArrayList<ArrayList<Action>> getChildren() {
		return mChildren;
	}
	
	public Date getStartDate() {
		return mStartDate;
	}

	
	public boolean hasChildren(){
		for(ArrayList<Action> list : mChildren){
			if(!list.isEmpty()) return true;
		}
		return false;
	}

	public void setChildren(ArrayList<ArrayList<Action>> children) {
		mChildren = children;
	}
	
	public String getParentUUIDString() {
		return mParentUUIDString;
	}
	
	private ArrayList<Action> getIncomplete(){
		return mChildren.get(INCOMPLETE);
	}
	private ArrayList<Action> getPending(){
		return mChildren.get(PENDING);
	}
	
	public Date getDueDate() {
		return mDueDate;
	}

	public void setDueDate(Date dueDate) {
		mDueDate = dueDate;
	}
	
	public Date getSavedTimeStamp() {
		return mSavedTimeStamp;
	}

	public void setSavedTimeStamp(Date savedTimeStamp) {
		mSavedTimeStamp = savedTimeStamp;
	}

	public Date getRepeatInterval() {
		return mRepeatInterval;
	}
	public void setRepeatInterval(Date repeatInterval) {
		mRepeatInterval = repeatInterval;
	}
	public Date getEndRepeat() {
		return mEndRepeat;
	}
	public void setEndRepeat(Date endRepeat) {
		mEndRepeat = endRepeat;
	}

	public boolean isPinned() {
		return mPinned;
	}

	public void setPinned(boolean pinned) {
		mPinned = pinned;
	}


}
