package intersection;

import cache.CheckerCache;
import clashfinder.ClashfinderSender;
import clashfinder.domain.ClashfinderData;
import clashfinder.domain.Event;
import com.google.inject.Inject;
import domain.ArtistMap;
import lastfm.LastFmSender;
import lastfm.domain.Artist;
import lastfm.domain.Recommendations;
import lastfm.domain.Response;
import spotify.SpotifyDataGrabber;
import spotify.domain.SpotifyArtists;
import strategy.PreferenceStrategy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static cache.CacheKeyPrefix.*;
import static java.util.stream.Collectors.toList;

/**
 * Created by Adam on 24/09/2015.
 */
public class ScheduleIntersectionFinder {
    @Inject
    private LastFmSender lastFmSender;
    @Inject
    private CheckerCache cache;
    @Inject
    private ArtistMapGenerator artistMapGenerator;
    @Inject
    private ClashfinderSender clashFinderSender;
    @Inject
    private SpotifyDataGrabber spotifyDataGrabber;
    @Inject
    private RecommendedArtistGenerator recommendedArtistGenerator;


    public List<Event> findSIntersection(String username, String festival, String year) {
        ClashfinderData clashfinderData =
                cache.getOrLookup(festival + year, () -> clashFinderSender.fetchData(festival,year),CLASHFINDER,ClashfinderData.class);
        Response response = cache.getOrLookup(username, () -> lastFmSender.simpleRequest(username), LISTENED, Response.class);
        List<Artist> artists = response.getTopartists().getArtist();
        ArtistMap artistMap = cache.getOrLookup(username + festival + year, () -> artistMapGenerator.generateLastFmMap(clashfinderData.getEvents(), artists), ARTISTMAP, ArtistMap.class);
        return matchingEventsByPlays(clashfinderData.getEvents(), artistMap.getArtistMap());
    }

    public List<Event> findReccoScheduleIntersection(String username, String festival, String year) {
        ClashfinderData clashfinderData =
                cache.getOrLookup(festival + year, () -> clashFinderSender.fetchData(festival,year),CLASHFINDER,ClashfinderData.class);
        Response response = cache.getOrLookup(username, () -> lastFmSender.simpleRequest(username), LISTENED, Response.class);
        List<Artist> artists = response.getTopartists().getArtist();
        Recommendations recArtists = cache.getOrLookup(username, () -> recommendedArtistGenerator.fetchRecommendations(artists), RECCOMENDED, Recommendations.class);
        ArtistMap artistMap = cache.getOrLookup(username + festival + year, () -> artistMapGenerator.generateLastFmMap(clashfinderData.getEvents(), recArtists.getArtist()), ARTISTMAPREC, ArtistMap.class);
        return matchingEventsByRank(clashfinderData.getEvents(), artistMap.getArtistMap());
    }

    public List<Event> findHybridScheduleIntersection(String username, String festival, String year, PreferenceStrategy strategy) {
        ClashfinderData clashfinderData =
                cache.getOrLookup(festival + year, () -> clashFinderSender.fetchData(festival,year),CLASHFINDER,ClashfinderData.class);
        Response response = cache.getOrLookup(username, () -> lastFmSender.simpleRequest(username), LISTENED, Response.class);
        List<Artist> artists = response.getTopartists().getArtist();
        Recommendations recArtists = cache.getOrLookup(username, () -> recommendedArtistGenerator.fetchRecommendations(artists), RECCOMENDED, Recommendations.class);

        ArtistMap reccoArtists =
                cache.getOrLookup(username + festival + year, () -> artistMapGenerator.generateLastFmMap(clashfinderData.getEvents(), recArtists.getArtist()), ARTISTMAPREC, ArtistMap.class);
        ArtistMap listenedArtists =
                cache.getOrLookup(username + festival + year, () -> artistMapGenerator.generateLastFmMap(clashfinderData.getEvents(), artists),ARTISTMAP, ArtistMap.class);

        return strategy.findOrderedInterection(clashfinderData.getEvents(), listenedArtists.getArtistMap(), reccoArtists.getArtistMap());
    }

