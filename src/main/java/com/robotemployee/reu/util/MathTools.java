package com.robotemployee.reu.util;

import org.joml.Vector3f;

public class MathTools {
    public static float getScalarProjection(Vector3f u, Vector3f v) {
        return new Vector3f(u).dot(v) / v.lengthSquared();
    }

    public static Vector3f getVectorProjection(Vector3f u, Vector3f v) {
        return new Vector3f(v).mul(getScalarProjection(u, v));
    }

    public static Vector3f getClosestPointOnVector(Vector3f origin, Vector3f vector, Vector3f point) {
        Vector3f originToPoint = new Vector3f(point).sub(origin);
        return getVectorProjection(originToPoint, vector);
    }

    public static Vector3f getPointProjectedToPlane(Vector3f planeOrigin, Vector3f planeNormal, Vector3f point) {
        float differenceFromPlane = getOffsetFromPlane(planeOrigin, planeNormal, point);
        Vector3f vecToPlane = new Vector3f(planeNormal).mul(-differenceFromPlane);
        return new Vector3f(point).add(vecToPlane);
    }

    public static float getOffsetFromPlane(Vector3f planeOrigin, Vector3f planeNormal, Vector3f point) {
        Vector3f vecToPoint = new Vector3f(point).sub(planeOrigin);
        return vecToPoint.dot(planeNormal);
    }
}
