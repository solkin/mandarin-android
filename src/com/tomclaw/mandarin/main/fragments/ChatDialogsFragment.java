package com.tomclaw.mandarin.main.fragments;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.GlobalProvider;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.main.AccountsActivity;
import com.tomclaw.mandarin.main.ChatActivity;
import com.tomclaw.mandarin.main.MainActivity;
import com.tomclaw.mandarin.main.RosterActivity;
import com.tomclaw.mandarin.main.SettingsActivity;
import com.tomclaw.mandarin.main.adapters.RosterDialogsAdapter;

public class ChatDialogsFragment extends Fragment {

    private RosterDialogsAdapter dialogsAdapter;
    private ListView dialogsList;

    private boolean isDualPane = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return getActivity().getLayoutInflater().inflate(R.layout.chat_dialogs_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        dialogsAdapter = new RosterDialogsAdapter(getActivity(), getActivity().getLoaderManager());

        dialogsList = (ListView) getActivity().findViewById(R.id.chats_list_view);
        dialogsList.setAdapter(dialogsAdapter);
        dialogsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showChat(position);
            }
        });
        dialogsList.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
            }
        });

        View chatFrame = getActivity().findViewById(R.id.chat);
        isDualPane = chatFrame != null && chatFrame.getVisibility() == View.VISIBLE;

        if (isDualPane) {
            dialogsList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main_activity_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                getActivity().onBackPressed();
                return true;
            }
            case R.id.accounts: {
                Intent intent = new Intent(getActivity(), AccountsActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.create_dialog: {
                Intent intent = new Intent(getActivity(), RosterActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.settings: {
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showChat(int position) {
        int buddyDbId = dialogsAdapter.getBuddyDbId(position);
        Log.d(Settings.LOG_TAG, "Check out dialog with buddy (db id): " + buddyDbId);
        if (isDualPane) {
            dialogsList.setItemChecked(position, true);

            ChatFragment chatFragment = (ChatFragment)
                    getFragmentManager().findFragmentById(R.id.chat);
            if (chatFragment == null || chatFragment.getBuddyDbId() != buddyDbId) {
                chatFragment = ChatFragment.newInstance(buddyDbId);

                getFragmentManager().beginTransaction().replace(R.id.chat, chatFragment).
                        setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE).commit();
            }
        } else {
            Intent intent = new Intent(getActivity(), ChatActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra(GlobalProvider.HISTORY_BUDDY_DB_ID, buddyDbId);
            startActivity(intent);
        }
    }

    public boolean isDualPane() {
        return isDualPane;
    }
}
