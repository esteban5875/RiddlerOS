package UnclesPC.virtual_hardware.io.disk;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Objects;

import UnclesPC.app_level.exception.UnclesPCException;
import UnclesPC.virtual_hardware.io.exception.VirtualHardwareException;
import UnclesPC.virtual_hardware.motherboard.error.ErrorCode;
import UnclesPC.virtual_hardware.ram.Memory;
import UnclesPC.virtual_hardware.ram.modules.MemoryMap;

public final class Disk {
    public static final int SECTOR_SIZE = 512;
    public static final long DISK_SIZE = 5L * 1024L * 1024L * 1024L;
    public static final long NUM_SECTORS = DISK_SIZE / SECTOR_SIZE;

    private static final int MAX_SECTOR_TRANSFER = 128;
    private static final int STATUS_BUSY = 0x01;
    private static final int STATUS_IDLE = 0x00;
    private static final long TIMEOUT_MILLIS = 5000L;

    private final RandomAccessFile image;
    private final Memory memory;

    public Disk(Memory memory, RandomAccessFile image) {
        this.memory = Objects.requireNonNull(memory, "memory");
        this.image = Objects.requireNonNull(image, "image");
    }

    public long size() {
        return DISK_SIZE;
    }

    public int sectorSize() {
        return SECTOR_SIZE;
    }

    public long numSectors() {
        return NUM_SECTORS;
    }

    public void executeCMD() {
        if (readRegister(DiskMap.COMMAND) == 0x00) {
            return;
        }

        long startTime = System.currentTimeMillis();

        while (readRegister(DiskMap.STATUS) == STATUS_BUSY) {
            if (System.currentTimeMillis() - startTime > TIMEOUT_MILLIS) {
                writeRegister(DiskMap.ERROR, ErrorCode.DISK_TIMEOUT.code());
                return;
            }
            Thread.onSpinWait();
        }

        writeRegister(DiskMap.STATUS, STATUS_BUSY);

        try {
            writeRegister(DiskMap.ERROR, ErrorCode.SUCCESS.code());

            int command = readRegister(DiskMap.COMMAND);
            switch (command) {
                case 0x01 -> this.read();
                case 0x02 -> this.write();
                case 0x03 -> this.format();
                default -> throw new VirtualHardwareException(
                    ErrorCode.INVALID_DISK_COMMAND,
                    "invalid disk command: " + command
                );
            }
        } catch (VirtualHardwareException e) {
            writeRegister(DiskMap.ERROR, e.getErrorCode());
        } finally {
            writeRegister(DiskMap.STATUS, STATUS_IDLE);
        }
    }

    private void read() throws VirtualHardwareException {
        try {
            int sectorCount = readSectorCount();
            int lba = readLba(sectorCount);
            int bufferPtr = readBufferPtr(sectorCount);

            for (int sector = 0; sector < sectorCount; sector++) {
                long sectorOffset = (long) (lba + sector) * SECTOR_SIZE;
                int memoryOffset = bufferPtr + (sector * SECTOR_SIZE);

                image.seek(sectorOffset);

                for (int byteIndex = 0; byteIndex < SECTOR_SIZE; byteIndex++) {
                    int value = image.read();
                    memory.writeByte(memoryOffset + byteIndex, value < 0 ? 0 : value);
                }
            }
        }
        catch (IOException e) {
            throw new UnclesPCException(
                ErrorCode.DISK_IMAGE_ERROR,
                "unable to read disk image",
                e
            );
        }
    }   

    private void write() throws VirtualHardwareException {
        try {
            int sectorCount = readSectorCount();
            int lba = readLba(sectorCount);
            int bufferPtr = readBufferPtr(sectorCount);

            for (int sector = 0; sector < sectorCount; sector++) {
                long sectorOffset = (long) (lba + sector) * SECTOR_SIZE;
                int memoryOffset = bufferPtr + (sector * SECTOR_SIZE);

                image.seek(sectorOffset);

                for (int byteIndex = 0; byteIndex < SECTOR_SIZE; byteIndex++) {
                    image.write(memory.readByte(memoryOffset + byteIndex));
                }
            }
        }
        catch (IOException e) {
            throw new UnclesPCException(
                ErrorCode.DISK_IMAGE_ERROR,
                "unable to write disk image",
                e
            );
        }
    }

    private void format() throws VirtualHardwareException {
        try {
            int sectorCount = readSectorCount();
            int lba = readLba(sectorCount);
            byte[] emptySector = new byte[SECTOR_SIZE];

            for (int sector = 0; sector < sectorCount; sector++) {
                long sectorOffset = (long) (lba + sector) * SECTOR_SIZE;
                image.seek(sectorOffset);
                image.write(emptySector);
            }
        }
        catch (IOException e) {
            throw new UnclesPCException(
                ErrorCode.DISK_IMAGE_ERROR,
                "unable to format disk image",
                e
            );
        }
    }

    private int readSectorCount() throws VirtualHardwareException {
        int sectorCount = memory.readWord(DiskMap.SECTOR_COUNT.address);
        if (sectorCount <= 0 || sectorCount > MAX_SECTOR_TRANSFER) {
            throw new VirtualHardwareException(
                ErrorCode.SECTOR_COUNT_ERROR,
                "invalid sector count: " + sectorCount
            );
        }
        return sectorCount;
    }

    private int readLba(int sectorCount) throws VirtualHardwareException {
        int lba = memory.readWord(DiskMap.LBA.address);
        if (lba < 0 || (long) lba + sectorCount > NUM_SECTORS) {
            throw new VirtualHardwareException(
                ErrorCode.SECTOR_COUNT_ERROR,
                "invalid LBA range: " + lba + " + " + sectorCount
            );
        }
        return lba;
    }

    private int readBufferPtr(int sectorCount) throws VirtualHardwareException {
        int bufferPtr = memory.readWord(DiskMap.BUFFER_PTR.address);
        int bytesToTransfer = sectorCount * SECTOR_SIZE;
        long bufferEnd = (long) bufferPtr + bytesToTransfer - 1;

        boolean inProgramSpace = bufferPtr >= MemoryMap.PROGRAM_START.value()
            && bufferEnd <= MemoryMap.PROGRAM_END.value();

        if (!inProgramSpace) {
            throw new VirtualHardwareException(
                ErrorCode.MEMORY_OUT_OF_BOUNDS,
                "disk buffer is outside program space: " + bufferPtr
            );
        }

        return bufferPtr;
    }

    private int readRegister(DiskMap register) {
        try {
            return memory.readWord(register.address);
        } catch (VirtualHardwareException e) {
            throw new UnclesPCException(
                ErrorCode.MEMORY_OUT_OF_BOUNDS,
                "disk register address is invalid: " + register,
                e
            );
        }
    }

    private void writeRegister(DiskMap register, int value) {
        try {
            memory.writeWord(register.address, value);
        } catch (VirtualHardwareException e) {
            throw new UnclesPCException(
                ErrorCode.MEMORY_OUT_OF_BOUNDS,
                "disk register address is invalid: " + register,
                e
            );
        }
    }
}
