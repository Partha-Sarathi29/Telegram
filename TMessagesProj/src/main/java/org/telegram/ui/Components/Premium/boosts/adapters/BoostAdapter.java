package org.telegram.ui.Components.Premium.boosts.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Components.ListView.AdapterWithDiffUtils;
import org.telegram.ui.Components.Premium.boosts.cells.AddChannelCell;
import org.telegram.ui.Components.Premium.boosts.cells.BoostTypeCell;
import org.telegram.ui.Components.Premium.boosts.cells.BoostTypeSingleCell;
import org.telegram.ui.Components.Premium.boosts.cells.ChatCell;
import org.telegram.ui.Components.Premium.boosts.cells.DateEndCell;
import org.telegram.ui.Components.Premium.boosts.cells.HeaderCell;
import org.telegram.ui.Components.Premium.boosts.cells.ParticipantsTypeCell;
import org.telegram.ui.Components.Premium.boosts.cells.DurationCell;
import org.telegram.ui.Components.Premium.boosts.cells.SliderCell;
import org.telegram.ui.Components.Premium.boosts.cells.SubtitleWithCounterCell;
import org.telegram.ui.Components.Premium.boosts.cells.TextInfoCell;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SlideChooseView;

import java.util.ArrayList;
import java.util.List;

public class BoostAdapter extends AdapterWithDiffUtils {

    public static final int
            HOLDER_TYPE_HEADER = 0,
            HOLDER_TYPE_BOOST_TYPE = 2,
            HOLDER_TYPE_EMPTY = 3,
            HOLDER_TYPE_SIMPLE_DIVIDER = 4,
            HOLDER_TYPE_SLIDER = 5,
            HOLDER_TYPE_SUBTITLE = 6,
            HOLDER_TYPE_TEXT_DIVIDER = 7,
            HOLDER_TYPE_ADD_CHANNEL = 8,
            HOLDER_TYPE_CHAT = 9,
            HOLDER_TYPE_DATE_END = 10,
            HOLDER_TYPE_PARTICIPANTS = 11,
            HOLDER_TYPE_DURATION = 12,
            HOLDER_TYPE_SUBTITLE_WITH_COUNTER = 13,
            HOLDER_TYPE_SINGLE_BOOST_TYPE = 14;

    private final Theme.ResourcesProvider resourcesProvider;
    private List<Item> items = new ArrayList<>();
    private RecyclerListView recyclerListView;
    private SlideChooseView.Callback sliderCallback;
    private ChatCell.ChatDeleteListener chatDeleteListener;
    private HeaderCell headerCell;

    public BoostAdapter(Theme.ResourcesProvider resourcesProvider) {
        this.resourcesProvider = resourcesProvider;
    }

    public void setItems(List<Item> items, RecyclerListView recyclerListView, SlideChooseView.Callback sliderCallback, ChatCell.ChatDeleteListener chatDeleteListener) {
        this.items = items;
        this.recyclerListView = recyclerListView;
        this.sliderCallback = sliderCallback;
        this.chatDeleteListener = chatDeleteListener;
    }

    public void updateBoostCounter(int value) {
        for (int i = 0; i < recyclerListView.getChildCount(); ++i) {
            View child = recyclerListView.getChildAt(i);
            if (child instanceof SubtitleWithCounterCell) {
                ((SubtitleWithCounterCell) child).updateCounter(true, value);
            }
            if (child instanceof ChatCell) {
                ((ChatCell) child).setCounter(value);
            }
        }
        //updates all prices
        notifyItemChanged(items.size() - 1);
        notifyItemChanged(items.size() - 2);
        notifyItemChanged(items.size() - 3);
        notifyItemChanged(items.size() - 4);
        notifyItemChanged(items.size() - 6);
    }

    public void setPausedStars(boolean paused) {
        if (headerCell != null) {
            headerCell.setPaused(paused);
        }
    }

