package com.piusvelte.dirigible.account;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.piusvelte.dirigible.R;
import com.piusvelte.dirigible.util.CredentialProvider;

/**
 * Created by bemmanuel on 2/23/17.
 */

public class AccountChooser extends Fragment implements View.OnClickListener {

    private static final String TAG = AccountChooser.class.getSimpleName();

    private static final int REQUEST_ACCOUNT_CHOOSER = 1;
    private static final int REQUEST_PERMISSIONS = 2;

    private AccountListener mAccountListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof AccountListener) {
            mAccountListener = (AccountListener) context;
        } else {
            throw new IllegalStateException(TAG + " must be attached to an AccountListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mAccountListener = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.account_chooser, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(android.R.id.button1).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (TextUtils.isEmpty(mAccountListener.getCredential().getSelectedAccountName())) {
            startActivityForResult(mAccountListener.getCredential().newChooseAccountIntent(), REQUEST_ACCOUNT_CHOOSER);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ACCOUNT_CHOOSER:
                if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

                    if (!TextUtils.isEmpty(accountName)) {
                        mAccountListener.onAccountSelected(accountName);
                    }
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    // TODO
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS:
                if (permissions.length > 0
                        && Manifest.permission.GET_ACCOUNTS.equals(permissions[0])
                        && grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    String accountName = mAccountListener.getCredential().getSelectedAccountName();

                    if (!TextUtils.isEmpty(accountName)) {
                        mAccountListener.onAccountSelected(accountName);
                    }
                } else {
                    hasPermissions();
                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean hasPermissions() {
        int permissionCheck = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.GET_ACCOUNTS);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) return true;

        requestPermissions(new String[]{Manifest.permission.GET_ACCOUNTS},
                REQUEST_PERMISSIONS);

        return false;
    }

    public interface AccountListener extends CredentialProvider {
        void onAccountSelected(@NonNull String accountName);
    }
}
