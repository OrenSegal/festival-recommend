package spotify;

import cache.CheckerCache;
import com.google.inject.Inject;
import intersection.SpotifyOrderingCreator;
import lastfm.domain.Artist;
import spotify.domain.AccessToken;
import spotify.domain.SpotifyArtist;
import spotify.domain.SpotifyPlaylistTracksResponse;
import spotify.domain.SpotifyTracksResponse;

import java.util.List;
import java.util.stream.Stream;

import static cache.CacheKeyPrefix.SPOTIFYACCESSTOKEN;
import static java.util.stream.Collectors.toList;

/**
 * Created by Adam on 01/10/2015.
 */


public class SpotifyDataGrabber {
    @Inject
    private SpotifySender spotifySender;
    @Inject
    private CheckerCache cache;
    @Inject
    private SpotifyOrderingCreator spotifyOrderingCreator;

    public List<Artist> fetchSpotifyArtists(String authCode) {
        AccessToken token = cache.getOrLookup(authCode, () -> spotifySender.getAuthToken(authCode), SPOTIFYACCESSTOKEN, AccessToken.class);
        System.out.println(token.getAccessToken());

        List<SpotifyTracksResponse> savedTracks = spotifySender.getSavedTracks(token.getAccessToken());
        List<SpotifyArtist> artists = savedTracks.stream().flatMap(x -> x.getItems().stream()).flatMap(x -> x.getTrack().getArtists().stream()).collect(toList());

        List<SpotifyPlaylistTracksResponse> playListTracks = spotifySender.getPlayListTracks(token.getAccessToken());
        List<SpotifyArtist> playlistArtists = playListTracks.stream().flatMap(x -> x.getItems().stream()).flatMap(x -> x.getTrack().getArtists().stream()).collect(toList());

        List<SpotifyArtist> combined = Stream.concat(artists.stream(),playlistArtists.stream()).collect(toList());

        //Get playlist info here
        return spotifyOrderingCreator.artistListByFrequency(combined);
    }

    public void setSpotifySender(SpotifySender spotifySender) {
        this.spotifySender = spotifySender;
    }

    public void setCache(CheckerCache cache) {
        this.cache = cache;
    }

    public void setSpotifyOrderingCreator(SpotifyOrderingCreator spotifyOrderingCreator) {
        this.spotifyOrderingCreator = spotifyOrderingCreator;
    }
}