    private RecyclerListView.Adapter realAdapter() {
        return recyclerListView.getAdapter();
    }

    @Override
    public void notifyItemChanged(int position) {
        realAdapter().notifyItemChanged(position + 1);
    }

    @Override
    public void notifyItemChanged(int position, @Nullable Object payload) {
        realAdapter().notifyItemChanged(position + 1, payload);
    }

    @Override
    public void notifyItemInserted(int position) {
        realAdapter().notifyItemInserted(position + 1);
    }

    @Override
    public void notifyItemMoved(int fromPosition, int toPosition) {
        realAdapter().notifyItemMoved(fromPosition + 1, toPosition);
    }

    @Override
    public void notifyItemRangeChanged(int positionStart, int itemCount) {
        realAdapter().notifyItemRangeChanged(positionStart + 1, itemCount);
    }

    @Override
    public void notifyItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
        realAdapter().notifyItemRangeChanged(positionStart + 1, itemCount, payload);
    }

    @Override
    public void notifyItemRangeInserted(int positionStart, int itemCount) {
        realAdapter().notifyItemRangeInserted(positionStart + 1, itemCount);
    }

    @Override
    public void notifyItemRangeRemoved(int positionStart, int itemCount) {
        realAdapter().notifyItemRangeRemoved(positionStart + 1, itemCount);
    }

    @Override
    public void notifyItemRemoved(int position) {
        realAdapter().notifyItemRemoved(position + 1);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void notifyDataSetChanged() {
        realAdapter().notifyDataSetChanged();
    }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
        int itemViewType = holder.getItemViewType();
        return itemViewType == HOLDER_TYPE_BOOST_TYPE
                || itemViewType == HOLDER_TYPE_PARTICIPANTS
                || itemViewType == HOLDER_TYPE_ADD_CHANNEL
                || itemViewType == HOLDER_TYPE_DATE_END
                || itemViewType == HOLDER_TYPE_DURATION;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        Context context = parent.getContext();
        switch (viewType) {
            default:
            case HOLDER_TYPE_HEADER:
                view = new HeaderCell(context, resourcesProvider);
                break;
            case HOLDER_TYPE_BOOST_TYPE:
                view = new BoostTypeCell(context, resourcesProvider);
                break;
            case HOLDER_TYPE_SINGLE_BOOST_TYPE:
                view = new BoostTypeSingleCell(context, resourcesProvider);
                break;
            case HOLDER_TYPE_EMPTY:
                view = new View(context);
                break;
            case HOLDER_TYPE_SIMPLE_DIVIDER:
                view = new ShadowSectionCell(context, 12, Theme.getColor(Theme.key_windowBackgroundGray, resourcesProvider));
                break;
            case HOLDER_TYPE_TEXT_DIVIDER:
                view = new TextInfoCell(context, resourcesProvider);
                break;
            case HOLDER_TYPE_ADD_CHANNEL:
                view = new AddChannelCell(context, resourcesProvider);
                break;
            case HOLDER_TYPE_SLIDER:
                view = new SliderCell(context, resourcesProvider);
                break;
            case HOLDER_TYPE_SUBTITLE:
                view = new org.telegram.ui.Cells.HeaderCell(context, resourcesProvider);
                view.setBackgroundColor(Theme.getColor(Theme.key_dialogBackground, resourcesProvider));
                break;
            case HOLDER_TYPE_SUBTITLE_WITH_COUNTER:
                view = new SubtitleWithCounterCell(context, resourcesProvider);
                view.setBackgroundColor(Theme.getColor(Theme.key_dialogBackground, resourcesProvider));
                break;
            case HOLDER_TYPE_CHAT:
                view = new ChatCell(context, resourcesProvider);
                break;
            case HOLDER_TYPE_DATE_END:
                view = new DateEndCell(context, resourcesProvider);
                break;
            case HOLDER_TYPE_PARTICIPANTS:
                view = new ParticipantsTypeCell(context, resourcesProvider);
                break;
            case HOLDER_TYPE_DURATION:
                view = new DurationCell(context, resourcesProvider);
                break;
        }
        view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new RecyclerListView.Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        final int viewType = holder.getItemViewType();
        final BoostAdapter.Item item = items.get(position);
        switch (viewType) {
            case HOLDER_TYPE_HEADER: {
                headerCell = (HeaderCell) holder.itemView;
                headerCell.setBoostViaGifsText();
                break;
            }
            case HOLDER_TYPE_BOOST_TYPE: {
                BoostTypeCell cell = (BoostTypeCell) holder.itemView;
                cell.setType(item.subType, item.intValue, (TLRPC.User) item.user, item.selectable);
                break;
            }
            case HOLDER_TYPE_SINGLE_BOOST_TYPE: {
                BoostTypeSingleCell cell = (BoostTypeSingleCell) holder.itemView;
                cell.setGiveaway((TL_stories.TL_prepaidGiveaway) item.user);
                break;
            }
            case HOLDER_TYPE_SLIDER: {
                SliderCell cell = (SliderCell) holder.itemView;
                cell.setValues(item.values, item.intValue);
                cell.setCallBack(sliderCallback);
                break;
            }
            case HOLDER_TYPE_SUBTITLE_WITH_COUNTER: {
                SubtitleWithCounterCell cell = (SubtitleWithCounterCell) holder.itemView;
                cell.setText(item.text);
                cell.updateCounter(false, item.intValue);
                break;
            }
            case HOLDER_TYPE_SUBTITLE: {
                org.telegram.ui.Cells.HeaderCell cell = (org.telegram.ui.Cells.HeaderCell) holder.itemView;
                cell.setText(item.text);
                break;
            }
            case HOLDER_TYPE_TEXT_DIVIDER: {
                TextInfoCell cell = (TextInfoCell) holder.itemView;
                cell.setText(item.text);
                cell.setBackground(item.boolValue);
                break;
            }
            case HOLDER_TYPE_CHAT: {
                ChatCell cell = (ChatCell) holder.itemView;
                if (item.peer != null) {
                    TLRPC.InputPeer peer = item.peer;
                    if (peer instanceof TLRPC.TL_inputPeerChat) {
                        cell.setChat(MessagesController.getInstance(UserConfig.selectedAccount).getChat(peer.chat_id), item.intValue, item.boolValue);
                    } else if (peer instanceof TLRPC.TL_inputPeerChannel) {
                        cell.setChat(MessagesController.getInstance(UserConfig.selectedAccount).getChat(peer.channel_id), item.intValue, item.boolValue);
                    }
                } else {
                    cell.setChat(item.chat, item.intValue, item.boolValue);
                }
                cell.setChatDeleteListener(chatDeleteListener);
                break;
            }
            case HOLDER_TYPE_PARTICIPANTS: {
                ParticipantsTypeCell cell = (ParticipantsTypeCell) holder.itemView;
                cell.setType(item.subType, item.selectable, item.boolValue, (List<TLRPC.TL_help_country>) item.user);
                break;
            }
            case HOLDER_TYPE_DATE_END: {
                DateEndCell cell = (DateEndCell) holder.itemView;
                cell.setDate(item.longValue);
                break;
            }
            case HOLDER_TYPE_DURATION: {
                DurationCell cell = (DurationCell) holder.itemView;
                cell.setDuration(item.object, item.intValue, item.intValue2, item.longValue, item.text, item.boolValue, item.selectable);
                break;
            }
            case HOLDER_TYPE_SIMPLE_DIVIDER: {
                break;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).viewType;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class Item extends AdapterWithDiffUtils.Item {
        public CharSequence text;
        public TLRPC.InputPeer peer;
        public TLRPC.Chat chat;
        public Object user;
        public boolean boolValue;
        public long longValue;
        public int intValue;
        public int intValue2;
        public int intValue3;
        public List<Integer> values;
        public float floatValue;
        public int subType;
        public Object object;

        private Item(int viewType, boolean selectable) {
            super(viewType, selectable);
        }

        public static Item asHeader() {
            return new Item(HOLDER_TYPE_HEADER, false);
        }

        public static Item asDivider() {
            return new Item(HOLDER_TYPE_SIMPLE_DIVIDER, false);
        }

        public static Item asDivider(CharSequence text, boolean onlyTopDivider) {
            Item item = new Item(HOLDER_TYPE_TEXT_DIVIDER, false);
            item.text = text;
            item.boolValue = onlyTopDivider;
            return item;
        }

        public static Item asChat(TLRPC.Chat chat, boolean removable, int count) {
            Item item = new Item(HOLDER_TYPE_CHAT, false);
            item.chat = chat;
            item.peer = null;
            item.boolValue = removable;
            item.intValue = count;
            return item;
        }

        public static Item asPeer(TLRPC.InputPeer peer, boolean removable, int count) {
            Item item = new Item(HOLDER_TYPE_CHAT, false);
            item.peer = peer;
            item.chat = null;
            item.boolValue = removable;
            item.intValue = count;
            return item;
        }

        public static Item asSingleBoost(Object user) {
            Item item = new Item(HOLDER_TYPE_SINGLE_BOOST_TYPE, false);
            item.user = user;
            return item;
        }

        public static Item asBoost(int subType, int count, Object user, int selectedSubType) {
            Item item = new Item(HOLDER_TYPE_BOOST_TYPE, selectedSubType == subType);
            item.subType = subType;
            item.intValue = count;
            item.user = user;
            return item;
        }

        public static Item asDateEnd(long time) {
            Item item = new Item(HOLDER_TYPE_DATE_END, false);
            item.longValue = time;
            return item;
        }

        public static Item asSlider(List<Integer> values, int selected) {
            Item item = new Item(HOLDER_TYPE_SLIDER, false);
            item.values = values;
            item.intValue = selected;
            return item;
        }

        public static Item asAddChannel() {
            return new Item(HOLDER_TYPE_ADD_CHANNEL, false);
        }

        public static Item asSubTitle(CharSequence text) {
            Item item = new Item(HOLDER_TYPE_SUBTITLE, false);
            item.text = text;
            return item;
        }

        public static Item asSubTitleWithCounter(CharSequence text, int counter) {
            Item item = new Item(HOLDER_TYPE_SUBTITLE_WITH_COUNTER, false);
            item.text = text;
            item.intValue = counter;
            return item;
        }

        public static Item asDuration(Object code, int months, int count, long price, int selectedMonths, String currency, boolean needDivider) {
            Item item = new Item(HOLDER_TYPE_DURATION, months == selectedMonths);
            item.intValue = months;
            item.intValue2 = count;
            item.longValue = price;
            item.boolValue = needDivider;
            item.text = currency;
            item.object = code;
            return item;
        }

        public static Item asParticipants(int subType, int selectedSubType, boolean needDivider, List<TLObject> countries) {
            Item item = new Item(HOLDER_TYPE_PARTICIPANTS, selectedSubType == subType);
            item.subType = subType;
            item.boolValue = needDivider;
            item.user = countries;
            return item;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Item i = (Item) o;
            if (viewType != i.viewType) {
                return false;
            }
            if (chat != i.chat || user != i.user || peer != i.peer || object != i.object
                    || boolValue != i.boolValue
                    || values != i.values
                    || intValue != i.intValue || intValue2 != i.intValue2 || intValue3 != i.intValue3
                    || longValue != i.longValue
                    || subType != i.subType
                    || floatValue != i.floatValue
                    || !TextUtils.equals(text, i.text)) {
                return false;
            }
            return true;
        }
    }
}
