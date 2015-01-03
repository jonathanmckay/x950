package com.apps.quantum;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

public class Action {
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

	// Used by ActionLab to prevent infinite loops
	private Date mSavedTimeStamp;

	private int mRepeatInterval;
    private int mRepeatNumber;

	private int mMinutesExpected;
	// This value not used yet
	private int mMinutesActual;
	private int mActionStatus;
	private int mPriority;
	private boolean mPinned;

	private int mCompletedToday;
	private Date mTodaysDate;

	private Action mParent;
	private ArrayList<ArrayList<Action>> mChildren;

	protected Action() {
		mId = UUID.randomUUID();
		mOutcomeName = "";
		mContextName = "";
		mTitle = "";

		mCreatedDate = new Date();
		mModifiedDate = new Date();
		mStartDate = null;
		mDueDate = null;
		mSavedTimeStamp = null;

		mMinutesExpected = 0;
		mMinutesActual = 0;
		mActionStatus = 0;

		mChildren = initializeChildren();
		mParent = null;
		mParentUUIDString = "";
		mPriority = -1;
		mRepeatInterval = 0;

		mPinned = false;

		mCompletedToday = 0;
		mTodaysDate = new Date();
	}

	public Action(String line) {
		String[] tokens = line.split("\\t");

		this.mTitle = tokens[0].toLowerCase(Locale.US);
		this.mOutcomeName = tokens[1].toLowerCase(Locale.US);
		this.mContextName = tokens[2].toLowerCase(Locale.US);
		try {
			this.mActionStatus = Integer.parseInt(tokens[3]);
			if (mActionStatus > 3)
				throw new IllegalArgumentException();
		} catch (Exception e) {
			// Log.d("Parser", "Invalid actionStatus: " + tokens[3]);
			mActionStatus = 0;
		}

		this.mCreatedDate = toJavaDate(tokens[4]);
		this.mModifiedDate = toJavaDate(tokens[5]);

		try {
			this.mStartDate = toJavaDate(tokens[6]);
		} catch (Exception e) {
			this.mStartDate = null;
		}

		try {
			this.mMinutesActual = Integer.parseInt(tokens[7]);
		} catch (Exception e) {
			mMinutesActual = 0;
		}

		mPinned = tokens[8].equals("pinned") ? true : false;

		try {
			this.mMinutesExpected = Integer.parseInt(tokens[9]);
		} catch (Exception e) {
			mMinutesExpected = 0;
		}
		try {
			this.mId = UUID.fromString(tokens[10]);
		} catch (Exception e) {
			this.mId = UUID.randomUUID();
		}
		if (tokens.length > 11) {
			this.mParentUUIDString = (tokens[11]);
			// don't handle yet, handle in Actionlab
		}

		try {
			this.mPriority = Integer.parseInt(tokens[12]);
		} catch (Exception e) {
			mPriority = -1;
		}
		try {
			if (!tokens[13].equals("") && !(tokens[13] == null)) {
				this.mDueDate = toJavaDate(tokens[13]);
			}
		} catch (Exception e) {
			this.mDueDate = null;
		}
		if (mDueDate != null) {
			if (mDueDate.equals(mStartDate))
				mDueDate = null;
		}

		try {
			this.mRepeatInterval = Integer.parseInt(tokens[14]);
		} catch (Exception e) {
			this.mRepeatInterval = 0;
		}

		mChildren = initializeChildren();

		mCompletedToday = Integer.parseInt(tokens[15]);
		/*
		 * catch (Exception e) { this.mCompletedToday = 0; }
		 */

		try {
			this.mTodaysDate = toJavaDate(tokens[16]);
		} catch (Exception e) {
			mTodaysDate = new Date();
		}

        try {
            this.mRepeatNumber = Integer.parseInt(tokens[17]);
        } catch (Exception e) {
            mRepeatNumber = 0;
        }


        //Convert legacy repititons to new repetition form
        if(this.mRepeatNumber == 0 && this.mRepeatInterval != 0) {
            this.setRepeatInfo(this.mRepeatInterval, 1);
        }

	}

