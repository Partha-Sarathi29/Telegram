package org.telegram.ui.Components.Premium.boosts;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.recyclerview.widget.DefaultItemAnimator;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BottomSheetWithRecyclerListView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Premium.PremiumPreviewBottomSheet;
import org.telegram.ui.Components.Premium.boosts.adapters.BoostAdapter;
import org.telegram.ui.Components.Premium.boosts.cells.ActionBtnCell;
import org.telegram.ui.Components.Premium.boosts.cells.AddChannelCell;
import org.telegram.ui.Components.Premium.boosts.cells.BaseCell;
import org.telegram.ui.Components.Premium.boosts.cells.DateEndCell;
import org.telegram.ui.Components.Premium.boosts.cells.ParticipantsTypeCell;
import org.telegram.ui.Components.Premium.boosts.cells.BoostTypeCell;
import org.telegram.ui.Components.Premium.boosts.cells.DurationCell;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.Premium.boosts.adapters.BoostAdapter.Item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BoostViaGiftsBottomSheet extends BottomSheetWithRecyclerListView implements SelectorBottomSheet.SelectedObjectsListener {

    private static final int BOTTOM_HEIGHT_DP = 68;

    public interface ActionListener {
        void onAddChat(List<TLObject> chats);

        void onSelectUser(List<TLObject> users);

        void onSelectCountries(List<TLObject> countries);
    }

    private final ArrayList<Item> items = new ArrayList<>();
    private final List<Integer> sliderValues = BoostRepository.isGoogleBillingAvailable() ? Arrays.asList(1, 3, 5, 7, 10, 25, 50) : Arrays.asList(1, 3, 5, 7, 10, 25, 50, 100);
    private final TLRPC.Chat currentChat;
    private final List<TLObject> selectedChats = new ArrayList<>();
    private final List<TLObject> selectedUsers = new ArrayList<>();
    private final List<TLObject> selectedCountries = new ArrayList<>();
    private final List<TLRPC.TL_premiumGiftCodeOption> giftCodeOptions = new ArrayList<>();
    private BoostAdapter adapter;
    private int selectedBoostType = BoostTypeCell.TYPE_GIVEAWAY;
    private int selectedParticipantsType = ParticipantsTypeCell.TYPE_ALL;
    private int selectedMonths = 3;
    private long selectedEndDate = BoostDialogs.getThreeDaysAfterToday();
    private int selectedSliderIndex = 0;
    private ActionBtnCell actionBtn;
    private ActionListener actionListener;
    private int top;
    private Runnable onCloseClick;
    private final TL_stories.TL_prepaidGiveaway prepaidGiveaway;

    public BoostViaGiftsBottomSheet(BaseFragment fragment, boolean needFocus, boolean hasFixedSize, long dialogId, TL_stories.TL_prepaidGiveaway prepaidGiveaway) {
        super(fragment, needFocus, hasFixedSize);
        this.prepaidGiveaway = prepaidGiveaway;
        this.topPadding = 0.3f;
        setApplyTopPadding(false);
        setApplyBottomPadding(false);
        useBackgroundTopPadding = false;
        backgroundPaddingLeft = 0;
        updateTitle();
        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setDurations(350);
        itemAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        itemAnimator.setDelayAnimations(false);
        itemAnimator.setSupportsChangeAnimations(false);
        ((ViewGroup.MarginLayoutParams) actionBar.getLayoutParams()).leftMargin = 0;
        ((ViewGroup.MarginLayoutParams) actionBar.getLayoutParams()).rightMargin = 0;

        recyclerListView.setPadding(backgroundPaddingLeft, 0, backgroundPaddingLeft, AndroidUtilities.dp(BOTTOM_HEIGHT_DP));
        recyclerListView.setItemAnimator(itemAnimator);
        recyclerListView.setOnItemClickListener((view, position) -> {
            if (view instanceof BaseCell) {
                if (view instanceof BoostTypeCell) {
                    int boostType = ((BoostTypeCell) view).getSelectedType();
                    if (boostType == BoostTypeCell.TYPE_SPECIFIC_USERS) {
                        if (actionListener != null) {
                            actionListener.onSelectUser(selectedUsers);
                        }
                    } else {
                        ((BaseCell) view).markChecked(recyclerListView);
                        selectedBoostType = boostType;
                        updateRows(true, true);
                        updateActionButton(true);
                        updateTitle();
                    }
                } else {
                    ((BaseCell) view).markChecked(recyclerListView);
                }
            }
            if (view instanceof ParticipantsTypeCell) {
                int tmpParticipantsType = ((ParticipantsTypeCell) view).getSelectedType();
                if (selectedParticipantsType == tmpParticipantsType) {
                    if (actionListener != null) {
                        actionListener.onSelectCountries(selectedCountries);
                    }
                }
                selectedParticipantsType = tmpParticipantsType;
            } else if (view instanceof DurationCell) {
                selectedMonths = ((TLRPC.TL_premiumGiftCodeOption) ((DurationCell) view).getGifCode()).months;
            } else if (view instanceof DateEndCell) {
                BoostDialogs.showDatePicker(fragment.getContext(), selectedEndDate, (notify, timeSec) -> {
                    selectedEndDate = timeSec * 1000L;
                    updateRows(false, true);
                }, resourcesProvider);
            } else if (view instanceof AddChannelCell) {
                if (actionListener != null) {
                    actionListener.onAddChat(selectedChats);
                }
            }
        });
        this.currentChat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
        this.adapter.setItems(items, recyclerListView, sliderIndex -> {
            selectedSliderIndex = sliderIndex;
            int counter = getSelectedSliderValue();
            actionBtn.updateCounter(counter);
            updateRows(false, false);
            adapter.updateBoostCounter(getSelectedSliderValueWithBoosts());
        }, deletedChat -> {
            selectedChats.remove(deletedChat);
            updateRows(true, true);
        });
        updateRows(false, false);
        actionBtn = new ActionBtnCell(getContext(), resourcesProvider);
        actionBtn.setOnClickListener(v -> {
            if (actionBtn.isLoading()) {
                return;
            }

            if (selectedBoostType == BoostTypeCell.TYPE_SPECIFIC_USERS) {
                List<TLRPC.TL_premiumGiftCodeOption> options = BoostRepository.filterGiftOptions(giftCodeOptions, selectedUsers.size());
                for (int i = 0; i < options.size(); i++) {
                    TLRPC.TL_premiumGiftCodeOption option = options.get(i);
                    if (option.months == selectedMonths && selectedUsers.size() > 0) {
                        if (BoostRepository.isGoogleBillingAvailable() && BoostDialogs.checkReduceUsers(getContext(), resourcesProvider, giftCodeOptions, option)) {
                            return;
                        }
                        actionBtn.updateLoading(true);
                        BoostRepository.payGiftCode(selectedUsers, option, currentChat, fragment, result -> {
                            dismiss();
                            NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.boostByChannelCreated, currentChat, false);
                        }, error -> {
                            actionBtn.updateLoading(false);
                            BoostDialogs.showToastError(getContext(), error);
                        });
                        break;
                    }
                }
            } else {
                List<TLRPC.TL_premiumGiftCodeOption> options = BoostRepository.filterGiftOptions(giftCodeOptions, getSelectedSliderValue());
                if (isPreparedGiveaway()) {
                    int dateInt = BoostRepository.prepareServerDate(selectedEndDate);
                    boolean onlyNewSubscribers = selectedParticipantsType == ParticipantsTypeCell.TYPE_NEW;
                    actionBtn.updateLoading(true);
                    BoostRepository.launchPreparedGiveaway(prepaidGiveaway, selectedChats, selectedCountries, currentChat, dateInt, onlyNewSubscribers,
                            result -> {
                                dismiss();
                                AndroidUtilities.runOnUIThread(() -> NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.boostByChannelCreated, currentChat, true, prepaidGiveaway), 220);
                            }, error -> {
                                actionBtn.updateLoading(false);
                                BoostDialogs.showToastError(getContext(), error);
                            });
                } else {
                    for (int i = 0; i < options.size(); i++) {
                        TLRPC.TL_premiumGiftCodeOption option = options.get(i);
                        if (option.months == selectedMonths) {
                            if (BoostRepository.isGoogleBillingAvailable() && BoostDialogs.checkReduceQuantity(getContext(), resourcesProvider, giftCodeOptions, option, arg -> {
                                selectedSliderIndex = sliderValues.indexOf(arg.users);
                                updateRows(true, true);
                                updateActionButton(true);
                            })) {
                                return;
                            }
                            boolean onlyNewSubscribers = selectedParticipantsType == ParticipantsTypeCell.TYPE_NEW;
                            int dateInt = BoostRepository.prepareServerDate(selectedEndDate);
                            actionBtn.updateLoading(true);
                            BoostRepository.payGiveAway(selectedChats, selectedCountries, option, currentChat, dateInt, onlyNewSubscribers, fragment, result -> {
                                dismiss();
                                AndroidUtilities.runOnUIThread(() -> NotificationCenter.getInstance(UserConfig.selectedAccount).postNotificationName(NotificationCenter.boostByChannelCreated, currentChat, true), 220);
                            }, error -> {
                                actionBtn.updateLoading(false);
                                BoostDialogs.showToastError(getContext(), error);
                            });
                            break;
                        }
                    }
                }
            }
        });
        updateActionButton(false);
        containerView.addView(actionBtn, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, BOTTOM_HEIGHT_DP, Gravity.BOTTOM, 0, 0, 0, 0));
        loadOptions();
    }

    @Override
    protected boolean needPaddingShadow() {
        return false;
    }

    public void setOnCloseClick(Runnable onCloseClick) {
        this.onCloseClick = onCloseClick;
    }

    @Override
    public void dismiss() {
        if (onCloseClick != null) {
            onCloseClick.run();
        }
    }

    private void loadOptions() {
        BoostRepository.loadGiftOptions(currentChat, arg -> {
            giftCodeOptions.clear();
            giftCodeOptions.addAll(arg);
            updateRows(true, true);
        });
    }

    private void updateActionButton(boolean animated) {
        if (isPreparedGiveaway()) {
            actionBtn.setStartGiveAwayStyle(prepaidGiveaway.quantity, animated);
        } else {
            if (selectedBoostType == BoostTypeCell.TYPE_GIVEAWAY) {
                actionBtn.setStartGiveAwayStyle(getSelectedSliderValue(), animated);
            } else {
                actionBtn.setGiftPremiumStyle(selectedUsers.size(), animated, selectedUsers.size() > 0);
            }
        }
    }

    private boolean isGiveaway() {
        return selectedBoostType == BoostTypeCell.TYPE_GIVEAWAY;
    }

    public void setActionListener(ActionListener actionListener) {
        this.actionListener = actionListener;
    }

    @Override
    protected void onPreDraw(Canvas canvas, int top, float progressToFullView) {
        this.top = top;
    }

    public int getTop() {
        return Math.max(-AndroidUtilities.dp(16), top - (actionBar.getVisibility() == View.VISIBLE ? (AndroidUtilities.statusBarHeight + AndroidUtilities.dp(16)) : 0));
    }

    private int getSelectedSliderValue() {
        return sliderValues.get(selectedSliderIndex);
    }

    private int getSelectedSliderValueWithBoosts() {
        return sliderValues.get(selectedSliderIndex) * BoostRepository.giveawayBoostsPerPremium();
    }

    private boolean isPreparedGiveaway() {
        return prepaidGiveaway != null;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateRows(boolean animated, boolean notify) {
        ArrayList<Item> oldItems = new ArrayList<>(items);
        items.clear();
        items.add(Item.asHeader());
        if (isPreparedGiveaway()) {
            items.add(Item.asSingleBoost(prepaidGiveaway));
        } else {
            items.add(Item.asBoost(BoostTypeCell.TYPE_GIVEAWAY, selectedUsers.size(), null, selectedBoostType));
            items.add(Item.asBoost(BoostTypeCell.TYPE_SPECIFIC_USERS, selectedUsers.size(), selectedUsers.size() > 0 ? selectedUsers.get(0) : null, selectedBoostType));
        }
        items.add(Item.asDivider());
        if (selectedBoostType == BoostTypeCell.TYPE_GIVEAWAY) {
            if (!isPreparedGiveaway()) {
                items.add(Item.asSubTitleWithCounter(LocaleController.getString("BoostingQuantityPrizes", R.string.BoostingQuantityPrizes), getSelectedSliderValueWithBoosts()));
                items.add(Item.asSlider(sliderValues, selectedSliderIndex));
                items.add(Item.asDivider(LocaleController.getString("BoostingChooseHowMany", R.string.BoostingChooseHowMany), false));
            }
            items.add(Item.asSubTitle(LocaleController.getString("BoostingChannelsIncludedGiveaway", R.string.BoostingChannelsIncludedGiveaway)));
            items.add(Item.asChat(currentChat, false, getSelectedSliderValueWithBoosts()));
            for (TLObject selectedChat : selectedChats) {
                if (selectedChat instanceof TLRPC.Chat) {
                    items.add(Item.asChat((TLRPC.Chat) selectedChat, true, getSelectedSliderValueWithBoosts()));
                }
                if (selectedChat instanceof TLRPC.InputPeer) {
                    items.add(Item.asPeer((TLRPC.InputPeer) selectedChat, true, getSelectedSliderValueWithBoosts()));
                }
            }
            if (selectedChats.size() < BoostRepository.giveawayAddPeersMax()) {
                items.add(Item.asAddChannel());
            }
            items.add(Item.asDivider(LocaleController.getString("BoostingChooseChannelsNeedToJoin", R.string.BoostingChooseChannelsNeedToJoin), false));
            items.add(Item.asSubTitle(LocaleController.getString("BoostingEligibleUsers", R.string.BoostingEligibleUsers)));
            items.add(Item.asParticipants(ParticipantsTypeCell.TYPE_ALL, selectedParticipantsType, true, selectedCountries));
            items.add(Item.asParticipants(ParticipantsTypeCell.TYPE_NEW, selectedParticipantsType, false, selectedCountries));
            items.add(Item.asDivider(LocaleController.getString("BoostingChooseLimitGiveaway", R.string.BoostingChooseLimitGiveaway), false));
            items.add(Item.asSubTitle(LocaleController.getString("BoostingDateWhenGiveawayEnds", R.string.BoostingDateWhenGiveawayEnds)));
            items.add(Item.asDateEnd(selectedEndDate));
            if (!isPreparedGiveaway()) {
                items.add(Item.asDivider(LocaleController.formatPluralString("BoostingChooseRandom", getSelectedSliderValue()), false));
            }
        }

        if (!isPreparedGiveaway()) {
            items.add(Item.asSubTitle(LocaleController.getString("BoostingDurationOfPremium", R.string.BoostingDurationOfPremium)));
            List<TLRPC.TL_premiumGiftCodeOption> options = BoostRepository.filterGiftOptions(giftCodeOptions, isGiveaway() ? getSelectedSliderValue() : selectedUsers.size());
            for (int i = 0; i < options.size(); i++) {
                TLRPC.TL_premiumGiftCodeOption option = options.get(i);
                items.add(Item.asDuration(option, option.months, isGiveaway() ? getSelectedSliderValue() : selectedUsers.size(), option.amount, selectedMonths, option.currency, i != options.size() - 1));
            }
        }
        String textDivider = !isPreparedGiveaway() ? LocaleController.getString("BoostingStoriesFeaturesAndTerms", R.string.BoostingStoriesFeaturesAndTerms)
                : LocaleController.formatPluralString("BoostingChooseRandom", prepaidGiveaway.quantity) + "\n\n" + LocaleController.getString("BoostingStoriesFeaturesAndTerms", R.string.BoostingStoriesFeaturesAndTerms);
        items.add(Item.asDivider(AndroidUtilities.replaceSingleTag(
                textDivider,
                Theme.key_chat_messageLinkIn, 0, () -> {
                    PremiumPreviewBottomSheet previewBottomSheet = new PremiumPreviewBottomSheet(getBaseFragment(), currentAccount, null, resourcesProvider);
                    previewBottomSheet.setOnDismissListener(dialog -> adapter.setPausedStars(false));
                    previewBottomSheet.setOnShowListener(dialog -> adapter.setPausedStars(true));
                    previewBottomSheet.show();
                },
                resourcesProvider), true));
        if (adapter == null) {
            return;
        }
        if (!notify) {
            return;
        }
        if (animated) {
            adapter.setItems(oldItems, items);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected CharSequence getTitle() {
        return selectedBoostType == BoostTypeCell.TYPE_SPECIFIC_USERS ?
                LocaleController.getString("GiftPremium", R.string.GiftPremium)
                : LocaleController.formatString("BoostingStartGiveaway", R.string.BoostingStartGiveaway);
    }

    @Override
    protected RecyclerListView.SelectionAdapter createAdapter() {
        return adapter = new BoostAdapter(resourcesProvider);
    }

    @Override
    public void onChatsSelected(List<TLRPC.Chat> chats) {
        selectedChats.clear();
        selectedChats.addAll(chats);
        updateRows(true, true);
    }

    @Override
    public void onUsersSelected(List<TLRPC.User> users) {
        selectedUsers.clear();
        selectedUsers.addAll(users);
        if (users.isEmpty()) {
            selectedBoostType = BoostTypeCell.TYPE_GIVEAWAY;
        } else {
            selectedBoostType = BoostTypeCell.TYPE_SPECIFIC_USERS;
        }
        selectedSliderIndex = 0;
        updateRows(true, true);
        updateActionButton(true);
        updateTitle();
    }

    @Override
    public void onCountrySelected(List<TLRPC.TL_help_country> countries) {
        selectedCountries.clear();
        selectedCountries.addAll(countries);
        updateRows(false, true);
    }
}
