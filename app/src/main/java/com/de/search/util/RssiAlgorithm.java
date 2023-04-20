package com.de.search.util;

import java.util.LinkedList;
import java.util.Queue;

public class RssiAlgorithm {

    // 1. Basic Algorithms
    public static float calculateDistance1(int rssi){
        float d;
        //d = (float) Math.pow(10, ((Math.abs(rssi) - rssi_1m) / (10 * n))); //rssi distance function
        d = (float) Math.pow(10, ((Math.abs(rssi) - 80) / (10 * 2.0f))); //调参
        int i = (int) (d * 100);
        d = (float) i / 100;
        return d;
    }

    // 2. Kalman filter
    private static final float R = 0.01f; // Process noise covariance
    private static final float Q = 0.1f;  // Measurement noise covariance
    private static float x = 0;          // Initial state estimate
    private static float p = 1;          // Initial estimate error covariance

    public static float calculateDistance2(float rssi) {
        float filteredRssi = applyKalmanFilter(rssi);
        return (float) Math.pow(10, ((Math.abs(filteredRssi) - 80) / (10 * 2.0f)));
    }

    private static float applyKalmanFilter(float rssi) {
        float k = p / (p + R); // Kalman gain
        x = x + k * (rssi - x); // Update state estimate
        p = (1 - k) * p + Q;    // Update estimate error covariance

        return x;
    }


    //3. Recursive Moving Average Filter
    private static final int QUEUE_SIZE = 10; // Queue length
    private static final Queue<Double> rssiQueue = new LinkedList<>(); // Queue for storing the last N RSSI values

    /**
     * Estimation of distances using the recursive mean filtering algorithm
     * @param rssi Current RSSI value
     * @return Estimated distance values
     */
    public static float calculateDistance3(double rssi) {
        double filteredRssi = applyRecursiveAverageFilter(rssi);
        return (float) Math.pow(10, ((Math.abs(filteredRssi) - 80) / (10 * 2.0)));
    }

    /**
     * Filtering of RSSI values using the recursive averaging filtering algorithm
     * @param rssi Current RSSI value
     * @return RSSI values after recursive averaging filtering
     */
    private static double applyRecursiveAverageFilter(double rssi) {
        if (rssiQueue.size() == QUEUE_SIZE) {
            rssiQueue.poll();
        }
        rssiQueue.offer(rssi);
        double sum = 0;
        for (Double item : rssiQueue) {
            sum += item;
        }
        return sum / rssiQueue.size();
    }
}
