package com.ntdhtcct.domain.diary;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tiện ích trích xuất siêu dữ liệu EXIF / GPS từ tệp ảnh nhị phân (T-74 / S-42).
 */
public final class ExifMetadataExtractor {

    private static final Pattern EXIF_DATE_PATTERN =
            Pattern.compile("^(\\d{4})[:/-](\\d{2})[:/-](\\d{2})\\s+(\\d{2}):(\\d{2}):(\\d{2})");

    private ExifMetadataExtractor() {
    }

    public record ParsedExif(OffsetDateTime capturedAt, Double latitude, Double longitude) {
        public static ParsedExif empty() {
            return new ParsedExif(null, null, null);
        }
    }

    /**
     * Trích xuất ngày giờ chụp và tọa độ GPS từ buffer ảnh JPEG.
     */
    public static ParsedExif extract(byte[] bytes) {
        if (bytes == null || bytes.length < 16) {
            return ParsedExif.empty();
        }

        // Kiểm tra SOI: FF D8
        if ((bytes[0] & 0xFF) != 0xFF || (bytes[1] & 0xFF) != 0xD8) {
            return ParsedExif.empty();
        }

        int offset = 2;
        int length = bytes.length;

        OffsetDateTime capturedAt = null;
        Double latitude = null;
        Double longitude = null;

        while (offset + 4 < length) {
            int marker = ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
            offset += 2;

            if (marker == 0xFFE1) { // APP1 Exif Marker
                int app1Length = ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
                int app1Start = offset + 2;

                // Kiểm tra "Exif\0\0"
                if (app1Start + 6 <= length
                        && bytes[app1Start] == 'E'
                        && bytes[app1Start + 1] == 'x'
                        && bytes[app1Start + 2] == 'i'
                        && bytes[app1Start + 3] == 'f'
                        && bytes[app1Start + 4] == 0
                        && bytes[app1Start + 5] == 0) {

                    int tiffStart = app1Start + 6;
                    ParsedExif parsed = parseTiff(bytes, tiffStart, Math.min(length, app1Start + app1Length));
                    if (parsed != null) {
                        return parsed;
                    }
                }
                break;
            } else if ((marker & 0xFF00) == 0xFF00 && marker != 0xFFD8 && marker != 0xFFD9) {
                int markerLength = ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
                offset += markerLength;
            } else {
                break;
            }
        }

        return new ParsedExif(capturedAt, latitude, longitude);
    }

    private static ParsedExif parseTiff(byte[] bytes, int tiffStart, int maxLen) {
        if (tiffStart + 8 > maxLen) {
            return ParsedExif.empty();
        }

        boolean littleEndian;
        if (bytes[tiffStart] == 'I' && bytes[tiffStart + 1] == 'I') {
            littleEndian = true;
        } else if (bytes[tiffStart] == 'M' && bytes[tiffStart + 1] == 'M') {
            littleEndian = false;
        } else {
            return ParsedExif.empty();
        }

        int fortyTwo = readShort(bytes, tiffStart + 2, littleEndian);
        if (fortyTwo != 42) {
            return ParsedExif.empty();
        }

        int firstIfdOffset = readInt(bytes, tiffStart + 4, littleEndian);
        if (firstIfdOffset < 8 || tiffStart + firstIfdOffset + 2 > maxLen) {
            return ParsedExif.empty();
        }

        int ifd0Start = tiffStart + firstIfdOffset;
        int numEntries = readShort(bytes, ifd0Start, littleEndian);
        int entryOffset = ifd0Start + 2;

        Integer exifIfdOffset = null;
        Integer gpsIfdOffset = null;
        OffsetDateTime capturedAt = null;

        for (int i = 0; i < numEntries && entryOffset + 12 <= maxLen; i++, entryOffset += 12) {
            int tag = readShort(bytes, entryOffset, littleEndian);
            if (tag == 0x0132 && capturedAt == null) { // DateTime
                capturedAt = parseExifDateString(readAsciiString(bytes, entryOffset, tiffStart, maxLen, littleEndian));
            } else if (tag == 0x8769) { // Exif IFD pointer
                exifIfdOffset = readInt(bytes, entryOffset + 8, littleEndian);
            } else if (tag == 0x8825) { // GPS IFD pointer
                gpsIfdOffset = readInt(bytes, entryOffset + 8, littleEndian);
            }
        }

        // Đọc Exif IFD
        if (exifIfdOffset != null && tiffStart + exifIfdOffset + 2 <= maxLen) {
            int exifStart = tiffStart + exifIfdOffset;
            int numExif = readShort(bytes, exifStart, littleEndian);
            int exifEntryOffset = exifStart + 2;

            for (int i = 0; i < numExif && exifEntryOffset + 12 <= maxLen; i++, exifEntryOffset += 12) {
                int tag = readShort(bytes, exifEntryOffset, littleEndian);
                if (tag == 0x9003 || tag == 0x9004) { // DateTimeOriginal / DateTimeDigitized
                    String dateStr = readAsciiString(bytes, exifEntryOffset, tiffStart, maxLen, littleEndian);
                    OffsetDateTime parsed = parseExifDateString(dateStr);
                    if (parsed != null) {
                        capturedAt = parsed;
                        break;
                    }
                }
            }
        }

        Double latitude = null;
        Double longitude = null;

        // Đọc GPS IFD
        if (gpsIfdOffset != null && tiffStart + gpsIfdOffset + 2 <= maxLen) {
            int gpsStart = tiffStart + gpsIfdOffset;
            int numGps = readShort(bytes, gpsStart, littleEndian);
            int gpsEntryOffset = gpsStart + 2;

            String latRef = null;
            String lonRef = null;
            double[] latCoords = null;
            double[] lonCoords = null;

            for (int i = 0; i < numGps && gpsEntryOffset + 12 <= maxLen; i++, gpsEntryOffset += 12) {
                int tag = readShort(bytes, gpsEntryOffset, littleEndian);
                if (tag == 0x0001) { // GPSLatitudeRef
                    latRef = readAsciiString(bytes, gpsEntryOffset, tiffStart, maxLen, littleEndian);
                } else if (tag == 0x0002) { // GPSLatitude
                    latCoords = readRationals(bytes, gpsEntryOffset, tiffStart, 3, maxLen, littleEndian);
                } else if (tag == 0x0003) { // GPSLongitudeRef
                    lonRef = readAsciiString(bytes, gpsEntryOffset, tiffStart, maxLen, littleEndian);
                } else if (tag == 0x0004) { // GPSLongitude
                    lonCoords = readRationals(bytes, gpsEntryOffset, tiffStart, 3, maxLen, littleEndian);
                }
            }

            if (latCoords != null && latCoords.length == 3) {
                double lat = latCoords[0] + (latCoords[1] / 60.0) + (latCoords[2] / 3600.0);
                if ("S".equalsIgnoreCase(latRef)) {
                    lat = -lat;
                }
                latitude = Math.round(lat * 1000000.0) / 1000000.0;
            }

            if (lonCoords != null && lonCoords.length == 3) {
                double lon = lonCoords[0] + (lonCoords[1] / 60.0) + (lonCoords[2] / 3600.0);
                if ("W".equalsIgnoreCase(lonRef)) {
                    lon = -lon;
                }
                longitude = Math.round(lon * 1000000.0) / 1000000.0;
            }
        }

        return new ParsedExif(capturedAt, latitude, longitude);
    }

