/*
 *  ======================================================================
 *  || Copyright (c) 2020 Colin Robertson (wobblyyyy@gmail.com)         ||
 *  ||                                                                  ||
 *  || This file is part of the "Pathfinder" project, which is licensed ||
 *  || and distributed under the GPU General Public License V3.         ||
 *  ||                                                                  ||
 *  || Pathfinder is available on GitHub:                               ||
 *  || https://github.com/Wobblyyyy/Pathfinder                          ||
 *  ||                                                                  ||
 *  || Pathfinder's license is available:                               ||
 *  || https://www.gnu.org/licenses/gpl-3.0.en.html                     ||
 *  ||                                                                  ||
 *  || Re-distribution of this, or any other files, is allowed so long  ||
 *  || as this same copyright notice is included and made evident.      ||
 *  ======================================================================
 */

package me.wobblyyyy.pathfinder.util;

import me.wobblyyyy.intra.ftc2.utils.math.Math;
import me.wobblyyyy.pathfinder.geometry.HeadingPoint;
import me.wobblyyyy.pathfinder.geometry.Point;

/**
 * Utility class for measuring distance.
 *
 * <p>
 * This is just a collection of simple distance-related utilities that
 * don't really belong in any specific place.
 * </p>
 *
 * <p>
 * Anything related to distance should go here - unless, of course, it pertains
 * only to a specific class. I mean... I doubt that anybody's going to be
 * studying the JavaDocs behind the "util" package on a pathfinding library,
 * but you never know, right?
 * </p>
 *
 * @author Colin Robertson
 */
public class Distance {
    /**
     * Get the distance between two points.
     *
     * <p>
     * This math is based on the distance formula, which you can Google if
     * you'd like to verify that this is, in fact, real math. The distance
     * formula is based upon the Pythagorean theory - simply put, it
     * accounts for the difference in X and Y to form two "legs" and then
     * calculates the hypotenuse of our theoretical triangle.
     * </p>
     *
     * <p>
     * I seriously do doubt that there will ever be any issues with the
     * functionality of this method - if there's a math issue, you should
     * look elsewhere.
     * </p>
     *
     * @param a the first of the two points.
     * @param b the second of the two points.
     * @return the distance between the two points.
     */
    public static double getDistance(Point a,
                                     Point b) {
        return Math.sqrt(
                Math.pow(b.getX() - a.getX(), 2) +
                        Math.pow(b.getY() - a.getY(), 2)
        );
    }

    /**
     * Create a new HeadingPoint a certain distance away from a direction.
     *
     * <p>
     * The returned point can be casted into a heading point if you'd so
     * desire. Otherwise, it can just be left as a regular point.
     * </p>
     *
     * <p>
     * The math for this class is a bit harder to find, however, a quick
     * Google search for "draw line in direction" should bring up something
     * relevant enough to get you started.
     * </p>
     *
     * <p>
     * As with the {@link #getDistance(Point, Point)} method, this code has
     * already been verified to work entirely as intended. If you feel that
     * you have a mathematical issue that's preventing your robot from working,
     * you can rule this method out as a source.
     * </p>
     *
     * @param start  the start position and angle. Please note that this angle
     *               should and is always be notated in DEGREES, not RADIANS.
     *               If you have a radian measure that you'd like to convert to
     *               a degrees measure, you can use the Math.toDegrees() method
     *               and you should be all good.
     * @param length the distance between the two points. This is known
     *               colloquially as the length of the line.
     * @return a new point, a set distance and direction away from the origin.
     */
    public static Point inDirection(HeadingPoint start,
                                    double length) {
        return new HeadingPoint(
                start.getX() + (length * Math.cos(
                        Math.toRadians(start.getHeading()))),
                start.getY() + (length * Math.sin(
                        Math.toRadians(start.getHeading()))),
                start.getHeading()
        );
    }

    /**
     * Create a new HeadingPoint a certain distance away from a direction.
     *
     * <p>
     * The returned point can be casted into a heading point if you'd so
     * desire. Otherwise, it can just be left as a regular point.
     * </p>
     *
     * <p>
     * The math for this class is a bit harder to find, however, a quick
     * Google search for "draw line in direction" should bring up something
     * relevant enough to get you started.
     * </p>
     *
     * <p>
     * As with the {@link #getDistance(Point, Point)} method, this code has
     * already been verified to work entirely as intended. If you feel that
     * you have a mathematical issue that's preventing your robot from working,
     * you can rule this method out as a source.
     * </p>
     *
     * <p>
     * This method is an overloaded variant of the other inDirection method
     * contained in this file - just as a PSA.
     * </p>
     *
     * @param start     the start position, expressed as a point.
     * @param direction the angle at which the line should extend, represented
     *                  as a measurement in degrees. If you have a radians
     *                  measurement, you're going to need to convert it to
     *                  a degrees one before continuing.
     * @param length    the distance between the two points. This is known
     *                  colloquially as the length of the line.
     * @return a new point, a set distance and direction away from the origin.
     */
    public static Point inDirection(Point start,
                                    double direction,
                                    double length) {
        return inDirection(
                HeadingPoint.withNewHeading(
                        start,
                        direction
                ),
                length
        );
    }

    /**
     * Determine whether or not point A is near point B with a given tolerance.
     *
     * <p>
     * If the tolerance value isn't greater than 0, this will always return
     * false. Make sure the tolerance values you're inputting are, in fact,
     * above zero, as distance is always measured as an absolute.
     * </p>
     *
     * @param a         the first of the two points.
     * @param b         the second of the two points.
     * @param tolerance the maximum allowable distance that's still "near".
     * @return whether or not the two points are within the tolerance.
     */
    public static boolean isNearPoint(Point a,
                                      Point b,
                                      double tolerance) {
        return getDistance(a, b) < tolerance;
    }

    /**
     * Convert a distance, given in inches, to a distance, given in meters.
     *
     * @param inches the distance, measured in inches.
     * @return the distance, measured in meters.
     */
    public static double inchesToMeters(double inches) {
        return inches * 0.0254;
    }

    /**
     * Convert a distance, given in meters, to a distance, given in inches.
     *
     * @param meters the distance, measured in meters.
     * @return the distance, measured in inches.
     */
    public static double metersToInches(double meters) {
        return meters * 39.37;
    }
}
