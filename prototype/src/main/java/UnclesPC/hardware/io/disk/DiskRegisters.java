package UnclesPC.hardware.io.disk;

enum DiskRegisters {

    COMMAND(0x007F1000),
    STATUS(0x007F1004),
    ERROR(0x007F1008),
    SECTOR_COUNT(0x007F100C),

    LBA(0x007F1010),
    BUFFER_PTR(0x007F1018);

    public final int address;

    DiskRegisters(int address) {
        this.address = address;
    }
}