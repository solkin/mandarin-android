package com.tomclaw.mandarin.main.fragments;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.net.Uri;
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
import com.tomclaw.mandarin.core.PreferenceHelper;
import com.tomclaw.mandarin.core.QueryHelper;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.im.icq.IcqAccountRoot;
import com.tomclaw.mandarin.main.AboutActivity;
import com.tomclaw.mandarin.main.AccountAddActivity;
import com.tomclaw.mandarin.main.AccountsActivity;
import com.tomclaw.mandarin.main.ChatActivity;
import com.tomclaw.mandarin.main.MainActivity;
import com.tomclaw.mandarin.main.RosterActivity;
import com.tomclaw.mandarin.main.SettingsActivity;
import com.tomclaw.mandarin.main.adapters.RosterDialogsAdapter;
import com.tomclaw.mandarin.util.SelectionHelper;

public class ChatDialogsFragment extends Fragment {

    private static String MARKET_URI = "market://details?id=";
    private static String GOOGLE_PLAY_URI = "http://play.google.com/store/apps/details?id=";

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

        // Dialogs list.
        dialogsAdapter = new RosterDialogsAdapter(getActivity(), getActivity().getLoaderManager());
        dialogsList = (ListView) getActivity().findViewById(R.id.chats_list_view);
        dialogsList.setAdapter(dialogsAdapter);
        dialogsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                showChat(position);
            }
        });
        dialogsList.setMultiChoiceModeListener(new MultiChoiceModeListener());

        dialogsList.setEmptyView(getActivity().findViewById(android.R.id.empty));

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

    /*public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        // SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        // Configure the search info and add any event listeners
        return true;
    }*/

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
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
            case R.id.rate_application: {
                rateApplication();
                return true;
            }
            case R.id.info: {
                Intent intent = new Intent(getActivity(), AboutActivity.class);
                startActivity(intent);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onBackPressed() {
        getActivity().finish();
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

    private class MultiChoiceModeListener implements AbsListView.MultiChoiceModeListener {

        private SelectionHelper<Integer, Integer> selectionHelper;

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            selectionHelper.onStateChanged(position, (int) id, checked);
            mode.setTitle(String.format(getString(R.string.selected_items), selectionHelper.getSelectedCount()));
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Create selection helper to store selected messages.
            selectionHelper = new SelectionHelper<Integer, Integer>();
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            // Assumes that you have menu resources
            inflater.inflate(R.menu.chat_list_edit_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;  // Return false if nothing is done.
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.close_chat_menu: {
                    try {
                        QueryHelper.modifyDialogs(getActivity().getContentResolver(), selectionHelper.getSelectedIds(), false);
                    } catch (Exception ignored) {
                        // Nothing to do in this case.
                    }
                    break;
                }
                case R.id.select_all_chats_menu: {
                    for (int c = 0; c < dialogsAdapter.getCount(); c++) {
                        dialogsList.setItemChecked(c, true);
                    }
                    return false;
                }
                default: {
                    return false;
                }
            }
            mode.finish();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            selectionHelper.clearSelection();
        }
    }

    private void rateApplication() {
        final String appPackageName = getActivity().getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(MARKET_URI + appPackageName)));
        } catch (android.content.ActivityNotFoundException ignored) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse(GOOGLE_PLAY_URI + appPackageName)));
        }
    }
}
