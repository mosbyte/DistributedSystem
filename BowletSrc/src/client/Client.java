package client;

import core.Coordinate;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import core.Post;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.log4j.BasicConfigurator;
import javax.jms.*;
import java.util.ArrayList;
import java.util.List;

public class Client {
    private Session session;
    private MessageProducer messageProducer;
    private MessageConsumer consumer;
    private ConnectionFactory connectionFactory;
    private String id;

    public static void main(String[] args) {

    }

    public String sendPost(Post post) throws JMSException
    {
        post.setId(id);
        ObjectMessage message= session.createObjectMessage(post);
        messageProducer.send(message);
        Message replyMessage = consumer.receive();
        if (replyMessage instanceof ObjectMessage) {
            message = (ObjectMessage) replyMessage;
            Object object = message.getObject();
            if(object instanceof String) {
                return "Post received : " + object;
            }
        }
        return "Post not received.";
    }

    public List<Post> requestPosts(Coordinate coordinate) throws JMSException
    {
        coordinate.setId(id);
        ObjectMessage message = session.createObjectMessage(coordinate);
        messageProducer.send(message);
        Message replyMessage = consumer.receive();
        if (replyMessage instanceof ObjectMessage) {
            ObjectMessage objectMessage = (ObjectMessage) replyMessage;
            Object response = objectMessage.getObject();
            if ((response instanceof ArrayList<?>)) {
                if(((ArrayList<?>)response).size()==0)
                    return new ArrayList<Post>();
                if (((ArrayList<Post>)response).get(0) instanceof Post) {
                    return (ArrayList<Post>)response;
                }
            }
        }
        return null;
    }

    public Client() {
        //BasicConfigurator.configure();
        connectionFactory = new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_BROKER_URL);

        try {
            Connection connection = connectionFactory.createConnection();
            id = "client" + System.currentTimeMillis();
            connection.setClientID(id);
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue queue = session.createQueue("MANAGER");
            messageProducer = session.createProducer(queue);
            Destination destination = session.createTopic("REPLY"+id);
            consumer = session.createConsumer(destination);
            connection.start();

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
    public void Disconnect() throws JMSException
    {
       ActiveMQConnection connection = (ActiveMQConnection) connectionFactory.createConnection();
        connection.destroyDestination(new ActiveMQQueue("REPLY"+id));
        connection.close();
        //System.exit(0);
    }
}
