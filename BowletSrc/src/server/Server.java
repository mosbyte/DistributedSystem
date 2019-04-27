package server;

import com.google.gson.Gson;
import com.mongodb.*;
import core.Coordinate;
import core.Post;
import core.ServerRoutingObject;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.bson.types.ObjectId;
import javax.jms.*;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private static final double NEWSERVERMINIMUMPERCENTAGE = .01;
    private static final double NEWSERVERLESSTHANPERCENTAGE = .5;
    private static final int NEWSERVERMINPOSTS=100;
    private String serverName;
    private int serverCount=0;
    private Gson gson;
    private DBCollection postsTable;
    private List<ObjectId> posts=new ArrayList<>();
    private MessageConsumer consumer;
    private MessageProducer postProducer;
    private Session session;
    private List<ServerRoutingObject> subServers=new ArrayList<>();


    public static void main(String[] args) {
        //BasicConfigurator.configure();
        new Server("SERVER",null,new ArrayList<>());

    }

    public Server(String serverName,Coordinate centerPoint,List<ObjectId> postsList)
    {
        if(centerPoint!=null)
        {
            this.posts=postsList;
        }
        try {
            this.serverName=serverName;
            ConnectionFactory factory = new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_BROKER_URL);
            Connection connection = factory.createConnection();
            connection.setClientID(this.serverName);
            session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

            MongoClient mongoClient = new MongoClient("localhost", 27017);
            DB database = mongoClient.getDB("PostDB");
            database.createCollection("posts", null);
            postsTable = database.getCollection("posts");
            gson=new Gson();

            Destination destination = session.createTopic(serverName);
            consumer = session.createConsumer(destination);

            // Link to the queue
            Queue queue = session.createQueue("RECEIVE");
            postProducer = session.createProducer(queue);
            connection.start();

            Thread thread = new Thread(() -> {
                while (true) {
                    try {
                        Message message = consumer.receive();
                        if (message instanceof ObjectMessage) {
                            ObjectMessage objectMessage = (ObjectMessage) message;
                            Object object =objectMessage.getObject();
                            Message replyMessage;
                            if(object instanceof Coordinate)
                            {
                                List<Post> posts = getPosts((Coordinate)object);
                                replyMessage = session.createObjectMessage((Serializable) posts);//posts.toArray(new Post[posts.size()]));
                                postProducer=session.createProducer(session.createTopic("REPLY"+((Coordinate)object).getId()));
                            }
                            else
                            {
                                posts.add((ObjectId)object);
                                DBObject dbObj = getDbObject((ObjectId)object);
                                replyMessage = session.createObjectMessage("True " + (dbObj).get("message"));
                                postProducer=session.createProducer(session.createTopic("REPLY"+( dbObj).get("id")));
                            }
                            postProducer.send(replyMessage);
                            // Acknowledge initial message
                        }
                        message.acknowledge();
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });

            thread.start();
        } catch (JMSException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            System.out.println("Database is down");
            e.printStackTrace();
        }

    }

    private List<Post> getPosts(Coordinate coordinate)
    {
        List newlist = new ArrayList<Post>();
        for (ObjectId post: posts)
        {
            DBObject DBpost= getDbObject(post);
            DBObject coords=(DBObject)DBpost.get("coordinate");
            String json = (coords.toString());
            Coordinate postCoord=gson.fromJson(json,Coordinate.class);
            if(Coordinate.distance(coordinate,postCoord)<coordinate.getRadius())
            {
                json=(DBpost.toString());
                Post postFromJson=gson.fromJson(json,Post.class);
                newlist.add(postFromJson);
            }
        }
        System.out.println(coordinate.toString()+" "+serverName+": "+newlist.size()+" "+newlist.toString());
       // System.out.println(newlist.size()+" "+(posts.size()*NEWSERVERMINIMUMPERCENTAGE)+" "+(posts.size()*NEWSERVERLESSTHANPERCENTAGE)+" "+NEWSERVERMINPOSTS);
        if(newlist.size()>(posts.size()*NEWSERVERMINIMUMPERCENTAGE)&&newlist.size()<(posts.size()*NEWSERVERLESSTHANPERCENTAGE)
                &&NEWSERVERMINPOSTS<newlist.size())
        {
            Thread thread = new Thread(() -> {
                try {
                    serverSplit(coordinate);
                } catch (JMSException e) {
                    System.out.println("Server Split Failed to JMS ERROR");
                }
            });
            thread.run();
        }
        return newlist;
    }

    private DBObject getDbObject(ObjectId id)
    {
        BasicDBObject query = new BasicDBObject();
        query.put("_id", id);
        return postsTable.findOne(query);

    }

    private void serverSplit(Coordinate coordinate) throws JMSException {
        serverCount++;
        String newServerName=serverName+"-"+serverCount;
        System.out.println("SERVER SPLIT OCCURRED, branching around coords: "+coordinate.toString()+" into :"+newServerName);
        List<ObjectId> postlist=new ArrayList<>();
        coordinate.setRadius(coordinate.getRadius()*2);
        for (ObjectId post: posts)
        {
            DBObject DBpost= getDbObject(post);
            DBObject coords=(DBObject)DBpost.get("coordinate");
            String json = (coords.toString());
            Coordinate postCoord=gson.fromJson(json,Coordinate.class);
            if(Coordinate.distance(coordinate,postCoord)<coordinate.getRadius())
            {
               postlist.add(post);
            }
        }
        Message replyMessage = session.createObjectMessage(new ServerRoutingObject(coordinate,newServerName,postlist));
        Queue queue = session.createQueue("MANAGER");
        MessageProducer messageProducer = session.createProducer(queue);
        messageProducer.send(replyMessage);
        //new Server(newServerName,coordinate,postlist);
    }
}