    public static OffsetDateTime parseExifDateString(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }

        Matcher matcher = EXIF_DATE_PATTERN.matcher(dateStr.trim());
        if (!matcher.find()) {
            return null;
        }

        try {
            int year = Integer.parseInt(matcher.group(1));
            int month = Integer.parseInt(matcher.group(2));
            int day = Integer.parseInt(matcher.group(3));
            int hour = Integer.parseInt(matcher.group(4));
            int min = Integer.parseInt(matcher.group(5));
            int sec = Integer.parseInt(matcher.group(6));

            LocalDateTime ldt = LocalDateTime.of(year, month, day, hour, min, sec);
            return ldt.atZone(ZoneId.systemDefault()).toOffsetDateTime();
        } catch (Exception e) {
            return null;
        }
    }

    private static int readShort(byte[] bytes, int offset, boolean littleEndian) {
        if (littleEndian) {
            return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
        } else {
            return ((bytes[offset] & 0xFF) << 8) | (bytes[offset + 1] & 0xFF);
        }
    }

    private static int readInt(byte[] bytes, int offset, boolean littleEndian) {
        if (littleEndian) {
            return (bytes[offset] & 0xFF)
                    | ((bytes[offset + 1] & 0xFF) << 8)
                    | ((bytes[offset + 2] & 0xFF) << 16)
                    | ((bytes[offset + 3] & 0xFF) << 24);
        } else {
            return ((bytes[offset] & 0xFF) << 24)
                    | ((bytes[offset + 1] & 0xFF) << 16)
                    | ((bytes[offset + 2] & 0xFF) << 8)
                    | (bytes[offset + 3] & 0xFF);
        }
    }

    private static String readAsciiString(byte[] bytes, int entryOffset, int tiffStart, int maxLen, boolean littleEndian) {
        int count = readInt(bytes, entryOffset + 4, littleEndian);
        if (count <= 0) {
            return null;
        }

        int strOffset;
        if (count <= 4) {
            strOffset = entryOffset + 8;
        } else {
            strOffset = tiffStart + readInt(bytes, entryOffset + 8, littleEndian);
        }

        if (strOffset < 0 || strOffset + count > maxLen) {
            return null;
        }

        int actualLen = count;
        while (actualLen > 0 && (bytes[strOffset + actualLen - 1] == 0 || bytes[strOffset + actualLen - 1] == ' ')) {
            actualLen--;
        }

        if (actualLen <= 0) {
            return null;
        }

        return new String(bytes, strOffset, actualLen, StandardCharsets.US_ASCII);
    }

    private static double[] readRationals(byte[] bytes, int entryOffset, int tiffStart, int count, int maxLen, boolean littleEndian) {
        int offset = tiffStart + readInt(bytes, entryOffset + 8, littleEndian);
        if (offset < 0 || offset + (count * 8) > maxLen) {
            return null;
        }

        double[] rationals = new double[count];
        for (int i = 0; i < count; i++) {
            long num = readInt(bytes, offset + (i * 8), littleEndian) & 0xFFFFFFFFL;
            long den = readInt(bytes, offset + (i * 8) + 4, littleEndian) & 0xFFFFFFFFL;
            rationals[i] = (den == 0) ? 0.0 : ((double) num / (double) den);
        }
        return rationals;
    }
}
