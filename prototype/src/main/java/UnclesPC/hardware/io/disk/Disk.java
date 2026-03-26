package UnclesPC.hardware.io.disk;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import UnclesPC.hardware.motherboard.error.ErrorCode;
import UnclesPC.hardware.motherboard.error.MyIBMGameException;

public final class Disk implements AutoCloseable {
    public static final int SECTOR_SIZE = 512;
    public static final long DISK_SIZE = 5L * 1024L * 1024L * 1024L;
    public static final long NUM_SECTORS = DISK_SIZE / SECTOR_SIZE;

    private final Path imagePath;
    private final long diskSize;
    private final int sectorSize;
    private RandomAccessFile file;
    private boolean dirty;

    public Disk() {
        this("disk.img", DISK_SIZE, SECTOR_SIZE);
    }

    public Disk(String imagePath, long diskSize, int sectorSize) {
        if (sectorSize <= 0) {
            throw new MyIBMGameException(ErrorCode.SECTOR_SIZE_ERROR, "sector size must be positive");
        }
        if (diskSize <= 0 || diskSize % sectorSize != 0) {
            throw new MyIBMGameException(ErrorCode.DISK_ERROR, "disk size must be a positive sector multiple");
        }

        this.imagePath = Path.of(imagePath);
        this.diskSize = diskSize;
        this.sectorSize = sectorSize;
        this.file = null;
        this.dirty = false;
    }

    public long size() {
        return diskSize;
    }

    public int sectorSize() {
        return sectorSize;
    }

    public long numSectors() {
        return diskSize / sectorSize;
    }

    public boolean isOpen() {
        return file != null;
    }

    public boolean exists() {
        return Files.exists(imagePath);
    }

    public Map<String, Object> info() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("path", imagePath.toString());
        info.put("size", size());
        info.put("sector_size", sectorSize());
        info.put("num_sectors", numSectors());
        info.put("exists", exists());
        info.put("is_open", isOpen());
        info.put("dirty", dirty);
        return info;
    }

    public void create(boolean overwrite) throws IOException {
        if (exists() && !overwrite) {
            throw new MyIBMGameException(ErrorCode.DISK_IMAGE_ERROR, "disk image already exists");
        }

        Path parent = imagePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (RandomAccessFile newFile = new RandomAccessFile(imagePath.toFile(), "rw")) {
            newFile.setLength(diskSize);
        }
    }

    public Disk open(boolean create) throws IOException {
        if (isOpen()) {
            return this;
        }

        if (!exists()) {
            if (!create) {
                throw new MyIBMGameException(ErrorCode.DISK_NOT_FOUND, "disk image not found");
            }
            create(false);
        }

        file = new RandomAccessFile(imagePath.toFile(), "rw");
        file.setLength(diskSize);
        dirty = false;
        return this;
    }

    public Disk initialize() throws IOException {
        if (!exists()) {
            create(false);
        }
        return open(false);
    }

    public void flush() throws IOException {
        RandomAccessFile diskFile = requireOpen();
        diskFile.getFD().sync();
        dirty = false;
    }

    @Override
    public void close() throws IOException {
        if (!isOpen()) {
            return;
        }

        flush();
        file.close();
        file = null;
    }

    public byte[] readSector(long sectorNumber) throws IOException {
        validateSectorNumber(sectorNumber);
        RandomAccessFile diskFile = requireOpen();
        diskFile.seek(sectorNumber * sectorSize);
        byte[] data = new byte[sectorSize];
        int bytesRead = diskFile.read(data);
        if (bytesRead < 0) {
            return data;
        }
        if (bytesRead < sectorSize) {
            for (int i = bytesRead; i < sectorSize; i++) {
                data[i] = 0;
            }
        }
        return data;
    }

    public void writeSector(long sectorNumber, byte[] data) throws IOException {
        validateSectorNumber(sectorNumber);
        if (data.length != sectorSize) {
            throw new MyIBMGameException(ErrorCode.SECTOR_SIZE_ERROR, "sector write must match sector size");
        }

        RandomAccessFile diskFile = requireOpen();
        diskFile.seek(sectorNumber * sectorSize);
        diskFile.write(data);
        dirty = true;
    }

    private RandomAccessFile requireOpen() {
        if (!isOpen()) {
            throw new MyIBMGameException(ErrorCode.DISK_NOT_FOUND, "disk image is not open");
        }
        return file;
    }

    private void validateSectorNumber(long sectorNumber) {
        if (sectorNumber < 0 || sectorNumber >= numSectors()) {
            throw new MyIBMGameException(ErrorCode.SECTOR_SIZE_ERROR, "sector number out of range");
        }
    }
}
