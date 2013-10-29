package com.tomclaw.mandarin.main.fragments;

import android.app.Fragment;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.main.ChatActivity;
import com.tomclaw.mandarin.main.adapters.ChatDialogsAdapter;

/**
 * Created with IntelliJ IDEA.
 * User: lapshin
 * Date: 10/25/13
 * Time: 7:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChatDialogsFragment extends Fragment {

    public ChatDialogsAdapter chatDialogsAdapter;
    private ListView dialogsList;

    private ChatActivity chatActivity;

    public ChatDialogsFragment(ChatActivity activity){
        chatActivity = activity;

        chatDialogsAdapter = new ChatDialogsAdapter(chatActivity, chatActivity.getLoaderManager());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return chatActivity.getLayoutInflater().inflate(R.layout.chat_dialogs_list_fragment, null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        dialogsList = (ListView) getActivity().findViewById(R.id.chat_dialogs_list);
        dialogsList.setAdapter(chatDialogsAdapter);
        dialogsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                chatActivity.selectItem(position);
            }
        });
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
}
