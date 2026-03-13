package config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public final class ImageStorageUtil {

    private ImageStorageUtil() {
    }

    public static String copyProfileImage(File sourceFile) throws Exception {
        return copyImage(sourceFile, "profile", "profile_");
    }

    public static String copyProductImage(File sourceFile) throws Exception {
        return copyImage(sourceFile, "products", "product_");
    }

    private static String copyImage(File sourceFile, String folderName, String filePrefix) throws Exception {
        if (sourceFile == null) {
            return "";
        }

        Path uploadsDir = Paths.get(System.getProperty("user.dir"), "uploads", folderName);
        Files.createDirectories(uploadsDir);

        String fileName = sourceFile.getName();
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0) {
            ext = fileName.substring(dot);
        }

        String newFileName = filePrefix + System.currentTimeMillis() + ext;
        Path targetPath = uploadsDir.resolve(newFileName);
        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        return "uploads/" + folderName + "/" + newFileName;
    }
}
