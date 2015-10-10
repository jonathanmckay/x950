package com.apps.quantum1;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SearchFragment extends Fragment {
    private ActionLab mActionLab;
    HashMap<UUID, Action> mActionHash;
    private SearchType mSearchType;


    private enum SearchType{ SEARCH_ACTION, SEARCH_CONTEXT };

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
    }

    @Override
    public void onDetach(){
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActionLab = ActionLab.get(getActivity());
        mActionHash = mActionLab.getActionHash();
        mSearchType = SearchType.SEARCH_ACTION;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent,
                             Bundle savedInstanceState) {
        View v = (inflater.inflate(R.layout.fragment_search, parent, false));

        //Determine which button to show
        final ImageButton searchButton = (ImageButton) v
                .findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mSearchType == SearchType.SEARCH_ACTION) {
                    mSearchType = SearchType.SEARCH_CONTEXT;
                    searchButton.setImageResource(R.drawable.ic_search_context);
                } else {
                    mSearchType = SearchType.SEARCH_ACTION;
                    searchButton.setImageResource(R.drawable.ic_search_action);
                }
                refreshAdapters();
            }
        });

        return v;
    }

    public void onStart() {
        super.onStart();

        refreshAdapters();

        //Hide the soft keyboard
        final InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
    }

    public void refreshAdapters() {
        mActionHash = mActionLab.getActionHash();
        if (mSearchType == SearchType.SEARCH_ACTION) {
            setSearchTaskAdapter();
        } else {
            setSearchContextAdapter();
        }
    }

    //Todo: Update adapters when action name is modified
    //(Low priority; inconsistent only if change action name, then search that action while already in that action)
    private void setSearchTaskAdapter() {
        ArrayList<String> tasknames = mActionLab.getTaskNames();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_dropdown_item_1line,
                tasknames.toArray(new String[tasknames.size()]));
        final AutoCompleteTextView searchTextView = (AutoCompleteTextView)
                getActivity().findViewById(R.id.searchTextView);
        searchTextView.setAdapter(adapter);
        searchTextView.clearFocus();

        //TODO: If same name applies to multiple actions, will only go to first one in list
        final HashMap<String, Action> titleActionHash = new HashMap();
        for (HashMap.Entry<UUID, Action> entry : mActionHash.entrySet()) {
            titleActionHash.put(entry.getValue().getTitle(), entry.getValue());
        }

        //go to action on click
        searchTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                searchTextView.setSelection(searchTextView.getText().length());
                String actionName = searchTextView.getText().toString();
                FragmentManager fm = getActivity().getSupportFragmentManager();
                ActionListFragmentDSLV listFragment = (ActionListFragmentDSLV) fm.findFragmentById(R.id.listFragment);
                searchTextView.setText("");

                listFragment.goToAction(titleActionHash.get(actionName));
            }
        });

        //TODO: Add listener to search on return
    }

    private void setSearchContextAdapter() {
        //Get context names and a final title-action hash to be used in click listener
        HashSet<String> contextNames = new HashSet<>();
        final HashMap<String, Action> titleActionHash = new HashMap();
        for (HashMap.Entry<UUID, Action> entry : mActionHash.entrySet()) {
            titleActionHash.put(entry.getValue().getTitle(), entry.getValue());
            String contextName = entry.getValue().getContextName();
            if (contextName.length() > 0) {
                contextNames.add(contextName);
            }
        }

        //TODO: This will hide all but one of actions sharing the same name
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_dropdown_item_1line,
                contextNames.toArray(new String[contextNames.size()]));
        final AutoCompleteTextView searchTextView = (AutoCompleteTextView)
                getActivity().findViewById(R.id.searchTextView);
        searchTextView.setAdapter(adapter);
        searchTextView.clearFocus();

        searchTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String contextName = searchTextView.getText().toString();
                FragmentManager fm = getActivity().getSupportFragmentManager();
                ActionListFragmentDSLV listFragment = (ActionListFragmentDSLV) fm.findFragmentById(R.id.listFragment);

                final ArrayList<Action> actions = new ArrayList<>();
                //Add all relevant actions (including pending/repeat
                for (HashMap.Entry<String, Action> entry : titleActionHash.entrySet()) {
                    Action a = entry.getValue();
                    if (a.getContextName().equals(contextName)) actions.add(a);
                }

                searchTextView.setText("");
                listFragment.goToAction(mActionLab.getRoot());
                listFragment.displayTheseActions(actions);
            }
        });

    }

    @Override
    public void onPause() {
        super.onPause();
    }

//    public void onActivityResult(int requestCode, int resultCode, Intent data){
//
//    }

}
