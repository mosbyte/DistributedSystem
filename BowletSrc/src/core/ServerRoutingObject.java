package core;

import org.bson.types.ObjectId;

import javax.jms.Destination;
import javax.jms.MessageProducer;
import java.io.Serializable;
import java.util.List;

public class ServerRoutingObject implements Serializable{
    private final Coordinate centerPoint;
    private final String serverName;
    private List<ObjectId> postList;
    private MessageProducer messageProducer;

    public ServerRoutingObject(Coordinate coords, String serverName,List<ObjectId> postList)
    {
        this.centerPoint=coords;
        this.serverName=serverName;
        this.postList=postList;
    }

    public String getServerName() {
        return serverName;
    }

    public void setMessageProducer(MessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    public MessageProducer getMessageProducer() {
        return messageProducer;
    }

    public Coordinate getCenterPoint() {
        return centerPoint;
    }

    public List<ObjectId> getPostList() {
        return postList;
    }
}
