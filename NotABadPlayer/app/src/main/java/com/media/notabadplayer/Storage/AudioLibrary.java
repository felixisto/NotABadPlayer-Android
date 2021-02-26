package com.media.notabadplayer.Storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import android.app.Application;
import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.media.notabadplayer.Audio.Model.AudioAlbum;
import com.media.notabadplayer.Audio.AudioInfo;
import com.media.notabadplayer.Audio.Model.AudioArtCover;
import com.media.notabadplayer.Audio.Model.AudioTrackBuilder;
import com.media.notabadplayer.Audio.Model.AudioTrackSource;
import com.media.notabadplayer.Audio.Model.BaseAudioTrack;
import com.media.notabadplayer.Audio.Model.BaseAudioTrackBuilderNode;
import com.media.notabadplayer.Constants.SearchFilter;
import com.media.notabadplayer.PlayerApplication;
import com.media.notabadplayer.Utilities.CollectionUtilities;
import com.media.notabadplayer.Utilities.MediaSorting;

// Provides simple interface to the audio library of the user.
// Before using the audio library, you MUST call initialize().
// Dependant on storage access permission:
// Make sure you have access to user storage before using the audio library.
public class AudioLibrary extends ContentObserver implements AudioInfo {
    public static int ALBUM_TRACK_CACHE_CAPACITY = 30;
    public static int FAVORITES_TRACK_CAPACITY = FavoritesStorage.CAPACITY;
    public static int SEARCH_TRACKS_CAP = 1000;
    public static int RECENTLY_ADDED_CAP = 100;
    public static int RECENTLY_ADDED_PREDICATE_DAYS_DIFFERENCE = 30;

    // From google sample https://android.googlesource.com/platform/packages/providers/MediaProvider/+/51cba5e1acf1c56be3dc6c7c46a73a5a0409b452/src/com/android/providers/media/MediaProvider.java
    public static final Uri LEGACY_ALBUMART_URI = Uri.parse("content://media/external/audio/albumart");

    private static AudioLibrary singleton;

    private final Object _lock = new Object();

    private Application _context;

    // List of all albums
    private boolean _albumsLoaded;
    private final ArrayList<AudioAlbum> _albums = new ArrayList<>();

    // Album id : list of audio tracks
    private final SortedMap<String, List<BaseAudioTrack>> _albumTracks = new TreeMap<>();

    // Alerted when the device library is changed
    private final HashSet<ChangesListener> _changesListeners = new HashSet<>();

    // List of recently added tracks
    private boolean _recentlyAddedLoaded;
    private final ArrayList<BaseAudioTrack> _recentlyAdded = new ArrayList<>();
    private @Nullable
    Date _favoritesLastUpdated = null;
    private List<BaseAudioTrack> _favoriteTracks = new ArrayList<>();

    public interface ChangesListener {
        void onMediaLibraryChanged();
    }

    private AudioLibrary() {
        // The handler decides which thread the callbacks will be performed on
        super(new Handler(Looper.getMainLooper()));

        Log.v(AudioLibrary.class.getCanonicalName(), "Initializing...");

        _albumsLoaded = false;
        _recentlyAddedLoaded = false;

        _context = PlayerApplication.getShared();

        Log.v(AudioLibrary.class.getCanonicalName(), "Initialized!");
    }

    public synchronized static AudioLibrary getShared() {
        if (singleton == null) {
            singleton = new AudioLibrary();
        }

        return singleton;
    }

    private @NonNull
    Application getContext() {
        if (_context == null) {
            throw new UncheckedExecutionException(new Exception("AudioLibrary cannot be used before being initialized, initialize() has never been called"));
        }

        return _context;
    }

    // # Init

    @Override
    public void loadIfNecessary() {
        synchronized (_lock) {
            if (_albumsLoaded) {
                return;
            }
        }

        load();
    }

    @Override
    public void load() {
        // Load whatever is needed to operate the library
        // Albums will be loaded and stored, since they hold huge amount of information

        loadAlbumsData();
    }

    // # Album info

