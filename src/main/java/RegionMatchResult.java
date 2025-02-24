

import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polygon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class RegionMatchResult{
    List<Point> trajectory;
    HashSet<Integer> queryIDs;

    public RegionMatchResult(List<Point> trajectory, HashSet<Integer> queryIDs){
        this.trajectory = new ArrayList<>(trajectory);

        // Create a new set to store a copy of the queryIDs
        this.queryIDs = new HashSet<>(queryIDs);
    }

    public RegionMatchResult(List<Point> trajectory, int queryID) {
        this.trajectory = new ArrayList<>(trajectory);

        // Create a new set to store a copy of the queryIDs
        this.queryIDs = new HashSet<>();
        this.queryIDs.add(queryID);
    }
}