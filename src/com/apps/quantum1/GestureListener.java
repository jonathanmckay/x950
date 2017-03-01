package com.apps.quantum1;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

class GestureListener extends GestureDetector.SimpleOnGestureListener
{
    
       static String currentGestureDetected;
       
      // Override s all the callback methods of GestureDetector.SimpleOnGestureListener
              
      @Override
      public boolean onSingleTapUp(MotionEvent ev) {
          currentGestureDetected=ev.toString();
          Log.d("GD", "Single tap" );
        return true;
      }
      @Override
      public void onShowPress(MotionEvent ev) {
          currentGestureDetected=ev.toString();

          Log.d("GD", "ShowPress");
        
      }
      @Override
      public void onLongPress(MotionEvent ev) {
          currentGestureDetected=ev.toString();

          Log.d("GD", "LongPress");
      }
      @Override
      public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
          currentGestureDetected=e1.toString()+ "  "+e2.toString();

          Log.d("GD", "onScroll");
        return false;
      }
      @Override
      public boolean onDown(MotionEvent ev) {
          currentGestureDetected=ev.toString();

          Log.d("GD", "onDown" );
        return true;
      }
      @Override
      public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
          currentGestureDetected=e1.toString()+ "  "+e2.toString();
          
          Log.d("GD", "fling" );
        
          return false;
      }

}
