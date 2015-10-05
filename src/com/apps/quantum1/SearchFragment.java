package com.apps.quantum1;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SearchFragment extends Fragment {
    private Action mAction;
    private ActionLab mActionLab;

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
    }

    @Override
    public void onDetach(){
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        mActionLab = ActionLab.get(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent,
                             Bundle savedInstanceState) {
        View v = (inflater.inflate(R.layout.fragment_search, parent, false));
        //TODO: Setup logic for search
        return v;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

//    public void onActivityResult(int requestCode, int resultCode, Intent data){
//
//    }

}
