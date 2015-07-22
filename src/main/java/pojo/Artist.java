package pojo;



import com.fasterxml.jackson.annotation.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Adam on 27/04/2015.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class Artist {
    private String name;
    private String playcount;
    private Integer rank;

    public Artist() {
    }

    public Artist(String name, String playcount, Integer rank) {
        this.name = name;
        this.playcount = playcount;
        this.rank = rank;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlaycount() {
        return playcount;
    }

    public void setPlaycount(String playcount) {
        this.playcount = playcount;
    }

    @JsonIgnore
    public Integer getRankValue() {
        return rank;
    }

    @JsonProperty("@attr")
    public Map<String,Integer> getRank() {
        Map<String,Integer> result = new HashMap<>();
        result.put("rank", this.rank);
        return result;
    }

    @JsonProperty("@attr")
    public void setRank(Map<String,Integer> attr) {
        this.rank = attr.get("rank");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Artist artist = (Artist) o;

        if (!name.equals(artist.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
