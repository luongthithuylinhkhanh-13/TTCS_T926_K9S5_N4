package com.ntdhtcct.domain.diary;

import com.ntdhtcct.domain.project.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("S-41 / T-72: PhotoLocationEvaluator Test - Đánh giá khoảng cách và trạng thái vị trí ảnh")
class PhotoLocationEvaluatorTest {

    private PhotoLocationEvaluator evaluator;

    // Tọa độ công trường mẫu: Trung tâm Hà Nội (21.028511, 105.804817)
    private static final double SITE_LAT = 21.028511;
    private static final double SITE_LON = 105.804817;
    private static final double SITE_RADIUS = 500.0; // 500 mét

    @BeforeEach
    void setUp() {
        evaluator = new PhotoLocationEvaluator();
    }

    @Test
    @DisplayName("Haversine: Khoảng cách giữa hai điểm trùng nhau là 0 mét")
    void testHaversineSamePoint() {
        double distance = PhotoLocationEvaluator.calculateHaversineDistanceMeters(SITE_LAT, SITE_LON, SITE_LAT, SITE_LON);
        assertEquals(0.0, distance, 0.001);
    }

    @Test
    @DisplayName("Haversine: Tính đúng khoảng cách giữa hai tọa độ đã biết")
    void testHaversineKnownDistance() {
        // Điểm dịch chuyển xấp xỉ 111 km theo phương vĩ độ (~1 độ vĩ độ)
        double distance = PhotoLocationEvaluator.calculateHaversineDistanceMeters(0.0, 0.0, 1.0, 0.0);
        // 1 độ kinh tuyến trên Trái Đất ~ 111.195 km
        assertTrue(distance > 111_000 && distance < 112_000);
    }

    @Test
    @DisplayName("Dự án null hoặc chưa cấu hình vị trí -> PROJECT_NOT_CONFIGURED")
    void testProjectNotConfigured() {
        // Project null
        PhotoLocationEvaluator.EvaluationResult resNull = evaluator.evaluate(21.0, 105.8, (Project) null);
        assertEquals(PhotoLocationEvaluator.STATUS_PROJECT_NOT_CONFIGURED, resNull.locationStatus());
        assertNull(resNull.distanceToCenterMeters());
        assertNull(resNull.excessDistanceMeters());
        assertFalse(resNull.isSuspicious());
        assertEquals("Dự án chưa cấu hình vị trí công trường", resNull.locationMessage());

        // Project thiếu bán kính
        Project projectNoRadius = new Project("PRJ-1", "Test 1");
        projectNoRadius.setSiteLatitude(SITE_LAT);
        projectNoRadius.setSiteLongitude(SITE_LON);
        PhotoLocationEvaluator.EvaluationResult resNoRadius = evaluator.evaluate(21.0, 105.8, projectNoRadius);
        assertEquals(PhotoLocationEvaluator.STATUS_PROJECT_NOT_CONFIGURED, resNoRadius.locationStatus());
        assertFalse(resNoRadius.isSuspicious());

        // Project bán kính âm hoặc bằng 0
        Project projectInvalidRadius = new Project("PRJ-2", "Test 2");
        projectInvalidRadius.setSiteLatitude(SITE_LAT);
        projectInvalidRadius.setSiteLongitude(SITE_LON);
        projectInvalidRadius.setSiteRadiusMeters(0.0);
        PhotoLocationEvaluator.EvaluationResult resZeroRadius = evaluator.evaluate(21.0, 105.8, projectInvalidRadius);
        assertEquals(PhotoLocationEvaluator.STATUS_PROJECT_NOT_CONFIGURED, resZeroRadius.locationStatus());
    }

    @Test
    @DisplayName("Ảnh không có tọa độ GPS -> NO_GPS, không bị coi là nghi vấn")
    void testPhotoNoGps() {
        Project project = createSampleProject();

        PhotoLocationEvaluator.EvaluationResult resBothNull = evaluator.evaluate(null, null, project);
        assertEquals(PhotoLocationEvaluator.STATUS_NO_GPS, resBothNull.locationStatus());
        assertNull(resBothNull.distanceToCenterMeters());
        assertNull(resBothNull.excessDistanceMeters());
        assertFalse(resBothNull.isSuspicious());
        assertEquals("Ảnh không có thông tin tọa độ GPS", resBothNull.locationMessage());

        PhotoLocationEvaluator.EvaluationResult resLatOnly = evaluator.evaluate(21.0, null, project);
        assertEquals(PhotoLocationEvaluator.STATUS_NO_GPS, resLatOnly.locationStatus());

        PhotoLocationEvaluator.EvaluationResult resLonOnly = evaluator.evaluate(null, 105.8, project);
        assertEquals(PhotoLocationEvaluator.STATUS_NO_GPS, resLonOnly.locationStatus());
    }