    public List<Event> findSpotifyScheduleIntersection(String authCode, String festival, String year, String redirectUrl) {
        SpotifyArtists artists = cache.getOrLookup(authCode, () -> spotifyDataGrabber.fetchSpotifyArtists(authCode, redirectUrl), SPOTIFYARTISTS, SpotifyArtists.class);
        ClashfinderData clashfinderData =
                cache.getOrLookup(festival + year, () -> clashFinderSender.fetchData(festival,year),CLASHFINDER,ClashfinderData.class);
        ArtistMap artistMap = cache.getOrLookup(authCode, () -> artistMapGenerator.generateLastFmMap(clashfinderData.getEvents(), artists.getArtists()), ARTISTMAP, ArtistMap.class);

        return matchingEventsByPlays(clashfinderData.getEvents(),artistMap.getArtistMap());
    }

    public List<Event> findSpotifyReccommendedScheduleIntersection(String authCode, String festival, String year, String redirectUrl) {
        ClashfinderData clashfinderData =
                cache.getOrLookup(festival + year, () -> clashFinderSender.fetchData(festival,year),CLASHFINDER,ClashfinderData.class);
        SpotifyArtists artists = cache.getOrLookup(authCode, () -> spotifyDataGrabber.fetchSpotifyArtists(authCode, redirectUrl), SPOTIFYARTISTS, SpotifyArtists.class);
        Recommendations recArtists = cache.getOrLookup(authCode, () -> recommendedArtistGenerator.fetchRecommendations(artists.getArtists()), RECCOMENDED, Recommendations.class);
        ArtistMap artistMap = cache.getOrLookup(authCode + festival + year, () -> artistMapGenerator.generateLastFmMap(clashfinderData.getEvents(), recArtists.getArtist()), ARTISTMAPREC, ArtistMap.class);
        return matchingEventsByRank(clashfinderData.getEvents(), artistMap.getArtistMap());
    }

    public List<Event> findHybridSpotifyScheduleIntersection(String authCode, String festival, String year, String redirectUrl, PreferenceStrategy strategy) {
        ClashfinderData clashfinderData =
                cache.getOrLookup(festival + year, () -> clashFinderSender.fetchData(festival,year),CLASHFINDER,ClashfinderData.class);
        SpotifyArtists artists = cache.getOrLookup(authCode, () -> spotifyDataGrabber.fetchSpotifyArtists(authCode, redirectUrl), SPOTIFYARTISTS, SpotifyArtists.class);
        Recommendations recArtists = cache.getOrLookup(authCode, () -> recommendedArtistGenerator.fetchRecommendations(artists.getArtists()), RECCOMENDED, Recommendations.class);
        ArtistMap reccoArtists =
                cache.getOrLookup(authCode + festival + year, () -> artistMapGenerator.generateLastFmMap(clashfinderData.getEvents(), recArtists.getArtist()), ARTISTMAPREC, ArtistMap.class);
        ArtistMap listenedArtists =
                cache.getOrLookup(authCode + festival + year, () -> artistMapGenerator.generateLastFmMap(clashfinderData.getEvents(), artists.getArtists()),ARTISTMAP, ArtistMap.class);

        return strategy.findOrderedInterection(clashfinderData.getEvents(), listenedArtists.getArtistMap(), reccoArtists.getArtistMap());
    }

    private List<Event> matchingEventsByPlays(Set<Event> clashfinderData, Map<String, Artist> artistMap) {
        return clashfinderData.stream().filter(g -> artistMap.containsKey(g.getName().toLowerCase()))
                .map(e -> new Event(e, Integer.parseInt(artistMap.get(e.getName().toLowerCase()).getPlaycount())))
                .sorted((x, y) -> Integer.compare(y.getScrobs(), x.getScrobs()))
                .collect(toList());
    }

    private List<Event> matchingEventsByRank(Set<Event> clashfinderData, Map<String, Artist> artistMap) {
        return clashfinderData.stream().filter(g -> artistMap.containsKey(g.getName().toLowerCase()))
                .map(e -> new Event(e, 0, artistMap.get(e.getName().toLowerCase()).getRankValue()))
                .sorted((x, y) -> Integer.compare(x.getReccorank(), y.getReccorank()))
                .collect(toList());
    }
}
