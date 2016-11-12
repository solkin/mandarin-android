package com.tomclaw.mandarin.main.views.history;

import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.tomclaw.mandarin.R;
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

    private ChatHistoryItem historyItem;
    private ChatHistoryAdapter.SelectionModeListener selectionModeListener;

    private SelectionHelper<Long> selectionHelper;

    public BaseHistoryView(View itemView) {
        super(itemView);
        this.itemView = itemView;
        dateLayout = itemView.findViewById(getDateLayoutViewId());
        messageDate = (TextView) itemView.findViewById(getDateTextViewId());
        if (hasDeliveryState()) {
            deliveryState = (ImageView) itemView.findViewById(R.id.message_delivery);
        }
        timeView = (TextView) itemView.findViewById(getTimeViewId());
    }

    protected int getDateLayoutViewId() {
        return R.id.date_layout;
    }

    protected int getDateTextViewId() {
        return R.id.message_date;
    }

    protected abstract int getTimeViewId();

    protected abstract boolean hasDeliveryState();

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
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectionHelper.isSelectionMode()) {
                    selectionHelper.toggleChecked(historyItem.getMessageDbId());
                    selectionModeListener.onItemStateChanged(historyItem);
                    // Check for this was last selected item.
                    if (selectionHelper.isEmptySelection()) {
                        selectionModeListener.onNothingSelected();
                    }
                }
            }
        });
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                selectionModeListener.onLongClicked(historyItem, selectionHelper);
                return true;
            }
        });
    }
}
