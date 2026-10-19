package com.ntdhtcct.domain.diary;

import com.ntdhtcct.domain.project.Project;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Đánh giá vị trí địa lý của ảnh chụp nhật ký so với bán kính công trường (S-41 / T-72).
 */
@Component
public class PhotoLocationEvaluator {

    public static final double EARTH_RADIUS_METERS = 6_371_000.0; // WGS84 mean earth radius

    public static final String STATUS_WITHIN_RADIUS = "WITHIN_RADIUS";
    public static final String STATUS_OUTSIDE_RADIUS = "OUTSIDE_RADIUS";
    public static final String STATUS_PROJECT_NOT_CONFIGURED = "PROJECT_NOT_CONFIGURED";
    public static final String STATUS_NO_GPS = "NO_GPS";
    public static final String STATUS_INVALID_GPS = "INVALID_GPS";

    public record EvaluationResult(
            String locationStatus,
            Double distanceToCenterMeters,
            Double excessDistanceMeters,
            Boolean isSuspicious,
            String locationMessage
    ) {
    }

    /**
     * Tính khoảng cách địa lý theo công thức Haversine giữa 2 tọa độ (đơn vị: mét).
     *
     * @param lat1 Vĩ độ điểm 1 (độ thập phân)
     * @param lon1 Kinh độ điểm 1 (độ thập phân)
     * @param lat2 Vĩ độ điểm 2 (độ thập phân)
     * @param lon2 Kinh độ điểm 2 (độ thập phân)
     * @return Khoảng cách tính bằng mét
     */
    public static double calculateHaversineDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Đánh giá vị trí ảnh dựa trên đối tượng Project.
     */
    public EvaluationResult evaluate(Double photoLat, Double photoLon, Project project) {
        if (project == null) {
            return evaluate(photoLat, photoLon, null, null, null);
        }
        return evaluate(photoLat, photoLon, project.getSiteLatitude(), project.getSiteLongitude(), project.getSiteRadiusMeters());
    }

    /**
     * Đánh giá vị trí ảnh dựa trên các giá trị tọa độ và bán kính công trường.
     */
    public EvaluationResult evaluate(Double photoLat, Double photoLon, Double siteLat, Double siteLon, Double siteRadiusMeters) {
        // 1. Kiểm tra cấu hình dự án
        if (siteLat == null || siteLon == null || siteRadiusMeters == null || siteRadiusMeters <= 0) {
            return new EvaluationResult(
                    STATUS_PROJECT_NOT_CONFIGURED,
                    null,
                    null,
                    false,
                    "Dự án chưa cấu hình vị trí công trường"
            );
        }

        // 2. Kiểm tra ảnh có tọa độ GPS hay không
        if (photoLat == null || photoLon == null) {
            return new EvaluationResult(
                    STATUS_NO_GPS,
                    null,
                    null,
                    false,
                    "Ảnh không có thông tin tọa độ GPS"
            );
        }

        // 3. Kiểm tra tính hợp lệ của tọa độ GPS của ảnh
        if (Double.isNaN(photoLat) || Double.isInfinite(photoLat) || photoLat < -90.0 || photoLat > 90.0
                || Double.isNaN(photoLon) || Double.isInfinite(photoLon) || photoLon < -180.0 || photoLon > 180.0) {
            return new EvaluationResult(
                    STATUS_INVALID_GPS,
                    null,
                    null,
                    false,
                    "Tọa độ GPS của ảnh không hợp lệ"
            );
        }

        // 4. Tính toán khoảng cách bằng Haversine
        double rawDistance = calculateHaversineDistanceMeters(photoLat, photoLon, siteLat, siteLon);
        double distance = Math.round(rawDistance * 10.0) / 10.0;
        double radius = siteRadiusMeters;

        // 5. So sánh với bán kính và gắn cờ nghi vấn
        if (distance <= radius) {
            String msg = String.format(Locale.US, "Trong bán kính công trường (cách tâm %.1fm, bán kính %.0fm)", distance, radius);
            return new EvaluationResult(
                    STATUS_WITHIN_RADIUS,
                    distance,
                    0.0,
                    false,
                    msg
            );
        } else {
            double rawExcess = rawDistance - radius;
            double excess = Math.max(0.0, Math.round(rawExcess * 10.0) / 10.0);
            String msg = String.format(Locale.US, "Ngoài bán kính công trường (vượt %.1fm so với bán kính %.0fm)", excess, radius);
            return new EvaluationResult(
                    STATUS_OUTSIDE_RADIUS,
                    distance,
                    excess,
                    true,
                    msg
            );
        }
    }
}
