package com.apps.quantum;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

public abstract class SingleFragmentActivity extends FragmentActivity  {
	
	protected abstract Fragment createFragment();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(getLayoutResId());
		
		FragmentManager fm = getSupportFragmentManager();
		
		Fragment fragment = fm.findFragmentById(R.id.listFragment);
		
		if(fragment == null){
			fragment = createFragment();
			fm.beginTransaction().add(R.id.listFragment, fragment).commit();
		}
	}
	
	protected int getLayoutResId(){
		return R.layout.activity_fragment;
	}
}
