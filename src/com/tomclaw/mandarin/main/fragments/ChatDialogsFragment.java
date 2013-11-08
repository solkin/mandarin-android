package com.tomclaw.mandarin.main.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.adapters.ChatDialogsAdapter;

/**
 * Created with IntelliJ IDEA.
 * User: lapshin
 * Date: 10/25/13
 * Time: 7:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChatDialogsFragment extends Fragment {

    private ChatDialogsAdapter chatDialogsAdapter;
    private ListView dialogsList;

    private Activity activity;

    private AdapterView.OnItemClickListener onItemClickListener;
    private DataSetObserver dataSetObserver;

    public ChatDialogsFragment(AdapterView.OnItemClickListener onItemClickListener, DataSetObserver dataSetObserver) {
        this.onItemClickListener = onItemClickListener;
        this.dataSetObserver = dataSetObserver;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return getActivity().getLayoutInflater().inflate(R.layout.chat_dialogs_list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        chatDialogsAdapter = new ChatDialogsAdapter(getActivity(), getActivity().getLoaderManager());
        chatDialogsAdapter.registerDataSetObserver(dataSetObserver);

        dialogsList = (ListView) getActivity().findViewById(R.id.chat_dialogs_list);
        dialogsList.setAdapter(chatDialogsAdapter);
        dialogsList.setOnItemClickListener(onItemClickListener);

    }

    public int getBuddyDbId(int position) {
        return chatDialogsAdapter.getBuddyDbId(position);
    }

    public String getBuddyNick(int position) {
        return chatDialogsAdapter.getBuddyNick(position);
    }

    public int getBuddyPosition(int buddyDbId) {
        return chatDialogsAdapter.getBuddyPosition(buddyDbId);
    }

    public int getCount() {
        return chatDialogsAdapter.getCount();
    }
}
