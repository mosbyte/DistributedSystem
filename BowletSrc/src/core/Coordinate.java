package core;

import java.io.Serializable;

public class Coordinate implements Serializable {
    //this will act as the mapping to area system.  gonna work off a 100*100 plane with decimal placement and whatnot,
    //will use modulo for comparison and dealing with overlap.
    private double x;
    private double y;
    private double radius;
    private String id;

    public Coordinate(double x,double y,double radius)
    {
            this.x = x%100;
            this.y = y%100;
            this.radius=radius%100;
    }

    public Coordinate(double x,double y)//,double radius)
    {
        this.x = x%100;
        this.y = y%100;
        this.radius=5;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String toString()
    {
        return "("+x+","+y+")";
    }

    double getX() {
        return x;
    }

    double getY() {
        return y;
    }

    public static double distance(Coordinate a, Coordinate b)
    {
        double distance;
        double xAdded=Math.abs(a.getX()-b.getX());
        if(xAdded>50)
            xAdded=Math.abs(xAdded-100);
        double yAdded=Math.abs(a.getY()-b.getY());
        if(yAdded>50)
            yAdded=Math.abs(yAdded-100);

        distance=Math.pow(xAdded,2)+Math.pow(yAdded,2);
        distance=Math.sqrt(distance);
        return distance;
    }//46,22 vs 48,80

    public double getRadius() {
        return radius;
    }
    public boolean withinRadius(Coordinate coordinate)
    {
        return distance(this,coordinate)<radius;
    }
    public boolean subCircle(Coordinate coordinate)
    {
        return (distance(this,coordinate)+coordinate.getRadius())<(radius);
    }
    public void setRadius(double radius) {
        this.radius = radius;
    }
}
