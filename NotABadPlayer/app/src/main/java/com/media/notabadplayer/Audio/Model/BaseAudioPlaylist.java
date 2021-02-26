package com.media.notabadplayer.Audio.Model;

import androidx.annotation.NonNull;
import java.io.Serializable;
import java.util.List;

import com.media.notabadplayer.Constants.AppSettings;

// Describes a playlist model.
// Thread safe: no
public interface BaseAudioPlaylist extends Serializable {
    @NonNull String getName();
    int size();

    boolean isPlaying();

    @NonNull List<BaseAudioTrack> getTracks();
    @NonNull BaseAudioTrack getTrack(int index);
    boolean hasTrack(@NonNull BaseAudioTrack track);
    int getPlayingTrackIndex();
    @NonNull BaseAudioTrack getPlayingTrack();
    
    boolean isPlayingFirstTrack();
    boolean isPlayingLastTrack();

    boolean isAlbum();
    boolean isTemporary();

    @NonNull BaseAudioPlaylist sortedPlaylist(AppSettings.TrackSorting sorting);
}
