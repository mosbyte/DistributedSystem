package core;

import com.mongodb.BasicDBObject;
import core.Coordinate;

import java.io.Serializable;

public class Post implements Serializable {
    private int upvotes = 0;
    private int downvotes = 0;
    private String message = "";
    private Coordinate coordinate;
    private String id;

    public Post(String message, Coordinate coordinate) {
        this.message = message;
        this.coordinate = coordinate;
    }

    public Post(){}


    public void setId(String id) {
        this.id = id;
    }

    public String toString()
    {
        return coordinate.toString()+" "+message;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }
}
