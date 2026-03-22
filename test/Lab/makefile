ASM        := nasm
ASMFLAGS   := -f bin -Wall

BUILD_DIR  := build

BOOT_SRC   := boot.asm
STAGE2_SRC := stagetwo.asm

BOOT_BIN   := $(BUILD_DIR)/boot.bin
STAGE2_BIN := $(BUILD_DIR)/stagetwo.bin
IMAGE      := $(BUILD_DIR)/os.img

QEMU       := qemu-system-i386

.PHONY: all clean run

# Default target
all: $(IMAGE)

# -----------------------------
# Build stage 1 (bootloader)
# -----------------------------
$(BOOT_BIN): $(BOOT_SRC) | $(BUILD_DIR)
	$(ASM) $(ASMFLAGS) $< -o $@

# -----------------------------
# Build stage 2
# -----------------------------
$(STAGE2_BIN): $(STAGE2_SRC) | $(BUILD_DIR)
	$(ASM) $(ASMFLAGS) $< -o $@

# -----------------------------
# Create disk image
# -----------------------------
$(IMAGE): $(BOOT_BIN) $(STAGE2_BIN)
	cat $(BOOT_BIN) $(STAGE2_BIN) > $(IMAGE)

# -----------------------------
# Ensure build directory exists
# -----------------------------
$(BUILD_DIR):
	mkdir -p $(BUILD_DIR)

# -----------------------------
# Run in QEMU
# -----------------------------
run: $(IMAGE)
	$(QEMU) -drive format=raw,file=$(IMAGE)

# -----------------------------
# Clean
# -----------------------------
clean:
	rm -rf $(BUILD_DIR)