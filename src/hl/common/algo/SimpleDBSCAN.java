package hl.common.algo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class SimpleDBSCAN 
{
	private static final int NOISE = -1;
    private static final int UNCLASSIFIED = -99;

	public SimpleDBSCAN() {
		
	}

    public static final class Result<T> {
        public final List<List<T>> clusters;
        public final List<T> noise;
        public final int[] labels;

        Result(List<List<T>> clusters, List<T> noise, int[] labels) {
            this.clusters = clusters;
            this.noise = noise;
            this.labels = labels;
        }
    }

    /** Cluster arbitrary objects T using DBSCAN. */
    public static <T> Result<T> cluster(List<T> items,
                                    Function<T, double[]> features,
                                    double eps, int minPts) {

        // Build data matrix (keeps original order)
        final int n = items.size();
        final double[][] data = new double[n][];
        for (int i = 0; i < n; i++) data[i] = features.apply(items.get(i));

        // Run DBSCAN over data indices
        int[] labels = dbscan(data, eps, minPts);

        // Build clusters/noise mapped back to T
        Map<Integer, List<T>> map = new LinkedHashMap<>();
        List<T> noise = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int lbl = labels[i];
            if (lbl == NOISE) {
                noise.add(items.get(i));
            } else {
                map.computeIfAbsent(lbl, k -> new ArrayList<>()).add(items.get(i));
            }
        }
        List<List<T>> clusters = new ArrayList<>(map.values());
        return new Result<>(clusters, noise, labels);
    }

    // -------- Core DBSCAN on double[][] --------

    private static int[] dbscan(double[][] data, double eps, int minPts) {
        final int n = data.length;
        int[] labels = new int[n];
        Arrays.fill(labels, UNCLASSIFIED);
        boolean[] visited = new boolean[n];

        int clusterId = 0;
        for (int i = 0; i < n; i++) {
            if (visited[i]) continue;
            visited[i] = true;

            List<Integer> neighbors = regionQuery(data, i, eps);
            if (neighbors.size() < minPts) {
                labels[i] = NOISE;
            } else {
                expandCluster(data, labels, visited, i, neighbors, clusterId, eps, minPts);
                clusterId++;
            }
        }
        return labels;
    }

    private static void expandCluster(
    		double[][] data, int[] labels, boolean[] visited,
    		int pointIdx, List<Integer> neighbors,
    		int clusterId, double eps, int minPts) 
    {
        labels[pointIdx] = clusterId;

        for (int k = 0; k < neighbors.size(); k++) {
            int j = neighbors.get(k);

            if (!visited[j]) {
                visited[j] = true;
                List<Integer> neighborsJ = regionQuery(data, j, eps);
                if (neighborsJ.size() >= minPts) {
                    neighbors.addAll(neighborsJ); // density-reachable expansion
                }
            }
            if (labels[j] == UNCLASSIFIED || labels[j] == NOISE) {
                labels[j] = clusterId;
            }
        }
    }

    private static List<Integer> regionQuery(double[][] data, int idx, double eps) {
        List<Integer> neighbors = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            if (euclidean(data[idx], data[i]) <= eps) neighbors.add(i);
        }
        return neighbors;
    }

    private static double euclidean(double[] a, double[] b) {
        double s = 0.0;
        for (int d = 0; d < a.length; d++) {
            double diff = a[d] - b[d];
            s += diff * diff;
        }
        return Math.sqrt(s);
    }
    
	public static <T> void printResult(Result<T> res) {
	    int c = 0;
	    for (List<T> cluster : res.clusters) {
	        System.out.println("  Cluster " + (c++) + ": " + cluster);
	    }
	    if (!res.noise.isEmpty()) {
	        System.out.println("  Noise: " + res.noise);
	    }
	    System.out.println("  Labels: " + Arrays.toString(res.labels));
	}
    
    ////////////////////////////////////////////////////////////////
    
    public static void main(String[] args) {
    	
    	class TextBox {
            public final String id;
            public final double x, y, y2; // y2 for height if useful (height = y2 - y)
            public TextBox(String id, double x, double y, double y2) {
                this.id = id; this.x = x; this.y = y; this.y2 = y2;
            }
            @Override public String toString() {
                return String.format("%s(x=%.1f,y=%.1f,h=%.1f)", id, x, y, y2 - y);
            }
        }
        // Sample data
        List<TextBox> boxes = List.of(
            new TextBox("a",  50, 100, 112),
            new TextBox("b",  52, 130, 142),
            new TextBox("c",  53, 160, 172),
            new TextBox("d", 320,  98, 110),
            new TextBox("e", 322, 128, 140),
            new TextBox("f", 700,  50,  62) // likely noise/outlier
        );

        // 1) Cluster by X only (e.g., columns)
        Result<TextBox> byX = cluster(
            boxes,
            tb -> new double[]{ tb.x },
            /*eps*/ 15.0,
            /*minPts*/ 2
        );
        System.out.println("Clusters by X:");
        printResult(byX);

    }
}