    @Test
    @DisplayName("Tọa độ GPS ảnh bất thường hoặc ngoài phạm vi -> INVALID_GPS")
    void testPhotoInvalidGps() {
        Project project = createSampleProject();

        // Vĩ độ > 90
        PhotoLocationEvaluator.EvaluationResult resLatTooHigh = evaluator.evaluate(90.1, 105.8, project);
        assertEquals(PhotoLocationEvaluator.STATUS_INVALID_GPS, resLatTooHigh.locationStatus());
        assertFalse(resLatTooHigh.isSuspicious());

        // Vĩ độ < -90
        PhotoLocationEvaluator.EvaluationResult resLatTooLow = evaluator.evaluate(-90.1, 105.8, project);
        assertEquals(PhotoLocationEvaluator.STATUS_INVALID_GPS, resLatTooLow.locationStatus());

        // Kinh độ > 180
        PhotoLocationEvaluator.EvaluationResult resLonTooHigh = evaluator.evaluate(21.0, 180.1, project);
        assertEquals(PhotoLocationEvaluator.STATUS_INVALID_GPS, resLonTooHigh.locationStatus());

        // Kinh độ < -180
        PhotoLocationEvaluator.EvaluationResult resLonTooLow = evaluator.evaluate(21.0, -180.1, project);
        assertEquals(PhotoLocationEvaluator.STATUS_INVALID_GPS, resLonTooLow.locationStatus());

        // Tọa độ NaN
        PhotoLocationEvaluator.EvaluationResult resNan = evaluator.evaluate(Double.NaN, 105.8, project);
        assertEquals(PhotoLocationEvaluator.STATUS_INVALID_GPS, resNan.locationStatus());
    }

    @Test
    @DisplayName("Ảnh chụp trùng tâm công trường -> WITHIN_RADIUS, excess = 0, isSuspicious = false")
    void testPhotoAtCenter() {
        Project project = createSampleProject();

        PhotoLocationEvaluator.EvaluationResult res = evaluator.evaluate(SITE_LAT, SITE_LON, project);
        assertEquals(PhotoLocationEvaluator.STATUS_WITHIN_RADIUS, res.locationStatus());
        assertNotNull(res.distanceToCenterMeters());
        assertEquals(0.0, res.distanceToCenterMeters(), 0.1);
        assertEquals(0.0, res.excessDistanceMeters());
        assertFalse(res.isSuspicious());
        assertTrue(res.locationMessage().contains("Trong bán kính công trường"));
    }

    @Test
    @DisplayName("Ảnh chụp nằm rõ trong bán kính -> WITHIN_RADIUS, isSuspicious = false")
    void testPhotoWithinRadius() {
        Project project = createSampleProject();

        // Dịch chuyển nhỏ khoảng ~150m (0.0013 độ vĩ độ)
        double nearLat = SITE_LAT + 0.0013;
        double nearLon = SITE_LON;

        PhotoLocationEvaluator.EvaluationResult res = evaluator.evaluate(nearLat, nearLon, project);
        assertEquals(PhotoLocationEvaluator.STATUS_WITHIN_RADIUS, res.locationStatus());
        assertTrue(res.distanceToCenterMeters() < SITE_RADIUS);
        assertEquals(0.0, res.excessDistanceMeters());
        assertFalse(res.isSuspicious());
    }

    @Test
    @DisplayName("Ảnh chụp nằm ngoài bán kính -> OUTSIDE_RADIUS, tính đúng excess distance, isSuspicious = true")
    void testPhotoOutsideRadius() {
        Project project = createSampleProject();

        // Dịch chuyển khoảng ~1.1 km về phía Bắc (0.01 độ vĩ độ)
        double farLat = SITE_LAT + 0.01;
        double farLon = SITE_LON;

        PhotoLocationEvaluator.EvaluationResult res = evaluator.evaluate(farLat, farLon, project);
        assertEquals(PhotoLocationEvaluator.STATUS_OUTSIDE_RADIUS, res.locationStatus());
        assertTrue(res.distanceToCenterMeters() > SITE_RADIUS);
        assertTrue(res.excessDistanceMeters() > 0);
        assertEquals(res.distanceToCenterMeters() - SITE_RADIUS, res.excessDistanceMeters(), 0.2);
        assertTrue(res.isSuspicious());
        assertTrue(res.locationMessage().contains("Ngoài bán kính công trường"));
    }

    @Test
    @DisplayName("Ảnh tại ranh giới bán kính (boundary) -> WITHIN_RADIUS, excess = 0")
    void testPhotoAtRadiusBoundary() {
        // Giả lập tọa độ có khoảng cách chính xác bằng bán kính
        // Nếu distance <= radius -> WITHIN_RADIUS
        PhotoLocationEvaluator.EvaluationResult res = evaluator.evaluate(SITE_LAT, SITE_LON, SITE_LAT, SITE_LON, 0.0);
        // Bán kính <= 0 coi là chưa cấu hình
        assertEquals(PhotoLocationEvaluator.STATUS_PROJECT_NOT_CONFIGURED, res.locationStatus());

        // Với bán kính 100m, khoảng cách tính ra = 0m -> nhỏ hơn 100m -> WITHIN_RADIUS
        PhotoLocationEvaluator.EvaluationResult resBoundary = evaluator.evaluate(SITE_LAT, SITE_LON, SITE_LAT, SITE_LON, 100.0);
        assertEquals(PhotoLocationEvaluator.STATUS_WITHIN_RADIUS, resBoundary.locationStatus());
        assertEquals(0.0, resBoundary.excessDistanceMeters());
        assertFalse(resBoundary.isSuspicious());
    }

    private Project createSampleProject() {
        Project p = new Project("PRJ-01", "Dự án Hà Nội");
        p.setSiteLatitude(SITE_LAT);
        p.setSiteLongitude(SITE_LON);
        p.setSiteRadiusMeters(SITE_RADIUS);
        return p;
    }
}
