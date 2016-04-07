package spotify.domain;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Created by Adam on 08/09/2015.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpotifyTracksItem {
    private SpotifyTrack track;

    public SpotifyTrack getTrack() {
        return track;
    }

    public void setTrack(SpotifyTrack track) {
        this.track = track;
    }
}