	private static String JSON_ID = "id";
	private static String JSON_TITLE = "title";
	private static String JSON_CONTEXT_NAME = "context_name";
	private static String JSON_PARENT_ID = "parent_id";
	private static String JSON_CREATED_DATE = "created_date";
	private static String JSON_START_DATE = "start_date";
	private static String JSON_MODIFIED_DATE = "modified_date";
	private static String JSON_DUE_DATE = "due_date";
	private static String JSON_REPEAT_INTERVAL = "repeat_interval";
	private static String JSON_MINUTES_EXPECTED = "minutes_expect";
	private static String JSON_MINUTES_ACTUAL = "minutes_actual";
	private static String JSON_ACTION_STATUS = "action_status";
	private static String JSON_PRIORITY = "priority";
	private static String JSON_PINNED = "pinned";
	private static String JSON_COMPLETED_TODAY = "completed_today";
	private static String JSON_TODAYS_DATE = "todays_date";

	public Action(JSONObject json) throws JSONException {
		// mParent must be initialized by ActionLab
		mParent = null;

		mId = UUID.fromString(json.getString(JSON_ID));
		mTitle = json.getString(JSON_TITLE);
		mMinutesExpected = json.getInt(JSON_MINUTES_EXPECTED);
		mMinutesActual = json.getInt(JSON_MINUTES_ACTUAL);
		mActionStatus = json.getInt(JSON_ACTION_STATUS);
		mContextName = json.getString(JSON_CONTEXT_NAME);

		mParentUUIDString = json.getString(JSON_PARENT_ID);
		mPriority = json.getInt(JSON_PRIORITY);
		mPinned = json.getBoolean(JSON_PINNED);

		mRepeatInterval = json.getInt(JSON_REPEAT_INTERVAL);

		if (json.has(JSON_CREATED_DATE))
			mCreatedDate = new Date(json.getLong(JSON_CREATED_DATE));
		if (json.has(JSON_DUE_DATE))
			mDueDate = new Date(json.getLong(JSON_DUE_DATE));
		if (json.has(JSON_MODIFIED_DATE))
			mModifiedDate = new Date(json.getLong(JSON_MODIFIED_DATE));
		if (json.has(JSON_START_DATE))
			mStartDate = new Date(json.getLong(JSON_START_DATE));

		if (json.has(JSON_TODAYS_DATE))
			mStartDate = new Date(json.getLong(JSON_TODAYS_DATE));
		mCompletedToday = json.getInt(JSON_COMPLETED_TODAY);

		mChildren = initializeChildren();
	}

	public JSONObject toJSON() throws JSONException {
		JSONObject json = new JSONObject();
		json.put(JSON_ID, mId.toString());
		json.put(JSON_TITLE, mTitle);
		json.put(JSON_MINUTES_EXPECTED, mMinutesExpected);
		json.put(JSON_MINUTES_ACTUAL, mMinutesActual);
		json.put(JSON_ACTION_STATUS, mActionStatus);
		json.put(JSON_PARENT_ID, mParentUUIDString);
		json.put(JSON_PRIORITY, mPriority);
		json.put(JSON_PINNED, mPinned);
		json.put(JSON_REPEAT_INTERVAL, mRepeatInterval);

		if (mCreatedDate != null)
			json.put(JSON_CREATED_DATE, mCreatedDate.getTime());
		if (mDueDate != null)
			json.put(JSON_DUE_DATE, mDueDate.getTime());
		if (mModifiedDate != null)
			json.put(JSON_MODIFIED_DATE, mModifiedDate.getTime());
		if (mStartDate != null)
			json.put(JSON_START_DATE, mStartDate.getTime());

		if (mTodaysDate != null)
			json.put(JSON_TODAYS_DATE, mTodaysDate.getTime());
		json.put(JSON_COMPLETED_TODAY, mCompletedToday);

		return json;
	}

	public void verifyStatusBasedOnChildren() {
		if (mParent == null)
			throw new IllegalArgumentException();

		if (this.hasActiveTasks()) {
			setActionStatus(INCOMPLETE);
		} else if (this.getPending().size() != 0
				&& this.mActionStatus == INCOMPLETE) {
			//setActionStatus(PENDING);
		} else {
			// Do nothing force manual delete
		}

		return;
	}

	protected void setStartDateRaw(Date startDate) {
		mStartDate = startDate;
	}

	protected void setActionStatus(int actionStatus) {
		if (actionStatus != mActionStatus) {
			mActionStatus = actionStatus;

			// Check for loops
			if (getParent() != null && !getParent().equals(this)) {

				// Check for status changes up the tree
				mParent.adopt(this);

			}
		}
		mModifiedDate = new Date();
	}

