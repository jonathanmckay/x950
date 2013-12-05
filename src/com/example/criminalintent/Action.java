package com.example.criminalintent;

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
	
	public Action(){
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
		mPriority = 0;
		
		mChildren = initializeChildren();
		mParent = null; // This will cause a null pointer exception if not handled!!
		mParentUUIDString = "";
		
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
		
		mChildren = initializeChildren();
		mDueDate = null;
	}
	public void makeRoot(){
		mId = UUID.fromString("fb7331db-919f-461b-be6b-1c7bc51a0075");
	}
	
	private ArrayList<ArrayList<Action>> initializeChildren(){
		ArrayList<ArrayList<Action>> children = new ArrayList<ArrayList<Action>>(OUTCOME_CATEGORIES);
		
		for(int i = 0; i < OUTCOME_CATEGORIES; i++){
			children.add(new ArrayList<Action>());
		}
		
		children.trimToSize();
		return children;
	}
	
	public void add(Action a){
		/*Not sure why I was worried about duplicate values*/
		Action oldParent = a.getParent();
		
		if(oldParent != null){
			oldParent.removeChild(a);
			oldParent.verifyActionIncomplete();
		}
		
		a.setParent(this);
		mChildren.get(a.getActionStatus()).add(a);	
	}
	
	public Action getAction(UUID id){
		if(mId.equals(id)) return this;
		
		for(ArrayList<Action> list : getChildren()){
			for(Action c : list){
				Action possibleResult = c.getAction(id);
				if(possibleResult != null) return possibleResult;
			}
		}
		return null;
	}
	public void removeChild(Action a){
		for(ArrayList<Action> list : mChildren){
			for(int i= 0; i < list.size(); i++){
				if(a.getId().equals(list.get(i).getId())) list.remove(i);
			}
		}
		mChildren.get(a.getActionStatus()).remove(a);
	}

	public Action peekStep(){
		if(this.hasActiveTasks()){
			return mChildren.get(0).get(0);
		}
		else return null;
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
	
	public ArrayList<Action> toList(){
		ArrayList<Action> list = new ArrayList<Action>();
		
		list.add(this);
		
		for(ArrayList<Action> subList : this.mChildren){
			for(Action a : subList){
				list.addAll(a.toList());
			}
		}
		
		return list;
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

	public void setActionStatus(int actionStatus) {
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
	

	public Date getStartDate() {
		return mStartDate;
	}

	public void setStartDate(Date startDate) {
		mStartDate = startDate;
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
	}

	public ArrayList<ArrayList<Action>> getChildren() {
		return mChildren;
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
	
	public boolean hasActiveTasks(){
		return !(mChildren.get(0).isEmpty());
	}
	
	public void verifyActionIncomplete(){
		if(this.hasActiveTasks()){
			mActionStatus = 0;
		}else if(this.hasChildren()){
			mActionStatus = 1;
		}else{
			//leave value unchanged
		}
	return;
	}
	public String getParentUUIDString() {
		return mParentUUIDString;
	}
	public void setParentUUIDString(String parentUUIDString) {
		mParentUUIDString = parentUUIDString;
	}
	
	public ArrayList<Action> getNoncompleted(){
		return mChildren.get(0);
	}
	public ArrayList<Action> getCompleted(){
		return mChildren.get(1);
	}

	public Date getDueDate() {
		return mDueDate;
	}

	public void setDueDate(Date dueDate) {
		mDueDate = dueDate;
	}
}
