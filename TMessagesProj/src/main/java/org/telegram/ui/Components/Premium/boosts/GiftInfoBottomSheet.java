package org.telegram.ui.Components.Premium.boosts;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BottomSheetWithRecyclerListView;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Premium.PremiumPreviewBottomSheet;
import org.telegram.ui.Components.Premium.boosts.adapters.GiftInfoAdapter;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UndoView;
import org.telegram.ui.LaunchActivity;

import java.util.concurrent.atomic.AtomicBoolean;

public class GiftInfoBottomSheet extends BottomSheetWithRecyclerListView {

    public static void show(BaseFragment fragment, String slug, Browser.Progress progress) {
        final AtomicBoolean isCanceled = new AtomicBoolean(false);
        if (progress != null) {
            progress.init();
            progress.onCancel(() -> isCanceled.set(true));
        }
        BoostRepository.checkGiftCode(slug, giftCode -> {
            if (isCanceled.get()) {
                return;
            }
            GiftInfoBottomSheet alert = new GiftInfoBottomSheet(fragment, false, true, giftCode, slug);
            if (fragment != null && fragment.getParentActivity() != null) {
                fragment.showDialog(alert);
            } else {
                alert.show();
            }
            if (progress != null) {
                progress.end();
            }
        }, error -> {
            if (isCanceled.get()) {
                return;
            }
            if (progress != null) {
                progress.end();
            }
        });
    }

    public static void show(BaseFragment fragment, String slug) {
        show(fragment, slug, null);
    }

    public static boolean handleIntent(Intent intent, Browser.Progress progress) {
        Uri data = intent.getData();
        if (data != null) {
            String scheme = data.getScheme();
            if (scheme != null) {
                if ((scheme.equals("http") || scheme.equals("https"))) {
                    String host = data.getHost().toLowerCase();
                    if (host.equals("telegram.me") || host.equals("t.me") || host.equals("telegram.dog")) {
                        String path = data.getPath();
                        if (path != null) {
                            String lastPathSegment = data.getLastPathSegment();
                            if (path.startsWith("/giftcode") && lastPathSegment != null) {
                                show(LaunchActivity.getLastFragment(), lastPathSegment, progress);
                                return true;
                            }
                        }
                    }
                } else if (scheme.equals("tg")) {
                    String url = data.toString();
                    String lastPathSegment = data.getLastPathSegment();
                    if (url.startsWith("tg:giftcode") || url.startsWith("tg://giftcode")) {
                        if (lastPathSegment != null) {
                            show(LaunchActivity.getLastFragment(), lastPathSegment, progress);
                            return true;
                        }

                    }
                }
            }
        }
        return false;
    }

    private final TLRPC.TL_payments_checkedGiftCode giftCode;
    private final boolean isUnused;
    private GiftInfoAdapter adapter;

    public GiftInfoBottomSheet(BaseFragment fragment, boolean needFocus, boolean hasFixedSize, TLRPC.TL_payments_checkedGiftCode giftCode, String slug) {
        super(fragment, needFocus, hasFixedSize);
        this.isUnused = giftCode.used_date == 0;
        this.giftCode = giftCode;
        setApplyTopPadding(false);
        setApplyBottomPadding(true);
        fixNavigationBar();
        updateTitle();
        adapter.init(fragment, giftCode, slug);
    }

    @Override
    public void onViewCreated(FrameLayout containerView) {
        super.onViewCreated(containerView);
        Bulletin.addDelegate(container, new Bulletin.Delegate() {
            @Override
            public int getTopOffset(int tag) {
                return AndroidUtilities.statusBarHeight;
            }
        });
    }

    @Override
    protected CharSequence getTitle() {
        return isUnused ? LocaleController.getString("BoostingGiftLink", R.string.BoostingGiftLink)
                : LocaleController.getString("BoostingUsedGiftLink", R.string.BoostingUsedGiftLink);
    }

    @Override
    protected RecyclerListView.SelectionAdapter createAdapter() {
        return adapter = new GiftInfoAdapter(resourcesProvider) {
            @Override
            protected void dismiss() {
                GiftInfoBottomSheet.this.dismiss();
            }

            @Override
            protected void afterCodeApplied() {
                AndroidUtilities.runOnUIThread(() -> {
                    PremiumPreviewBottomSheet previewBottomSheet = new PremiumPreviewBottomSheet(getBaseFragment(), currentAccount, null, null, resourcesProvider)
                            .setAnimateConfetti(true)
                            .setOutboundGift(true);
                    getBaseFragment().showDialog(previewBottomSheet);
                }, 200);
            }

            @Override
            protected void onObjectClicked(TLObject object) {
                dismiss();
                if (object instanceof TLRPC.Chat) {
                    getBaseFragment().presentFragment(ChatActivity.of(-((TLRPC.Chat) object).id));
                } else if (object instanceof TLRPC.User) {
                    getBaseFragment().presentFragment(ChatActivity.of(((TLRPC.User) object).id));
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putLong("chat_id", -DialogObject.getPeerDialogId(giftCode.from_id));
                    bundle.putInt("message_id", giftCode.giveaway_msg_id);
                    ChatActivity chatFragment = new ChatActivity(bundle);
                    getBaseFragment().presentFragment(chatFragment);
                }
            }

            @Override
            protected void onHiddenLinkClicked() {
                String text = LocaleController.getString("BoostingOnlyRecipientCode", R.string.BoostingOnlyRecipientCode);
                BulletinFactory.of(container, resourcesProvider).createSimpleBulletin(R.raw.chats_infotip, text).show(true);
            }
        };
    }
}
