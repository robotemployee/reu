package com.robotemployee.reu.util;

import com.mojang.logging.LogUtils;
import org.joml.Vector3f;
import org.slf4j.Logger;

public class MathTools {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static float getScalarProjection(Vector3f u, Vector3f v) {
        return new Vector3f(u).dot(v) / v.length();
    }

    public static Vector3f getVectorProjection(Vector3f u, Vector3f v) {
        return new Vector3f(v).mul(getScalarProjection(u, v) / v.length());
    }

    public static Vector3f getClosestPointOnVector(Vector3f origin, Vector3f vector, Vector3f point) {
        Vector3f originToPoint = new Vector3f(point).sub(origin);
        return new Vector3f(origin).add(getVectorProjection(originToPoint, vector));
    }

    public static Vector3f getPointProjectedToPlane(Vector3f planeOrigin, Vector3f planeNormal, Vector3f point) {
        float differenceFromPlane = getOffsetFromPlane(planeOrigin, planeNormal, point);
        Vector3f vecToPlane = new Vector3f(planeNormal).mul(-differenceFromPlane);
        /*
        NumberFormat format = NumberFormat.getInstance();
        format.setMaximumFractionDigits(1);
        LOGGER.info(String.format("projecting point to plane; origin: %s, normal: %s, point: %s, diff: %.1f, vecToPlane: %s, result: %s",
                planeOrigin.toString(format),
                planeNormal.toString(format),
                point.toString(format),
                differenceFromPlane,
                vecToPlane.toString(format),
                new Vector3f(point).add(vecToPlane).toString(format)
                ));
         */
        return new Vector3f(point).add(vecToPlane);
    }

    public static float getOffsetFromPlane(Vector3f planeOrigin, Vector3f planeNormal, Vector3f point) {
        Vector3f vecToPoint = new Vector3f(point).sub(planeOrigin);
        return vecToPoint.dot(planeNormal);
    }
}
