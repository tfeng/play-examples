package controllers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

import org.apache.avro.AvroRemoteException;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import controllers.protocols.KTooLargeError;
import controllers.protocols.Point;
import controllers.protocols.Points;

@Component("points")
public class PointsImpl implements Points {

  private static class DescendingPointComparator implements Comparator<Point> {

    private final Point from;

    public DescendingPointComparator(Point from) {
      this.from = from;
    }

    @Override
    public int compare(Point point1, Point point2) {
      return - Double.compare(distanceSquare(from, point1), distanceSquare(from, point2));
    }

    private double distanceSquare(Point point1, Point point2) {
      double xDistance = point1.getX() - point2.getX();
      double yDistance = point1.getY() - point2.getY();
      return xDistance * xDistance + yDistance * yDistance;
    }
  }

  private final List<Point> points = new ArrayList<>();

  @Override
  public Void addPoint(Point point) throws AvroRemoteException {
    points.add(point);
    return null;
  }

  @Override
  public void clear() {
    points.clear();
  }

  @Override
  public List<Point> getNearestPoints(Point from, int k) throws KTooLargeError {
    if (points.size() < k) {
      throw KTooLargeError.newBuilder().setK(k).build();
    }

    PriorityQueue<Point> queue = new PriorityQueue<>(new DescendingPointComparator(from));
    for (Point point : points) {
      queue.add(point);
      if (queue.size() > k) {
        queue.poll();
      }
    }
    List<Point> points = new ArrayList<>(queue.size());
    while (!queue.isEmpty()) {
      points.add(queue.poll());
    }
    return Lists.reverse(points);
  }
}
