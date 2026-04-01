package UnclesPC.hardware.io.disk;

// TODO: Refactor disk to assume .img file is pre-created and always exists. This will simplify and modularize the code.


import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import UnclesPC.hardware.motherboard.error.ErrorCode;
import UnclesPC.hardware.ram.Memory;
import UnclesPC.exception.UnclesPCException;
import UnclesPC.hardware.io.exception.VirtualHardwareException;
import UnclesPC.hardware.ram.modules.MemoryMap;

public final class Disk implements AutoCloseable {
    private final int SECTOR_SIZE = 512;
    private final long DISK_SIZE = 5L * 1024L * 1024L * 1024L;
    private final long NUM_SECTORS = DISK_SIZE / SECTOR_SIZE;
    private final int MAX_SECTOR_READ = 128;

    private final Path imagePath;
    private RandomAccessFile file;
    private final Memory memory;

    public Disk() {
        this(new Memory());
    }

    public Disk(Memory memory) {
        this.memory = memory;
        this.imagePath = Path.of("disk", "img", "disk.img");
    }

    public long size() {
        return DISK_SIZE;
    }

    public int sectorSize() {
        return SECTOR_SIZE;
    }

    public long numSectors() {
        return DISK_SIZE / SECTOR_SIZE;
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
        return info;
    }

    public void create(boolean overwrite) throws IOException {
        if (exists() && !overwrite) {
            throw new UnclesPCException(ErrorCode.DISK_IMAGE_ERROR, "disk image already exists");
        }

        Path parent = imagePath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (RandomAccessFile newFile = new RandomAccessFile(imagePath.toFile(), "rw")) {
            initializeImageFile(newFile);
        }
    }