	private ArrayList<ArrayList<Action>> initializeChildren() {
		ArrayList<ArrayList<Action>> children = new ArrayList<ArrayList<Action>>(
				OUTCOME_CATEGORIES);

		for (int i = 0; i < OUTCOME_CATEGORIES; i++) {
			children.add(new ArrayList<Action>());
		}

		children.trimToSize();
		return children;
	}

	public boolean hasActiveTasks() {
		return !(mChildren.get(0).isEmpty());
	}

    public boolean hasPendingTasks() {
        for(Action sub : mChildren.get(PENDING)){
            Log.d("ACTION", sub.toString());
        }

        boolean empty = !(mChildren.get(PENDING).isEmpty());
        Log.d("ACTIAN" , this.toString() + " " + String.valueOf(empty) );
        return empty;
    }

	public void adopt(Action a) {

		Action oldParent = a.getParent();
		if (oldParent != null)
			oldParent.removeChild(a);

		a.setParent(this);

		ArrayList<Action> currentList = mChildren.get(a.getActionStatus());
		a.setPriority(currentList.size());

		currentList.add(a);
		verifyStatusBasedOnChildren();
	}

	public void removeChild(Action a) {
		int index = -1;
		int listIndex = -1;
		for (int j = 0; j < mChildren.size(); j++) {
			for (Iterator<Action> it = mChildren.get(j).iterator(); it
					.hasNext();) {
				Action b = it.next();

				if (a.getId().equals(b.getId())) {
					index = mChildren.get(j).indexOf(b);
					Log.d("RemoveChild at index ", String.valueOf(index)
							+ " of " + String.valueOf(j));
					listIndex = j;
					it.remove();
				}
			}
		}

		if (index > -1) {
			for (int i = index; i < mChildren.get(listIndex).size() - 1; i++) {
				mChildren.get(listIndex).get(i).setPriority(i);
			}
		}
	}

	public boolean equals(Action a) {
		return (this.getId().equals(a.getId())) ? true : false;
	}

	public String toString() {
		String Uuid = getId().toString();
		String shortId;

		if (Uuid.length() > 5) {
			shortId = (String) Uuid.subSequence(Uuid.length() - 4,
					Uuid.length() - 1);
		} else {
			shortId = Uuid;
		}
		return getTitle() + " Id: " + shortId;
	}

	public static final int TOP_FIVE_VIEW = 4;

	public ArrayList<Action> getActions(int viewSetting) {
		if (viewSetting >= 0 && viewSetting < 4) {
			return this.getChildren().get(viewSetting);
		} else if (viewSetting == TOP_FIVE_VIEW) {
			ArrayList<Action> output = new ArrayList<Action>();
			ArrayList<Action> fullList = getIncomplete();
			for (int i = 0; i < 5 && i < fullList.size(); i++) {
				output.add(fullList.get(i));
			}
			return output;
		} else {
			Log.e("getActions",
					"getActions has been passed an illegal argument");
			return null;
		}
	}

	private int getUnpinnedTop(ArrayList<Action> list) {
		for (int i = 0; i < list.size(); i++) {
			if (!list.get(i).isPinned())
				return i;
		}
		return list.size();
	}

	public void moveToUnpinnedTop() {
		ArrayList<Action> currentList = this.getContainingList();
		moveWithinList(mPriority, getUnpinnedTop(currentList));
	}

	public void moveWithinList(int from, int to) {
		ArrayList<Action> currentList = getContainingList();

		if (to < from) {
			for (int i = to; i < from; i++) {
				currentList.get(i).mPriority++;
			}

		} else {
			for (int i = from + 1; i <= to; i++) {
				currentList.get(i).mPriority--;
			}
		}

		currentList.get(from).setPriority(to);
		currentList.add(to, currentList.remove(from));

	}

	public Action getFirstSubAction() {
		if (this.hasActiveTasks()) {
			return mChildren.get(0).get(0);
		} else
			return null;
	}

