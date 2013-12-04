package com.example.criminalintent;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

public abstract class DoubleFragmentActivity extends FragmentActivity {
	
	protected abstract Fragment createFragment();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_fragment);
		
		FragmentManager fm = getSupportFragmentManager();
		
		Fragment fragment = fm.findFragmentById(R.id.fragmentContainer);
		Fragment actionFragment = fm.findFragmentById(R.id.fragmentAction);
		
		
		if(fragment == null){
			fragment = createFragment();
			fm.beginTransaction().add(R.id.fragmentContainer, fragment).commit();
		}
		if(actionFragment == null){
			actionFragment = createFragment();
			fm.beginTransaction().add(R.id.fragmentAction, actionFragment).commit();
		}
	}
}