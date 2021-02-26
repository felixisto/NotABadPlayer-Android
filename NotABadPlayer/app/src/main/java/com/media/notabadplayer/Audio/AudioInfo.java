package com.media.notabadplayer.Audio;

import java.util.List;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.media.notabadplayer.Audio.Model.AudioAlbum;
import com.media.notabadplayer.Audio.Model.BaseAudioTrack;
import com.media.notabadplayer.Constants.SearchFilter;

public interface AudioInfo {
    void loadIfNecessary();
    void load();

    @NonNull List<AudioAlbum> getAlbums();
    @Nullable AudioAlbum getAlbumByID(@NonNull String identifier);
    @NonNull List<BaseAudioTrack> getAlbumTracks(@NonNull AudioAlbum album);
    @NonNull List<BaseAudioTrack> searchForTracks(@NonNull String query, SearchFilter filter);
    @Nullable BaseAudioTrack findTrackByPath(@NonNull Uri path);
}
