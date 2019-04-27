package server;

import com.google.gson.Gson;
import com.mongodb.*;
import com.mongodb.util.JSON;
import core.Coordinate;
import core.Post;
import core.ServerRoutingObject;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.bson.types.ObjectId;
import sun.security.krb5.internal.crypto.Des;

import javax.jms.*;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class Manager {

    public static void main(String[] args) {
        //BasicConfigurator.configure();
        new Manager();
    }
    private Session session;
    private MessageProducer messageProducer;
    private MessageConsumer managerConsumer;
    private DBCollection postsTable;
    private Gson gson;
    private List<ServerRoutingObject> serverRoutes=new ArrayList<>();


    private Manager() {
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_BROKER_URL);
        try {
            Connection connection = connectionFactory.createConnection();
            connection.setClientID("Management");

            MongoClient mongoClient = null;
            try {
                mongoClient = new MongoClient("localhost", 27017);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            DB database = mongoClient.getDB("PostDB");
            database.createCollection("posts", null);
            gson=new Gson();
            //System.out.println(database.toString());
            //mongoClient.getDatabaseNames().forEach(System.out::println);
            //boolean auth = database.authenticate("username", "pwd".toCharArray());
            postsTable = database.getCollection("posts");

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createTopic("SERVER");
            messageProducer = session.createProducer(destination);

            Queue queue = session.createQueue("MANAGER");
            managerConsumer = session.createConsumer(queue);

            connection.start();

            Thread thread = new Thread(() -> {
                ServerRoutingObject newRoute=null;
                while (true) {
                    try {
                        //System.out.println("yeet");
                        Message message = managerConsumer.receive();
                        //System.out.println("yote");
                        if (message instanceof ObjectMessage) {
                            Object object = ((ObjectMessage) message).getObject();
                            if(object instanceof Coordinate)
                            {
                                getPosts((Coordinate) object);
                            }
                            else if(object instanceof Post)
                            {
                                  newPost((Post)object);
                            }
                            else if(object instanceof ServerRoutingObject)
                            {
                                newRoute=(ServerRoutingObject)object;
                                Queue serverframes = session.createQueue("SERVERFRAMES");
                                MessageProducer prod = session.createProducer(serverframes);
                                prod.send(message);
                                Destination server = session.createTopic(newRoute.getServerName());
                                MessageProducer serverProd = session.createProducer(server);
                                newRoute.setMessageProducer(serverProd);
                                //add to start as that would be the most recently requested route.
                                serverRoutes.add(0,newRoute);
                                System.out.println("New Server ("+((ServerRoutingObject)object).getServerName()+") Received, adding to routing table");
                            }
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
        }
    }

    private void newPost(Post object) {
        try {
            String json=gson.toJson(object);
            DBObject dbObject = (DBObject)JSON.parse(json);
            postsTable.insert((dbObject));
            ObjectId id = (ObjectId)dbObject.get( "_id" );
            ObjectMessage objectMessage = session.createObjectMessage(id);
            messageProducer.send(objectMessage);
            for (ServerRoutingObject route: serverRoutes) {
                if(route.getCenterPoint().withinRadius(object.getCoordinate()))
                {
                    route.getMessageProducer().send(objectMessage);
                }
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    private void getPosts(Coordinate coordinate) {
        MessageProducer producer;
        try {
            ObjectMessage objectMessage = session.createObjectMessage(coordinate);
            producer=chooseServer(coordinate);
            producer.send(objectMessage);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    private MessageProducer chooseServer(Coordinate coordinate) throws JMSException {
        for (ServerRoutingObject route: serverRoutes
             ) {
            if(route.getCenterPoint().subCircle(coordinate))
            {
                return route.getMessageProducer();
            }
        }
        //if no routes, return default server
        return session.createProducer(session.createTopic("SERVER"));
    }
}
