package spotify;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.config.SpotifyConfig;
import spotify.domain.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.BiFunction;

import static java.util.stream.Collectors.toList;


/**
 * Created by Adam on 23/08/2015.
 */
@Singleton
public class SpotifySender {
    private static final Logger logger = LoggerFactory.getLogger(SpotifySender.class);

    private final Client client;

    private static final String baseUrl = "https://accounts.spotify.com/api/token";
    private static final String tracksUrl = "https://api.spotify.com/v1/me/tracks";

    private final String clientId;
    private final String secret;

    @Inject
    public SpotifySender(SpotifyConfig config) {
        clientId = config.getClientId();
        secret = config.getSecret();
//        cc.getClasses().add(JacksonJsonProvider.class);
        client = JerseyClientBuilder.newClient();
    }

    public AccessToken getAuthToken(final String authCode, final String redirectUrl) {
        WebTarget resource = client.target(baseUrl);
        MultivaluedMap<String,String> request = new MultivaluedHashMap<>();
        request.add("grant_type", "authorization_code");
        //TODO Remove occasional suffixed garbage before sending
        request.add("code", authCode);
        request.add("redirect_uri", redirectUrl);
        request.add("client_id", clientId);
        request.add("client_secret", secret);
        return resource.request(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.form(request), AccessToken.class);
    }


    // Use limit and offset to paginate
    public List<SpotifyTracksResponse> getSavedTracks(final String accessCode) {
        return AsyncPaginationUtils.paginateAsync(this::savedTracksRequest, new SpotifyDetails(accessCode),50);
    }


    private Future<SpotifyTracksResponse> savedTracksRequest(int retrieved, SpotifyDetails details) {
        WebTarget resource = client.target(tracksUrl)
        .queryParam("limit", "50")
        .queryParam("offset", String.valueOf(retrieved));
        return resource.request().header("Authorization", "Bearer " + details.getAccessCode()).accept(MediaType.APPLICATION_JSON_TYPE).async()
                .get(SpotifyTracksResponse.class);
    }

    private <T extends SpotifyResponse> List<T> paginate(BiFunction<Integer, SpotifyDetails, T> func, SpotifyDetails details) {
        List<T> responseList = new ArrayList<>();
        int total = 0;
        int retrieved = 0;
        do {
            // TODO Async requests like LastFm similar artists
            T response = func.apply(retrieved, details);
            total = response.getTotal();
            retrieved += response.getItems().size();
            responseList.add(response);
        } while(retrieved < total);
        return responseList;
    }




    private UserProfile getUserId(final String accessCode) {
        WebTarget resource = client.target("https://api.spotify.com/v1/me");
        return resource.request().header("Authorization", "Bearer " + accessCode).accept(MediaType.APPLICATION_JSON_TYPE)
                .get(UserProfile.class);
    }


    public List<SpotifyPlaylistTracksResponse> getPlayListTracks(String accessCode) {
        UserProfile userId = getUserId(accessCode);
        List<SpotifyPlaylist> playlists = getPlaylists(accessCode, userId.getId());
        logger.info("Playlists for : " + userId.getId() + " :: " + playlists.stream().map(x -> x.getId()).collect(toList()));
        return getTracksFromPlaylists(accessCode,userId.getId(),playlists);
    }


    private List<SpotifyPlaylist> getPlaylists(final String accessCode, final String userId) {
        SpotifyPlaylistResponse playlistResponse = getPlaylistResponse(0, new SpotifyDetails(accessCode, userId));
        return playlistResponse.getItems();
    }

    private SpotifyPlaylistResponse getPlaylistResponse(int offset, SpotifyDetails details) {
        WebTarget resource = client.target("https://api.spotify.com/v1/users/" + details.getUserId() + "/playlists")
                .queryParam("limit", "50")
                .queryParam("offset", String.valueOf(offset));
        return resource.request().header("Authorization", "Bearer " + details.getAccessCode()).accept(MediaType.APPLICATION_JSON_TYPE)
                .get(SpotifyPlaylistResponse.class);
    }

    private List<SpotifyPlaylistTracksResponse> getTracksFromPlaylists(final String accessCode, final String userId,
                                                                       final List<SpotifyPlaylist> playlists) {
        List<SpotifyPlaylistTracksResponse> responses = new ArrayList<>();
        for (SpotifyPlaylist playlist : playlists) {
            List<SpotifyPlaylistTracksResponse> res = AsyncPaginationUtils.paginateAsync(this::getSpotifyPlaylistTracksResponse, new SpotifyDetails(accessCode, playlist.getId(), userId),100);
            responses.addAll(res);
        }
        return responses;
    }

    private Future<SpotifyPlaylistTracksResponse> getSpotifyPlaylistTracksResponse(int retrieved, SpotifyDetails details) {
            WebTarget resource =
                    client.target("https://api.spotify.com/v1/users/" + details.getUserId() + "/playlists/" + details.getPlaylistId() + "/tracks")
                            .queryParam("limit", "100")
                            .queryParam("offset", String.valueOf(retrieved));
            return resource.request().header("Authorization", "Bearer " + details.getAccessCode()).accept(MediaType.APPLICATION_JSON_TYPE).async()
                    .get(SpotifyPlaylistTracksResponse.class);

    }

    private SpotifyPlaylistTracksResponse emptySpotifyPlaylistTracksResponse() {
        SpotifyPlaylistTracksResponse response = new SpotifyPlaylistTracksResponse();
        response.setItems(new ArrayList<>());
        response.setTotal(0);
        return response;
    }

}
