; Multi-Stage bootloader
; Read sector from disk
; Load 0x0000:0x1000 and Jump <- Stage two bootloader (Address 0x1000)

;0x0000:0x7c00

org 0x7c00
bits 16 ;Real Mode
jmp __start

__start:
    mov [Disk], dl ;Save disk number passed by bios in dl to our variable
    mov bx, WelcomeMessage
    xor ax, ax
    mov ds, ax
    .print_loop:
        mov al, [bx]
        cmp al, 0
        je end

        mov ah, 0x0E
        int 0x10
        inc bx
        jmp .print_loop
end:
    ;Load and jmp to sector
    mov dl, [Disk] ;Load disk number to dl
    mov ax, 0x0000
    mov es, ax ;Can only move general purpose registers to segment registers
    mov ch, 0x0000
    mov dh, 0x0000
    mov bx, 0x1000
    mov al, 1 ;Read one sector
    mov ah, 0x02
    mov cl, 2 ;Read sector 2
    
    int 0x13 ;Call bios
    jc diskError ;Error handle

    jmp 0x0000:0x1000 ;Jump to address where sector was read
diskError:
    mov bx, ErrorMessage
    .printError:
        mov al, [bx]
        cmp al, 0
        je exit

        mov ah, 0x0E
        int 0x10
        inc bx
        jmp .printError
exit:
    jmp $
WelcomeMessage: db 'Welcome To Nova', 0
ErrorMessage: db 'Disk Error', 0
Disk: db 0 ;Disk number to read from, 0 for floppy, 0x80 for first hard disk


times 510 - ($-$$) db 0 ; Fill remaining size untel 510 with 0s
db 0x55, 0xAA ; Fill remaining two with boot signature
; These 512 bytes are at 0x7C00