package UnclesPC.app_level.emu_manager;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import UnclesPC.app_level.exception.UnclesPCException;
import UnclesPC.virtual_hardware.io.disk.Disk;
import UnclesPC.virtual_hardware.motherboard.error.ErrorCode;

public final class DiskImgManager implements AutoCloseable {
    private static final Path DEFAULT_IMAGE_PATH = Path.of("disk", "img", "disk.img");

    private final Path imagePath;
    private RandomAccessFile image;

    public DiskImgManager() {
        this(DEFAULT_IMAGE_PATH);
    }

    public DiskImgManager(Path imagePath) {
        this.imagePath = Objects.requireNonNull(imagePath, "imagePath");
    }

    public Path imagePath() {
        return imagePath;
    }

    public boolean exists() {
        return Files.exists(imagePath);
    }

    public boolean isOpen() {
        return image != null;
    }

    public Map<String, Object> info() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("path", imagePath.toString());
        info.put("size", Disk.DISK_SIZE);
        info.put("sector_size", Disk.SECTOR_SIZE);
        info.put("num_sectors", Disk.NUM_SECTORS);
        info.put("exists", exists());
        info.put("is_open", isOpen());
        return info;
    }

    public RandomAccessFile open() throws IOException {
        if (isOpen()) {
            return image;
        }

        if (!exists()) {
            throw new UnclesPCException(ErrorCode.DISK_NOT_FOUND, "disk image not found");
        }

        image = new RandomAccessFile(imagePath.toFile(), "rw");
        image.setLength(Disk.DISK_SIZE);
        return image;
    }

    public RandomAccessFile create(boolean overwrite) throws IOException {
        if (isOpen()) {
            throw new UnclesPCException(ErrorCode.OCCUPIED_DISK_ERROR, "disk image is already open");
        }

        if (exists() && !overwrite) {
            throw new UnclesPCException(ErrorCode.DISK_IMAGE_ERROR, "disk image already exists");
        }

        Path parent = imagePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (RandomAccessFile newImage = new RandomAccessFile(imagePath.toFile(), "rw")) {
            newImage.setLength(Disk.DISK_SIZE);
        }

        return open();
    }

    public RandomAccessFile requireOpen() {
        if (!isOpen()) {
            throw new UnclesPCException(ErrorCode.DISK_NOT_OPEN, "disk image is not open");
        }
        return image;
    }

    public void flush() throws IOException {
        requireOpen().getFD().sync();
    }

    @Override
    public void close() throws IOException {
        if (!isOpen()) {
            return;
        }

        flush();
        image.close();
        image = null;
    }
}