	private Date toJavaDate(String excelDate) {
		if (excelDate == null || excelDate == "")
			return null;

		Date output = new Date();

		try {
			output = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.US)
					.parse(excelDate);
		} catch (ParseException e) {
			output = new Date();
		}
		return output;
	}

	public String toFileTextLine() {
		StringBuilder sb = new StringBuilder();
		sb.append(mTitle);
		sb.append("\t");
		sb.append(mOutcomeName);
		sb.append("\t");
		sb.append(mContextName);
		sb.append("\t");
		sb.append(String.valueOf(mActionStatus));
		sb.append("\t");
		sb.append(android.text.format.DateFormat.format("yyyy.MM.dd HH:mm",
				mCreatedDate));
		sb.append("\t");
		sb.append(android.text.format.DateFormat.format("yyyy.MM.dd HH:mm",
				mModifiedDate));
		sb.append("\t");
		if (mStartDate != null)
			sb.append(android.text.format.DateFormat.format("yyyy.MM.dd HH:mm",
					mStartDate));
		sb.append("\t");
		sb.append(String.valueOf(mMinutesActual));
		sb.append("\t");
		sb.append((mPinned) ? "pinned" : "");
		sb.append("\t");
		sb.append(String.valueOf(mMinutesExpected));
		sb.append("\t");
		sb.append(mId.toString());
		sb.append("\t");
		if (mParent != null)
			sb.append(mParent.getId().toString());
		sb.append("\t");
		sb.append(String.valueOf(mPriority));
		sb.append("\t");
		if (mDueDate != null)
			sb.append(android.text.format.DateFormat.format("yyyy.MM.dd HH:mm",
					mDueDate));
		sb.append("\t");
		sb.append(String.valueOf(mRepeatInterval));
		sb.append("\t");
		sb.append(String.valueOf(mCompletedToday));
		sb.append("\t");
		if (mTodaysDate != null)
			sb.append(android.text.format.DateFormat.format("yyyy.MM.dd HH:mm",
					mTodaysDate));
        sb.append("\t");
        sb.append(String.valueOf(mRepeatNumber));
		sb.append("\n");
		return sb.toString();
	}

	private boolean confirmDay() {
		Date rightNow = new Date();
		long rightNowMilis = rightNow.getTime();
		Calendar storedDate = Calendar.getInstance();
		storedDate.setTime(mTodaysDate);

		// The day ends at 4am, so get the calendar date and add 28 hours
		long beginningOfDay = new GregorianCalendar(
				storedDate.get(Calendar.YEAR), storedDate.get(Calendar.MONTH),
				storedDate.get(Calendar.DAY_OF_MONTH)).getTime().getTime()
				+ 1000 * 60 * 60 * 4;

		long endOfDay = new GregorianCalendar(storedDate.get(Calendar.YEAR),
				storedDate.get(Calendar.MONTH),
				storedDate.get(Calendar.DAY_OF_MONTH)).getTime().getTime()
				+ 1000 * 60 * 60 * 28;

		if (rightNowMilis < endOfDay && rightNowMilis > beginningOfDay) {
			return true;
		} else {
			Log.e("Action.java", "confirmDay returned false");
			return false;
		}
	}

	public int getCompletedToday() {
		if (confirmDay()) {
			return mCompletedToday;
		} else {
			mCompletedToday = 0;
			return mCompletedToday;
		}
	}

	private void incrementCompletedHelper() {
		mCompletedToday++;
	}

	public void incrementCompleted() {
		if (mCompletedToday == 0) {
			mTodaysDate = new Date();
		}

		if (confirmDay()) {
			Action a = this;

			a.incrementCompletedHelper();

			while (a.getParent() != null && !a.getParent().equals(a)) {
				a = a.getParent();
				a.incrementCompletedHelper();
			}
		} else {
			mCompletedToday = 1;
		}

	}
	
	public int countChildren(){
		int count = 0;
		
		for(Action a: getIncomplete()){
			if(a.hasChildren()){
				count += a.countChildren();
			} else {
				count ++;
			}
		}
		
		return count;
	}

	public ArrayList<Action> getContainingList() {
		return getParent().getChildren().get(this.mActionStatus);
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

	public boolean hasChildren() {
		for (ArrayList<Action> list : mChildren) {
			if (!list.isEmpty())
				return true;
		}
		return false;
	}

	public void setChildren(ArrayList<ArrayList<Action>> children) {
		mChildren = children;
	}

	public String getParentUUIDString() {
		return mParentUUIDString;
	}

	public ArrayList<Action> getIncomplete() {
		return mChildren.get(INCOMPLETE);
	}

    public ArrayList<Action> getAllAsArrayList() {
        ArrayList<Action> all = this.getIncomplete();
        all.addAll(this.getPending());
        return all;
    }

	public ArrayList<Action> getPending() {
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

	public int getRepeatInterval() {
		return mRepeatInterval;
	}

    public int getRepeatNumber() { return mRepeatNumber;}

	public void setRepeatInfo(int repeatInterval, int repeatNumber) {
		mRepeatInterval = repeatInterval;
        mRepeatNumber = repeatNumber;
	}

	public boolean isPinned() {
		return mPinned;
	}

	public void setPinned(boolean pinned) {
		mPinned = pinned;
	}
	
	public String getFirstSubtaskPath(){
		if(this.hasActiveTasks()){
			int depth = 1;
			StringBuilder output = new StringBuilder();
			
			output.append(this.getTitle());
			
			Action firstChild = getIncomplete().get(0);
			Action firstChildParent = this;
			
			//Nested more than 1 deep, show the first and last parts of the branch
			while(firstChild.hasActiveTasks()){
				depth++;
				firstChildParent = firstChild;
				firstChild = firstChild.getIncomplete().get(0);
			}
			
			if(depth > 2) output.append(" ... ");
			else if(depth == 2) output.append(" -> ");
			
			if(depth >= 2) output.append(firstChildParent.getTitle());
			
			output.append("  ↴");
			
			return output.toString();
			
		} else {
			return null;
		}
	}

    public boolean isRoot() {
        return (this.getParent() == this);
    }

    public boolean isIncomplete() {
        return this.getActionStatus() == INCOMPLETE;
    }

    public boolean isPending() {
        return this.getActionStatus() == PENDING;
    }

    public boolean isRepeat() {
        return this.getRepeatInterval() != 0 && this.getRepeatNumber() != 0;
    }

    public Action createNextRepeat(Action repeatOriginal) {
        Action nextRepeat = this.copyActionNames();
        int interval = repeatOriginal.getRepeatInterval();
        int number = repeatOriginal.getRepeatNumber();
        Date nextStart = nextRepeatTime(repeatOriginal.getStartDate(), interval, number);
        nextRepeat.setStartDateRaw(nextStart);
        if (nextStart.after(new Date())) nextRepeat.setActionStatusRaw(PENDING);
        nextRepeat.setRepeatInfo(interval, number);
        nextRepeat.setDueDate(nextRepeatTime(nextStart, interval, number));
        return nextRepeat;
    }

    private void setActionStatusRaw(int actionStatus) {
        mActionStatus = actionStatus;
    }

    public Action createNextRepeatSub(Action repeatRoot){
        Action nextRepeat = this.copyActionNames();
        Date nextStart = repeatRoot.getStartDate();
        nextRepeat.setStartDateRaw(nextStart);
        if (nextStart.after(new Date())) nextRepeat.setActionStatusRaw(PENDING);
        nextRepeat.setRepeatInfo(0,0);
        nextRepeat.setDueDate(repeatRoot.getDueDate());
        return nextRepeat;
    }

    public Action copyActionNames() {
        Action nextRepeat = new Action();
        nextRepeat.setTitle(this.getTitle());
        nextRepeat.setContextName(this.getContextName());
        nextRepeat.setMinutesExpected(this.getMinutesExpected());
        return nextRepeat;
    }

    public static final int REPEAT_DAY = 1;
    public static final int REPEAT_WEEK = 2;
    public static final int REPEAT_MONTH = 3;
    public static final int REPEAT_YEAR = 4;

    public Date nextRepeatTime(Date start, int repeatInterval, int repeatNumber){
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);

        Log.d("ACTION", String.valueOf(repeatInterval));

        if(repeatNumber < 1){
            Log.d("ACTION", "get repeat set to zero");
            //repeatNumber = 1;
        }

        switch(repeatInterval){
            case REPEAT_DAY:
                calendar.add(Calendar.DATE, repeatNumber);
                break;
            case REPEAT_WEEK:
                calendar.add(Calendar.DATE, 7*repeatNumber);
                break;
            case REPEAT_MONTH:
                calendar.add(Calendar.MONTH, repeatNumber);
                break;
            case REPEAT_YEAR:
                calendar.add(Calendar.YEAR, repeatNumber);
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

    public void setRepeatDue(){
        if(mStartDate == null || mRepeatInterval == 0 || mRepeatNumber == 0){
            Log.d("ACTION", "Set repeat due date, but have invalid priors");
        }

        setDueDate(nextRepeatTime(mStartDate, mRepeatInterval, mRepeatNumber));
    }

}
