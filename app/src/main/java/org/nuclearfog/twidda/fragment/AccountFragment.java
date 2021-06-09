package org.nuclearfog.twidda.fragment;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import org.nuclearfog.twidda.adapter.AccountAdapter;
import org.nuclearfog.twidda.adapter.AccountAdapter.OnLoginClickListener;
import org.nuclearfog.twidda.backend.LoginLoader;
import org.nuclearfog.twidda.backend.engine.EngineException;
import org.nuclearfog.twidda.backend.model.Account;
import org.nuclearfog.twidda.backend.utils.ErrorHandler;
import org.nuclearfog.twidda.database.GlobalSettings;
import org.nuclearfog.twidda.dialog.ConfirmDialog;
import org.nuclearfog.twidda.dialog.ConfirmDialog.OnConfirmListener;

import java.util.List;

import static android.os.AsyncTask.Status.RUNNING;
import static org.nuclearfog.twidda.activity.AccountActivity.RET_ACCOUNT_CHANGE;
import static org.nuclearfog.twidda.dialog.ConfirmDialog.DialogType;


/**
 * fragment class of the account manager
 * all registered accounts are listed here
 *
 * @author nuclearfog
 */
public class AccountFragment extends ListFragment implements OnLoginClickListener, OnConfirmListener {

    @Nullable
    private LoginLoader loginTask;
    private GlobalSettings settings;
    private AccountAdapter adapter;
    private AlertDialog dialog;
    private Account selection;


    @Override
    protected void onCreate() {
        dialog = new ConfirmDialog(requireContext(), DialogType.REMOVE_ACCOUNT, this);
        settings = GlobalSettings.getInstance(requireContext());
    }


    @Override
    public void onStart() {
        super.onStart();
        if (loginTask == null) {
            setRefresh(true);
            loginTask = new LoginLoader(this);
            loginTask.execute();
        }
    }


    @Override
    public void onDestroy() {
        if (loginTask != null && loginTask.getStatus() == RUNNING)
            loginTask.cancel(true);
        super.onDestroy();
    }


    @Override
    protected void onReload() {
        if (loginTask == null || loginTask.getStatus() != RUNNING)
            loginTask = new LoginLoader(this);
        loginTask.execute();
    }


    @Override
    protected void onReset() {
        adapter.clear();
        loginTask = new LoginLoader(this);
        loginTask.execute();
        setRefresh(true);
    }


    @Override
    protected AccountAdapter initAdapter() {
        adapter = new AccountAdapter(settings, this);
        return adapter;
    }


    @Override
    public void onLoginClick(Account account) {
        // set new account
        String[] token = account.getKeys();
        settings.setConnection(token[0], token[1], account.getId());
        // finish activity and return to parent activity
        requireActivity().setResult(RET_ACCOUNT_CHANGE);
        requireActivity().finish();
    }


    @Override
    public void onDeleteClick(Account account) {
        if (!dialog.isShowing()) {
            selection = account;
            dialog.show();
        }
    }


    @Override
    public void onConfirm(DialogType type) {
        loginTask = new LoginLoader(this);
        loginTask.execute(selection);
    }

    /**
     * called from {@link LoginLoader}
     *
     * @param result login information
     */
    public void onSuccess(List<Account> result) {
        adapter.setData(result);
        setRefresh(false);
    }

    /**
     * called from {@link LoginLoader} when an error occurs
     */
    public void onError(EngineException err) {
        ErrorHandler.handleFailure(requireContext(), err);
        setRefresh(false);
    }
}