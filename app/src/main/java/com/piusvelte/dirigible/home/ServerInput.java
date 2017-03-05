package com.piusvelte.dirigible.home;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;

import com.piusvelte.dirigible.R;

/**
 * Created by bemmanuel on 3/5/17.
 */

public class ServerInput extends AppCompatDialogFragment implements DialogInterface.OnClickListener {

    private EditText mServerName;
    @Nullable
    private String mResult;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mServerName = new EditText(getContext());

        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.input_server)
                .setView(mServerName)
                .setPositiveButton(android.R.string.ok, this)
                .create();
    }

    @Override
    public void onDestroyView() {
        mServerName = null;
        super.onDestroyView();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                // result delivered by onDismiss
                mResult = mServerName != null ? mServerName.getText().toString() : null;
                if (TextUtils.isEmpty(mResult)) mResult = null;// prevent empty string
                break;
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        deliverResult();
    }

    private void deliverResult() {
        if (getActivity() instanceof OnServerInputListener) {
            OnServerInputListener listener = (OnServerInputListener) getActivity();
            listener.onServerInput(mResult);
        }
    }

    public interface OnServerInputListener {
        void onServerInput(String server);
    }
}
