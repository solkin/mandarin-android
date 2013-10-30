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

    public ChatDialogsFragment(Activity activity){
        this.activity = activity;
        chatDialogsAdapter = new ChatDialogsAdapter(this.activity, this.activity.getLoaderManager());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return activity.getLayoutInflater().inflate(R.layout.chat_dialogs_list_fragment, null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

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

    public void registerDataSetObserver(DataSetObserver observer) {
        chatDialogsAdapter.registerDataSetObserver(observer);
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
        onItemClickListener = listener;
    }
}