    private void loadAlbumsData()
    {
        synchronized (_lock)
        {
            Context context = getContext();

            Log.v(AudioLibrary.class.getCanonicalName(), "Loading albums from media store...");

            _albums.clear();

            Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    null, null, null, null);

            if (cursor == null)
            {
                return;
            }

            int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);
            int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ARTIST);
            int albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Albums._ID);

            while (cursor.moveToNext())
            {
                long albumID = cursor.getLong(albumIdColumn);
                String title = cursor.getString(titleColumn);
                String artist = cursor.getString(artistColumn);

                AudioArtCover artCover;

                if (Build.VERSION.SDK_INT >= 29) {
                    int thumbColumn = cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID);
                    int thumpId = cursor.getInt(thumbColumn);
                    Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, thumpId);
                    artCover = new AudioArtCover(null, uri);
                } else {
                    Uri artURI = ContentUris.withAppendedId(LEGACY_ALBUMART_URI, albumID);
                    artCover = new AudioArtCover(null, artURI);
                }

                AudioAlbum albumItem = new AudioAlbum(String.valueOf(albumID), artist, title, artCover);

                if (albumItem != null) {
                    _albums.add(albumItem);
                }
            }

            cursor.close();

            MediaSorting.sortAlbumsByTitle(_albums);

            Log.v(AudioLibrary.class.getCanonicalName(), "Successfully loaded " +  String.valueOf(_albums.size()) + " albums from media store.");

            _albumsLoaded = true;
        }
    }

    @Override
    public @NonNull List<AudioAlbum> getAlbums()
    {
        loadIfNecessary();

        synchronized (_lock)
        {
            return CollectionUtilities.copy(_albums);
        }
    }

    @Override
    public @Nullable AudioAlbum getAlbumByID(@NonNull String identifier)
    {
        List<AudioAlbum> albums = getAlbums();

        for (AudioAlbum album: albums)
        {
            if (album.albumID.equals(identifier))
            {
                return album;
            }
        }

        return null;
    }

    @Override
    public @NonNull List<BaseAudioTrack> getAlbumTracks(@NonNull AudioAlbum album)
    {
        synchronized (_lock)
        {
            List<BaseAudioTrack> tracks = _albumTracks.get(album.albumID);
            
            if (tracks != null)
            {
                return tracks;
            }
        }

        Context context = getContext();

        String selection = "is_music != 0";

        if (Integer.parseInt(album.albumID) > 0)
        {
            selection = selection + " and album_id = " + album.albumID;
        }

        List<BaseAudioTrack> result = fetchAndParseMediaStoreTracksData(context, 
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                selection,
                null,
                Integer.MAX_VALUE);

        storeTracksIntoCache(album.albumID, result);

        return result;
    }

    private void storeTracksIntoCache(@NonNull String albumID, @NonNull List<BaseAudioTrack> tracks)
    {
        synchronized (_lock)
        {
            _albumTracks.put(albumID, tracks);

            if (_albumTracks.size() > AudioLibrary.ALBUM_TRACK_CACHE_CAPACITY)
            {
                String firstKey = _albumTracks.firstKey();

                _albumTracks.remove(firstKey);
            }
        }
    }

    // # Search

    @Override
    public @NonNull List<BaseAudioTrack> searchForTracks(@NonNull String query, @NonNull SearchFilter filter)
    {
        if (query.isEmpty())
        {
            return new ArrayList<>();
        }

        Context context = getContext();

        String selectionFilter;

        switch (filter)
        {
            case Title:
                selectionFilter = MediaStore.Audio.Media.TITLE;
                break;
            case Album:
                selectionFilter = MediaStore.Audio.Media.ALBUM;
                break;
            case Artist:
                selectionFilter = MediaStore.Audio.Media.ARTIST;
                break;
            default:
                selectionFilter = MediaStore.Audio.Media.TITLE;
                break;
        }

        // Predicate
        String selection = selectionFilter + " LIKE ?";
        String[] selectionArgs = new String[] {""};

        String[] words = query.split(" ");

        for (int i = 0; i < words.length; i++)
        {
            selectionArgs[0] += "%" + words[i] + "%";
        }

        return fetchAndParseMediaStoreTracksData(context,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                selection,
                selectionArgs,
                SEARCH_TRACKS_CAP);
    }

    @Override
    public @Nullable BaseAudioTrack findTrackByPath(@NonNull Uri path)
    {
        Context context = getContext();

        List<BaseAudioTrack> track = fetchAndParseMediaStoreTracksData(context, path, null, null, 1);

        if (track.size() > 0)
        {
            return track.get(0);
        }

        return null;
    }

    // # Recently added

    public @NonNull List<BaseAudioTrack> getRecentlyAddedTracks()
    {
        boolean recentlyAddedLoaded;

        synchronized (_lock)
        {
            recentlyAddedLoaded = _recentlyAddedLoaded;
        }

        if (!recentlyAddedLoaded)
        {
            loadRecentlyAddedTracks();
        }

        synchronized (_lock)
        {
            return _recentlyAdded;
        }
    }

    private void loadRecentlyAddedTracks()
    {
        // Albums should be loaded, in order to parse tracks properly
        loadIfNecessary();

        synchronized (_lock)
        {
            _recentlyAddedLoaded = true;

            _recentlyAdded.clear();

            Context context = getContext();

            long daysDifference = RECENTLY_ADDED_PREDICATE_DAYS_DIFFERENCE;
            long daysAgoInMS = daysDifference * 24 * 60 * 60 * 1000;
            Date now = new Date();
            Date minimumValueDate = new Date(now.getTime() - daysAgoInMS);
            String dateComparison = String.valueOf(buildDatabaseLongDateFromDate(minimumValueDate));

            String selection = "is_music != 0";
            selection = selection + " and " + MediaStore.Audio.Media.DATE_ADDED + " >= " + dateComparison;

            _recentlyAdded.addAll(fetchAndParseMediaStoreTracksData(context,
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    selection,
                    null,
                    RECENTLY_ADDED_CAP));
        }
    }
    
    // # Favorite tracks

    public @NonNull List<BaseAudioTrack> getFavoriteTracks()
    {
        loadIfNecessary();

        Date lastStorageUpdate = GeneralStorage.getShared().favorites.getLastTimeUpdate();
        
        synchronized (_lock)
        {
            if (_favoritesLastUpdated != null) {
                if (_favoritesLastUpdated.after(lastStorageUpdate)) {
                    return _favoriteTracks;
                }
            }
        }

        List<BaseAudioTrack> tracks = loadFavoriteTracks();
        
        synchronized (_lock)
        {
            _favoriteTracks = tracks;
            _favoritesLastUpdated = lastStorageUpdate;
        }
        
        return new ArrayList<>(tracks);
    }

    private @NonNull List<BaseAudioTrack> loadFavoriteTracks()
    {
        List<FavoriteStorageItem> favorites = GeneralStorage.getShared().favorites.getItems();

        ArrayList<BaseAudioTrack> tracks = new ArrayList<>();
        ArrayList<FavoriteItem> items = new ArrayList<>();

        String projections[] = fetchMediaStoreProjections();

        Context context = getContext();

        String selection = "is_music != 0";

        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projections, selection, null, null);

        if (cursor == null)
        {
            return tracks;
        }

        BaseAudioTrackBuilderNode node = AudioTrackBuilder.start();
        TrackColumnIndexes indexes = new TrackColumnIndexes(cursor);

        while (cursor.moveToNext())
        {
            // Check for cap
            if (items.size() >= FAVORITES_TRACK_CAPACITY)
            {
                break;
            }
            
            // Build tracks whose path matches the path of the favorite item
            String path = cursor.getString(indexes.dataColumn);

            FavoriteStorageItem matchingFavorite = null;
            
            for (FavoriteStorageItem favorite : favorites) {
                if (path.equals(favorite.trackPath)) {
                    matchingFavorite = favorite;
                    break;
                }
            }
            
            if (matchingFavorite == null) {
                continue;
            }
            
            try {
                BaseAudioTrack result = buildTrackFromCursor(cursor, node, indexes);
                items.add(new FavoriteItem(matchingFavorite, result));
            } catch (Exception e) {

            }
        }

        cursor.close();
        
        // Sort by favorite date
        Collections.sort(items, new Comparator<FavoriteItem>() {
            @Override
            public int compare(FavoriteItem o1, FavoriteItem o2) {
                return o2.favorite.dateFavorited.compareTo(o1.favorite.dateFavorited);
            }
        });
        
        for (FavoriteItem item : items) {
            tracks.add(item.track);
        }
        
        return tracks;
    }

    // # MediaStore fetch

    String[] fetchMediaStoreProjections() {
        String projections[] = {
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DATE_MODIFIED,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.BOOKMARK
        };
        return projections;
    }

    @NonNull List<BaseAudioTrack> fetchAndParseMediaStoreTracksData(@NonNull Context context,
                                                                    @NonNull Uri path,
                                                                    @Nullable String selection,
                                                                    @Nullable String[] selectionArgs,
                                                                    int cap)
    {
        ArrayList<BaseAudioTrack> tracks = new ArrayList<>();

        String projections[] = fetchMediaStoreProjections();

        Cursor cursor = context.getContentResolver().query(path, projections, selection, selectionArgs, null);

        if (cursor == null)
        {
            return tracks;
        }

        BaseAudioTrackBuilderNode node = AudioTrackBuilder.start();
        TrackColumnIndexes indexes = new TrackColumnIndexes(cursor);

        while (cursor.moveToNext())
        {
            // Check for cap
            if (tracks.size() >= cap)
            {
                break;
            }
            
            try {
                BaseAudioTrack result = buildTrackFromCursor(cursor, node, indexes);
                tracks.add(result);
            } catch (Exception e) {

            }
        }

        cursor.close();

        return tracks;
    }
    
    @NonNull BaseAudioTrack buildTrackFromCursor(@NonNull Cursor cursor, @Nullable BaseAudioTrackBuilderNode reusableNode, @Nullable TrackColumnIndexes cIndexes) throws Exception {
        BaseAudioTrackBuilderNode node = reusableNode != null ? reusableNode : AudioTrackBuilder.start();

        TrackColumnIndexes indexes = cIndexes != null ? cIndexes : new TrackColumnIndexes(cursor);

        String filePath = cursor.getString(indexes.dataColumn);
        Date dateAdded = buildDateFromDatabaseLong(cursor.getLong(indexes.dateAddedColumn));
        Date dateModified = buildDateFromDatabaseLong(cursor.getLong(indexes.dateModifiedColumn));
        String title = cursor.getString(indexes.titleColumn);
        String artist = cursor.getString(indexes.artistColumn);
        String albumTitle = cursor.getString(indexes.albumTitleColumn);
        String albumID = cursor.getString(indexes.albumIDColumn);
        int trackNum = cursor.getInt(indexes.trackNumColumn);
        double duration = cursor.getLong(indexes.durationColumn) / 1000.0;
        double lastPlayedPosition = cursor.getLong(indexes.bookmarkColumn) / 1000.0;

        @Nullable AudioAlbum album = getAlbumByID(albumID);

        AudioTrackSource source = album != null ? AudioTrackSource.createAlbumSource(albumID) : AudioTrackSource.createPlaylistSource(title);

        node.reset();

        node.setFilePath(filePath);
        node.setTitle(title);
        node.setArtist(artist);
        node.setAlbumTitle(albumTitle);
        node.setAlbumID(albumID);
        node.setArtCover(album.artCover);
        node.setTrackNum(trackNum);
        node.setDuration(duration);
        node.setSource(source);

        node.setDateAdded(dateAdded);
        node.setDateModified(dateModified);
        node.setLastPlayedPosition(lastPlayedPosition);

        return node.build();
    }

    // # Library changes

    public void registerLibraryChangesListener(@NonNull ChangesListener listener)
    {
        // Register once, the singleton itself
        if (_changesListeners.size() == 0)
        {
            getContext().getContentResolver().registerContentObserver(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, true, this);
        }

        _changesListeners.add(listener);
    }

    public void unregisterLibraryChangesListener(@NonNull ChangesListener listener)
    {
        _changesListeners.remove(listener);

        if (_changesListeners.size() == 0)
        {
            getContext().getContentResolver().unregisterContentObserver(this);
        }
    }

    @Override
    public boolean deliverSelfNotifications() {
        return true;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        Log.v(AudioLibrary.class.getCanonicalName(), "Device media library was changed! Reloading album data...");

        // Reload library
        load();
        loadRecentlyAddedTracks();

        // Alert listeners
        for (ChangesListener listener : _changesListeners)
        {
            listener.onMediaLibraryChanged();
        }
    }

    // # Date value parsing

    public @NonNull Date buildDateFromDatabaseLong(long value)
    {
        return new Date(value * 1000L);
    }

    public long buildDatabaseLongDateFromDate(@NonNull Date date)
    {
        return (date.getTime() / 1000L);
    }
    
    // Column indexes
    class TrackColumnIndexes {
        final int dataColumn;
        final int dateAddedColumn;
        final int dateModifiedColumn;
        final int titleColumn;
        final int artistColumn;
        final int albumTitleColumn;
        final int albumIDColumn;
        final int trackNumColumn;
        final int durationColumn;
        final int bookmarkColumn;

        TrackColumnIndexes(@NonNull Cursor cursor) {
            dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            dateAddedColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED);
            dateModifiedColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED);
            titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            albumTitleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            albumIDColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            trackNumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TRACK);
            durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            bookmarkColumn = cursor.getColumnIndex(MediaStore.Audio.Media.BOOKMARK);
        }
    }
    
    // Favorite item helper
    class FavoriteItem {
        final @NonNull FavoriteStorageItem favorite;
        final @NonNull BaseAudioTrack track;

        FavoriteItem(@NonNull FavoriteStorageItem favorite, @NonNull BaseAudioTrack track) {
            this.favorite = favorite;
            this.track = track;
        }
    }
}
