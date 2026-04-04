package UnclesPC.virtual_hardware.io.disk;

public enum DiskMap {

    COMMAND(0x00701000),
    STATUS(0x00701004),
    ERROR(0x00701008),
    SECTOR_COUNT(0x0070100C),

    LBA(0x00701010),
    BUFFER_PTR(0x00701018);

    public final int address;

    DiskMap(int address) {
        this.address = address & 0xFFFFFFFF; // Ensure address is treated as 32-bit unsigned
    }
}
