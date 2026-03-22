;Stage 2 is 512 bytes long cause we only read one sector into the address

org 0x1000
bits 16
jmp __start

__start:
    xor ax, ax
    mov ds, ax
    mov bx, success ; DS:BX
    .print_loop:
        mov al, [bx]
        cmp al, 0
        je exit

        mov ah, 0x0E
        int 0x10

        inc bx
        jmp .print_loop
exit:
    jmp $
success: db 'Nova Success', 0