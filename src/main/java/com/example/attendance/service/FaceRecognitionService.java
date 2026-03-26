package com.example.attendance.service;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.*;
import java.util.logging.Logger;

@Service
public class FaceRecognitionService {

    private static final Logger log = Logger.getLogger(FaceRecognitionService.class.getName());
    private static final Size FACE_SIZE = new Size(100, 100);
    // Absolute max distance to consider a match
    private static final double MATCH_THRESHOLD = 6000;
    // Best match must be at least this ratio better than second best (prevents wrong-student matches)
    private static final double MIN_GAP_RATIO = 1.20;

    @Value("${app.faces.dir:data/faces}")
    private String facesDir;

    private CascadeClassifier faceDetector;

    @PostConstruct
    public void init() {
        nu.pattern.OpenCV.loadLocally();

        // Load Haar cascade from classpath resource
        try (InputStream is = getClass().getResourceAsStream("/haarcascade_frontalface_default.xml")) {
            if (is != null) {
                Path tmp = Files.createTempFile("haarcascade", ".xml");
                Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
                faceDetector = new CascadeClassifier(tmp.toString());
                log.info("Haar cascade loaded.");
            } else {
                log.warning("Haar cascade not found in classpath.");
            }
        } catch (IOException e) {
            log.warning("Failed to load Haar cascade: " + e.getMessage());
        }

        Path dir = Paths.get(facesDir);
        if (!Files.exists(dir)) {
            try { Files.createDirectories(dir); }
            catch (IOException e) { throw new IllegalStateException("Could not create faces directory", e); }
        }
    }

    public void saveFaceImage(long studentId, int imageIndex, byte[] jpegBytes) throws IOException {
        // Extract and save only the face crop
        Mat image = Imgcodecs.imdecode(new MatOfByte(jpegBytes), Imgcodecs.IMREAD_COLOR);
        Mat face = extractFace(image);
        if (face == null) {
            // No face detected — save full image as fallback
            log.warning("No face detected in uploaded image " + imageIndex + " for student " + studentId + ", saving full image.");
            face = preprocess(image);
        }

        Path studentDir = Paths.get(facesDir, String.valueOf(studentId));
        if (!Files.exists(studentDir)) Files.createDirectories(studentDir);

        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".jpg", face, mob);
        Files.write(studentDir.resolve("face_" + imageIndex + ".jpg"), mob.toArray());
    }

    public Long identifyStudent(byte[] jpegBytes) {
        Mat image = Imgcodecs.imdecode(new MatOfByte(jpegBytes), Imgcodecs.IMREAD_COLOR);
        if (image.empty()) { log.warning("Could not decode scan image"); return null; }

        Mat probe = extractFace(image);
        if (probe == null) {
            log.warning("No face detected in scan image, using full frame fallback.");
            probe = preprocess(image);
        }

        File root = new File(facesDir);
        File[] studentDirs = root.listFiles(File::isDirectory);
        if (studentDirs == null || studentDirs.length == 0) {
            log.warning("No student face directories found.");
            return null;
        }

        long bestLabel = -1;
        double bestDistance = Double.MAX_VALUE;
        double secondBestDistance = Double.MAX_VALUE;

        for (File studentDir : studentDirs) {
            long studentId;
            try { studentId = Long.parseLong(studentDir.getName()); }
            catch (NumberFormatException e) { continue; }

            File[] faceFiles = studentDir.listFiles((d, n) ->
                n.toLowerCase().endsWith(".jpg") || n.toLowerCase().endsWith(".png"));
            if (faceFiles == null || faceFiles.length == 0) continue;

            // Use average distance across all stored images for this student
            double totalDist = 0;
            int count = 0;
            for (File f : faceFiles) {
                Mat stored = Imgcodecs.imread(f.getAbsolutePath(), Imgcodecs.IMREAD_GRAYSCALE);
                if (stored.empty()) continue;
                if (stored.width() != 100 || stored.height() != 100) {
                    Imgproc.resize(stored, stored, FACE_SIZE);
                }
                Core.normalize(stored, stored, 0, 255, Core.NORM_MINMAX);
                totalDist += Core.norm(probe, stored, Core.NORM_L2);
                count++;
            }
            if (count == 0) continue;

            double avgDist = totalDist / count;
            log.info("student=" + studentId + " avgDistance=" + String.format("%.1f", avgDist));

            if (avgDist < bestDistance) {
                secondBestDistance = bestDistance;
                bestDistance = avgDist;
                bestLabel = studentId;
            } else if (avgDist < secondBestDistance) {
                secondBestDistance = avgDist;
            }
        }

        log.info("Best: student=" + bestLabel + " dist=" + String.format("%.1f", bestDistance)
            + " secondBest=" + String.format("%.1f", secondBestDistance)
            + " threshold=" + MATCH_THRESHOLD);

        if (bestLabel < 0 || bestDistance > MATCH_THRESHOLD) return null;

        // Reject if second best is too close (ambiguous match)
        if (secondBestDistance < Double.MAX_VALUE && secondBestDistance < bestDistance * MIN_GAP_RATIO) {
            log.warning("Match rejected: gap too small (best=" + String.format("%.1f", bestDistance)
                + " secondBest=" + String.format("%.1f", secondBestDistance) + ")");
            return null;
        }

        return bestLabel;
    }

    /** Detect face, crop, resize to 100x100 grayscale normalized. Returns null if no face found. */
    private Mat extractFace(Mat colorImage) {
        if (faceDetector == null || faceDetector.empty()) return null;

        Mat gray = new Mat();
        Imgproc.cvtColor(colorImage, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(gray, gray);

        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(gray, faces, 1.1, 5, 0, new Size(60, 60), new Size());

        Rect[] rects = faces.toArray();
        if (rects.length == 0) return null;

        // Use the largest detected face
        Rect best = rects[0];
        for (Rect r : rects) if (r.area() > best.area()) best = r;

        Mat crop = new Mat(gray, best);
        Mat resized = new Mat();
        Imgproc.resize(crop, resized, FACE_SIZE);
        Core.normalize(resized, resized, 0, 255, Core.NORM_MINMAX);
        return resized;
    }

    private Mat preprocess(Mat colorImage) {
        Mat gray = new Mat();
        Imgproc.cvtColor(colorImage, gray, Imgproc.COLOR_BGR2GRAY);
        Mat resized = new Mat();
        Imgproc.resize(gray, resized, FACE_SIZE);
        Core.normalize(resized, resized, 0, 255, Core.NORM_MINMAX);
        return resized;
    }
}
