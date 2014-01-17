package com.apps.quantum;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.LinearLayout;


public class BackViewGroup extends LinearLayout {


	public static final String TAG = "BackViewGroup";
	float mStartX, mStartY, mLastX, mLastY;
	private GestureDetector mDetector;
    
    public BackViewGroup(Context context) {
		super(context);
		 GestureListener detector = new GestureListener();
		//gestureScanner = new GestureDetector(context, this);
		// TODO Auto-generated constructor stub
		 
		  mDetector = new GestureDetector(context, detector);

	}
	
	public BackViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);   

		GestureListener detector = new GestureListener();
        mDetector = new GestureDetector(context, detector);
    }	
	
    @Override 
    public boolean onTouchEvent(MotionEvent event){ 
    	boolean detector = mDetector.onTouchEvent(event);
    	
    	
    	//Log.d(TAG, "event detected, " + event.toString());
    	if(detector && event.getAction() == MotionEvent.ACTION_DOWN){
    		//super.onTouchEvent(event);
    		if(!super.onTouchEvent(event)) return true;
    	}
    	if(detector && event.getAction() != MotionEvent.ACTION_DOWN){
    		Log.d(TAG, "click detected");
    	} else {   
    		
    		if(event.getAction() == MotionEvent.ACTION_UP){
        		this.setVisibility(INVISIBLE);
        	
        	}
    		
    		//Log.d(TAG, "no click detected");
    		//super.onTouchEvent(event);
    	}
    	return false;
    	
    }
    
   
   public boolean onInterceptTouchEvent(MotionEvent event) {
	  boolean detector = mDetector.onTouchEvent(event);
	   if(detector){
		   Log.d(TAG, "backview does not intercept");
	   		return false;
   		}
	   else{
		   Log.d(TAG, "backview intercepts");
		   return true;
	   } 
	   	   
	   
	  
    
       
       /*boolean superBool = super.onInterceptTouchEvent(ev);
        Log.d("Parent interceptiong touch event ", String.valueOf(superBool));
        boolean intercept =  mDetector.onTouchEvent(ev);
        
        
        if(intercept){
        	
        	
        } else {
        	
        }
        
        
        return superBool && intercept ;*/
    }
}

