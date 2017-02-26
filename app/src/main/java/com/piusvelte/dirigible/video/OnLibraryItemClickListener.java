package com.piusvelte.dirigible.video;

import android.support.annotation.NonNull;

import com.piusvelte.dirigible.util.CredentialProvider;

/**
 * @author bemmanuel
 * @since 3/4/16
 */
public interface OnLibraryItemClickListener extends CredentialProvider {
    void onLibraryItemClick(@NonNull LibraryItem libraryItem);
}
