package com.apps.quantum;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
	
	private int mMinutesExpected;
	private int mMinutesActual;
	private int mActionStatus;
	private int mPriority;
	
	private Action mParent;
	private ArrayList<ArrayList<Action>> mChildren;
	
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
		
		
		// Tokens[8] is a deprecated value 
		
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
		
		mChildren = initializeChildren();
		mDueDate = null;
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
		
		
		Log.d("ACTION", "Priority of " + String.valueOf(a.getPriority()));
		
		currentList.add(a);
		verifyStatusBasedOnChildren();
	}
	public ArrayList<String> toList(){
		
		return this.toActionList();
	}
	
	private ArrayList<String> toActionList(){
		ArrayList<String> list = new ArrayList<String>();
		
		list.add(this.toFileTextLine());
		
		for(ArrayList<Action> subList : this.mChildren){
			for(int i = 0; i < subList.size(); i++){
				ArrayList<String> subListActions = subList.get(i).toActionList();
				list.addAll(subListActions);
			}
		}
		
		return list;
	}	
	
	
	public void removeChild(Action a){
		int index = -1;
		for(ArrayList<Action> list : mChildren){
			for(int i= 0; i < list.size(); i++){
				if(a.getId().equals(list.get(i).getId())){
					list.remove(i);
					index = i;
					break;
				}
			}
			if(index > -1){
				for(int i = index; i < list.size(); i++){
					list.get(i).setPriority(i);
				}
			}
		}
		
		/*
		ArrayList<Action> currentList = mChildren.get(a.getActionStatus());
		int index = currentList.indexOf(a);
		if(index > -1){
			currentList.remove(a);
			for(int i = index; i < currentList.size(); i++){
				currentList.get(i).setPriority(i);
			}
			this.verifyStatusBasedOnChildren();
		}*/
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

	
	public ArrayList<Action> getActions(boolean allActions){
		if(allActions){
			ArrayList<Action> output = new ArrayList<Action>();
			
			for(ArrayList<Action> list : getChildren()){
				output.addAll(list);
			}
			
			return output;
		} else {
			checkForPendingActions();
			return getIncomplete();
		}
	}
	
	public void checkForPendingActions(){
		int i = 0;
		
		while(i < mChildren.get(PENDING).size() 
				&& mChildren.get(PENDING).get(i).getStartDate().before(new Date())){
			mChildren.get(PENDING).get(i).mActionStatus = INCOMPLETE;
			this.getIncomplete().add( this.getPending().remove(i));
			i++;
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
		sb.append(""); // blank, originally for boolean isCompleted field.
		sb.append("\t");
		sb.append(String.valueOf(mMinutesExpected));
		sb.append("\t");
		sb.append(mId.toString());
		sb.append("\t");
		if(mParent != null) sb.append(mParent.getId().toString());
		sb.append("\t");
		sb.append(String.valueOf(mPriority));
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
}
