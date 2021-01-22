package com.tomclaw.mandarin.main.views.history;

import android.content.res.Resources;
import androidx.recyclerview.widget.RecyclerView;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import androidx.recyclerview.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.tomclaw.mandarin.R;
import com.tomclaw.mandarin.core.Settings;
import com.tomclaw.mandarin.main.ChatHistoryItem;
import com.tomclaw.mandarin.main.adapters.ChatHistoryAdapter;
import com.tomclaw.mandarin.util.SelectionHelper;
import com.tomclaw.mandarin.util.Unobfuscatable;

/**
 * Created by Solkin on 30.11.2014.
 */
public abstract class BaseHistoryView extends RecyclerView.ViewHolder implements Unobfuscatable {

    private View itemView;

    private View dateLayout;
    private TextView messageDate;
    private ImageView deliveryState;
    private TextView timeView;
    private View addonView;
    private TextView fromIdView;
    private TextView tillIdView;

    private ChatHistoryItem historyItem;
    private ChatHistoryAdapter.SelectionModeListener selectionModeListener;

    private SelectionHelper<Long> selectionHelper;

    public BaseHistoryView(View itemView) {
        super(itemView);
        this.itemView = itemView;
        dateLayout = itemView.findViewById(getDateLayoutViewId());
        messageDate = itemView.findViewById(getDateTextViewId());
        addonView = itemView.findViewById(R.id.addon_layout);
        fromIdView = itemView.findViewById(R.id.from_message_id);
        tillIdView = itemView.findViewById(R.id.till_message_id);
        if (hasDeliveryState()) {
            deliveryState = itemView.findViewById(R.id.message_delivery);
        }
        timeView = itemView.findViewById(getTimeViewId());
    }

    @SuppressWarnings("WeakerAccess")
    protected int getDateLayoutViewId() {
        return R.id.date_layout;
    }

    @SuppressWarnings("WeakerAccess")
    protected int getDateTextViewId() {
        return R.id.message_date;
    }

    protected abstract int getTimeViewId();

    protected abstract boolean hasDeliveryState();

    @SuppressWarnings("WeakerAccess")
    public SelectionHelper<Long> getSelectionHelper() {
        return selectionHelper;
    }

    public void setSelectionHelper(SelectionHelper<Long> selectionHelper) {
        this.selectionHelper = selectionHelper;
    }

    public void bind(ChatHistoryItem historyItem) {
        int backColor = selectionHelper.isChecked(historyItem.getMessageDbId()) ?
                R.color.orange_normal : android.R.color.transparent;
        itemView.setBackgroundColor(getResources().getColor(backColor));
        this.historyItem = historyItem;
        if (historyItem.isDateVisible()) {
            // Update visibility.
            dateLayout.setVisibility(View.VISIBLE);
            // Update date text view.
            messageDate.setText(historyItem.getMessageDateText());
        } else {
            // Update visibility.
            dateLayout.setVisibility(View.GONE);
        }
        if (Settings.HISTORY_DEBUG) {
            addonView.setVisibility(View.VISIBLE);
            fromIdView.setText(String.valueOf(historyItem.getMessagePrevId()));
            tillIdView.setText(String.valueOf(historyItem.getMessageId()));
        } else {
            addonView.setVisibility(View.GONE);
        }
        if (hasDeliveryState()) {
            // TODO: Implement delivery and read history based on message id.
//            Drawable drawable;
//            boolean animated = false;
//            switch (historyItem.getMessageState()) {
//                case GlobalProvider.HISTORY_MESSAGE_STATE_ERROR:
//                    drawable = getResources().getDrawable(R.drawable.ic_error);
//                    break;
//                case GlobalProvider.HISTORY_MESSAGE_STATE_UNDETERMINED:
//                case GlobalProvider.HISTORY_MESSAGE_STATE_SENDING:
//                    drawable = getResources().getDrawable(R.drawable.sending_anim);
//                    animated = true;
//                    break;
//                case GlobalProvider.HISTORY_MESSAGE_STATE_SENT:
//                    drawable = getResources().getDrawable(R.drawable.ic_sent);
//                    break;
//                case GlobalProvider.HISTORY_MESSAGE_STATE_DELIVERED:
//                    drawable = getResources().getDrawable(R.drawable.ic_delivered);
//                    break;
//                default:
//                    drawable = null;
//            }
//            if (drawable == null || historyItem.getContentState() != GlobalProvider.HISTORY_CONTENT_STATE_STABLE) {
//                deliveryState.setVisibility(View.INVISIBLE);
//            } else {
//                deliveryState.setVisibility(View.VISIBLE);
//                deliveryState.setImageDrawable(drawable);
//                if (animated) {
//                    AnimationDrawable animatedState = ((AnimationDrawable) drawable);
//                    animatedState.stop();
//                    animatedState.start();
//                }
//            }
        }
        timeView.setText(historyItem.getMessageTimeText());
        bindClickListeners();
    }

    protected Resources getResources() {
        return itemView.getResources();
    }

    protected View findViewById(int id) {
        return itemView.findViewById(id);
    }

    @SuppressWarnings("WeakerAccess")
    public ChatHistoryItem getHistoryItem() {
        return historyItem;
    }

    public void setContentClickListener(ChatHistoryAdapter.ContentMessageClickListener contentClickListener) {
    }

    protected int getStyledColor(int attrId) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = itemView.getContext().getTheme();
        theme.resolveAttribute(attrId, typedValue, true);
        return typedValue.data;
    }

    public void setSelectionModeListener(ChatHistoryAdapter.SelectionModeListener selectionModeListener) {
        this.selectionModeListener = selectionModeListener;
    }

    protected abstract View getClickableView();

    private void bindClickListeners() {
        View view = getClickableView();
        view.setOnClickListener(v -> {
            if (selectionHelper.isSelectionMode()) {
                selectionHelper.toggleChecked(historyItem.getMessageDbId());
                selectionModeListener.onItemStateChanged(historyItem);
                // Check for this was last selected item.
                if (selectionHelper.isEmptySelection()) {
                    selectionModeListener.onNothingSelected();
                }
            }
        });
        view.setOnLongClickListener(v -> {
            selectionModeListener.onLongClicked(historyItem, selectionHelper);
            return true;
        });
    }
}