    public Disk open(boolean create) throws IOException {
        if (isOpen()) {
            return this;
        }

        if (!exists()) {
            if (!create) {
                throw new UnclesPCException(ErrorCode.DISK_NOT_FOUND, "disk image not found");
            }
            create(false);
        }

        file = new RandomAccessFile(imagePath.toFile(), "rw");
        file.setLength(DISK_SIZE);
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

    private RandomAccessFile requireOpen() {
        if (!isOpen()) {
            throw new UnclesPCException(ErrorCode.DISK_NOT_FOUND, "disk image is not open");
        }
        return file;
    }

    private void initializeImageFile(RandomAccessFile diskFile) throws IOException {
        // Reuse one zero-filled buffer so we can build the full disk image without
        // allocating a multi-gigabyte byte array in memory.
        byte[] emptyChunk = new byte[SECTOR_SIZE * MAX_SECTOR_READ];
        long remainingBytes = DISK_SIZE;

        diskFile.setLength(0);
        diskFile.seek(0);

        while (remainingBytes > 0) {
            int bytesToWrite = (int) Math.min(emptyChunk.length, remainingBytes);
            diskFile.write(emptyChunk, 0, bytesToWrite);
            remainingBytes -= bytesToWrite;
        }
    }

    public void executeCMD() {
        long startTime = System.currentTimeMillis();
        long timeoutMillis = 5000;

        while (memory.readWord(DiskRegisters.STATUS.address) == 0x01) {
            if (System.currentTimeMillis() - startTime > timeoutMillis) {
                memory.writeWord(DiskRegisters.ERROR.address, ErrorCode.DISK_TIMEOUT.code());
                return;
            }
            Thread.onSpinWait();
        }

        memory.writeWord(DiskRegisters.STATUS.address, 0x01);

        try {
            int command = memory.readWord(DiskRegisters.COMMAND.address);
            switch (command) {
                case 0x01 -> this.read();
                case 0x02 -> this.write();
                case 0x03 -> this.format();
                default   -> throw new VirtualHardwareException(ErrorCode.INVALID_DISK_COMMAND.code());
            }
        } catch (VirtualHardwareException e) {
            memory.writeWord(DiskRegisters.ERROR.address, e.getErrorCode());
        } finally {
            memory.writeWord(DiskRegisters.STATUS.address, 0x00);
        }
    }

    private void read() {
        try {
            int sectorCount = memory.readWord(DiskRegisters.SECTOR_COUNT.address);
            int lba = memory.readWord(DiskRegisters.LBA.address);
            int bufferPtr = memory.readWord(DiskRegisters.BUFFER_PTR.address);

            if (sectorCount > MAX_SECTOR_READ) {
                throw new VirtualHardwareException(ErrorCode.SECTOR_COUNT_ERROR.code());

            }

            if (lba < 0 || (long) lba + sectorCount > NUM_SECTORS) {
                throw new VirtualHardwareException(ErrorCode.SECTOR_COUNT_ERROR.code());
            }

            int bytesToRead = sectorCount * SECTOR_SIZE;
            long bufferEnd = (long) bufferPtr + bytesToRead - 1;
            boolean inKernelHeap = bufferPtr >= MemoryMap.KERNEL_HEAP_START.value()
                && bufferEnd <= MemoryMap.KERNEL_HEAP_END.value();
            boolean inUserSpace = bufferPtr >= MemoryMap.USER_START.value()
                && bufferEnd <= MemoryMap.USER_END.value();

            if (!inKernelHeap && !inUserSpace) {
                throw new VirtualHardwareException(ErrorCode.MEMORY_OUT_OF_BOUNDS.code());
            }

            RandomAccessFile diskFile = requireOpen();

            for (int sector = 0; sector < sectorCount; sector++) {
                long sectorOffset = (long) (lba + sector) * SECTOR_SIZE;
                int memoryOffset = bufferPtr + (sector * SECTOR_SIZE);

                diskFile.seek(sectorOffset);

                for (int byteIndex = 0; byteIndex < SECTOR_SIZE; byteIndex++) {
                    int value = diskFile.read();
                    memory.writeByte(memoryOffset + byteIndex, value < 0 ? 0 : value);
                }
            }

        }
        catch (IOException e) {
            throw new UnclesPCException(ErrorCode.DISK_IMAGE_ERROR, "unable to read disk image");
        }
        catch (VirtualHardwareException e) {
            memory.writeWord(DiskRegisters.ERROR.address, e.getErrorCode());
        }
    }   

    private void write() {
        try {
            int sectorCount = memory.readWord(DiskRegisters.SECTOR_COUNT.address);
            int lba = memory.readWord(DiskRegisters.LBA.address);
            int bufferPtr = memory.readWord(DiskRegisters.BUFFER_PTR.address);

            if (sectorCount > MAX_SECTOR_READ) {
                throw new VirtualHardwareException(ErrorCode.SECTOR_COUNT_ERROR.code());
            }

            if (lba < 0 || (long) lba + sectorCount > NUM_SECTORS) {
                throw new VirtualHardwareException(ErrorCode.SECTOR_COUNT_ERROR.code());
            }

            int bytesToWrite = sectorCount * SECTOR_SIZE;
            long bufferEnd = (long) bufferPtr + bytesToWrite - 1;
            boolean inKernelHeap = bufferPtr >= MemoryMap.KERNEL_HEAP_START.value()
                && bufferEnd <= MemoryMap.KERNEL_HEAP_END.value();
            boolean inUserSpace = bufferPtr >= MemoryMap.USER_START.value()
                && bufferEnd <= MemoryMap.USER_END.value();

            if (!inKernelHeap && !inUserSpace) {
                throw new VirtualHardwareException(ErrorCode.MEMORY_OUT_OF_BOUNDS.code());
            }

            RandomAccessFile diskFile = requireOpen();

            for (int sector = 0; sector < sectorCount; sector++) {
                long sectorOffset = (long) (lba + sector) * SECTOR_SIZE;
                int memoryOffset = bufferPtr + (sector * SECTOR_SIZE);

                diskFile.seek(sectorOffset);

                for (int byteIndex = 0; byteIndex < SECTOR_SIZE; byteIndex++) {
                    diskFile.write(memory.readByte(memoryOffset + byteIndex));
                }
            }
        }
        catch (IOException e) {
            throw new UnclesPCException(ErrorCode.DISK_IMAGE_ERROR, "unable to write disk image");
        }
        catch (VirtualHardwareException e) {
            memory.writeWord(DiskRegisters.ERROR.address, e.getErrorCode());
        }
    }

    private void format() {
        try {
            int sectorCount = memory.readWord(DiskRegisters.SECTOR_COUNT.address);
            int lba = memory.readWord(DiskRegisters.LBA.address);

            if (sectorCount > MAX_SECTOR_READ) {
                throw new VirtualHardwareException(ErrorCode.SECTOR_COUNT_ERROR.code());
            }

            if (lba < 0 || (long) lba + sectorCount > NUM_SECTORS) {
                throw new VirtualHardwareException(ErrorCode.SECTOR_COUNT_ERROR.code());
            }

            RandomAccessFile diskFile = requireOpen();
            byte[] emptySector = new byte[SECTOR_SIZE];

            for (int sector = 0; sector < sectorCount; sector++) {
                long sectorOffset = (long) (lba + sector) * SECTOR_SIZE;
                diskFile.seek(sectorOffset);
                diskFile.write(emptySector);
            }
        }
        catch (IOException e) {
            throw new UnclesPCException(ErrorCode.DISK_IMAGE_ERROR, "unable to format disk image");
        }
        catch (VirtualHardwareException e) {
            memory.writeWord(DiskRegisters.ERROR.address, e.getErrorCode());
        }
    }
}
