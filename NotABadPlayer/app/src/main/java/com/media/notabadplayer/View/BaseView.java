package com.media.notabadplayer.View;

import java.util.ArrayList;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.media.notabadplayer.Audio.AudioAlbum;
import com.media.notabadplayer.Audio.AudioInfo;
import com.media.notabadplayer.Audio.AudioPlaylist;
import com.media.notabadplayer.Audio.AudioTrack;
import com.media.notabadplayer.Constants.AppSettings;

public interface BaseView {
    void enableInteraction();
    void disableInteraction();

    void openPlaylistScreen(@NonNull AudioInfo audioInfo, @NonNull AudioPlaylist playlist);
    
    void onMediaAlbumsLoad(@NonNull ArrayList<AudioAlbum> albums);

    void onPlaylistLoad(@NonNull AudioPlaylist playlist);
    
    void openPlayerScreen(@NonNull AudioPlaylist playlist);
    void updatePlayerScreen(@NonNull AudioPlaylist playlist);
    
    void searchQueryResults(@NonNull String searchQuery, @NonNull ArrayList<AudioTrack> songs, @Nullable String searchTip);
    
    void appSettingsReset();
    void appThemeChanged(AppSettings.AppTheme appTheme);
    void appTrackSortingChanged(AppSettings.TrackSorting trackSorting);
    void onShowVolumeBarSettingChange(AppSettings.ShowVolumeBar showVolumeBar);
    
    void onPlayerErrorEncountered(@NonNull Exception error);
}
