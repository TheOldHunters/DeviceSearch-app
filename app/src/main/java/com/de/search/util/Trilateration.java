package com.de.search.util;

import android.location.Location;

import com.de.search.bean.DeviceLocationBean;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;

import java.util.List;
import java.util.Random;

public class Trilateration {

    public static Location getLocationByTrilateration(
            String latitude1, String longitude1, double rssi1,
            String latitude2, String longitude2, double rssi2,
            String latitude3, String longitude3, double rssi3) {

        // Converting latitude and longitude to radians
        double lat1 = Math.toRadians(Double.parseDouble(latitude1));
        double lon1 = Math.toRadians(Double.parseDouble(longitude1));
        double lat2 = Math.toRadians(Double.parseDouble(latitude2));
        double lon2 = Math.toRadians(Double.parseDouble(longitude2));
        double lat3 = Math.toRadians(Double.parseDouble(latitude3));
        double lon3 = Math.toRadians(Double.parseDouble(longitude3));

        // Distance calculation based on RSSI values, here using Kalman filter
        double dist1 = RssiAlgorithm.calculateDistance2((float) rssi1);
        double dist2 = RssiAlgorithm.calculateDistance2((float) rssi2);
        double dist3 = RssiAlgorithm.calculateDistance2((float) rssi3);

        //Constructing trigonometric positioning functions
        MultivariateJacobianFunction trilaterationFunction = new MultivariateJacobianFunction() {
            @Override
            public Pair<RealVector, RealMatrix> value(final RealVector point) {
                RealVector value = new ArrayRealVector(3);
                value.setEntry(0, Math.sqrt(Math.pow(Math.cos(lat1) * Math.cos(lon1) - point.getEntry(0), 2) +
                        Math.pow(Math.cos(lat1) * Math.sin(lon1) - point.getEntry(1), 2) +
                        Math.pow(Math.sin(lat1) - point.getEntry(2), 2)) - dist1);
                value.setEntry(1, Math.sqrt(Math.pow(Math.cos(lat2) * Math.cos(lon2) - point.getEntry(0), 2) +
                        Math.pow(Math.cos(lat2) * Math.sin(lon2) - point.getEntry(1), 2) +
                        Math.pow(Math.sin(lat2) - point.getEntry(2), 2)) - dist2);
                value.setEntry(2, Math.sqrt(Math.pow(Math.cos(lat3) * Math.cos(lon3) - point.getEntry(0), 2) +
                        Math.pow(Math.cos(lat3) * Math.sin(lon3) - point.getEntry(1), 2) +
                        Math.pow(Math.sin(lat3) - point.getEntry(2), 2)) - dist3);

                RealMatrix jacobian = new Array2DRowRealMatrix(3, 3);
                // Calculating the Jacobi matrix
                //The purpose of calculating the Jacobi matrix is to provide a local linear approximation about the position during the optimisation process.
                //In this way, we can better understand how the function changes with position, thus helping us to find the position that minimises the error.
                double[] derivatives1 = computeDerivatives(lat1, lon1, point);
                double[] derivatives2 = computeDerivatives(lat2, lon2, point);
                double[] derivatives3 = computeDerivatives(lat3, lon3, point);

                jacobian.setRow(0, derivatives1);
                jacobian.setRow(1, derivatives2);
                jacobian.setRow(2, derivatives3);

                return new Pair<>(value, jacobian);
            }
        };

// Optimisation using the Levenberg-Marquardt algorithm
        //In optimisation methods such as the Levenberg-Marquardt algorithm, the Jacobi matrix is used as an approximation to the gradient and is used to guide the algorithm in its search along the direction of the gradient.
        //This helps the algorithm to converge to the correct solution, thus improving the accuracy of calculating the position of the unknown object.
        LeastSquaresOptimizer optimizer = new LevenbergMarquardtOptimizer();
        LeastSquaresOptimizer.Optimum optimum = optimizer.optimize(
                new LeastSquaresBuilder()
                        .maxEvaluations(Integer.MAX_VALUE)
                        .maxIterations(Integer.MAX_VALUE)
                        .start(new ArrayRealVector(new double[]{1, 1, 1}))
                        .target(new ArrayRealVector(new double[]{0, 0, 0}))
                        .weight(new Array2DRowRealMatrix(new double[][]{
                                {1, 0, 0},
                                {0, 1, 0},
                                {0, 0, 1}
                        }))
                        .model(trilaterationFunction)
                        .build());

        RealVector coordinates = optimum.getPoint();
        double x = coordinates.getEntry(0);
        double y = coordinates.getEntry(1);
        double z = coordinates.getEntry(2);

// Converting the Cartesian coordinate system back to latitude and longitude coordinates
        double latitude = Math.atan2(z, Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)));
        double longitude = Math.atan2(y, x);


        Location result = new Location("");

        result.setLatitude(Math.toDegrees(latitude));
        result.setLongitude(Math.toDegrees(longitude));

        return result;
    }

    // Calculating the derivative of a distance
    private static double[] computeDerivatives(double lat, double lon, RealVector point) {
        double x = point.getEntry(0) - Math.cos(lat) * Math.cos(lon);
        double y = point.getEntry(1) - Math.cos(lat) * Math.sin(lon);
        double z = point.getEntry(2) - Math.sin(lat);

        double distance = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
        return new double[]{x / distance, y / distance, z / distance};
    }

}
