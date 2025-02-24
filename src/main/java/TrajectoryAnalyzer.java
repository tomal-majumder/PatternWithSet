

import java.util.List;
import java.util.Map;
import com.esri.arcgisruntime.geometry.Point;

public class TrajectoryAnalyzer {

    // Method to calculate the number of points in each trajectory
    public static int getTrajectoryLength(List<Point> trajectory) {
        return trajectory.size();
    }

    // Method to generate statistics for all trajectories
    public static void generateStatistics(Map<String, List<Point>> trajectories) {
        int totalObjects = trajectories.size();
        int maxLength = Integer.MIN_VALUE;
        int minLength = Integer.MAX_VALUE;
        int totalLength = 0;

        for (Map.Entry<String, List<Point>> entry : trajectories.entrySet()) {
            int length = getTrajectoryLength(entry.getValue());
            totalLength += length;
            if (length > maxLength) maxLength = length;
            if (length < minLength) minLength = length;
        }

        double averageLength = (double) totalLength / totalObjects;

        System.out.println("Total Moving Objects: " + totalObjects);
        System.out.println("Max Trajectory Length (number of points): " + maxLength);
        System.out.println("Min Trajectory Length (number of points): " + minLength);
        System.out.println("Average Trajectory Length (number of points): " + averageLength);
    }
}
